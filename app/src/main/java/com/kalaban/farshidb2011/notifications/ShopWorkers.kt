package com.kalaban.farshidb2011.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.kalaban.farshidb2011.KalabanApp
import com.kalaban.farshidb2011.MainActivity
import com.kalaban.farshidb2011.R
import java.util.Calendar
import java.util.concurrent.TimeUnit

object ShopWorkers {
    const val ALERTS_CHANNEL_ID = "kalaban_alerts"
    private const val HEALTH_WORK = "shop_health_daily"
    private const val PRICE_BATCH_PREFIX = "price_batch_"

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(ALERTS_CHANNEL_ID, context.getString(R.string.alerts_channel), NotificationManager.IMPORTANCE_DEFAULT),
        )
    }

    /** Daily check around 09:00; KEEP so app restarts do not push the next run further out. */
    fun scheduleHealthCheck(context: Context) {
        val now = Calendar.getInstance()
        val next = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        val request = PeriodicWorkRequestBuilder<ShopHealthWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(next.timeInMillis - now.timeInMillis, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(HEALTH_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun schedulePriceBatch(context: Context, batchId: Long, atEpochMs: Long) {
        val delay = (atEpochMs - System.currentTimeMillis()).coerceAtLeast(0L)
        val request = OneTimeWorkRequestBuilder<ScheduledPriceWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf("batchId" to batchId))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork("$PRICE_BATCH_PREFIX$batchId", ExistingWorkPolicy.REPLACE, request)
    }

    fun cancelPriceBatch(context: Context, batchId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork("$PRICE_BATCH_PREFIX$batchId")
    }

    suspend fun rescheduleAll(context: Context) {
        scheduleHealthCheck(context)
        KalabanApp.instance.pricing.scheduledBatches().forEach { batch ->
            schedulePriceBatch(context, batch.id, batch.scheduledAtEpochMs ?: System.currentTimeMillis())
        }
    }

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun notify(context: Context, id: Int, title: String, text: String) {
        if (!canNotify(context)) return
        ensureChannel(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, ALERTS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_kalaban)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }
}

/** One summary notification per day, limited to the alert types enabled in notification settings. */
class ShopHealthWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = try {
        val app = KalabanApp.instance
        val cfg = app.config.current()
        val snapshot = app.reports.dashboard()
        val ctx = applicationContext
        val lines = buildList {
            if (cfg.notifyLowStock && snapshot.lowStock > 0) add(ctx.getString(R.string.alert_low_stock, snapshot.lowStock))
            if (cfg.notifyNegativeProfit && snapshot.negativeProfit > 0) add(ctx.getString(R.string.alert_negative_profit, snapshot.negativeProfit))
            if (cfg.notifyLowMargin && snapshot.lowMargin > 0) add(ctx.getString(R.string.alert_low_margin, snapshot.lowMargin))
            if (snapshot.backupDue) add(ctx.getString(R.string.alert_backup_due))
        }
        if (lines.isNotEmpty()) {
            ShopWorkers.notify(ctx, 7001, ctx.getString(R.string.alert_title), lines.joinToString("\n"))
        }
        Result.success()
    } catch (_: Exception) {
        Result.retry()
    }
}

class ScheduledPriceWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val batchId = inputData.getLong("batchId", -1L)
        if (batchId < 0) return Result.failure()
        return try {
            val app = KalabanApp.instance
            val result = app.pricing.runScheduled(batchId)
            if (result != null && app.config.current().notifyScheduledPrices) {
                val ctx = applicationContext
                ShopWorkers.notify(
                    ctx,
                    (8000 + batchId % 1000).toInt(),
                    ctx.getString(R.string.alert_scheduled_title),
                    ctx.getString(R.string.alert_scheduled_text, result.applied, result.conflicts.size),
                )
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}

package com.kalaban.farshidb2011.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.kalaban.farshidb2011.KalabanApp
import com.kalaban.farshidb2011.MainActivity
import com.kalaban.farshidb2011.R
import com.kalaban.farshidb2011.data.local.entity.SaleEntity
import java.util.concurrent.TimeUnit

object ReminderNotifications {
    const val CHANNEL_ID = "kalaban_credit_reminders"
    private const val WORK_PREFIX = "credit_reminder_"

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminders),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        manager.createNotificationChannel(channel)
    }

    fun scheduleForSale(context: Context, sale: SaleEntity) {
        val due = sale.dueAtEpochMs ?: return
        if (sale.paymentType.name != "CREDIT" || sale.openBalance <= 0.0 || sale.manuallyZeroed) {
            cancelForSale(context, sale.id)
            return
        }
        ensureChannel(context)
        val now = System.currentTimeMillis()
        val dayMs = TimeUnit.DAYS.toMillis(1)
        scheduleOne(context, sale, due - dayMs, now, kind = "before")
        scheduleOne(context, sale, due, now, kind = "due")
    }

    private fun scheduleOne(
        context: Context,
        sale: SaleEntity,
        triggerAt: Long,
        now: Long,
        kind: String,
    ) {
        val delay = (triggerAt - now).coerceAtLeast(0L)
        // Skip if more than a few seconds in the past beyond grace.
        if (triggerAt < now - TimeUnit.HOURS.toMillis(12)) return
        val unique = "${WORK_PREFIX}${sale.id}_$kind"
        val request = OneTimeWorkRequestBuilder<CreditReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    "saleId" to sale.id,
                    "buyer" to sale.buyerName,
                    "balance" to sale.openBalance,
                    "kind" to kind,
                ),
            )
            .addTag(unique)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            unique,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun cancelForSale(context: Context, saleId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork("${WORK_PREFIX}${saleId}_before")
        WorkManager.getInstance(context).cancelUniqueWork("${WORK_PREFIX}${saleId}_due")
    }

    suspend fun rescheduleAll(context: Context) {
        ensureChannel(context)
        val repo = KalabanApp.instance.repository
        repo.openCreditsSnapshot().forEach { scheduleForSale(context, it) }
    }

    fun show(context: Context, saleId: Long, buyer: String, balance: Double, kind: String) {
        ensureChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val title = if (kind == "before") {
            context.getString(R.string.reminder_day_before)
        } else {
            context.getString(R.string.reminder_due_today)
        }
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            saleId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText("$buyer — ${context.getString(R.string.open_balance)}: $balance")
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify((saleId * 10 + if (kind == "due") 1 else 0).toInt(), notification)
    }
}

class CreditReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val saleId = inputData.getLong("saleId", -1L)
        val buyer = inputData.getString("buyer").orEmpty()
        val balance = inputData.getDouble("balance", 0.0)
        val kind = inputData.getString("kind") ?: "due"
        if (saleId < 0) return Result.failure()
        ReminderNotifications.show(applicationContext, saleId, buyer, balance, kind)
        return Result.success()
    }
}

class BootReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        Thread {
            try {
                // Database may need Application — start work that loads sales.
                WorkManager.getInstance(context).enqueue(
                    OneTimeWorkRequestBuilder<RescheduleRemindersWorker>().build(),
                )
            } finally {
                pending.finish()
            }
        }.start()
    }
}

class RescheduleRemindersWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        return try {
            ReminderNotifications.rescheduleAll(applicationContext)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}

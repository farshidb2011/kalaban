package com.kalaban.farshidb2011

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.kalaban.farshidb2011.data.local.KalabanDatabase
import com.kalaban.farshidb2011.data.local.seedInBackground
import com.kalaban.farshidb2011.data.prefs.SecurePrefs
import com.kalaban.farshidb2011.data.repository.AuditRepository
import com.kalaban.farshidb2011.data.repository.ConfigRepository
import com.kalaban.farshidb2011.data.repository.ImportExportRepository
import com.kalaban.farshidb2011.data.repository.PricingRepository
import com.kalaban.farshidb2011.data.repository.ProductRepository
import com.kalaban.farshidb2011.data.repository.ReportRepository
import com.kalaban.farshidb2011.data.repository.ShopRepository
import com.kalaban.farshidb2011.data.repository.UserRepository
import com.kalaban.farshidb2011.domain.SessionManager
import com.kalaban.farshidb2011.notifications.ReminderNotifications
import com.kalaban.farshidb2011.notifications.RescheduleRemindersWorker
import com.kalaban.farshidb2011.notifications.ShopWorkers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class KalabanApp : Application() {
    /** Process-wide scope for hot flows and fire-and-forget startup work. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var database: KalabanDatabase
        private set
    lateinit var repository: ShopRepository
        private set
    lateinit var securePrefs: SecurePrefs
        private set
    lateinit var session: SessionManager
        private set
    lateinit var config: ConfigRepository
        private set
    lateinit var audit: AuditRepository
        private set
    lateinit var pricing: PricingRepository
        private set
    lateinit var products: ProductRepository
        private set
    lateinit var users: UserRepository
        private set
    lateinit var reports: ReportRepository
        private set
    lateinit var importExport: ImportExportRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = KalabanDatabase.get(this)
        database.seedInBackground()
        repository = ShopRepository(database, this)
        securePrefs = SecurePrefs(this)
        session = SessionManager()
        config = ConfigRepository(database, appScope)
        audit = AuditRepository(database)
        pricing = PricingRepository(database, config, audit)
        products = ProductRepository(database, pricing, audit)
        users = UserRepository(database, securePrefs, session, audit)
        reports = ReportRepository(database, pricing, config)
        importExport = ImportExportRepository(database, this, pricing, config, audit)
        ReminderNotifications.ensureChannel(this)
        ShopWorkers.ensureChannel(this)
        WorkManager.getInstance(this).enqueueUniqueWork(
            "reschedule_credit_reminders",
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<RescheduleRemindersWorker>().build(),
        )
        // Apply persisted language (default fa). Do not force "fa" first — that overwrote
        // the user's saved language on every cold start until the async load finished.
        appScope.launch {
            val lang = database.settingsDao().get()?.languageTag?.ifBlank { "fa" } ?: "fa"
            applyLanguage(lang)
        }
    }

    fun applyLanguage(tag: String) {
        val normalized = tag.ifBlank { "fa" }
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(normalized))
    }

    companion object {
        lateinit var instance: KalabanApp
            private set
    }
}

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
import com.kalaban.farshidb2011.data.repository.ShopRepository
import com.kalaban.farshidb2011.notifications.ReminderNotifications
import com.kalaban.farshidb2011.notifications.RescheduleRemindersWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class KalabanApp : Application() {
    lateinit var database: KalabanDatabase
        private set
    lateinit var repository: ShopRepository
        private set
    lateinit var securePrefs: SecurePrefs
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = KalabanDatabase.get(this)
        database.seedInBackground()
        repository = ShopRepository(database, this)
        securePrefs = SecurePrefs(this)
        ReminderNotifications.ensureChannel(this)
        WorkManager.getInstance(this).enqueueUniqueWork(
            "reschedule_credit_reminders",
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<RescheduleRemindersWorker>().build(),
        )
        // Apply persisted language (default fa). Do not force "fa" first — that overwrote
        // the user's saved language on every cold start until the async load finished.
        CoroutineScope(Dispatchers.IO).launch {
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

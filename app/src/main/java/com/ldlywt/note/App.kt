package com.ldlywt.note

import android.app.Application
import androidx.lifecycle.asLiveData
import com.ldlywt.note.backup.BackupScheduler
import com.ldlywt.note.utils.SharedPreferencesUtils
import dagger.hilt.android.HiltAndroidApp

fun getAppName(): String {
    return "IdeaMemo"
}


@HiltAndroidApp
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        val localAutoBackup=SharedPreferencesUtils.localAutoBackup.asLiveData().value
        if (localAutoBackup == true) {
            BackupScheduler.scheduleDailyBackup(this)
        } else {
            BackupScheduler.cancelDailyBackup(this)
        }
    }

    companion object {
        lateinit var instance: App
            private set
    }
}
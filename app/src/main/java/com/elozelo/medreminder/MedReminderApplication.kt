package com.elozelo.medreminder

import android.app.Application
import com.elozelo.medreminder.utils.NotificationHelper

class MedReminderApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Utwórz kanał powiadomień przy starcie aplikacji
        NotificationHelper.createNotificationChannel(this)
    }
}


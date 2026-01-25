package com.elozelo.medreminder.utils

import android.content.Context
import android.content.res.Configuration
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

class AppointmentReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val appointmentId = inputData.getString("appointmentId") ?: return Result.failure()
        val appointmentName = inputData.getString("appointmentName") ?: return Result.failure()
        val appointmentDateTime = inputData.getLong("appointmentDateTime", 0L)
        val daysUntil = inputData.getInt("daysUntil", 0)

        if (appointmentDateTime == 0L) {
            return Result.failure()
        }


        val localizedContext = getLocalizedContext()

        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(appointmentDateTime))

        NotificationHelper.showAppointmentNotification(
            context = localizedContext,
            appointmentId = appointmentId,
            appointmentName = appointmentName,
            appointmentDate = formattedDate,
            daysUntil = daysUntil
        )

        return Result.success()
    }

    private suspend fun getLocalizedContext(): Context {
        val languageCode = try {
            applicationContext.languageDataStore.data.first()[LanguagePreferences.LANGUAGE_KEY]
                ?: LanguagePreferences.POLISH
        } catch (e: Exception) {
            LanguagePreferences.POLISH
        }

        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(applicationContext.resources.configuration)
        config.setLocale(locale)

        return applicationContext.createConfigurationContext(config)
    }
}


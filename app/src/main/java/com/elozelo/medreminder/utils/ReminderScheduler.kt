package com.elozelo.medreminder.utils

import android.content.Context
import androidx.work.*
import com.elozelo.medreminder.data.model.Appointment
import java.util.concurrent.TimeUnit

object ReminderScheduler {

    fun scheduleAppointmentReminders(context: Context, appointment: Appointment) {
        if (!appointment.reminderEnabled) {
            cancelAppointmentReminders(context, appointment.id)
            return
        }

        val currentTime = System.currentTimeMillis()
        val appointmentTime = appointment.dateTime

        // Przypomnienie 3 dni przed
        scheduleReminder(
            context = context,
            appointment = appointment,
            daysBeforeAppointment = 3,
            workTag = "${appointment.id}_3days"
        )

        // Przypomnienie 1 dzień przed
        scheduleReminder(
            context = context,
            appointment = appointment,
            daysBeforeAppointment = 1,
            workTag = "${appointment.id}_1day"
        )

        // Przypomnienie w dniu wizyty (1 godzina przed)
        val oneHourBefore = appointmentTime - TimeUnit.HOURS.toMillis(1)
        if (oneHourBefore > currentTime) {
            val delay = oneHourBefore - currentTime
            scheduleReminderWithDelay(
                context = context,
                appointment = appointment,
                delayMillis = delay,
                daysUntil = 0,
                workTag = "${appointment.id}_today"
            )
        }
    }

    private fun scheduleReminder(
        context: Context,
        appointment: Appointment,
        daysBeforeAppointment: Int,
        workTag: String
    ) {
        val reminderTime = appointment.dateTime - TimeUnit.DAYS.toMillis(daysBeforeAppointment.toLong())
        val currentTime = System.currentTimeMillis()

        // Tylko zaplanuj, jeśli czas przypomnienia jest w przyszłości
        if (reminderTime > currentTime) {
            val delay = reminderTime - currentTime
            scheduleReminderWithDelay(
                context = context,
                appointment = appointment,
                delayMillis = delay,
                daysUntil = daysBeforeAppointment,
                workTag = workTag
            )
        }
    }

    private fun scheduleReminderWithDelay(
        context: Context,
        appointment: Appointment,
        delayMillis: Long,
        daysUntil: Int,
        workTag: String
    ) {
        val data = workDataOf(
            "appointmentId" to appointment.id,
            "appointmentName" to appointment.name,
            "appointmentDateTime" to appointment.dateTime,
            "daysUntil" to daysUntil
        )

        val reminderWork = OneTimeWorkRequestBuilder<AppointmentReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag(workTag)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            workTag,
            ExistingWorkPolicy.REPLACE,
            reminderWork
        )
    }

    fun cancelAppointmentReminders(context: Context, appointmentId: String) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelAllWorkByTag("${appointmentId}_3days")
        workManager.cancelAllWorkByTag("${appointmentId}_1day")
        workManager.cancelAllWorkByTag("${appointmentId}_today")
    }
}


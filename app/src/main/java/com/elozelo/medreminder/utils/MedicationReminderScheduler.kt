package com.elozelo.medreminder.utils

import android.content.Context
import androidx.work.*
import com.elozelo.medreminder.data.model.Medication
import com.elozelo.medreminder.data.model.frequencyUnit
import java.util.*
import java.util.concurrent.TimeUnit

object MedicationReminderScheduler {

    fun scheduleMedicationReminders(context: Context, medication: Medication) {
        // Nie planuj przypomnień jeśli lek jest pusty, przypomnienia wyłączone lub brak godzin
        if (medication.remainingQuantity == 0 || !medication.reminderEnabled || medication.reminderTimes.isEmpty()) {
            cancelMedicationReminders(context, medication.id)
            return
        }


        cancelMedicationReminders(context, medication.id)

        medication.reminderTimes.forEachIndexed { index, timeString ->
            scheduleForTime(
                context = context,
                medication = medication,
                timeString = timeString,
                index = index
            )
        }
    }

    private fun scheduleForTime(
        context: Context,
        medication: Medication,
        timeString: String,
        index: Int
    ) {
        try {
            val timeParts = timeString.split(":")
            if (timeParts.size != 2) return

            val hour = timeParts[0].toIntOrNull() ?: return
            val minute = timeParts[1].toIntOrNull() ?: return

            val now = Calendar.getInstance()
            val reminderTime = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                if (before(now)) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }

            val delay = reminderTime.timeInMillis - now.timeInMillis

            when (medication.frequency) {
                frequencyUnit.DAILY -> scheduleDailyReminder(context, medication, delay, timeString, index)
                frequencyUnit.WEEKLY -> scheduleWeeklyReminder(context, medication, delay, timeString, index)
                frequencyUnit.MONTHLY -> scheduleMonthlyReminders(context, medication, reminderTime, timeString, index)
                frequencyUnit.YEARLY -> scheduleYearlyReminder(context, medication, reminderTime, timeString, index)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun scheduleDailyReminder(
        context: Context,
        medication: Medication,
        delay: Long,
        timeString: String,
        index: Int
    ) {
        for (dayOffset in 0..30) {
            val adjustedDelay = delay + TimeUnit.DAYS.toMillis(dayOffset.toLong())
            scheduleOneTimeReminder(
                context = context,
                medication = medication,
                delay = adjustedDelay,
                timeString = timeString,
                workTag = "${medication.id}_time_${index}_day_$dayOffset"
            )
        }
    }

    private fun scheduleWeeklyReminder(
        context: Context,
        medication: Medication,
        delay: Long,
        timeString: String,
        index: Int
    ) {
        // Zaplanuj na ten tydzień i kolejne 12 tygodni
        for (weekOffset in 0..12) {
            val adjustedDelay = delay + TimeUnit.DAYS.toMillis(7L * weekOffset)
            scheduleOneTimeReminder(
                context = context,
                medication = medication,
                delay = adjustedDelay,
                timeString = timeString,
                workTag = "${medication.id}_time_${index}_week_$weekOffset"
            )
        }
    }

    private fun scheduleYearlyReminder(
        context: Context,
        medication: Medication,
        startTime: Calendar,
        timeString: String,
        index: Int
    ) {
        // Zaplanuj na ten rok i kolejne 2 lata
        for (yearOffset in 0..2) {
            val nextReminderTime = startTime.clone() as Calendar
            nextReminderTime.add(Calendar.YEAR, yearOffset)

            val now = Calendar.getInstance()
            if (nextReminderTime.after(now)) {
                val delay = nextReminderTime.timeInMillis - now.timeInMillis
                scheduleOneTimeReminder(
                    context = context,
                    medication = medication,
                    delay = delay,
                    timeString = timeString,
                    workTag = "${medication.id}_time_${index}_year_$yearOffset"
                )
            }
        }
    }

    private fun scheduleOneTimeReminder(
        context: Context,
        medication: Medication,
        delay: Long,
        timeString: String,
        workTag: String
    ) {
        val data = workDataOf(
            "medicationId" to medication.id,
            "medicationName" to medication.name,
            "dosage" to medication.dosage.name, // Używamy enum.name zamiast displayName
            "quantity" to medication.quantity,
            "timeString" to timeString
        )

        val workRequest = OneTimeWorkRequestBuilder<MedicationReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag(workTag)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            workTag,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    private fun scheduleMonthlyReminders(
        context: Context,
        medication: Medication,
        startTime: Calendar,
        timeString: String,
        index: Int
    ) {
        for (monthOffset in 1..12) {
            val nextReminderTime = startTime.clone() as Calendar
            nextReminderTime.add(Calendar.MONTH, monthOffset)

            val now = Calendar.getInstance()
            if (nextReminderTime.after(now)) {
                val delay = nextReminderTime.timeInMillis - now.timeInMillis
                val workTag = "${medication.id}_time_${index}_month_$monthOffset"

                val data = workDataOf(
                    "medicationId" to medication.id,
                    "medicationName" to medication.name,
                    "dosage" to medication.dosage.name, // Używamy enum.name zamiast displayName
                    "quantity" to medication.quantity,
                    "timeString" to timeString
                )

                val workRequest = OneTimeWorkRequestBuilder<MedicationReminderWorker>()
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .setInputData(data)
                    .addTag(workTag)
                    .build()

                WorkManager.getInstance(context).enqueueUniqueWork(
                    workTag,
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )
            }
        }
    }

    fun cancelMedicationReminders(context: Context, medicationId: String) {
        val workManager = WorkManager.getInstance(context)

        // Anuluj wszystkie przypomnienia dla tego leku
        for (i in 0..10) { // Maksymalnie 10 godzin dziennie
            // Daily
            for (dayOffset in 0..30) {
                workManager.cancelAllWorkByTag("${medicationId}_time_${i}_day_$dayOffset")
            }

            // Weekly
            for (weekOffset in 0..12) {
                workManager.cancelAllWorkByTag("${medicationId}_time_${i}_week_$weekOffset")
            }

            // Monthly
            for (monthOffset in 1..12) {
                workManager.cancelAllWorkByTag("${medicationId}_time_${i}_month_$monthOffset")
            }

            // Yearly
            for (yearOffset in 0..2) {
                workManager.cancelAllWorkByTag("${medicationId}_time_${i}_year_$yearOffset")
            }
        }
    }
}


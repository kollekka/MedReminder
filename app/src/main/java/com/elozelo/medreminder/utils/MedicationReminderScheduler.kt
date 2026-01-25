package com.elozelo.medreminder.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.elozelo.medreminder.data.model.Medication
import com.elozelo.medreminder.data.model.frequencyUnit
import java.util.*

object MedicationReminderScheduler {

    fun scheduleMedicationReminders(context: Context, medication: Medication) {
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

            when (medication.frequency) {
                frequencyUnit.DAILY -> scheduleDailyReminder(context, medication, reminderTime, timeString, index)
                frequencyUnit.WEEKLY -> scheduleWeeklyReminder(context, medication, reminderTime, timeString, index)
                frequencyUnit.MONTHLY -> scheduleMonthlyReminder(context, medication, reminderTime, timeString, index)
                frequencyUnit.SPECIFIC_DAYS -> scheduleSpecificDaysReminder(context, medication, reminderTime, timeString, index)
                frequencyUnit.EVERY_X_DAYS -> scheduleEveryXDaysReminder(context, medication, reminderTime, timeString, index)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun scheduleDailyReminder(
        context: Context,
        medication: Medication,
        startTime: Calendar,
        timeString: String,
        index: Int
    ) {
        scheduleExactAlarm(
            context = context,
            medication = medication,
            triggerTimeMillis = startTime.timeInMillis,
            timeString = timeString,
            requestCode = generateRequestCode(medication.id, index, 0)
        )
    }

    private fun scheduleWeeklyReminder(
        context: Context,
        medication: Medication,
        startTime: Calendar,
        timeString: String,
        index: Int
    ) {
        scheduleExactAlarm(
            context = context,
            medication = medication,
            triggerTimeMillis = startTime.timeInMillis,
            timeString = timeString,
            requestCode = generateRequestCode(medication.id, index, 1)
        )
    }

    private fun scheduleMonthlyReminder(
        context: Context,
        medication: Medication,
        startTime: Calendar,
        timeString: String,
        index: Int
    ) {
        scheduleExactAlarm(
            context = context,
            medication = medication,
            triggerTimeMillis = startTime.timeInMillis,
            timeString = timeString,
            requestCode = generateRequestCode(medication.id, index, 2)
        )
    }

    private fun scheduleSpecificDaysReminder(
        context: Context,
        medication: Medication,
        startTime: Calendar,
        timeString: String,
        index: Int
    ) {
        if (medication.customDaysOfWeek.isEmpty()) return

        val now = Calendar.getInstance()
        val reminderTime = startTime.clone() as Calendar

        for (i in 0..7) {
            val dayOfWeek = reminderTime.get(Calendar.DAY_OF_WEEK)
            val normalizedDay = if (dayOfWeek == Calendar.SUNDAY) 7 else dayOfWeek - 1

            if (medication.customDaysOfWeek.contains(normalizedDay) && reminderTime.after(now)) {
                scheduleExactAlarm(
                    context = context,
                    medication = medication,
                    triggerTimeMillis = reminderTime.timeInMillis,
                    timeString = timeString,
                    requestCode = generateRequestCode(medication.id, index, 3)
                )
                return
            }
            reminderTime.add(Calendar.DAY_OF_MONTH, 1)
        }
    }

    private fun scheduleEveryXDaysReminder(
        context: Context,
        medication: Medication,
        startTime: Calendar,
        timeString: String,
        index: Int
    ) {
        val now = Calendar.getInstance()
        val reminderTime = startTime.clone() as Calendar

        if (reminderTime.before(now) || reminderTime == now) {
            reminderTime.add(Calendar.DAY_OF_MONTH, medication.customIntervalDays)
        }

        scheduleExactAlarm(
            context = context,
            medication = medication,
            triggerTimeMillis = reminderTime.timeInMillis,
            timeString = timeString,
            requestCode = generateRequestCode(medication.id, index, 4)
        )
    }

    private fun scheduleExactAlarm(
        context: Context,
        medication: Medication,
        triggerTimeMillis: Long,
        timeString: String,
        requestCode: Int
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, MedicationAlarmReceiver::class.java).apply {
            putExtra("medicationId", medication.id)
            putExtra("medicationName", medication.name)
            putExtra("dosage", medication.dosage.name)
            putExtra("quantity", medication.quantity)
            putExtra("timeString", timeString)
            putExtra("frequency", medication.frequency.name)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMillis,
                pendingIntent
            )
        }
    }

    private fun generateRequestCode(medicationId: String, timeIndex: Int, frequencyType: Int): Int {
        return (medicationId.hashCode() + timeIndex * 1000 + frequencyType * 100)
    }

    fun cancelMedicationReminders(context: Context, medicationId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        for (timeIndex in 0..10) {
            for (frequencyType in 0..4) {
                val requestCode = generateRequestCode(medicationId, timeIndex, frequencyType)

                val intent = Intent(context, MedicationAlarmReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )

                pendingIntent?.let {
                    alarmManager.cancel(it)
                    it.cancel()
                }
            }
        }
    }
}

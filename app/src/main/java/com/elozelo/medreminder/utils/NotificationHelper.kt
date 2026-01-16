package com.elozelo.medreminder.utils

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.elozelo.medreminder.MainActivity
import com.elozelo.medreminder.R

object NotificationHelper {
    private const val CHANNEL_ID = "appointment_reminders"
    private const val MEDICATION_CHANNEL_ID = "medication_reminders"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Kanał dla wizyt
            val appointmentChannel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_appointments),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_appointments_desc)
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(appointmentChannel)

            val medicationChannel = NotificationChannel(
                MEDICATION_CHANNEL_ID,
                context.getString(R.string.notification_channel_medications),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_medications_desc)
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(medicationChannel)
        }
    }

    @SuppressLint("MissingPermission")
    fun showAppointmentNotification(
        context: Context,
        appointmentId: String,
        appointmentName: String,
        appointmentDate: String,
        daysUntil: Int
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            appointmentId.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = when (daysUntil) {
            0 -> context.getString(R.string.notification_appointment_today)
            1 -> context.getString(R.string.notification_appointment_tomorrow)
            else -> context.getString(R.string.notification_appointment_in_days, daysUntil)
        }

        val content = "$appointmentName - $appointmentDate"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_today)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(appointmentId.hashCode(), notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    @SuppressLint("MissingPermission")
    fun showMedicationNotification(
        context: Context,
        medicationId: String,
        medicationName: String,
        dosage: String,
        quantity: Int,
        timeString: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            medicationId.hashCode() + timeString.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = context.getString(R.string.notification_medication_title)
        val content = context.getString(R.string.notification_medication_text, medicationName, quantity, dosage, timeString)

        val notification = NotificationCompat.Builder(context, MEDICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_today)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(
                medicationId.hashCode() + timeString.hashCode(),
                notification
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}


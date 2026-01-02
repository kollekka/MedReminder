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

object NotificationHelper {
    private const val CHANNEL_ID = "appointment_reminders"
    private const val CHANNEL_NAME = "Przypomnienia o wizytach"
    private const val CHANNEL_DESCRIPTION = "Powiadomienia o nadchodzących wizytach lekarskich"

    private const val MEDICATION_CHANNEL_ID = "medication_reminders"
    private const val MEDICATION_CHANNEL_NAME = "Przypomnienia o lekach"
    private const val MEDICATION_CHANNEL_DESCRIPTION = "Powiadomienia o zażyciu leków"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Kanał dla wizyt
            val appointmentChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(appointmentChannel)

            val medicationChannel = NotificationChannel(
                MEDICATION_CHANNEL_ID,
                MEDICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = MEDICATION_CHANNEL_DESCRIPTION
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
            0 -> "Wizyta dzisiaj!"
            1 -> "Wizyta jutro"
            else -> "Wizyta za $daysUntil dni"
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

        val title = "Pamiętaj o swoim lekarstwie!"
        val content = "$medicationName, dawka: $quantity : $dosage o $timeString"

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


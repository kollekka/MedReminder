package com.elozelo.medreminder.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.elozelo.medreminder.data.repository.MedicationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Przywróć wszystkie alarmy po restarcie urządzenia
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repository = MedicationRepository()
                    val medications = repository.getMedications().first()

                    medications.forEach { medication ->
                        if (medication.reminderEnabled &&
                            medication.reminderTimes.isNotEmpty() &&
                            medication.remainingQuantity > 0) {
                            MedicationReminderScheduler.scheduleMedicationReminders(context, medication)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}

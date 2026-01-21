package com.elozelo.medreminder.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import com.elozelo.medreminder.data.model.DosageUnit
import com.elozelo.medreminder.data.model.frequencyUnit
import com.elozelo.medreminder.data.repository.MedicationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

class MedicationAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val medicationId = intent.getStringExtra("medicationId") ?: return
        val medicationName = intent.getStringExtra("medicationName") ?: return
        val dosageEnumName = intent.getStringExtra("dosage") ?: return
        val quantity = intent.getIntExtra("quantity", 0)
        val timeString = intent.getStringExtra("timeString") ?: return
        val frequencyName = intent.getStringExtra("frequency")

        CoroutineScope(Dispatchers.IO).launch {
            val localizedContext = getLocalizedContext(context)

            val dosageUnit = try {
                DosageUnit.valueOf(dosageEnumName)
            } catch (e: IllegalArgumentException) {
                DosageUnit.PILLS
            }
            val localizedDosage = dosageUnit.getLocalizedName(localizedContext)

            NotificationHelper.showMedicationNotification(
                context = localizedContext,
                medicationId = medicationId,
                medicationName = medicationName,
                dosage = localizedDosage,
                quantity = quantity,
                timeString = timeString
            )

            // Po wyświetleniu powiadomienia, zaplanuj następne
            // Pobierz aktualny stan leku z bazy danych
            try {
                val repository = MedicationRepository()
                val medications = repository.getMedications().first()
                val medication = medications.find { it.id == medicationId }

                if (medication != null && medication.reminderEnabled && medication.remainingQuantity > 0) {
                    // Przeplanuj przypomnienia dla tego leku
                    MedicationReminderScheduler.scheduleMedicationReminders(context, medication)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun getLocalizedContext(context: Context): Context {
        val languageCode = try {
            context.languageDataStore.data.first()[LanguagePreferences.LANGUAGE_KEY]
                ?: LanguagePreferences.POLISH
        } catch (e: Exception) {
            LanguagePreferences.POLISH
        }

        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        return context.createConfigurationContext(config)
    }
}

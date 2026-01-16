package com.elozelo.medreminder.utils

import android.content.Context
import android.content.res.Configuration
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.elozelo.medreminder.data.model.DosageUnit
import kotlinx.coroutines.flow.first
import java.util.Locale

class MedicationReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val medicationId = inputData.getString("medicationId") ?: return Result.failure()
        val medicationName = inputData.getString("medicationName") ?: return Result.failure()
        val dosageEnumName = inputData.getString("dosage") ?: return Result.failure()
        val quantity = inputData.getInt("quantity", 0)
        val timeString = inputData.getString("timeString") ?: return Result.failure()

        // Pobierz kontekst z prawidłowym Locale
        val localizedContext = getLocalizedContext()

        val dosageUnit = try {
            DosageUnit.valueOf(dosageEnumName)
        } catch (e: IllegalArgumentException) {
            DosageUnit.PILLS // fallback
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


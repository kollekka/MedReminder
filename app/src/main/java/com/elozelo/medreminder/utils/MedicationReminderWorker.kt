package com.elozelo.medreminder.utils

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.elozelo.medreminder.data.model.DosageUnit

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

        val dosageUnit = try {
            DosageUnit.valueOf(dosageEnumName)
        } catch (e: IllegalArgumentException) {
            DosageUnit.PILLS // fallback
        }
        val localizedDosage = dosageUnit.getLocalizedName(applicationContext)

        NotificationHelper.showMedicationNotification(
            context = applicationContext,
            medicationId = medicationId,
            medicationName = medicationName,
            dosage = localizedDosage,
            quantity = quantity,
            timeString = timeString
        )

        return Result.success()
    }
}


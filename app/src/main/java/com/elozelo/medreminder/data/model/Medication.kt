package com.elozelo.medreminder.data.model

import android.content.Context
import androidx.annotation.StringRes
import com.elozelo.medreminder.R
import com.google.firebase.firestore.DocumentId
import java.util.Date

data class Medication(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val dosage: DosageUnit,
    val quantity: Int = 0,
    val initialQuantity: Int = 0,
    val remainingQuantity: Int = 0,
    val frequency: frequencyUnit,
    val endDate: Date? = null,
    val notes: String? = "",
    val reminderEnabled: Boolean = true,
    val reminderTimes: List<String> = emptyList(),
    val lastTakenTime: String? = null,
    val dailyTakenCount: Int = 0,
    val lastTakenDate: String? = null
)

enum class DosageUnit(@StringRes val stringResId: Int) {
    MG(R.string.dosage_unit_mg),
    ML(R.string.dosage_unit_ml),
    PILLS(R.string.dosage_unit_pills),
    TABLETS(R.string.dosage_unit_tablets),
    CAPSULES(R.string.dosage_unit_capsules);

    fun getLocalizedName(context: Context): String {
        return context.getString(stringResId)
    }
}

enum class frequencyUnit(@StringRes val stringResId: Int) {
    DAILY(R.string.frequency_daily),
    WEEKLY(R.string.frequency_weekly),
    MONTHLY(R.string.frequency_monthly),
    YEARLY(R.string.frequency_yearly);

    fun getLocalizedName(context: Context): String {
        return context.getString(stringResId)
    }
}

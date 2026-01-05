package com.elozelo.medreminder.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.elozelo.medreminder.data.model.DosageUnit
import com.elozelo.medreminder.data.model.Medication
import com.elozelo.medreminder.data.model.frequencyUnit
import com.elozelo.medreminder.utils.MedicationReminderScheduler
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MedicationViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()


    private val currentUser get() = auth.currentUser

    private val _medications = MutableStateFlow<List<Medication>>(emptyList())
    val medications: StateFlow<List<Medication>> = _medications.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val medicationCollectionRef
        get() = currentUser?.let { user ->
            db.collection("users").document(user.uid).collection("MyMeds")
        }

    private var authStateListener: FirebaseAuth.AuthStateListener? = null

    init {
        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                fetchMedications()
            }
        }
        auth.addAuthStateListener(authStateListener!!)
    }

    override fun onCleared() {
        super.onCleared()
        authStateListener?.let { auth.removeAuthStateListener(it) }
    }

    private fun fetchMedications() {
        val collectionRef = medicationCollectionRef ?: run {
            _errorMessage.value = "Błąd: Użytkownik nie jest zalogowany."
            return
        }

        _isLoading.value = true
        collectionRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                _errorMessage.value = "Błąd podczas ładowania leków: ${error.message}"
                _isLoading.value = false
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val medsList = snapshot.documents.mapNotNull { doc ->
                    try {
                        // Robust Dosage parsing
                        val dosageStr = doc.getString("dosage")
                        val dosage = dosageStr?.let {
                            try { DosageUnit.valueOf(it.uppercase()) } catch (e: IllegalArgumentException) { null }
                        } ?: DosageUnit.PILLS // default

                        // Robust Frequency parsing
                        val freqStr = doc.getString("frequency")
                        val frequency = freqStr?.let {
                            try { frequencyUnit.valueOf(it.uppercase()) } catch (e: IllegalArgumentException) { null }
                        } ?: frequencyUnit.DAILY // default

                        // Robust Quantity parsing
                        val quantityRaw = doc.get("quantity")
                        val quantity = when (quantityRaw) {
                            is Long -> quantityRaw.toInt()
                            is String -> quantityRaw.toIntOrNull() ?: 1
                            else -> 1
                        }

                        val initialQuantityRaw = doc.get("initialQuantity")
                        val initialQuantity = when (initialQuantityRaw) {
                            is Long -> initialQuantityRaw.toInt()
                            is String -> initialQuantityRaw.toIntOrNull() ?: 0
                            else -> 0
                        }

                        val remainingQuantityRaw = doc.get("remainingQuantity")
                        val remainingQuantity = when (remainingQuantityRaw) {
                            is Long -> remainingQuantityRaw.toInt()
                            is String -> remainingQuantityRaw.toIntOrNull() ?: initialQuantity
                            else -> initialQuantity
                        }

                        val dailyTakenCountRaw = doc.get("dailyTakenCount")
                        val dailyTakenCount = when (dailyTakenCountRaw) {
                            is Long -> dailyTakenCountRaw.toInt()
                            is String -> dailyTakenCountRaw.toIntOrNull() ?: 0
                            else -> 0
                        }

                        Medication(
                            id = doc.id,
                            userId = doc.getString("userId") ?: "",
                            name = doc.getString("name") ?: "",
                            dosage = dosage,
                            quantity = quantity,
                            initialQuantity = initialQuantity,
                            remainingQuantity = remainingQuantity,
                            frequency = frequency,
                            endDate = (doc.get("endDate") as? Timestamp)?.toDate(),
                            notes = doc.getString("notes"),
                            reminderEnabled = doc.getBoolean("reminderEnabled") ?: true,
                            reminderTimes = doc.get("reminderTimes") as? List<String> ?: emptyList(),
                            lastTakenTime = doc.getString("lastTakenTime"),
                            dailyTakenCount = dailyTakenCount,
                            lastTakenDate = doc.getString("lastTakenDate")
                        )
                    } catch (e: Exception) {
                        _errorMessage.value = "Błąd parsowania danych: ${e.localizedMessage}"
                        null
                    }
                }
                _medications.value = medsList

                // Zaplanuj przypomnienia dla wszystkich leków z włączonymi przypomnieniami i zapasem
                medsList.forEach { medication ->
                    if (medication.reminderEnabled && medication.reminderTimes.isNotEmpty() && medication.remainingQuantity > 0) {
                        MedicationReminderScheduler.scheduleMedicationReminders(getApplication(), medication)
                    }
                }
            }
            _isLoading.value = false
        }
    }

    fun addMedication(medication: Medication) {
        val collectionRef = medicationCollectionRef ?: run {
            _errorMessage.value = "Błąd: Użytkownik nie jest zalogowany."
            return
        }

        collectionRef.add(medication)
            .addOnSuccessListener { documentReference ->
                val medicationWithId = medication.copy(id = documentReference.id)
                // Zaplanuj przypomnienia
                MedicationReminderScheduler.scheduleMedicationReminders(getApplication(), medicationWithId)
            }
            .addOnFailureListener { e ->
                _errorMessage.value = "Błąd podczas dodawania leku: ${e.message}"
            }
    }

    fun updateMedication(medication: Medication) {
        if (medication.id.isBlank()) return
        val collectionRef = medicationCollectionRef ?: run {
            _errorMessage.value = "Błąd: Użytkownik nie jest zalogowany."
            return
        }

        collectionRef.document(medication.id).set(medication)
            .addOnSuccessListener {
                // Zaktualizuj przypomnienia
                MedicationReminderScheduler.scheduleMedicationReminders(getApplication(), medication)
            }
            .addOnFailureListener { e ->
                _errorMessage.value = "Błąd podczas aktualizacji leku: ${e.message}"
            }
    }

    fun deleteMedication(medicationId: String) {
        if (medicationId.isBlank()) return
        val collectionRef = medicationCollectionRef ?: run {
            _errorMessage.value = "Błąd: Użytkownik nie jest zalogowany."
            return
        }

        collectionRef.document(medicationId).delete()
            .addOnSuccessListener {
                // Anuluj przypomnienia
                MedicationReminderScheduler.cancelMedicationReminders(getApplication(), medicationId)
            }
            .addOnFailureListener { e ->
                _errorMessage.value = "Błąd podczas usuwania leku: ${e.message}"
            }
    }

    fun markMedicationTaken(medication: Medication) {
        if (medication.id.isBlank()) return
        val collectionRef = medicationCollectionRef ?: run {
            _errorMessage.value = "Błąd: Użytkownik nie jest zalogowany."
            return
        }

        val newRemaining = (medication.remainingQuantity - medication.quantity).coerceAtLeast(0)

        // Pobierz dzisiejszą datę w formacie yyyy-MM-dd
        val calendar = java.util.Calendar.getInstance()
        val today = String.format(
            "%04d-%02d-%02d",
            calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH) + 1,
            calendar.get(java.util.Calendar.DAY_OF_MONTH)
        )

        // Sprawdź czy to ta sama data co ostatnie wzięcie
        val isToday = medication.lastTakenDate == today

        // Zaktualizuj licznik dziennych wzięć
        val newDailyCount = if (isToday) {
            medication.dailyTakenCount + 1
        } else {
            1 // Reset licznika na nowy dzień
        }

        val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(java.util.Calendar.MINUTE)
        val currentTimeInMinutes = currentHour * 60 + currentMinute

        // Znajdź aktualnie braną godzinę
        val currentTakingTime = if (medication.lastTakenTime != null && isToday) {
            val lastParts = medication.lastTakenTime.split(":")
            if (lastParts.size == 2) {
                val lastHour = lastParts[0].toIntOrNull() ?: 0
                val lastMinute = lastParts[1].toIntOrNull() ?: 0
                lastHour * 60 + lastMinute
            } else currentTimeInMinutes
        } else {
            // Nowy dzień lub brak lastTakenTime - znajdź najbliższą godzinę przypomnienia
            medication.reminderTimes
                .mapNotNull { timeString ->
                    val parts = timeString.split(":")
                    if (parts.size == 2) {
                        val hour = parts[0].toIntOrNull() ?: return@mapNotNull null
                        val minute = parts[1].toIntOrNull() ?: return@mapNotNull null
                        val timeInMinutes = hour * 60 + minute
                        if (timeInMinutes >= currentTimeInMinutes - 30) timeInMinutes else null
                    } else null
                }
                .minOrNull() ?: currentTimeInMinutes
        }

        // Znajdź następną godzinę przypomnienia (po aktualnie branej)
        val nextReminderTime = medication.reminderTimes
            .mapNotNull { timeString ->
                val parts = timeString.split(":")
                if (parts.size == 2) {
                    val hour = parts[0].toIntOrNull() ?: return@mapNotNull null
                    val minute = parts[1].toIntOrNull() ?: return@mapNotNull null
                    val timeInMinutes = hour * 60 + minute
                    if (timeInMinutes > currentTakingTime) timeString to timeInMinutes else null
                } else null
            }
            .minByOrNull { it.second }
            ?.first

        val updatedMedication = medication.copy(
            remainingQuantity = newRemaining,
            lastTakenTime = nextReminderTime,
            dailyTakenCount = newDailyCount,
            lastTakenDate = today
        )

        collectionRef.document(medication.id).set(updatedMedication)
            .addOnSuccessListener {
                if (newRemaining == 0) {
                    MedicationReminderScheduler.cancelMedicationReminders(getApplication(), medication.id)
                }
            }
            .addOnFailureListener { e ->
                _errorMessage.value = "Błąd podczas aktualizacji ilości: ${e.message}"
            }
    }

    fun refillMedication(medication: Medication) {
        if (medication.id.isBlank()) return
        val collectionRef = medicationCollectionRef ?: run {
            _errorMessage.value = "Błąd: Użytkownik nie jest zalogowany."
            return
        }

        // Przywróć zapas do początkowej wartości i zresetuj licznik dziennych dawek
        val updatedMedication = medication.copy(
            remainingQuantity = medication.initialQuantity,
            dailyTakenCount = 0,
            lastTakenTime = null,
            lastTakenDate = null
        )

        collectionRef.document(medication.id).set(updatedMedication)
            .addOnSuccessListener {
                // Zaplanuj przypomnienia ponownie
                MedicationReminderScheduler.scheduleMedicationReminders(getApplication(), updatedMedication)
            }
            .addOnFailureListener { e ->
                _errorMessage.value = "Błąd podczas uzupełniania leku: ${e.message}"
            }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}

package com.elozelo.medreminder.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.elozelo.medreminder.data.model.Appointment
import com.elozelo.medreminder.utils.ReminderScheduler
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppointmentViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val currentUser get() = auth.currentUser

    private val _appointments = MutableStateFlow<List<Appointment>>(emptyList())
    val appointments: StateFlow<List<Appointment>> = _appointments.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val appointmentCollectionRef
        get() = currentUser?.let { user ->
            db.collection("users").document(user.uid).collection("MyAppointments")
        }

    private var authStateListener: FirebaseAuth.AuthStateListener? = null

    init {
        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                fetchAppointments()
            }
        }
        auth.addAuthStateListener(authStateListener!!)
    }

    override fun onCleared() {
        super.onCleared()
        authStateListener?.let { auth.removeAuthStateListener(it) }
    }

    fun fetchAppointments() {
        val collectionRef = appointmentCollectionRef ?: run {
            _errorMessage.value = "Błąd: Użytkownik nie jest zalogowany."
            return
        }

        _isLoading.value = true
        collectionRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                _errorMessage.value = "Błąd podczas ładowania wizyt: ${error.message}"
                _isLoading.value = false
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val apptList = snapshot.documents.mapNotNull { doc ->
                    val appt = doc.toObject<Appointment>()
                    appt?.copy(id = doc.id)
                }
                _appointments.value = apptList

                // Zaplanuj przypomnienia dla wszystkich wizyt z włączonymi przypomnieniami
                apptList.forEach { appointment ->
                    if (appointment.reminderEnabled && appointment.dateTime > System.currentTimeMillis()) {
                        ReminderScheduler.scheduleAppointmentReminders(getApplication(), appointment)
                    }
                }
            }
            _isLoading.value = false
        }
    }

    fun addAppointment(appointment: Appointment) {
        val collectionRef = appointmentCollectionRef ?: run {
            _errorMessage.value = "Błąd: Użytkownik nie jest zalogowany."
            return
        }

        collectionRef.add(appointment)
            .addOnSuccessListener { documentReference ->
                val appointmentWithId = appointment.copy(id = documentReference.id)
                // Zaplanuj przypomnienia
                ReminderScheduler.scheduleAppointmentReminders(getApplication(), appointmentWithId)
            }
            .addOnFailureListener { e ->
                _errorMessage.value = "Błąd podczas dodawania wizyty: ${e.message}"
            }
    }

    fun updateAppointment(appointment: Appointment) {
        if (appointment.id.isBlank()) return
        val collectionRef = appointmentCollectionRef ?: run {
            _errorMessage.value = "Błąd: Użytkownik nie jest zalogowany."
            return
        }

        collectionRef.document(appointment.id).set(appointment)
            .addOnSuccessListener {
                // Zaktualizuj przypomnienia
                ReminderScheduler.scheduleAppointmentReminders(getApplication(), appointment)
            }
            .addOnFailureListener { e ->
                _errorMessage.value = "Błąd podczas aktualizacji wizyty: ${e.message}"
            }
    }

    fun deleteAppointment(appointmentId: String) {
        if (appointmentId.isBlank()) return
        val collectionRef = appointmentCollectionRef ?: run {
            _errorMessage.value = "Błąd: Użytkownik nie jest zalogowany."
            return
        }

        collectionRef.document(appointmentId).delete()
            .addOnSuccessListener {
                // Anuluj przypomnienia
                ReminderScheduler.cancelAppointmentReminders(getApplication(), appointmentId)
            }
            .addOnFailureListener { e ->
                _errorMessage.value = "Błąd podczas usuwania wizyty: ${e.message}"
            }
    }

    fun markAppointmentCompleted(appointment: Appointment) {
        if (appointment.id.isBlank()) return
        val collectionRef = appointmentCollectionRef ?: run {
            _errorMessage.value = "Błąd: Użytkownik nie jest zalogowany."
            return
        }

        val updatedAppointment = appointment.copy(completed = true)

        collectionRef.document(appointment.id).set(updatedAppointment)
            .addOnFailureListener { e ->
                _errorMessage.value = "Błąd podczas oznaczania wizyty: ${e.message}"
            }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}

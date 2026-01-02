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
    private val currentUser = auth.currentUser

    private val _appointments = MutableStateFlow<List<Appointment>>(emptyList())
    val appointments: StateFlow<List<Appointment>> = _appointments.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val appointmentCollectionRef
        get() = db.collection("users").document(currentUser!!.uid).collection("MyAppointments")

    init {
        if (currentUser != null) {
            fetchAppointments()
        } else {
            _errorMessage.value = "Błąd: Użytkownik nie jest zalogowany."
        }
    }

    fun fetchAppointments() {
        _isLoading.value = true
        appointmentCollectionRef.addSnapshotListener { snapshot, error ->
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
        if (currentUser == null) return

        appointmentCollectionRef.add(appointment)
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
        if (currentUser == null || appointment.id.isBlank()) return

        appointmentCollectionRef.document(appointment.id).set(appointment)
            .addOnSuccessListener {
                // Zaktualizuj przypomnienia
                ReminderScheduler.scheduleAppointmentReminders(getApplication(), appointment)
            }
            .addOnFailureListener { e ->
                _errorMessage.value = "Błąd podczas aktualizacji wizyty: ${e.message}"
            }
    }

    fun deleteAppointment(appointmentId: String) {
        if (currentUser == null || appointmentId.isBlank()) return

        appointmentCollectionRef.document(appointmentId).delete()
            .addOnSuccessListener {
                // Anuluj przypomnienia
                ReminderScheduler.cancelAppointmentReminders(getApplication(), appointmentId)
            }
            .addOnFailureListener { e ->
                _errorMessage.value = "Błąd podczas usuwania wizyty: ${e.message}"
            }
    }

    fun markAppointmentCompleted(appointment: Appointment) {
        if (currentUser == null || appointment.id.isBlank()) return

        val updatedAppointment = appointment.copy(completed = true)

        appointmentCollectionRef.document(appointment.id).set(updatedAppointment)
            .addOnFailureListener { e ->
                _errorMessage.value = "Błąd podczas oznaczania wizyty: ${e.message}"
            }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}

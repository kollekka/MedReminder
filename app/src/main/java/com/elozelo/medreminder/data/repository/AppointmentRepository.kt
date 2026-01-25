package com.elozelo.medreminder.data.repository

import com.elozelo.medreminder.data.model.Appointment
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AppointmentRepository {
    private val db: FirebaseFirestore = Firebase.firestore
    private val appointmentsCollection = db.collection("appointments")
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    fun getAppointments(): Flow<List<Appointment>> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = appointmentsCollection
            .whereEqualTo("userId", userId)
            .orderBy("dateTime", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val appointments = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Appointment::class.java)
                    }
                    trySend(appointments)
                }
            }

        awaitClose { listener.remove() }
    }

    fun getUpcomingAppointments(): Flow<List<Appointment>> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val currentTime = System.currentTimeMillis()
        val listener = appointmentsCollection
            .whereEqualTo("userId", userId)
            .whereGreaterThanOrEqualTo("dateTime", currentTime)
            .orderBy("dateTime", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val appointments = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Appointment::class.java)
                    }
                    trySend(appointments)
                }
            }

        awaitClose { listener.remove() }
    }


    suspend fun getAppointmentById(id: String): Appointment? {
        return try {
            val userId = auth.currentUser?.uid ?: return null
            val document = appointmentsCollection.document(id).get().await()
            val appointment = document.toObject(Appointment::class.java)
            if (appointment?.userId == userId) appointment else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    suspend fun addAppointment(appointment: Appointment): Result<String> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("Użytkownik niezalogowany"))

            val appointmentWithUser = appointment.copy(userId = userId)
            val docRef = appointmentsCollection.add(appointmentWithUser).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun updateAppointment(appointment: Appointment): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("Użytkownik niezalogowany"))

            if (appointment.id.isEmpty()) {
                return Result.failure(IllegalArgumentException("Appointment ID nie może być pusty"))
            }
            if (appointment.userId != userId) {
                return Result.failure(IllegalStateException("Brak uprawnień"))
            }
            appointmentsCollection.document(appointment.id).set(appointment).await()
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun deleteAppointment(id: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("Użytkownik niezalogowany"))

            val appointment = getAppointmentById(id)
            if (appointment == null || appointment.userId != userId) {
                return Result.failure(IllegalStateException("Brak uprawnień"))
            }

            appointmentsCollection.document(id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}

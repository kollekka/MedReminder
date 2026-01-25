package com.elozelo.medreminder.data.repository

import com.elozelo.medreminder.data.model.Medication
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await


class MedicationRepository {
    private val db: FirebaseFirestore = Firebase.firestore
    private val medicationsCollection = db.collection("medications")
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    val userId = auth.currentUser?.uid


    fun getMedications(): Flow<List<Medication>> = callbackFlow {
        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = medicationsCollection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val medications = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Medication::class.java)
                    }
                    trySend(medications)
                }
            }

        awaitClose { listener.remove() }
    }

    suspend fun getMedicationById(id: String): Medication? {
        return try {
            val userId = auth.currentUser?.uid ?: return null
            val document = medicationsCollection.document(id).get().await()
            val medication = document.toObject(Medication::class.java)
            if (medication?.userId == userId) medication else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    suspend fun addMedication(medication: Medication): Result<String> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("Użytkownik niezalogowany"))

            val medicationWithUser = medication.copy(userId = userId)
            val docRef = medicationsCollection.add(medicationWithUser).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun updateMedication(medication: Medication): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("Użytkownik niezalogowany"))

            if (medication.id.isEmpty()) {
                return Result.failure(IllegalArgumentException("Medication ID nie może być pusty"))
            }
            if (medication.userId != userId) {
                return Result.failure(IllegalStateException("Brak uprawnień"))
            }
            medicationsCollection.document(medication.id).set(medication).await()
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun deleteMedication(id: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("Użytkownik niezalogowany"))

            val medication = getMedicationById(id)
            if (medication == null || medication.userId != userId) {
                return Result.failure(IllegalStateException("Brak uprawnień"))
            }

            medicationsCollection.document(id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    fun getActiveMedications(): Flow<List<Medication>> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = medicationsCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("reminderEnabled", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val medications = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Medication::class.java)
                    }
                    trySend(medications)
                }
            }

        awaitClose { listener.remove() }
    }
}


package com.elozelo.medreminder.data.model

import com.google.firebase.firestore.DocumentId


data class Appointment(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val userId: String = "",
    val dateTime: Long = System.currentTimeMillis(),
    val location: String = "",
    val notes: String = "",
    val reminderEnabled: Boolean = true,
    val completed: Boolean = false
)

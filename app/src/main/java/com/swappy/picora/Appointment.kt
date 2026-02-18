package com.swappy.picora

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "appointments")
data class Appointment(
    @PrimaryKey(autoGenerate = true) var id: Int = 0,
    var appointmentNumber: Int = 0,
    val cabinNumber: String,
    val guests: String,
    val photographer: String,
    val occasion: String,
    val dateTime: Long,
    val creationDate: Long = System.currentTimeMillis(),
    val photoUri: String? = null,
    val isDeleted: Boolean = false
)
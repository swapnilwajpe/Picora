package com.swappy.picora

import androidx.room.Entity

@Entity(tableName = "time_slots", primaryKeys = ["date", "time"])
data class TimeSlot(
    val date: String,
    val time: String
)

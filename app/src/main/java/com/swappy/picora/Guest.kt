package com.swappy.picora

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "guests")
data class Guest(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    val cabin: String,
    val name: String
)

package com.swappy.picora

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "occasions")
data class Occasion(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    val name: String
)

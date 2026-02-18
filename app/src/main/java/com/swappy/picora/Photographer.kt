package com.swappy.picora

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "photographers")
data class Photographer(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    val name: String
)

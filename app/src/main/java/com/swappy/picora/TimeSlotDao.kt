package com.swappy.picora

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TimeSlotDao {
    @Query("SELECT * FROM time_slots")
    fun getAll(): Flow<List<TimeSlot>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(timeSlots: List<TimeSlot>)

    @Query("DELETE FROM time_slots")
    suspend fun clearAll()
}

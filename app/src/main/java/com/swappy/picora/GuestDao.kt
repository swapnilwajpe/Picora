package com.swappy.picora

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GuestDao {
    @Query("SELECT * FROM guests")
    fun getAll(): Flow<List<Guest>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(guests: List<Guest>)

    @Query("DELETE FROM guests")
    suspend fun clearAll()
}

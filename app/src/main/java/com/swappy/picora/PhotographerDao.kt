package com.swappy.picora

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotographerDao {
    @Query("SELECT * FROM photographers")
    fun getAll(): Flow<List<Photographer>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(photographers: List<Photographer>)

    @Query("DELETE FROM photographers")
    suspend fun clearAll()
}

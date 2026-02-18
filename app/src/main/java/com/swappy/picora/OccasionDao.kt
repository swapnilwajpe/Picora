package com.swappy.picora

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OccasionDao {
    @Query("SELECT * FROM occasions")
    fun getAll(): Flow<List<Occasion>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(occasions: List<Occasion>)

    @Query("DELETE FROM occasions")
    suspend fun clearAll()
}

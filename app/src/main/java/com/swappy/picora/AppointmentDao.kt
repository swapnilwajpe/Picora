package com.swappy.picora

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppointmentDao {
    @Insert
    suspend fun insert(appointment: Appointment)

    @Update
    suspend fun update(appointment: Appointment)

    @Delete
    suspend fun delete(appointment: Appointment)

    @Query("DELETE FROM appointments")
    suspend fun clearAll()

    @Query("UPDATE appointments SET isDeleted = true")
    suspend fun setAllAsDeleted()

    @Query("SELECT * FROM appointments WHERE isDeleted = false ORDER BY dateTime DESC")
    fun getAll(): Flow<List<Appointment>>

    @Query("SELECT * FROM appointments WHERE dateTime = :dateTime AND isDeleted = false")
    suspend fun getAppointmentByDateTime(dateTime: Long): Appointment?

    @Query("SELECT MAX(appointmentNumber) FROM appointments")
    suspend fun getMaxAppointmentNumber(): Int?

    @Query("SELECT * FROM appointments ORDER BY dateTime DESC")
    fun getAllIncludingDeleted(): Flow<List<Appointment>>
}
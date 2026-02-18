package com.swappy.picora

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AppointmentViewModel(
    private val appointmentDao: AppointmentDao,
    private val guestDao: GuestDao,
    private val photographerDao: PhotographerDao,
    private val occasionDao: OccasionDao,
    private val timeSlotDao: TimeSlotDao
) : ViewModel() {

    val allAppointments: Flow<List<Appointment>> = appointmentDao.getAll()
    val allAppointmentsForExport: Flow<List<Appointment>> = appointmentDao.getAllIncludingDeleted()
    val allGuests: Flow<List<Guest>> = guestDao.getAll()
    val allPhotographers: Flow<List<Photographer>> = photographerDao.getAll()
    val allOccasions: Flow<List<Occasion>> = occasionDao.getAll()
    val timeSlots: Flow<List<TimeSlot>> = timeSlotDao.getAll()

    private val _appointmentExists = MutableLiveData<Boolean>()
    val appointmentExists: LiveData<Boolean> = _appointmentExists

    private val _appointmentsToExport = MutableStateFlow<List<Appointment>>(emptyList())
    val appointmentsToExport: StateFlow<List<Appointment>> = _appointmentsToExport

    fun setAppointmentsToExport(appointments: List<Appointment>) {
        _appointmentsToExport.value = appointments
    }

    fun clearAppointmentsToExport() {
        _appointmentsToExport.value = emptyList()
    }

    fun insert(appointment: Appointment) = viewModelScope.launch {
        if (appointmentDao.getAppointmentByDateTime(appointment.dateTime) != null) {
            _appointmentExists.value = true
        } else {
            val maxAppointmentNumber = appointmentDao.getMaxAppointmentNumber() ?: 0
            appointment.appointmentNumber = maxAppointmentNumber + 1
            appointmentDao.insert(appointment)
        }
    }

    fun update(appointment: Appointment) = viewModelScope.launch {
        appointmentDao.update(appointment)
    }

    fun delete(appointment: Appointment) = viewModelScope.launch {
        appointmentDao.update(appointment.copy(isDeleted = true))
    }

    fun clearAllAppointments() = viewModelScope.launch {
        appointmentDao.setAllAsDeleted()
    }

    fun onAppointmentExistsShown() {
        _appointmentExists.value = false
    }

    fun clearAndInsertGuests(guests: List<GuestInfo>) = viewModelScope.launch {
        guestDao.clearAll()
        guestDao.insertAll(guests.map { Guest(cabin = it.cabin, name = it.name) })
    }

    fun clearAndInsertPhotographers(photographers: List<String>) = viewModelScope.launch {
        photographerDao.clearAll()
        photographerDao.insertAll(photographers.map { Photographer(name = it) })
    }

    fun clearAndInsertOccasions(occasions: List<String>) = viewModelScope.launch {
        occasionDao.clearAll()
        occasionDao.insertAll(occasions.map { Occasion(name = it) })
    }

    fun setTimeSlots(newTimeSlots: List<TimeSlot>) = viewModelScope.launch {
        timeSlotDao.clearAll()
        timeSlotDao.insertAll(newTimeSlots)
    }

    fun clearAllData() = viewModelScope.launch {
        appointmentDao.clearAll()
        guestDao.clearAll()
        photographerDao.clearAll()
        occasionDao.clearAll()
        timeSlotDao.clearAll()
    }
}

class AppointmentViewModelFactory(
    private val appointmentDao: AppointmentDao,
    private val guestDao: GuestDao,
    private val photographerDao: PhotographerDao,
    private val occasionDao: OccasionDao,
    private val timeSlotDao: TimeSlotDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppointmentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AppointmentViewModel(appointmentDao, guestDao, photographerDao, occasionDao, timeSlotDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

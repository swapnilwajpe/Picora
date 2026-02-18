
package com.swappy.picora

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentCreationDialog(
    slot: TimeSlot,
    allGuestsInfo: List<GuestInfo>,
    photographers: List<String>,
    occasions: List<String>,
    viewModel: AppointmentViewModel,
    onDismiss: () -> Unit
) {
    var cabinNumber by remember { mutableStateOf("") }
    var selectedGuestInfo by remember { mutableStateOf<GuestInfo?>(null) }
    var selectedPhotographer by remember { mutableStateOf(photographers.firstOrNull() ?: "") }
    var selectedOccasion by remember { mutableStateOf(occasions.firstOrNull() ?: "") }
    var guestDropdownExpanded by remember { mutableStateOf(false) }
    var photographerDropdownExpanded by remember { mutableStateOf(false) }
    var occasionDropdownExpanded by remember { mutableStateOf(false) }
    var cabinError by remember { mutableStateOf<String?>(null) }

    val guestsInCabin = allGuestsInfo.filter { it.cabin.equals(cabinNumber, ignoreCase = true) }
    val partialCabinMatch = allGuestsInfo.any { it.cabin.startsWith(cabinNumber, ignoreCase = true) }

    fun validateCabin() {
        cabinError = if (!partialCabinMatch && cabinNumber.isNotEmpty()) {
            "Invalid cabin number"
        } else {
            null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Appointment") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Slot: ${slot.date} at ${slot.time}")
                Spacer(modifier = Modifier.height(16.dp))

                // Cabin number input
                OutlinedTextField(
                    value = cabinNumber,
                    onValueChange = { 
                        cabinNumber = it
                        selectedGuestInfo = null // Reset guest selection
                        validateCabin()
                     },
                    label = { Text("Cabin Number") },
                    isError = cabinError != null,
                )
                if (cabinError != null) {
                    Text(cabinError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Guest selection
                ExposedDropdownMenuBox(
                    expanded = guestDropdownExpanded,
                    onExpandedChange = { guestDropdownExpanded = !guestDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedGuestInfo?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Guest") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = guestDropdownExpanded) },
                        modifier = Modifier.menuAnchor(),
                        enabled = cabinNumber.isNotEmpty() && cabinError == null
                    )
                    if (cabinNumber.isNotEmpty() && cabinError == null) {
                        ExposedDropdownMenu(
                            expanded = guestDropdownExpanded,
                            onDismissRequest = { guestDropdownExpanded = false }
                        ) {
                            guestsInCabin.forEach { guest ->
                                DropdownMenuItem(
                                    text = { Text(guest.name) },
                                    onClick = {
                                        selectedGuestInfo = guest
                                        guestDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Photographer selection
                ExposedDropdownMenuBox(
                    expanded = photographerDropdownExpanded,
                    onExpandedChange = { photographerDropdownExpanded = !photographerDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedPhotographer,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Photographer") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = photographerDropdownExpanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = photographerDropdownExpanded,
                        onDismissRequest = { photographerDropdownExpanded = false }
                    ) {
                        photographers.forEach { photographer ->
                            DropdownMenuItem(
                                text = { Text(photographer) },
                                onClick = {
                                    selectedPhotographer = photographer
                                    photographerDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Occasion selection
                ExposedDropdownMenuBox(
                    expanded = occasionDropdownExpanded,
                    onExpandedChange = { occasionDropdownExpanded = !occasionDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedOccasion,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Occasion") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = occasionDropdownExpanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = occasionDropdownExpanded,
                        onDismissRequest = { occasionDropdownExpanded = false }
                    ) {
                        occasions.forEach { occasion ->
                            DropdownMenuItem(
                                text = { Text(occasion) },
                                onClick = {
                                    selectedOccasion = occasion
                                    occasionDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val guest = selectedGuestInfo
                    if (guest != null && selectedPhotographer != "Select Photographer" && selectedOccasion != "Select Occasion") {
                        val sdf = SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.US)
                        val date = sdf.parse("${slot.date} ${slot.time}")
                        val appointment = Appointment(
                            guests = guest.name,
                            cabinNumber = guest.cabin,
                            photographer = selectedPhotographer,
                            occasion = selectedOccasion,
                            dateTime = date?.time ?: System.currentTimeMillis()
                        )
                        viewModel.insert(appointment)
                        onDismiss()
                    }
                },
                enabled = selectedGuestInfo != null && selectedPhotographer != "Select Photographer" && selectedOccasion != "Select Occasion" && cabinError == null
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

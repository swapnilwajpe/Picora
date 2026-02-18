
package com.swappy.picora

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swappy.picora.ui.theme.PicoraTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

data class GuestInfo(val cabin: String, val name: String)
data class ExcelData(val guests: List<GuestInfo>, val photographers: List<String>, val occasions: List<String>, val timeSlots: List<TimeSlot>)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainTabContent(viewModel: AppointmentViewModel) {
    val allGuests by viewModel.allGuests.collectAsState(initial = emptyList())
    val allPhotographers by viewModel.allPhotographers.collectAsState(initial = emptyList())
    val allOccasions by viewModel.allOccasions.collectAsState(initial = emptyList())
    val timeSlots by viewModel.timeSlots.collectAsState(initial = emptyList())
    val appointments by viewModel.allAppointments.collectAsState(initial = emptyList())
    var showAppointmentCreationDialogForSlot by remember { mutableStateOf<TimeSlot?>(null) }
    var selectedAppointment by remember { mutableStateOf<Appointment?>(null) }
    var showDeleteConfirmationId by remember { mutableStateOf<Int?>(null) }
    var showQrCodeDialog by remember { mutableStateOf<Appointment?>(null) }
    var editingDate by remember { mutableStateOf<String?>(null) }
    val portNames = remember { mutableStateMapOf<String, String>() }
    val focusManager = LocalFocusManager.current
    var showAddTimeSlotDialogForDate by remember { mutableStateOf<String?>(null) }
    var showEditAppointmentDialog by remember { mutableStateOf<Appointment?>(null) }
    var showClearAllDataConfirmation by remember { mutableStateOf(false) }

    if (showAddTimeSlotDialogForDate != null) {
        AddTimeSlotDialog(
            date = showAddTimeSlotDialogForDate!!,
            onDismiss = { showAddTimeSlotDialogForDate = null },
            onSave = { date, time ->
                val newTimeSlot = TimeSlot(date = date, time = time.uppercase(Locale.US))
                val dateComparator = compareBy<TimeSlot> {
                    try {
                        SimpleDateFormat("dd-MM-yyyy", Locale.US).parse(it.date)?.time
                    } catch (e: ParseException) {
                        Long.MAX_VALUE // put invalid dates at the end
                    }
                }
                val timeComparator = compareBy<TimeSlot> {
                    try {
                        SimpleDateFormat("hh:mm a", Locale.US).parse(it.time)?.time
                    } catch (e: ParseException) {
                        Long.MAX_VALUE // put invalid times at the end
                    }
                }
                val updatedTimeSlots = (timeSlots + newTimeSlot).distinct().sortedWith(dateComparator.then(timeComparator))
                viewModel.setTimeSlots(updatedTimeSlots)
                showAddTimeSlotDialogForDate = null
            }
        )
    }

    val groupedTimeSlots = remember(timeSlots) {
        timeSlots.groupBy { it.date }
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Load saved port names from SharedPreferences
    LaunchedEffect(groupedTimeSlots.keys.toList()) {
        groupedTimeSlots.keys.forEach { date ->
            val saved = PortNameStorage.getPortName(context, date)
            if (saved.isNotEmpty()) portNames[date] = saved
        }
    }

    val allGuestsInfo = allGuests.map { GuestInfo(it.cabin, it.name) }
    val photographers = remember(allPhotographers) { listOf("Select Photographer") + allPhotographers.map { it.name } }
    val occasions = remember(allOccasions) { listOf("Select Occasion") + allOccasions.map { it.name } }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { fileUri ->
            coroutineScope.launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(fileUri)
                    val excelData = readExcelData(inputStream, context)
                    if (excelData.guests.isNotEmpty()) viewModel.clearAndInsertGuests(excelData.guests)
                    if (excelData.photographers.isNotEmpty()) viewModel.clearAndInsertPhotographers(excelData.photographers)
                    if (excelData.occasions.isNotEmpty()) viewModel.clearAndInsertOccasions(excelData.occasions)
                    if (excelData.timeSlots.isNotEmpty()) viewModel.setTimeSlots(excelData.timeSlots)
                    snackbarHostState.showSnackbar("Excel file loaded successfully.")
                } catch (t: Throwable) {
                    t.printStackTrace()
                    snackbarHostState.showSnackbar("Error loading Excel file: ${t.localizedMessage ?: "An unknown error occurred."}")
                }
            }
        }
    }


    val sdf = SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.US)
    val bookedSlots = remember(appointments) {
        appointments.associateBy { sdf.format(Date(it.dateTime)) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .clickable { editingDate = null },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(4.dp)
                ) {
                    Text(
                        text = "Picora - The Appointments App",
                        style = MaterialTheme.typography.headlineSmall,
                        fontFamily = FontFamily.Default,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row {
                Button(onClick = { launcher.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") }) {
                    Text(text = "Upload Excel File")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { showClearAllDataConfirmation = true }) {
                    Text(text = "Clear All Data")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                groupedTimeSlots.forEach { (date, slots) ->
                    item {
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(animationSpec = tween(300)),
                            exit = fadeOut(animationSpec = tween(300))
                        ) {
                            val inputFormat = SimpleDateFormat("dd-MM-yyyy", Locale.US)
                            val outputFormat = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.US)
                            val formattedDate = try {
                                inputFormat.parse(date)?.let { outputFormat.format(it) } ?: date
                            } catch (e: ParseException) {
                                date
                            }

                            var portName by remember(date) { mutableStateOf(portNames[date] ?: "") }
                            // Keep portName in sync with portNames map and storage
                            LaunchedEffect(portNames[date]) {
                                if (portNames[date] != portName) {
                                    portName = portNames[date] ?: ""
                                }
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    if (editingDate == date) {
                                        OutlinedTextField(
                                            value = portName,
                                            onValueChange = {
                                                // Capitalize first letter of every word
                                                val capitalized = it.split(" ").joinToString(" ") { word ->
                                                    word.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase() else c.toString() }
                                                }
                                                portName = capitalized
                                                portNames[date] = capitalized
                                                PortNameStorage.savePortName(context, date, capitalized)
                                             },
                                            label = { Text("Port Name") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions.Default.copy(
                                                capitalization = KeyboardCapitalization.Words,
                                                imeAction = ImeAction.Done
                                            ),
                                            keyboardActions = KeyboardActions(onDone = { editingDate = null })
                                        )
                                    } else {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = portNames[date] ?: "", style = MaterialTheme.typography.titleMedium)
                                            if ((portNames[date] ?: "").isNotEmpty()) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                TextButton(onClick = {
                                                    portNames[date] = ""
                                                    PortNameStorage.clearPortName(context, date)
                                                }) {
                                                    Text("Clear")
                                                }
                                            }
                                        }
                                    }
                                    Text(text = formattedDate, style = MaterialTheme.typography.titleMedium, modifier = Modifier.clickable { editingDate = date })
                                    Spacer(modifier = Modifier.height(4.dp))

                                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                        val numColumns = (maxWidth / 120.dp).toInt().coerceAtLeast(1)
                                        val chunkedSlots = slots.chunked(numColumns)

                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            chunkedSlots.forEach { rowSlots ->
                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    rowSlots.forEach { slot ->
                                                        val inputTimeFormat = SimpleDateFormat("hh:mm a", Locale.US)
                                                        val outputTimeFormat = SimpleDateFormat("hh:mm a", Locale.US)
                                                        val formattedTime = try {
                                                            inputTimeFormat.parse(slot.time)?.let { outputTimeFormat.format(it) } ?: slot.time
                                                        } catch (e: ParseException) {
                                                            slot.time
                                                        }
                                                        val slotDateTime = "$date $formattedTime"
                                                        val appointment = bookedSlots[slotDateTime]

                                                        Button(
                                                            onClick = {
                                                                if (appointment != null) {
                                                                    selectedAppointment = appointment
                                                                } else {
                                                                    showAppointmentCreationDialogForSlot = slot
                                                                }
                                                            },
                                                            modifier = Modifier.weight(1f),
                                                            colors = if (appointment != null) {
                                                                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                                            } else {
                                                                ButtonDefaults.buttonColors()
                                                            }
                                                        ) {
                                                            Text(text = formattedTime)
                                                        }
                                                    }
                                                    val emptySpace = numColumns - rowSlots.size
                                                    if (emptySpace > 0) {
                                                        Spacer(modifier = Modifier.weight(emptySpace.toFloat()))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(onClick = { showAddTimeSlotDialogForDate = date }) {
                                        Text("Add Time Slot")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAppointmentCreationDialogForSlot != null) {
        AppointmentCreationDialog(
            slot = showAppointmentCreationDialogForSlot!!,
            allGuestsInfo = allGuestsInfo,
            photographers = photographers,
            occasions = occasions,
            viewModel = viewModel,
            onDismiss = { showAppointmentCreationDialogForSlot = null }
        )
    }

    if (selectedAppointment != null) {
        AppointmentActionsDialog(
            appointment = selectedAppointment!!,
            onDismiss = { selectedAppointment = null },
            onDelete = {
                showDeleteConfirmationId = it.id
                selectedAppointment = null
            },
            onEdit = {
                showEditAppointmentDialog = it
                selectedAppointment = null
            },
            onAddPhoto = { appt, uri ->
                coroutineScope.launch(Dispatchers.IO) {
                    saveImageToInternalStorage(context, uri, appt.id)?.let {
                        viewModel.update(appt.copy(photoUri = it.toString()))
                    }
                }
            },
            onShare = { appointment ->
                coroutineScope.launch {
                    try {
                        val icsContent = createIcsContent(appointment)
                        val icsFile = File(context.cacheDir, "appointment.ics")
                        icsFile.writeText(icsContent)
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", icsFile)

                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/calendar"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Appointment Event"))
                    } catch (e: Exception) {
                        e.printStackTrace()
                        coroutineScope.launch { snackbarHostState.showSnackbar("Failed to share appointment.") }
                    }
                }
            },
            onShowQrCode = { showQrCodeDialog = it }
        )
    }

    if (showEditAppointmentDialog != null) {
        AppointmentEditDialog(
            appointment = showEditAppointmentDialog!!,
            allGuestsInfo = allGuestsInfo,
            photographers = photographers,
            occasions = occasions,
            onDismiss = { showEditAppointmentDialog = null },
            onSave = {
                viewModel.update(it)
                showEditAppointmentDialog = null
            }
        )
    }

    if (showQrCodeDialog != null) {
        QrCodeDialog(appointment = showQrCodeDialog!!, onDismiss = { showQrCodeDialog = null })
    }

    if (showDeleteConfirmationId != null) {
        val appointmentToDelete = appointments.find { it.id == showDeleteConfirmationId }
        if (appointmentToDelete != null) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmationId = null },
                title = { Text("Delete Appointment") },
                text = { Text("Are you sure you want to delete this appointment?") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.delete(appointmentToDelete)
                        showDeleteConfirmationId = null
                    }) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmationId = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }

    if (showClearAllDataConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearAllDataConfirmation = false },
            title = { Text("Clear All Data") },
            text = { Text("Are you sure you want to clear all data? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllData()
                    showClearAllDataConfirmation = false
                }) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDataConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentEditDialog(
    appointment: Appointment,
    allGuestsInfo: List<GuestInfo>,
    photographers: List<String>,
    occasions: List<String>,
    onDismiss: () -> Unit,
    onSave: (Appointment) -> Unit
) {
    var selectedGuestInfo by remember { mutableStateOf(GuestInfo(appointment.cabinNumber, appointment.guests)) }
    var selectedPhotographer by remember { mutableStateOf(appointment.photographer) }
    var selectedOccasion by remember { mutableStateOf(appointment.occasion) }

    var guestDropdownExpanded by remember { mutableStateOf(false) }
    var photographerDropdownExpanded by remember { mutableStateOf(false) }
    var occasionDropdownExpanded by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("${selectedGuestInfo.cabin} - ${selectedGuestInfo.name}") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Appointment") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // Guest selection
                ExposedDropdownMenuBox(
                    expanded = guestDropdownExpanded,
                    onExpandedChange = { guestDropdownExpanded = !guestDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        label = { Text("Guest") },
                        modifier = Modifier.menuAnchor()
                    )
                    val filteredGuests = allGuestsInfo.filter {
                        "${it.cabin} - ${it.name}".contains(searchText, ignoreCase = true)
                    }
                    if (filteredGuests.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = guestDropdownExpanded,
                            onDismissRequest = { guestDropdownExpanded = false }
                        ) {
                            filteredGuests.forEach { guest ->
                                DropdownMenuItem(
                                    text = { Text("${guest.cabin} - ${guest.name}") },
                                    onClick = {
                                        selectedGuestInfo = guest
                                        searchText = "${guest.cabin} - ${guest.name}"
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
                    val updatedAppointment = appointment.copy(
                        guests = selectedGuestInfo.name,
                        cabinNumber = selectedGuestInfo.cabin,
                        photographer = selectedPhotographer,
                        occasion = selectedOccasion
                    )
                    onSave(updatedAppointment)
                },
                enabled = selectedGuestInfo.name.isNotBlank() && selectedPhotographer != "Select Photographer" && selectedOccasion != "Select Occasion"
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

@Throws(Exception::class)
private suspend fun readExcelData(inputStream: InputStream?, context: Context): ExcelData {
    return withContext(Dispatchers.IO) {
        if (inputStream == null) throw Exception("Input stream is null")

        val workbook = XSSFWorkbook(inputStream)
        val guestSheet = workbook.getSheet("Guests")
        val settingsSheet = workbook.getSheet("Settings")
        val timeSlotsSheet = workbook.getSheet("Time Slots")

        val guests = readGuestSheet(guestSheet)
        val (photographers, occasions) = readSettingsSheet(settingsSheet)
        val timeSlots = readTimeSlotsSheet(timeSlotsSheet)

        workbook.close()
        inputStream.close()
        ExcelData(guests, photographers, occasions, timeSlots)
    }
}


private fun readGuestSheet(sheet: Sheet?): List<GuestInfo> {
    if (sheet == null) return emptyList()
    val guests = mutableListOf<GuestInfo>()
    val formatter = DataFormatter()
    for (rowNum in 1..sheet.lastRowNum) {
        val row = sheet.getRow(rowNum)
        if (row != null) {
            val cabinCell = row.getCell(0)
            val cabin = if (cabinCell != null && cabinCell.cellType == CellType.NUMERIC) {
                cabinCell.numericCellValue.toLong().toString()
            } else {
                formatter.formatCellValue(cabinCell).trim()
            }
            val name = formatter.formatCellValue(row.getCell(1)).trim()
            if (cabin.isNotBlank() && name.isNotBlank()) {
                guests.add(GuestInfo(cabin, name))
            }
        }
    }
    return guests
}

private fun readSettingsSheet(sheet: Sheet?): Pair<List<String>, List<String>> {
    if (sheet == null) return Pair(emptyList(), emptyList())
    val photographers = mutableListOf<String>()
    val occasions = mutableListOf<String>()
    val formatter = DataFormatter()

    // Read Photographers from column A
    for (rowNum in 1..sheet.lastRowNum) {
        val row = sheet.getRow(rowNum)
        val photographer = row?.getCell(0)?.let { formatter.formatCellValue(it) }
        if (!photographer.isNullOrBlank()) {
            photographers.add(photographer)
        }
    }

    // Read Occasions from column B
    for (rowNum in 1..sheet.lastRowNum) {
        val row = sheet.getRow(rowNum)
        val occasion = row?.getCell(1)?.let { formatter.formatCellValue(it) }
        if (!occasion.isNullOrBlank()) {
            occasions.add(occasion)
        }
    }
    return Pair(photographers, occasions)
}

private fun readTimeSlotsSheet(sheet: Sheet?): List<TimeSlot> {
    if (sheet == null) return emptyList()
    val timeSlots = mutableListOf<TimeSlot>()
    val formatter = DataFormatter()
    val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.US)

    for (rowNum in 1..sheet.lastRowNum) {
        val row = sheet.getRow(rowNum)
        if (row != null) {
            val dateCell = row.getCell(0)
            val timeCell = row.getCell(1)

            if (dateCell != null && timeCell != null) {
                try {
                    val date = if (DateUtil.isCellDateFormatted(dateCell)) {
                        dateFormat.format(dateCell.dateCellValue)
                    } else {
                        formatter.formatCellValue(dateCell)
                    }
                    val time = formatter.formatCellValue(timeCell)

                    if (date.isNotBlank() && time.isNotBlank()) {
                        timeSlots.add(TimeSlot(date = date, time = time))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    return timeSlots
}

@Composable
private fun AddTimeSlotDialog(
    date: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    var time by remember { mutableStateOf("") }

    val timePickerDialog = TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hourOfDay)
                set(Calendar.MINUTE, minute)
            }
            time = SimpleDateFormat("hh:mm a", Locale.US).format(calendar.time)
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        false
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Time Slot for $date") },
        text = {
            Column {
                Button(
                    onClick = { timePickerDialog.show() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = if (time.isEmpty()) "Select Time" else time)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (time.isNotEmpty()) {
                        onSave(date, time)
                    }
                },
                enabled = time.isNotEmpty()
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

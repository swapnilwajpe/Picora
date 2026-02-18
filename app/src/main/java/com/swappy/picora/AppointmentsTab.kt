package com.swappy.picora

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.swappy.picora.ui.theme.PicoraTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AppointmentsTabContent(viewModel: AppointmentViewModel) {
    val appointments by viewModel.allAppointments.collectAsState(initial = emptyList())
    val allAppointmentsForExport by viewModel.allAppointmentsForExport.collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var filterExpanded by remember { mutableStateOf(false) }
    var sortOrder by remember { mutableStateOf(SortOrder.DEFAULT) }
    val focusRequester = remember { FocusRequester() }
    var selectedAppointmentId by remember { mutableStateOf<Int?>(null) }
    var showDeleteConfirmationId by remember { mutableStateOf<Int?>(null) }
    var showClearAllConfirmation by remember { mutableStateOf(false) }
    var isFabExpanded by remember { mutableStateOf(false) }
    var showQrCodeDialog by remember { mutableStateOf<Appointment?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val selectedAppointment = remember(selectedAppointmentId, appointments) {
        selectedAppointmentId?.let { id -> appointments.find { it.id == id } }
    }
    val appointmentToDelete = remember(showDeleteConfirmationId, appointments) {
        showDeleteConfirmationId?.let { id -> appointments.find { it.id == id } }
    }

    val filteredAppointments = remember(searchQuery, appointments, sortOrder) {
        val now = System.currentTimeMillis()
        val (upcoming, past) = appointments.partition { it.dateTime >= now }

        val sortedList = when (sortOrder) {
            SortOrder.CABIN_NUMBER -> appointments.sortedBy { it.cabinNumber }
            SortOrder.CREATION_DATE -> appointments.sortedByDescending { it.creationDate }
            SortOrder.APPOINTMENT_DATE -> appointments.sortedBy { it.dateTime }
            SortOrder.DEFAULT -> upcoming.sortedBy { it.dateTime } + past.sortedByDescending { it.dateTime }
        }

        if (searchQuery.isBlank()) {
            sortedList
        } else {
            sortedList.filter {
                it.cabinNumber.contains(searchQuery, ignoreCase = true) ||
                        it.guests.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!isSearchActive) {
                Column(horizontalAlignment = Alignment.End) {
                    AnimatedVisibility(visible = isFabExpanded) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Export", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(end = 8.dp))
                                FloatingActionButton(
                                    onClick = {
                                        isFabExpanded = false
                                        if (allAppointmentsForExport.isNotEmpty()) {
                                            coroutineScope.launch(Dispatchers.IO) {
                                                try {
                                                    val excelFile = createTempExcelFile(context, allAppointmentsForExport)
                                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", excelFile)

                                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                        type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                                        putExtra(Intent.EXTRA_STREAM, uri)
                                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    }

                                                    // Launch on the main thread
                                                    with(Dispatchers.Main) {
                                                        context.startActivity(Intent.createChooser(shareIntent, "Export Appointments"))
                                                    }
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar("Error exporting appointments: ${e.message}")
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Export Appointments")
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Clear All", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(end = 8.dp))
                                FloatingActionButton(
                                    onClick = {
                                        showClearAllConfirmation = true
                                        isFabExpanded = false
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear All")
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Filter", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(end = 8.dp))
                                FloatingActionButton(
                                    onClick = {
                                        filterExpanded = true
                                        isFabExpanded = false
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.FilterList, contentDescription = "Filter")
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Search", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(end = 8.dp))
                                FloatingActionButton(
                                    onClick = {
                                        isSearchActive = true
                                        isFabExpanded = false
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Search, contentDescription = "Search")
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    FloatingActionButton(
                        onClick = { isFabExpanded = !isFabExpanded },
                    ) {
                        Icon(
                            imageVector = if (isFabExpanded) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = if (isFabExpanded) "Close" else "Add"
                        )
                    }
                }
            }
        },
        bottomBar = {
            AnimatedVisibility(visible = isSearchActive) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search by cabin or guest") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .focusRequester(focusRequester),
                    trailingIcon = {
                        IconButton(onClick = {
                            isSearchActive = false
                            searchQuery = ""
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Close Search")
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            DropdownMenu(
                expanded = filterExpanded,
                onDismissRequest = { filterExpanded = false }
            ) {
                 DropdownMenuItem(text = { Text("Default") }, onClick = {
                    sortOrder = SortOrder.DEFAULT
                    filterExpanded = false
                })
                DropdownMenuItem(text = { Text("Cabin Number") }, onClick = {
                    sortOrder = SortOrder.CABIN_NUMBER
                    filterExpanded = false
                })
                DropdownMenuItem(text = { Text("Creation Date") }, onClick = {
                    sortOrder = SortOrder.CREATION_DATE
                    filterExpanded = false
                })
                DropdownMenuItem(text = { Text("Appointment Date") }, onClick = {
                    sortOrder = SortOrder.APPOINTMENT_DATE
                    filterExpanded = false
                })
            }
            LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
                items(filteredAppointments, key = { it.id }) { appointment ->
                    AppointmentCard(appointment = appointment) {
                        selectedAppointmentId = appointment.id
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
        LaunchedEffect(isSearchActive) {
            if (isSearchActive) {
                focusRequester.requestFocus()
            }
        }
    }

    if (selectedAppointment != null) {
        AppointmentActionsDialog(
            appointment = selectedAppointment,
            onDismiss = { selectedAppointmentId = null },
            onDelete = { showDeleteConfirmationId = it.id },
            onEdit = { viewModel.update(it) },
            onAddPhoto = { appointment, uri ->
                coroutineScope.launch(Dispatchers.IO) {
                    val savedUri = saveImageToInternalStorage(context, uri, appointment.id)
                    savedUri?.let { 
                        viewModel.update(appointment.copy(photoUri = it.toString()))
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

    if (showQrCodeDialog != null) {
        QrCodeDialog(appointment = showQrCodeDialog!!, onDismiss = { showQrCodeDialog = null })
    }

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

    if (showClearAllConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirmation = false },
            title = { Text("Clear All Appointments") },
            text = { Text("Are you sure you want to delete all appointments?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllAppointments()
                    showClearAllConfirmation = false
                }) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

enum class SortOrder {
    DEFAULT,
    CABIN_NUMBER,
    CREATION_DATE,
    APPOINTMENT_DATE
}

@Composable
fun AppointmentCard(appointment: Appointment, onClick: () -> Unit) {
    val imageSize = 160.dp
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .height(imageSize + 24.dp) // ensure card is tall enough for the larger image
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            if (appointment.photoUri != null) {
                AsyncImage(
                    model = Uri.parse(appointment.photoUri),
                    contentDescription = "Appointment Photo",
                    modifier = Modifier.size(imageSize),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.People,
                    contentDescription = "No Photo",
                    modifier = Modifier.size(imageSize)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "#${appointment.appointmentNumber} - Cabin: ${appointment.cabinNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Guest: ${appointment.guests}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(6.dp))
                val date = Date(appointment.dateTime)
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                val formattedDate = dateFormat.format(date)
                val formattedTime = timeFormat.format(date)
                Text(
                    text = "Date: $formattedDate, $formattedTime",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private fun createTempExcelFile(context: Context, appointments: List<Appointment>): File {
    val tempFile = File(context.cacheDir, "appointments.xlsx")
    FileOutputStream(tempFile).use { fos ->
        createAppointmentsExcel(appointments, fos)
    }
    return tempFile
}

private fun createAppointmentsExcel(appointments: List<Appointment>, outputStream: OutputStream) {
    try {
        XSSFWorkbook().use { workbook ->
            val sheet = workbook.createSheet("Appointments")

            val headers = listOf(
                "Appointment Number", "Cabin Number", "Guest", "Photographer",
                "Occasion", "Date", "Time", "Status"
            )

            val headerRow = sheet.createRow(0)
            headers.forEachIndexed { index, header ->
                headerRow.createCell(index).setCellValue(header)
            }

            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)

            appointments.forEachIndexed { index, appointment ->
                try {
                    val row = sheet.createRow(index + 1)
                    val appointmentDate = Date(appointment.dateTime)

                    row.createCell(0).setCellValue(appointment.appointmentNumber.toDouble())
                    row.createCell(1).setCellValue(appointment.cabinNumber ?: "")
                    row.createCell(2).setCellValue(appointment.guests ?: "")
                    row.createCell(3).setCellValue(appointment.photographer ?: "")
                    row.createCell(4).setCellValue(appointment.occasion ?: "")
                    row.createCell(5).setCellValue(dateFormat.format(appointmentDate))
                    row.createCell(6).setCellValue(timeFormat.format(appointmentDate))
                    row.createCell(7).setCellValue(if (appointment.isDeleted) "Deleted" else "Active")
                } catch (e: Exception) {
                    val errorRow = sheet.createRow(index + 1)
                    errorRow.createCell(0).setCellValue("Error in row: ${e.message}")
                }
            }

            workbook.write(outputStream)
        }
    } catch (e: Exception) {
        throw e
    }
}

@Preview(showBackground = true)
@Composable
fun AppointmentsTabContentPreview() {
    PicoraTheme {
        AppointmentsTabContent(viewModel = viewModel())
    }
}

@Preview(showBackground = true)
@Composable
fun AppointmentCardPreview() {
    PicoraTheme {
        AppointmentCard(
            appointment = Appointment(
                id = 1,
                cabinNumber = "123",
                guests = "John Doe",
                photographer = "Photographer 1",
                occasion = "Occasion 1",
                dateTime = System.currentTimeMillis(),
                appointmentNumber = 12345
            ),
            onClick = {}
        )
    }
}

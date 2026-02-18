package com.swappy.picora

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.swappy.picora.ui.theme.PicoraTheme
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import com.swappy.picora.R

@Composable
fun AppointmentActionsDialog(
    appointment: Appointment,
    onDismiss: () -> Unit,
    onDelete: (Appointment) -> Unit,
    onEdit: (Appointment) -> Unit,
    onAddPhoto: (Appointment, Uri) -> Unit,
    onShare: (Appointment) -> Unit,
    onShowQrCode: (Appointment) -> Unit
) {
    val context = LocalContext.current
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val getContent = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onAddPhoto(appointment, it) }
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            getContent.launch("image/*")
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (appointment.photoUri != null) {
                    AsyncImage(
                        model = Uri.parse(appointment.photoUri),
                        contentDescription = "Appointment Photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = "No Photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "#${appointment.appointmentNumber} - Cabin: ${appointment.cabinNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Guest: ${appointment.guests}")
                Text(text = "Photographer: ${appointment.photographer}")
                Text(text = "Occasion: ${appointment.occasion}")

                val date = Date(appointment.dateTime)
                val dayOfWeekFormat = SimpleDateFormat("EEEE", Locale.getDefault())
                val dayOfWeek = dayOfWeekFormat.format(date)

                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val formattedDate = dateFormat.format(date)

                val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                val formattedTime = timeFormat.format(date)

                Text(text = "Date: $dayOfWeek, $formattedDate")
                Text(text = "Time: $formattedTime")
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = {
                        val calendar = Calendar.getInstance()
                        calendar.timeInMillis = appointment.dateTime
                        val year = calendar.get(Calendar.YEAR)
                        val month = calendar.get(Calendar.MONTH)
                        val day = calendar.get(Calendar.DAY_OF_MONTH)
                        val hour = calendar.get(Calendar.HOUR_OF_DAY)
                        val minute = calendar.get(Calendar.MINUTE)

                        val datePickerDialog = DatePickerDialog(
                            context,
                            { _, selectedYear, selectedMonth, selectedDay ->
                                val newCalendar = Calendar.getInstance().apply {
                                    timeInMillis = appointment.dateTime
                                    set(Calendar.YEAR, selectedYear)
                                    set(Calendar.MONTH, selectedMonth)
                                    set(Calendar.DAY_OF_MONTH, selectedDay)
                                }
                                val timePickerDialog = TimePickerDialog(
                                    context,
                                    { _, selectedHour, selectedMinute ->
                                        newCalendar.set(Calendar.HOUR_OF_DAY, selectedHour)
                                        newCalendar.set(Calendar.MINUTE, selectedMinute)
                                        onEdit(appointment.copy(dateTime = newCalendar.timeInMillis))
                                        onDismiss()
                                    }, hour, minute, false
                                )
                                timePickerDialog.show()
                            }, year, month, day
                        )
                        datePickerDialog.show()
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { onDelete(appointment) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                    IconButton(onClick = {
                        if (context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
                            getContent.launch("image/*")
                        } else {
                            requestPermissionLauncher.launch(permission)
                        }
                    }) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = "Add Photo")
                    }
                    IconButton(onClick = { onShare(appointment) }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = { onShowQrCode(appointment) }) {
                        Icon(painterResource(id = R.drawable.ic_qr_code), contentDescription = "QR Code")
                    }
                }
            }
        }
    }
}

@Composable
fun QrCodeDialog(appointment: Appointment, onDismiss: () -> Unit) {
    val qrCodeContent = createIcsContent(appointment)
    val qrCodeBitmap = remember(qrCodeContent) {
        try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(qrCodeContent, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bmp.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bmp
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Scan to Add to Calendar", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                if (qrCodeBitmap != null) {
                    Image(
                        bitmap = qrCodeBitmap.asImageBitmap(),
                        contentDescription = "Appointment QR Code",
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                    )
                } else {
                    Text("Error generating QR Code")
                }
            }
        }
    }
}

fun createIcsContent(appointment: Appointment): String {
    val dateFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US)
    dateFormat.timeZone = TimeZone.getTimeZone("UTC")

    val startTime = dateFormat.format(Date(appointment.dateTime))
    // 30 min duration
    val endTime = dateFormat.format(Date(appointment.dateTime + 30 * 60 * 1000))

    return """
BEGIN:VCALENDAR
VERSION:2.0
PRODID:-//com.swappy.picora//EN
BEGIN:VEVENT
UID:${appointment.id}@picora.com
DTSTAMP:${dateFormat.format(Date())}
DTSTART:$startTime
DTEND:$endTime
SUMMARY:Photo Appointment with ${appointment.guests}
LOCATION:Cabin: ${appointment.cabinNumber}
DESCRIPTION:Occasion: ${appointment.occasion}\nPhotographer: ${appointment.photographer}
BEGIN:VALARM
TRIGGER:-PT30M
ACTION:DISPLAY
DESCRIPTION:Reminder
END:VALARM
END:VEVENT
END:VCALENDAR
""".trimIndent()
}

fun saveImageToInternalStorage(context: Context, uri: Uri, appointmentId: Int): Uri? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val file = File(context.cacheDir, "$appointmentId.jpg")
        val outputStream = FileOutputStream(file)
        inputStream?.copyTo(outputStream)
        androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Preview(showBackground = true)
@Composable
fun AppointmentActionsDialogPreview() {
    PicoraTheme {
        AppointmentActionsDialog(
            appointment = Appointment(
                id = 1,
                cabinNumber = "123",
                guests = "John Doe",
                photographer = "Photographer 1",
                occasion = "Occasion 1",
                dateTime = System.currentTimeMillis(),
                appointmentNumber = 12345
            ),
            onDismiss = {},
            onDelete = {},
            onEdit = {},
            onAddPhoto = { _, _ -> },
            onShare = {},
            onShowQrCode = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun QrCodeDialogPreview() {
    PicoraTheme {
        QrCodeDialog(
            appointment = Appointment(
                id = 1,
                cabinNumber = "123",
                guests = "John Doe",
                photographer = "Photographer 1",
                occasion = "Occasion 1",
                dateTime = System.currentTimeMillis(),
                appointmentNumber = 12345
            ),
            onDismiss = {}
        )
    }
}

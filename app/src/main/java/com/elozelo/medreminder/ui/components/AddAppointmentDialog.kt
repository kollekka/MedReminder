package com.elozelo.medreminder.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.elozelo.medreminder.R
import com.elozelo.medreminder.data.model.Appointment
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAppointmentDialog(
    appointment: Appointment? = null,
    onDismiss: () -> Unit,
    onConfirm: (Appointment) -> Unit
) {
    val isEditing = appointment != null

    var name by remember { mutableStateOf(appointment?.name ?: "") }
    var location by remember { mutableStateOf(appointment?.location ?: "") }
    var notes by remember { mutableStateOf(appointment?.notes ?: "") }

    val initialDateTime = appointment?.dateTime ?: System.currentTimeMillis()
    val initialCalendar = Calendar.getInstance().apply { timeInMillis = initialDateTime }

    var appointmentDate by remember { mutableStateOf(initialDateTime) }
    var appointmentTime by remember {
        mutableStateOf(Pair(initialCalendar.get(Calendar.HOUR_OF_DAY), initialCalendar.get(Calendar.MINUTE)))
    }
    var reminderEnabled by remember { mutableStateOf(appointment?.reminderEnabled ?: true) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    var nameError by remember { mutableStateOf(false) }
    var dateError by remember { mutableStateOf(false) }

    val dateTime by remember(appointmentDate, appointmentTime) {
        derivedStateOf {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = appointmentDate
                set(Calendar.HOUR_OF_DAY, appointmentTime.first)
                set(Calendar.MINUTE, appointmentTime.second)
                set(Calendar.SECOND, 0)
            }
            calendar.timeInMillis
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 700.dp),
        title = { Text(if (isEditing) stringResource(R.string.appointment_edit_dialog_title) else stringResource(R.string.appointment_add_dialog_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Nazwa wizyty
                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        if (it.length <= 16) {
                            name = it
                            nameError = it.isBlank()
                        }
                    },
                    label = { Text(stringResource(R.string.appointment_name)) },
                    isError = nameError,
                    supportingText = {
                        if (nameError) Text(stringResource(R.string.appointment_name_required)) else Text("${name.length}/16")
                    },
                    leadingIcon = { Icon(Icons.Default.Event, "Nazwa") },
                    placeholder = { Text(stringResource(R.string.appointment_name_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                HorizontalDivider()

                // Data i godzina
                OutlinedCard(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = if (dateError) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                        else MaterialTheme.colorScheme.surface
                    )
                ) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.appointment_date)) },
                        supportingContent = {
                            Column {
                                Text(formatDate(appointmentDate))
                                if (dateError) {
                                    Text(
                                        stringResource(R.string.error_invalid_date),
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        },
                        leadingContent = { Icon(Icons.Default.CalendarToday, "Data") }
                    )
                }
                OutlinedCard(onClick = { showTimePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.appointment_time)) },
                        supportingContent = { Text(formatTime(appointmentTime.first, appointmentTime.second)) },
                        leadingContent = { Icon(Icons.Default.AccessTime, "Godzina") }
                    )
                }

                HorizontalDivider()

                // Miejsce
                OutlinedTextField(
                    value = location,
                    onValueChange = {
                        if (it.length <= 100) {
                            location = it
                        }
                    },
                    label = { Text(stringResource(R.string.appointment_location_optional)) },
                    leadingIcon = { Icon(Icons.Default.LocationOn, "Miejsce") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = {
                        Text("${location.length}/100")
                    }
                )

                // Notatki
                OutlinedTextField(
                    value = notes,
                    onValueChange = {
                        if (it.length <= 500) {
                            notes = it
                        }
                    },
                    label = { Text(stringResource(R.string.appointment_notes_optional)) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, "Notatki") },
                    placeholder = { Text(stringResource(R.string.appointment_notes_placeholder)) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                    maxLines = 4,
                    supportingText = {
                        Text("${notes.length}/500")
                    }
                )

                // Przypomnienie
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.appointment_reminders), style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = reminderEnabled, onCheckedChange = { reminderEnabled = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    nameError = name.isBlank()

                    val now = System.currentTimeMillis()
                    dateError = dateTime < now - 60000

                    if (!nameError && !dateError) {
                        onConfirm(
                            Appointment(
                                id = appointment?.id ?: "",
                                userId = appointment?.userId ?: "",
                                name = name.trim(),
                                dateTime = dateTime,
                                location = location.trim(),
                                notes = notes.trim(),
                                reminderEnabled = reminderEnabled,
                                completed = appointment?.completed ?: false
                            )
                        )
                    }
                }
            ) {
                Text(if (isEditing) stringResource(R.string.save_changes) else stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismiss = { showDatePicker = false },
            onDateSelected = {
                appointmentDate = it
                dateError = false
            },
            initialDate = appointmentDate
        )
    }

    if (showTimePicker) {
        TimePickerDialog(
            onDismiss = { showTimePicker = false },
            onTimeSelected = { hour, minute ->
                appointmentTime = Pair(hour, minute)
                dateError = false
            },
            initialHour = appointmentTime.first,
            initialMinute = appointmentTime.second
        )
    }
}
package com.elozelo.medreminder.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.elozelo.medreminder.R
import com.elozelo.medreminder.data.model.DosageUnit
import com.elozelo.medreminder.data.model.Medication
import com.elozelo.medreminder.data.model.frequencyUnit
import java.text.SimpleDateFormat
import java.util.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMedicationDialog(
    medication: Medication? = null,
    onDismiss: () -> Unit,
    onConfirm: (Medication) -> Unit
) {
    val context = LocalContext.current
    val isEditing = medication != null

    var name by remember { mutableStateOf(medication?.name ?: "") }
    var dosage by remember { mutableStateOf<DosageUnit?>(medication?.dosage) }
    var quantity by remember { mutableStateOf(medication?.quantity?.toString() ?: "") }
    var initialStock by remember { mutableStateOf(medication?.initialQuantity?.toString() ?: "") }
    var frequency by remember { mutableStateOf<frequencyUnit?>(medication?.frequency) }
    var notes by remember { mutableStateOf(medication?.notes ?: "") }
    var startDate by remember { mutableStateOf(Date()) }
    var endDate by remember { mutableStateOf<Date?>(medication?.endDate) }
    var hasEndDate by remember { mutableStateOf(medication?.endDate != null) }
    var reminderEnabled by remember { mutableStateOf(medication?.reminderEnabled ?: true) }
    var reminderTimes by remember { mutableStateOf<List<String>>(medication?.reminderTimes ?: emptyList()) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    var nameError by remember { mutableStateOf(false) }
    var dosageError by remember { mutableStateOf(false) }
    var quantityError by remember { mutableStateOf(false) }
    var initialStockError by remember { mutableStateOf(false) }
    var frequencyError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 600.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.MedicalServices,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    if (isEditing) stringResource(R.string.medication_edit) else stringResource(R.string.medication_add),
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        if (it.length <= 16) {
                            name = it
                            nameError = false
                        }
                    },
                    label = { Text(stringResource(R.string.medication_name)) },
                    leadingIcon = {
                        Icon(Icons.Default.Medication, "Nazwa")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    isError = nameError,
                    supportingText = {
                        if (nameError) Text(stringResource(R.string.medication_field_required)) else Text("${name.length}/16")
                    },
                    singleLine = true
                )

                // Dawka i Ilość
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var expanded by remember { mutableStateOf(false) }

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = dosage?.getLocalizedName(context) ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("${stringResource(R.string.medication_dose)} *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .height(56.dp),
                            isError = dosageError,
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DosageUnit.values().forEach { unit ->
                                DropdownMenuItem(
                                    text = { Text(unit.getLocalizedName(context)) },
                                    onClick = {
                                        dosage = unit
                                        dosageError = false
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = {
                            val filtered = it.filter { char -> char.isDigit() }
                            val numValue = filtered.toIntOrNull() ?: 0
                            if (filtered.isEmpty() || numValue <= 100) {
                                quantity = filtered
                                quantityError = false
                            }
                        },
                        label = { Text("${stringResource(R.string.medication_quantity)} *") },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        isError = quantityError,
                        singleLine = true,
                    )
                }

                // Zapas początkowy
                OutlinedTextField(
                    value = initialStock,
                    onValueChange = {
                        val filtered = it.filter { char -> char.isDigit() }
                        val numValue = filtered.toIntOrNull() ?: 0
                        if (filtered.isEmpty() || numValue <= 10000) {
                            initialStock = filtered
                            initialStockError = false
                        }
                    },
                    label = { Text("${stringResource(R.string.medication_initial_stock)} *") },
                    leadingIcon = { Icon(Icons.Default.Inventory, "Zapas") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = initialStockError,
                    supportingText = {
                        if (initialStockError) Text(stringResource(R.string.medication_field_required)) else Text(stringResource(R.string.medication_initial_stock_hint))
                    },
                    singleLine = true
                )

                // Częstotliwość
                var freqExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = freqExpanded,
                    onExpandedChange = { freqExpanded = !freqExpanded },
                ) {
                    OutlinedTextField(
                        value = frequency?.getLocalizedName(context) ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("${stringResource(R.string.medication_frequency)} *") },
                        leadingIcon = { Icon(Icons.Default.Schedule, "Częstotliwość") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = freqExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        isError = frequencyError,
                        supportingText = {
                            if (frequencyError) Text(stringResource(R.string.medication_field_required)) else Text("")
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = freqExpanded,
                        onDismissRequest = { freqExpanded = false }
                    ) {
                        frequencyUnit.values().forEach { unit ->
                            DropdownMenuItem(
                                text = { Text(unit.getLocalizedName(context)) },
                                onClick = {
                                    frequency = unit
                                    frequencyError = false
                                    freqExpanded = false
                                }
                            )
                        }
                    }
                }

                // Data zakończenia
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.medication_end_date), style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = hasEndDate,
                        onCheckedChange = {
                            hasEndDate = it
                            if (!it) endDate = null
                        }
                    )
                }

                if (hasEndDate) {
                    OutlinedCard(
                        onClick = { showEndDatePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.medication_end_date_label)) },
                            supportingContent = {
                                Text(endDate?.let { formatDate(it) } ?: stringResource(R.string.medication_select_date))
                            },
                            leadingContent = {
                                Icon(Icons.Default.Event, "Data zakończenia")
                            },
                            trailingContent = {
                                Icon(Icons.Default.Edit, "Zmień")
                            }
                        )
                    }
                }

                Divider()

                // Przypomnienia
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Notifications, "Przypomnienia")
                        Text(stringResource(R.string.medication_reminders), style = MaterialTheme.typography.bodyLarge)
                    }
                    Switch(
                        checked = reminderEnabled,
                        onCheckedChange = { reminderEnabled = it }
                    )
                }

                if (reminderEnabled) {
                    // Lista godzin przypomnień
                    if (reminderTimes.isNotEmpty()) {
                        reminderTimes.forEachIndexed { index, time ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                ListItem(
                                    headlineContent = { Text(time) },
                                    leadingContent = {
                                        Icon(Icons.Default.AccessTime, "Godzina")
                                    },
                                    trailingContent = {
                                        IconButton(onClick = {
                                            reminderTimes = reminderTimes.toMutableList().apply {
                                                removeAt(index)
                                            }
                                        }) {
                                            Icon(Icons.Default.Delete, "Usuń")
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Przycisk dodawania godziny
                    FilledTonalButton(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, "Dodaj")
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.medication_add_reminder_time))
                    }
                }

                Divider()

                // Notatki
                OutlinedTextField(
                    value = notes,
                    onValueChange = {
                        if (it.length <= 500) {
                            notes = it
                        }
                    },
                    label = { Text(stringResource(R.string.medication_notes_optional)) },
                    leadingIcon = {
                        Icon(Icons.Default.Notes, "Notatki")
                    },
                    placeholder = { Text(stringResource(R.string.medication_notes_placeholder)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp),
                    maxLines = 4,
                    supportingText = {
                        Text("${notes.length}/500")
                    }
                )
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = {
                    // Walidacja
                    var hasError = false
                    if (name.isBlank()) {
                        nameError = true
                        hasError = true
                    }
                    if (dosage == null) {
                        dosageError = true
                        hasError = true
                    }
                    if (quantity.isBlank()) {
                        quantityError = true
                        hasError = true
                    }
                    if (initialStock.isBlank()) {
                        initialStockError = true
                        hasError = true
                    }
                    if (frequency == null) {
                        frequencyError = true
                        hasError = true
                    }

                    if (!hasError) {
                        val initialQty = initialStock.toInt()
                        onConfirm(
                            Medication(
                                id = medication?.id ?: "",
                                userId = medication?.userId ?: "",
                                name = name.trim(),
                                dosage = dosage!!,
                                quantity = quantity.toInt(),
                                initialQuantity = initialQty,
                                remainingQuantity = medication?.remainingQuantity ?: initialQty, // Przy edycji zachowaj, przy dodawaniu ustaw na initial
                                frequency = frequency!!,
                                endDate = if (hasEndDate) endDate else null,
                                notes = notes.trim(),
                                reminderEnabled = reminderEnabled,
                                reminderTimes = reminderTimes
                            )
                        )
                    }
                }
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (isEditing) stringResource(R.string.save_changes) else stringResource(R.string.medication_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )

    // Date Pickers
    if (showStartDatePicker) {
        DatePickerDialog(
            onDismiss = { showStartDatePicker = false },
            onDateSelected = { startDate = Date(it) },
            initialDate = startDate.time
        )
    }

    if (showEndDatePicker) {
        DatePickerDialog(
            onDismiss = { showEndDatePicker = false },
            onDateSelected = { endDate = Date(it) },
            initialDate = endDate?.time ?: Date().time
        )
    }

    // Time Picker
    if (showTimePicker) {
        TimePickerDialog(
            onDismiss = { showTimePicker = false },
            onTimeSelected = { hour, minute ->
                val timeString = formatTime(hour, minute)
                if (timeString !in reminderTimes) {
                    reminderTimes = reminderTimes + timeString
                }
            }
        )
    }
}

fun formatDate(date: Date): String {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    return dateFormat.format(date)
}

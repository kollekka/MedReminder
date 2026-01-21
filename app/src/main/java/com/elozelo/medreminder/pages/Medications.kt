package com.elozelo.medreminder.pages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.elozelo.medreminder.R
import com.elozelo.medreminder.data.model.DosageUnit
import com.elozelo.medreminder.data.model.Medication
import com.elozelo.medreminder.data.model.frequencyUnit
import com.elozelo.medreminder.ui.components.AddMedicationDialog
import com.elozelo.medreminder.ui.components.formatDate
import com.elozelo.medreminder.ui.theme.MedReminderTheme
import com.elozelo.medreminder.viewmodel.MedicationViewModel
import java.util.*

@Composable
fun MedicationsPage(
    viewModel: MedicationViewModel = viewModel(),
    paddingValues: PaddingValues = PaddingValues()
) {
    val medications by viewModel.medications.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var medicationToEdit by remember { mutableStateOf<Medication?>(null) }
    var showEmpty by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Filtrowanie i sortowanie leków
    val filteredMedications = remember(medications, showEmpty, searchQuery) {
        val baseList = if (showEmpty) {
            // Puste - leki ze stanem 0, sortowane alfabetycznie
            medications.filter { it.remainingQuantity == 0 }
                .sortedBy { it.name.lowercase() }
        } else {
            // Aktualne - leki z zapasem, sortowane po najbliższej przyszłej godzinie przypomnienia
            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(Calendar.MINUTE)
            val currentTimeInMinutes = currentHour * 60 + currentMinute

            medications.filter { it.remainingQuantity > 0 }
                .sortedBy { med ->
                    if (med.reminderTimes.isEmpty()) {
                        return@sortedBy Int.MAX_VALUE
                    }

                    // Jeśli jest lastTakenTime, znajdź następną godzinę po niej
                    // W przeciwnym razie znajdź następną godzinę po aktualnym czasie
                    val lastTakenMinutes = med.lastTakenTime?.let { timeString ->
                        val parts = timeString.split(":")
                        if (parts.size == 2) {
                            val hour = parts[0].toIntOrNull() ?: return@let null
                            val minute = parts[1].toIntOrNull() ?: return@let null
                            hour * 60 + minute
                        } else null
                    }

                    val referenceTime = lastTakenMinutes ?: currentTimeInMinutes

                    // Znajdź najbliższą przyszłą godzinę
                    val nextReminderTime = med.reminderTimes
                        .mapNotNull { timeString ->
                            val parts = timeString.split(":")
                            if (parts.size == 2) {
                                val hour = parts[0].toIntOrNull() ?: return@mapNotNull null
                                val minute = parts[1].toIntOrNull() ?: return@mapNotNull null
                                hour * 60 + minute
                            } else null
                        }
                        .filter { it > referenceTime } // Tylko godziny po ostatnim wzięciu/aktualnym czasie
                        .minOrNull()

                    // Jeśli jest przyszła godzina dzisiaj, użyj jej
                    // Jeśli nie, użyj pierwszej godziny z jutrzejszego dnia (dodaj 24h)
                    nextReminderTime ?: (med.reminderTimes
                        .mapNotNull { timeString ->
                            val parts = timeString.split(":")
                            if (parts.size == 2) {
                                val hour = parts[0].toIntOrNull() ?: return@mapNotNull null
                                val minute = parts[1].toIntOrNull() ?: return@mapNotNull null
                                hour * 60 + minute + (24 * 60) // Dodaj 24h
                            } else null
                        }
                        .minOrNull() ?: Int.MAX_VALUE)
                }
        }

        // Filtrowanie po wyszukiwaniu
        if (searchQuery.isBlank()) {
            baseList
        } else {
            baseList.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.medications_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                // Przycisk Puste/Aktualne
                OutlinedButton(
                    onClick = { showEmpty = !showEmpty },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (showEmpty) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = if (showEmpty) Icons.Default.Inventory2 else Icons.Default.RemoveCircleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (showEmpty) stringResource(R.string.medications_current) else stringResource(R.string.medications_empty))
                }
            }

            // Pole wyszukiwania
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text(stringResource(R.string.search_medications)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.search_clear))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading && medications.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filteredMedications.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        when {
                            searchQuery.isNotEmpty() -> stringResource(R.string.search_no_results)
                            showEmpty -> stringResource(R.string.medications_no_empty)
                            else -> stringResource(R.string.medications_no_added)
                        },
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredMedications) { medication ->
                        MedicationItem(
                            medication = medication,
                            onDelete = { viewModel.deleteMedication(medication.id) },
                            onClick = { medicationToEdit = medication },
                            onMarkTaken = { viewModel.markMedicationTaken(medication) },
                            onRefill = { viewModel.refillMedication(medication) },
                            onToggleNotifications = { med ->
                                val updatedMedication = if (med.reminderEnabled) {
                                    // Wyłącz powiadomienia, ale zachowaj godziny
                                    med.copy(reminderEnabled = false)
                                } else {
                                    // Włącz powiadomienia - użyj zapisanych godzin lub domyślnej
                                    if (med.reminderTimes.isEmpty()) {
                                        med.copy(reminderEnabled = true, reminderTimes = listOf("08:00"))
                                    } else {
                                        med.copy(reminderEnabled = true)
                                    }
                                }
                                viewModel.updateMedication(updatedMedication)
                            },
                            isEmpty = showEmpty
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .padding(bottom = 5.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.medications_add), tint = MaterialTheme.colorScheme.onPrimary)
        }
    }

    if (showAddDialog) {
        AddMedicationDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { medication ->
                viewModel.addMedication(medication)
                showAddDialog = false
            }
        )
    }

    medicationToEdit?.let { medication ->
        AddMedicationDialog(
            medication = medication,
            onDismiss = { medicationToEdit = null },
            onConfirm = { updatedMedication ->
                viewModel.updateMedication(updatedMedication)
                medicationToEdit = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MedicationItem(
    medication: Medication,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    onMarkTaken: () -> Unit = {},
    onRefill: () -> Unit = {},
    onToggleNotifications: (Medication) -> Unit = {},
    isEmpty: Boolean = false
) {
    val context = LocalContext.current

    // Low stock gdy zostały 2 lub mniej dawki
    val dosesRemaining = if (medication.quantity > 0) {
        medication.remainingQuantity / medication.quantity
    } else 0
    val isLowStock = dosesRemaining <= 2 && medication.remainingQuantity > 0

    // Lokalny licznik wzięć w tej sesji (resetowany przy zmianie medication.id)
    var localTakenCount by remember(medication.id) { mutableStateOf(0) }

    // Sprawdź czy lek był już dzisiaj wzięty
    val calendar = Calendar.getInstance()
    val today = String.format(
        "%04d-%02d-%02d",
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH) + 1,
        calendar.get(Calendar.DAY_OF_MONTH)
    )
    val alreadyTakenToday = medication.lastTakenDate == today && medication.dailyTakenCount > 0

    // Stan dialogów
    var showAlreadyTakenWarningDialog by remember { mutableStateOf(false) }
    var showConfirmTakeDialog by remember { mutableStateOf(false) }
    var showEmptyMedicationDialog by remember { mutableStateOf(false) }

    // Kolor paska bocznego zależny od stanu
    val accentColor = when {
        isLowStock -> MaterialTheme.colorScheme.error
        isEmpty -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.primary
    }

    // Stan rozwinięcia karty
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row {
            // Kolorowy pasek boczny
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .background(accentColor)
                    .align(Alignment.CenterVertically)
                    .then(
                        if (isExpanded) Modifier.fillMaxHeight()
                        else Modifier.height(56.dp)
                    )
            )

            Column(modifier = Modifier.weight(1f)) {
                // Nagłówek - klikalny do zwijania/rozwijania
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isExpanded = !isExpanded }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = medication.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (isLowStock) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Ikony po prawej stronie
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Ikona powiadomień (klikalny dzwonek)
                        var showDisableDialog by remember { mutableStateOf(false) }

                        IconButton(
                            onClick = {
                                if (medication.reminderEnabled) {
                                    showDisableDialog = true
                                } else {
                                    onToggleNotifications(medication)
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (medication.reminderEnabled) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                                contentDescription = null,
                                tint = if (medication.reminderEnabled)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Ikona strzałki
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) stringResource(R.string.collapse) else stringResource(R.string.expand),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )

                        // Dialog potwierdzenia
                        if (showDisableDialog) {
                            AlertDialog(
                                onDismissRequest = { showDisableDialog = false },
                                icon = {
                                    Icon(Icons.Default.NotificationsOff, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                title = {
                                    Text(
                                        stringResource(R.string.medication_notifications_disable_title),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                },
                                text = {
                                    Text(
                                        stringResource(R.string.medication_notifications_disable_message, medication.name),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                },
                                confirmButton = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        OutlinedButton(onClick = { showDisableDialog = false }) {
                                            Text(stringResource(R.string.cancel))
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        Button(onClick = {
                                            onToggleNotifications(medication)
                                            showDisableDialog = false
                                        }) {
                                            Text(stringResource(R.string.medication_notifications_disable_confirm))
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                // Sekcja środkowa - zwijalna, klikalna do edycji
                AnimatedVisibility(visible = isExpanded) {
                    Column {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onClick() }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Lewa kolumna - informacje
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Dawka
                                InfoRow(
                                    icon = Icons.Default.Medication,
                                    text = "${medication.quantity} ${medication.dosage.getLocalizedName(context)}",
                                    color = MaterialTheme.colorScheme.primary
                                )

                                // Stan magazynowy
                                if (medication.initialQuantity > 0) {
                                    InfoRow(
                                        icon = Icons.Default.Inventory,
                                        text = "${medication.remainingQuantity}/${medication.initialQuantity}",
                                        color = if (isLowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Częstotliwość
                                val frequencyText = when (medication.frequency) {
                                    frequencyUnit.SPECIFIC_DAYS -> {
                                        val dayNames = medication.customDaysOfWeek.map { dayNum ->
                                            when (dayNum) {
                                                1 -> stringResource(R.string.day_monday)
                                                2 -> stringResource(R.string.day_tuesday)
                                                3 -> stringResource(R.string.day_wednesday)
                                                4 -> stringResource(R.string.day_thursday)
                                                5 -> stringResource(R.string.day_friday)
                                                6 -> stringResource(R.string.day_saturday)
                                                7 -> stringResource(R.string.day_sunday)
                                                else -> ""
                                            }
                                        }.filter { it.isNotEmpty() }
                                        dayNames.joinToString(", ")
                                    }
                                    frequencyUnit.EVERY_X_DAYS -> {
                                        stringResource(R.string.frequency_every_x_days_format, medication.customIntervalDays)
                                    }
                                    else -> medication.frequency.getLocalizedName(context)
                                }
                                InfoRow(
                                    icon = Icons.Default.EventRepeat,
                                    text = frequencyText,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                // Data końcowa
                                medication.endDate?.let {
                                    InfoRow(
                                        icon = Icons.Default.CalendarToday,
                                        text = "${stringResource(R.string.medications_to)} ${formatDate(it)}",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Godziny przypomnień
                                if (medication.reminderTimes.isNotEmpty()) {
                                    InfoRow(
                                        icon = Icons.Default.Alarm,
                                        text = medication.reminderTimes.joinToString(", "),
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }

                            // Prawa strona - notatki
                            if (!medication.notes.isNullOrBlank()) {
                                var showNotesDialog by remember { mutableStateOf(false) }

                                OutlinedButton(
                                    onClick = { showNotesDialog = true },
                                    modifier = Modifier.align(Alignment.Top),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(stringResource(R.string.notes_title), style = MaterialTheme.typography.labelMedium)
                                }

                                if (showNotesDialog) {
                                    AlertDialog(
                                        onDismissRequest = { showNotesDialog = false },
                                        title = {
                                            Text("${stringResource(R.string.notes_title)} - ${medication.name}")
                                        },
                                        text = {
                                            Text(
                                                text = medication.notes,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                        },
                                        confirmButton = {
                                            TextButton(onClick = { showNotesDialog = false }) {
                                                Text(stringResource(R.string.close))
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        // Przyciski akcji - stopka
                        var showDeleteDialog by remember { mutableStateOf(false) }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (isEmpty) {
                                Button(
                                    onClick = onRefill,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.medications_refill))
                                }
                            } else {
                                Button(
                                    onClick = {
                                        if (alreadyTakenToday) {
                                            showAlreadyTakenWarningDialog = true
                                        } else {
                                            showConfirmTakeDialog = true
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.medications_mark_taken))
                                }
                            }

                            OutlinedButton(
                                onClick = { showDeleteDialog = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }

                        // Dialog potwierdzenia usunięcia
                        if (showDeleteDialog) {
                            AlertDialog(
                                onDismissRequest = { showDeleteDialog = false },
                                icon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(32.dp)
                                    )
                                },
                                title = {
                                    Text(
                                        stringResource(R.string.medication_delete_title),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                },
                                text = {
                                    Text(
                                        stringResource(R.string.medication_delete_message, medication.name),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                },
                                confirmButton = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        OutlinedButton(onClick = { showDeleteDialog = false }) {
                                            Text(stringResource(R.string.cancel))
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        Button(
                                            onClick = {
                                                onDelete()
                                                showDeleteDialog = false
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                        ) {
                                            Text(stringResource(R.string.medication_delete_confirm))
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog standardowy - potwierdzenie wzięcia leku
    if (showConfirmTakeDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmTakeDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            icon = {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Medication,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            },
            title = {
                Text(
                    medication.name,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    stringResource(R.string.home_confirm_take_medication),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    OutlinedButton(onClick = { showConfirmTakeDialog = false }) {
                        Text(stringResource(R.string.home_no))
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {

                            val effectiveRemainingPieces = medication.remainingQuantity - (localTakenCount * medication.quantity)
                            // Ile SZTUK zostanie po tej dawce
                            val remainingAfterTaking = effectiveRemainingPieces - medication.quantity
                            // Ostatnia dawka = po tej dawce nie zostanie ani jedna sztuka
                            val willBeEmpty = remainingAfterTaking <= 0

                            localTakenCount++
                            onMarkTaken()
                            showConfirmTakeDialog = false

                            if (willBeEmpty) {
                                showEmptyMedicationDialog = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.home_yes))
                    }
                }
            }
        )
    }

    // Dialog ostrzegawczy - lek już dzisiaj wzięty
    if (showAlreadyTakenWarningDialog) {
        AlertDialog(
            onDismissRequest = { showAlreadyTakenWarningDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            icon = {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp)
                    )
                }
            },
            title = {
                Text(
                    stringResource(R.string.home_already_taken_warning_title),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    stringResource(R.string.home_already_taken_warning_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    OutlinedButton(onClick = { showAlreadyTakenWarningDialog = false }) {
                        Text(stringResource(R.string.home_no))
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val effectiveRemainingPieces = medication.remainingQuantity - (localTakenCount * medication.quantity)
                            val remainingAfterTaking = effectiveRemainingPieces - medication.quantity
                            val willBeEmpty = remainingAfterTaking <= 0

                            localTakenCount++
                            onMarkTaken()
                            showAlreadyTakenWarningDialog = false

                            if (willBeEmpty) {
                                showEmptyMedicationDialog = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.home_yes))
                    }
                }
            }
        )
    }

    // Dialog - lek się skończył
    if (showEmptyMedicationDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyMedicationDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            icon = {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            },
            title = {
                Text(
                    stringResource(R.string.medication_empty_title),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    stringResource(R.string.medication_empty_message, medication.name),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showEmptyMedicationDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.medication_archive))
                    }
                    OutlinedButton(
                        onClick = {
                            onDelete()
                            showEmptyMedicationDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.medication_delete))
                    }
                }
            }
        )
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    text: String,
    color: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = color
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MedicationItemPreview() {
    MedReminderTheme {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(16.dp)) {
            MedicationItem(
                medication = Medication(
                    name = "Witamina D",
                    dosage = DosageUnit.PILLS,
                    quantity = 1,
                    frequency = frequencyUnit.DAILY,
                    endDate = Date(),
                    reminderTimes = listOf("09:00", "21:00")
                ),
                onDelete = {},
                onClick = {}
            )
            MedicationItem(
                medication = Medication(
                    name = "Paracetamol",
                    dosage = DosageUnit.PILLS,
                    quantity = 2,
                    frequency = frequencyUnit.WEEKLY,
                    endDate = null,
                    reminderTimes = emptyList()
                ),
                onDelete = {},
                onClick = {}
            )
        }
    }
}

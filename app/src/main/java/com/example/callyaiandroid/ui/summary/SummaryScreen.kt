package com.example.callyaiandroid.ui.summary

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    vm: SummaryViewModel,
    token: String,
    showSnack: (String) -> Unit
) {
    val st by vm.st.collectAsState()
    var editingItem by remember { mutableStateOf<FoodLogItem?>(null) }

    LaunchedEffect(token) { vm.loadAll(token) }
    LaunchedEffect(st.error) { st.error?.let(showSnack) }
    LaunchedEffect(st.message) {
        st.message?.let {
            showSnack(it)
            vm.clearMessage()
        }
    }

    val listState = rememberLazyListState()
    val formatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }
    val periodLabel = remember(st.periodStart, st.periodEnd) {
        if (st.periodStart == st.periodEnd) {
            st.periodStart.format(formatter)
        } else {
            "${st.periodStart.format(formatter)} – ${st.periodEnd.format(formatter)}"
        }
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showRangePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = st.periodStart.toEpochMilli()
    )

    val rangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = st.periodStart.toEpochMilli(),
        initialSelectedEndDateMillis = st.periodEnd.toEpochMilli()
    )

    LaunchedEffect(st.periodStart) {
        datePickerState.selectedDateMillis = st.periodStart.toEpochMilli()
    }

    LaunchedEffect(st.periodStart, st.periodEnd) {
        rangePickerState.setSelection(st.periodStart.toEpochMilli(), st.periodEnd.toEpochMilli())
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp).navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Suvestinė", style = MaterialTheme.typography.titleLarge)

        SingleChoiceSegmentedButtonRow {
            SummaryPeriodType.values().forEachIndexed { index, type ->
                SegmentedButton(
                    selected = st.periodType == type,
                    onClick = { vm.setPeriodType(type) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index,
                        SummaryPeriodType.values().size
                    )
                ) {
                    Text(
                        when (type) {
                            SummaryPeriodType.DAY -> "Diena"
                            SummaryPeriodType.WEEK -> "Savaitė"
                            SummaryPeriodType.MONTH -> "Mėnuo"
                        }
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(onClick = { vm.shiftPeriod(forward = false) }) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = "Ankstesnis laikotarpis")
            }

            TextButton(
                modifier = Modifier.weight(1f),
                onClick = {
                    if (st.periodType == SummaryPeriodType.DAY) showDatePicker =
                        true else showRangePicker = true
                }
            ) {
                Text(periodLabel, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }

            IconButton(onClick = { vm.shiftPeriod(forward = true) }) {
                Icon(Icons.Filled.ChevronRight, contentDescription = "Kitas laikotarpis")
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Kalorijos: ${st.caloriesTotal} kcal", style = MaterialTheme.typography.bodyMedium)
            Text(
                "Baltymai: ${st.proteinTotal.formatAsGrams()} g  Riebalai: ${st.fatTotal.formatAsGrams()} g  Angliavandeniai: ${st.carbsTotal.formatAsGrams()} g",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        val groups = remember(st.periodGroups) { st.periodGroups.sortedByDescending { it.date } }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            if (groups.isEmpty()) {
                item("empty-state") {
                    Text(
                        "Pasirinktam laikotarpiui įrašų nėra.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                groups.forEach { group ->
                    item("header-${group.date}") {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                group.date.format(formatter),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Kalorijos: ${group.dayTotal} kcal",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "B: ${group.proteinTotal.formatAsGrams()} g  R: ${group.fatTotal.formatAsGrams()} g  A: ${group.carbsTotal.formatAsGrams()} g",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    items(
                        items = group.items,
                        key = { it.id }
                    ) { item ->
                        SummaryEntryCard(item = item, onEdit = { editingItem = item })
                    }
                }
            }
        }

        editingItem?.let { item ->
            var gramsText by remember(item.id) { mutableStateOf(item.grams.toString()) }
            var quantityText by remember(item.id) { mutableStateOf(item.quantity.toString()) }
            var gramsError by remember(item.id) { mutableStateOf(false) }
            var quantityError by remember(item.id) { mutableStateOf(false) }
            var submitted by remember(item.id) { mutableStateOf(false) }
            val updating = st.updatingItemId == item.id

            LaunchedEffect(st.updatingItemId) {
                if (submitted && st.updatingItemId != item.id) {
                    editingItem = null
                    submitted = false
                }
            }

            AlertDialog(
                onDismissRequest = { if (!updating) editingItem = null },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val g = gramsText.toIntOrNull()
                            val q = quantityText.toIntOrNull()
                            gramsError = g == null || g <= 0
                            quantityError = q == null || q <= 0
                            if (!gramsError && !quantityError) {
                                submitted = true
                                vm.updateItem(token, item.id, g!!, q!!)
                            }
                        },
                        enabled = !updating
                    ) {
                        Text(if (updating) "Saugoma..." else "Išsaugoti")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { if (!updating) editingItem = null }) {
                        Text("Atšaukti")
                    }
                },
                title = { Text("Redaguoti įrašą") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = gramsText,
                            onValueChange = {
                                gramsText = it.filter { ch -> ch.isDigit() }
                                if (gramsError) gramsError = false
                            },
                            label = { Text("Gramų kiekis") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = gramsError,
                            supportingText = if (gramsError) {
                                { Text("Įveskite teigiamą skaičių") }
                            } else null
                        )
                        OutlinedTextField(
                            value = quantityText,
                            onValueChange = {
                                quantityText = it.filter { ch -> ch.isDigit() }
                                if (quantityError) quantityError = false
                            },
                            label = { Text("Porcijų kiekis") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = quantityError,
                            supportingText = if (quantityError) {
                                { Text("Įveskite teigiamą skaičių") }
                            } else null
                        )
                    }
                }
            )
        }
    }
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        val date = millis.toLocalDate()
                        vm.selectPeriod(date, date)
                    }
                    showDatePicker = false
                }) { Text("Patvirtinti") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Atšaukti") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showRangePicker) {
        DatePickerDialog(
            onDismissRequest = { showRangePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val start = rangePickerState.selectedStartDateMillis
                    val end = rangePickerState.selectedEndDateMillis ?: start
                    if (start != null) {
                        vm.selectPeriod(start.toLocalDate(), (end ?: start).toLocalDate())
                    }
                    showRangePicker = false
                }) { Text("Patvirtinti") }
            },
            dismissButton = { TextButton(onClick = { showRangePicker = false }) { Text("Atšaukti") } }
        ) {
            DateRangePicker(state = rangePickerState)
        }
    }
}

@Composable
private fun SummaryEntryCard(item: FoodLogItem, onEdit: () -> Unit) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    ElevatedCard {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(item.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(item.consumedAt.format(timeFormatter), style = MaterialTheme.typography.bodyMedium)
            }
            Text("Gramai: ${item.grams} g", style = MaterialTheme.typography.bodyMedium)
            Text("Kiekis: ${item.quantity}", style = MaterialTheme.typography.bodyMedium)
            Text("Kalorijos: ${item.portionCalories} kcal", style = MaterialTheme.typography.bodyMedium)
            Text(
                "B: ${item.totalProtein.formatAsGrams()} g  R: ${item.totalFat.formatAsGrams()} g  A: ${item.totalCarbs.formatAsGrams()} g",
                style = MaterialTheme.typography.bodySmall
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onEdit) { Text("Redaguoti") }

            }
        }
    }
}
private fun LocalDate.toEpochMilli(zoneId: ZoneId = ZoneId.systemDefault()): Long =
    this.atStartOfDay(zoneId).toInstant().toEpochMilli()
private fun Long.toLocalDate(zoneId: ZoneId = ZoneId.systemDefault()): LocalDate =
    Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate()
private fun Double.formatAsGrams(): String = String.format("%.1f", this)
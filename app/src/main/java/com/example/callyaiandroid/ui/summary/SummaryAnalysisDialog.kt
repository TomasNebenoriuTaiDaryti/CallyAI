package com.example.callyaiandroid.ui.summary

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DateRangePickerState
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryAnalysisDialog(
    groups: List<DayGroup>,
    onDismiss: () -> Unit
) {
    val baseDate = remember(groups) { groups.maxOfOrNull { it.date } ?: LocalDate.now() }
    var periodType by remember(baseDate) { mutableStateOf(SummaryPeriodType.MONTH) }
    var periodStart by remember(baseDate) { mutableStateOf(baseDate.withDayOfMonth(1)) }
    var periodEnd by remember(baseDate) { mutableStateOf(baseDate.endOfMonth()) }
    var showRangePicker by remember { mutableStateOf(false) }

    val formatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    val periodLabel = remember(periodStart, periodEnd) {
        if (periodStart == periodEnd) {
            periodStart.format(formatter)
        } else {
            "${periodStart.format(formatter)} – ${periodEnd.format(formatter)}"
        }
    }

    val rangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = periodStart.toEpochMilli(),
        initialSelectedEndDateMillis = periodEnd.toEpochMilli()
    )

    LaunchedEffect(periodStart, periodEnd) {
        rangePickerState.setSelection(periodStart.toEpochMilli(), periodEnd.toEpochMilli())
    }

    val filteredGroups = remember(groups, periodStart, periodEnd) {
        groups.filter { !it.date.isBefore(periodStart) && !it.date.isAfter(periodEnd) }
    }
    val entries = remember(filteredGroups) { filteredGroups.flatMap { it.items } }
    val dayCount = remember(periodStart, periodEnd) {
        ChronoUnit.DAYS.between(periodStart, periodEnd).toInt().coerceAtLeast(0) + 1
    }
    val totalCalories = remember(filteredGroups) { filteredGroups.sumOf { it.dayTotal } }
    val averageCalories = remember(totalCalories, dayCount) {
        if (dayCount > 0) totalCalories.toDouble() / dayCount else 0.0
    }
    val proteinTotal = remember(filteredGroups) { filteredGroups.sumOf { it.proteinTotal } }
    val fatTotal = remember(filteredGroups) { filteredGroups.sumOf { it.fatTotal } }
    val carbsTotal = remember(filteredGroups) { filteredGroups.sumOf { it.carbsTotal } }
    val macroSum = remember(proteinTotal, fatTotal, carbsTotal) {
        proteinTotal + fatTotal + carbsTotal
    }
    val macroDistribution = remember(proteinTotal, fatTotal, carbsTotal, macroSum) {
        listOf(
            Triple("Baltymai", proteinTotal, if (macroSum > 0) proteinTotal / macroSum * 100 else 0.0),
            Triple("Riebalai", fatTotal, if (macroSum > 0) fatTotal / macroSum * 100 else 0.0),
            Triple("Angliavandeniai", carbsTotal, if (macroSum > 0) carbsTotal / macroSum * 100 else 0.0)
        )
    }

    val topProducts = remember(entries) {
        entries.groupingBy { it.name }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(3)
    }

    val hourCounts = remember(entries) {
        entries.groupingBy { it.consumedAt.hour }.eachCount()
    }
    val peakHour = remember(hourCounts) { hourCounts.maxByOrNull { it.value }?.key }
    val earliestMeal = remember(entries) { entries.minByOrNull { it.consumedAt }?.consumedAt?.toLocalTime() }
    val latestMeal = remember(entries) { entries.maxByOrNull { it.consumedAt }?.consumedAt?.toLocalTime() }

    fun recalcPeriod(newType: SummaryPeriodType, anchor: LocalDate = periodStart): Pair<LocalDate, LocalDate> {
        val start = when (newType) {
            SummaryPeriodType.DAY -> anchor
            SummaryPeriodType.WEEK -> anchor.startOfWeek()
            SummaryPeriodType.MONTH -> anchor.withDayOfMonth(1)
        }
        val end = when (newType) {
            SummaryPeriodType.DAY -> start
            SummaryPeriodType.WEEK -> start.plusDays(6)
            SummaryPeriodType.MONTH -> start.endOfMonth()
        }
        return start to end
    }

    fun shiftPeriod(forward: Boolean) {
        val delta = if (forward) 1L else -1L
        val newStart = when (periodType) {
            SummaryPeriodType.DAY -> periodStart.plusDays(delta)
            SummaryPeriodType.WEEK -> periodStart.plusWeeks(delta)
            SummaryPeriodType.MONTH -> periodStart.plusMonths(delta)
        }
        val (_, newEnd) = recalcPeriod(periodType, newStart)
        periodStart = newStart
        periodEnd = newEnd
    }

    Dialog(onDismissRequest = onDismiss) {
        androidx.compose.material3.Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.95f),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Mitybos analizė") },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Outlined.ArrowBack, contentDescription = "Grįžti")
                            }
                        }
                    )
                }
            ) { innerPadding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                "Pasirinkite laikotarpį",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            SingleChoiceSegmentedButtonRow {
                                SummaryPeriodType.values().forEachIndexed { index, type ->
                                    SegmentedButton(
                                        selected = periodType == type,
                                        onClick = {
                                            periodType = type
                                            val (start, end) = recalcPeriod(type, periodStart)
                                            periodStart = start
                                            periodEnd = end
                                        },
                                        shape = SegmentedButtonDefaults.itemShape(index, SummaryPeriodType.values().size)
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
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { shiftPeriod(false) }) {
                                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Ankstesnis laikotarpis")
                                }
                                TextButton(
                                    modifier = Modifier.weight(1f),
                                    onClick = { showRangePicker = true }
                                ) {
                                    Text(periodLabel, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                                }
                                IconButton(onClick = { shiftPeriod(true) }) {
                                    Icon(
                                        Icons.Outlined.ArrowBack,
                                        contentDescription = "Kitas laikotarpis",
                                        modifier = Modifier.rotate(180f)
                                    )
                                }
                            }
                        }
                    }

                    if (entries.isEmpty()) {
                        item {
                            Text(
                                "Pasirinktam laikotarpiui duomenų nėra.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        item {
                            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        "Vidutinės kcal per dieną",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        "${averageCalories.toInt()} kcal/d.",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        "Per laikotarpį: $totalCalories kcal per $dayCount d.",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }

                        item {
                            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        "Makroelementų pasiskirstymas",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    macroDistribution.forEach { (label, grams, percent) ->
                                        Text(
                                            "$label: ${grams.formatAsGrams()} g (${percent.formatAsPercent()}%)",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        "Dažniausi produktai",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (topProducts.isEmpty()) {
                                        Text(
                                            "Nėra pakankamai duomenų",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    } else {
                                        topProducts.forEachIndexed { index, entry ->
                                            Text(
                                                "${index + 1}. ${entry.key} (${entry.value} kart.)",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        "Valgymo laiko įpročiai",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        peakHour?.let {
                                            val start = LocalTime.of(it, 0)
                                            val end = start.plusHours(1)
                                            "Dažniausiai valgoma tarp ${start.format(timeFormatter)} – ${end.format(timeFormatter)}"
                                        } ?: "Nėra pakankamai duomenų",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    earliestMeal?.let {
                                        Text(
                                            "Ankstyviausias valgis: ${it.format(timeFormatter)}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    latestMeal?.let {
                                        Text(
                                            "Vėliausias valgis: ${it.format(timeFormatter)}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRangePicker) {
        DateRangePickerDialog(
            state = rangePickerState,
            onDismiss = { showRangePicker = false },
            onConfirm = {
                val start = rangePickerState.selectedStartDateMillis?.toLocalDate()
                val end = rangePickerState.selectedEndDateMillis?.toLocalDate()
                if (start != null) {
                    periodStart = start
                    periodEnd = end ?: start
                    periodType = when {
                        periodStart == periodEnd -> SummaryPeriodType.DAY
                        ChronoUnit.DAYS.between(periodStart, periodEnd) == 6L -> SummaryPeriodType.WEEK
                        periodStart.dayOfMonth == 1 && periodEnd == periodStart.endOfMonth() -> SummaryPeriodType.MONTH
                        else -> periodType
                    }
                }
                showRangePicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangePickerDialog(
    state: DateRangePickerState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    androidx.compose.material3.DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onConfirm) { Text("Patvirtinti") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Atšaukti") } }
    ) {
        DateRangePicker(state = state)
    }
}

private fun LocalDate.startOfWeek(): LocalDate {
    val dow = this.dayOfWeek.value
    return this.minusDays(((dow + 6) % 7).toLong())
}

private fun LocalDate.endOfMonth(): LocalDate = this.withDayOfMonth(this.lengthOfMonth())

private fun LocalDate.toEpochMilli(zoneId: ZoneId = ZoneId.systemDefault()): Long =
    this.atStartOfDay(zoneId).toInstant().toEpochMilli()

private fun Long.toLocalDate(zoneId: ZoneId = ZoneId.systemDefault()): LocalDate =
    java.time.Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate()

private fun Double.formatAsGrams(): String = String.format("%.1f", this)

private fun Double.formatAsPercent(): String = String.format("%.1f", this)
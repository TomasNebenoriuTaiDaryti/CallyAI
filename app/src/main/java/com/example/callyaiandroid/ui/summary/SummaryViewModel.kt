package com.example.callyaiandroid.ui.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.callyaiandroid.network.RetrofitClient
import com.example.callyaiandroid.network.dto.FoodLogDtos
import com.example.callyaiandroid.network.dto.FoodLogUpdateReq
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.example.callyaiandroid.data.PrefsGateway
import org.json.JSONArray
import java.time.temporal.ChronoUnit

data class FoodLogItem(
    val id: Long,
    val name: String,
    val portionCalories: Int,
    val caloriesPer100g: Int,
    val grams: Int,
    val quantity: Int,
    val totalCalories: Int,
    val consumedAt: LocalDateTime,
    val protein: Double,
    val fat: Double,
    val carbs: Double,
    val totalProtein: Double,
    val totalFat: Double,
    val totalCarbs: Double
)

data class DayGroup(
    val date: LocalDate,
    val items: List<FoodLogItem>,
    val dayTotal: Int,
    val proteinTotal: Double,
    val fatTotal: Double,
    val carbsTotal: Double
)
enum class SummaryPeriodType { DAY, WEEK, MONTH }
enum class CalorieSortOrder { NONE, ASCENDING, DESCENDING }
data class SummaryState(
    val loading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val updatingItemId: Long? = null,
    val groups: List<DayGroup> = emptyList(),
    val periodType: SummaryPeriodType = SummaryPeriodType.DAY,
    val periodStart: LocalDate = LocalDate.now(),
    val periodEnd: LocalDate = LocalDate.now(),
    val deletingItemId: Long? = null,
    val dailyGoal: Int = 2000,
    val searchQuery: String = "",
    val calorieSort: CalorieSortOrder = CalorieSortOrder.NONE
) {
    val periodGroups: List<DayGroup>
        get() = groups.filter { !it.date.isBefore(periodStart) && !it.date.isAfter(periodEnd) }
    val filteredGroups: List<DayGroup>
        get() {
            val query = searchQuery.trim().lowercase()

            return periodGroups.mapNotNull { group ->
                var filteredItems = group.items

                if (query.isNotEmpty()) {
                    filteredItems = filteredItems.filter { it.name.lowercase().contains(query) }
                }

                filteredItems = when (calorieSort) {
                    CalorieSortOrder.NONE -> filteredItems
                    CalorieSortOrder.ASCENDING -> filteredItems.sortedBy { it.totalCalories }
                    CalorieSortOrder.DESCENDING -> filteredItems.sortedByDescending { it.totalCalories }
                }

                if (filteredItems.isEmpty()) return@mapNotNull null

                group.copy(
                    items = filteredItems,
                    dayTotal = filteredItems.sumOf { it.totalCalories },
                    proteinTotal = filteredItems.sumOf { it.totalProtein },
                    fatTotal = filteredItems.sumOf { it.totalFat },
                    carbsTotal = filteredItems.sumOf { it.totalCarbs }
                )
            }
        }
    val caloriesTotal: Int get() = periodGroups.sumOf { it.dayTotal }
    val proteinTotal: Double get() = periodGroups.sumOf { it.proteinTotal }
    val fatTotal: Double get() = periodGroups.sumOf { it.fatTotal }
    val periodDayCount: Int
        get() {
            val days = ChronoUnit.DAYS.between(periodStart, periodEnd)
            return (if (days < 0) 0 else days.toInt()) + 1
        }
    val totalGoal: Int get() = dailyGoal * periodDayCount
    val caloriesRemaining: Int get() = (totalGoal - caloriesTotal).coerceAtLeast(0)
    val caloriesOver: Int get() = (caloriesTotal - totalGoal).coerceAtLeast(0)
    val caloriesProgress: Float
        get() = if (totalGoal <= 0) 0f else caloriesTotal.toFloat() / totalGoal.toFloat()
    val carbsTotal: Double get() = periodGroups.sumOf { it.carbsTotal }
}

class SummaryViewModel(private val prefs: PrefsGateway) : ViewModel() {

    private val _st = MutableStateFlow(SummaryState())
    val st: StateFlow<SummaryState> = _st

    private val formats = listOf(
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ISO_LOCAL_DATE_TIME
    )

    init {
        viewModelScope.launch {
            launch {
                prefs.summaryCacheFlow.collect { cached ->
                    if (cached.isNullOrBlank()) return@collect
                    val groups = parseCachedGroups(cached)
                    if (groups.isEmpty()) return@collect
                    val firstDate = groups.first().date
                    _st.value = _st.value.copy(
                        groups = groups,
                        periodStart = firstDate,
                        periodEnd = firstDate
                    )
                }
            }
            launch {
                prefs.kcalFlow.collect { kcal ->
                    if (kcal > 0) {
                        _st.value = _st.value.copy(dailyGoal = kcal)
                    }
                }
            }
        }
    }
    private fun parseDateTime(s: String): LocalDateTime {
        for (f in formats) {
            try { return LocalDateTime.parse(s.replace('T', ' '), f) } catch (_: Exception) {}
        }
        return LocalDateTime.parse(s.replace('T', ' ').substring(0, 19), formats[1])
    }

    fun loadAll(token: String) {
        viewModelScope.launch {
            try {
                _st.value = _st.value.copy(loading = true, error = null, message = null)

                val list: List<FoodLogDtos> = RetrofitClient.api.diaryAll("Bearer $token")

                val items = list.map { dto ->
                    val dt = parseDateTime(dto.consumedAt)
                    val total = dto.totalCalories ?: (dto.portionCalories * dto.quantity)
                    FoodLogItem(
                        id = dto.id,
                        name = dto.name,
                        portionCalories = dto.portionCalories,
                        caloriesPer100g = dto.caloriesPer100g,
                        grams = dto.grams,
                        quantity = dto.quantity,
                        totalCalories = total,
                        consumedAt = dt,
                        protein = dto.protein,
                        fat = dto.fat,
                        carbs = dto.carbs,
                        totalProtein = dto.totalProtein,
                        totalFat = dto.totalFat,
                        totalCarbs = dto.totalCarbs
                    )
                }

                val grouped = items
                    .groupBy { it.consumedAt.toLocalDate() }
                    .toSortedMap(compareByDescending { it })
                    .map { (date, dayItems) ->
                        val sorted = dayItems.sortedBy { it.consumedAt }
                        DayGroup(
                            date = date,
                            items = sorted,
                            dayTotal = sorted.sumOf { it.totalCalories },
                            proteinTotal = sorted.sumOf { it.totalProtein },
                            fatTotal = sorted.sumOf { it.totalFat },
                            carbsTotal = sorted.sumOf { it.totalCarbs }
                        )
                    }

                saveCache(grouped)

                val current = _st.value
                val newState = if (current.groups.isEmpty()) {
                    val baseDate = grouped.firstOrNull()?.date ?: current.periodStart
                    val start = when (current.periodType) {
                        SummaryPeriodType.DAY -> baseDate
                        SummaryPeriodType.WEEK -> baseDate.startOfWeek()
                        SummaryPeriodType.MONTH -> baseDate.withDayOfMonth(1)
                    }
                    val end = when (current.periodType) {
                        SummaryPeriodType.DAY -> start
                        SummaryPeriodType.WEEK -> start.plusDays(6)
                        SummaryPeriodType.MONTH -> start.endOfMonth()
                    }
                    current.copy(
                        loading = false,
                        groups = grouped,
                        updatingItemId = null,
                        deletingItemId = null,
                        periodStart = start,
                        periodEnd = end
                    )
                } else {
                    current.copy(
                        loading = false,
                        groups = grouped,
                        updatingItemId = null,
                        deletingItemId = null
                    )
                }

                _st.value = newState
            } catch (e: HttpException) {
                val msg = e.response()?.errorBody()?.string()?.let {
                    try { JSONObject(it).optString("message") } catch (_: Exception) { null }
                } ?: "Nepavyko įkelti suvestinės"
                _st.value = _st.value.copy(loading = false, error = msg, updatingItemId = null, deletingItemId = null)
            } catch (_: Exception) {
                _st.value = _st.value.copy(loading = false, error = "Nepavyko įkelti suvestinės", updatingItemId = null, deletingItemId = null)
            }
        }
    }

    fun setPeriodType(type: SummaryPeriodType) {
        val start = when (type) {
            SummaryPeriodType.DAY -> _st.value.periodStart
            SummaryPeriodType.WEEK -> _st.value.periodStart.startOfWeek()
            SummaryPeriodType.MONTH -> _st.value.periodStart.withDayOfMonth(1)
        }
        val end = when (type) {
            SummaryPeriodType.DAY -> start
            SummaryPeriodType.WEEK -> start.plusDays(6)
            SummaryPeriodType.MONTH -> start.endOfMonth()
        }
        _st.value = _st.value.copy(periodType = type, periodStart = start, periodEnd = end)
    }

    fun shiftPeriod(forward: Boolean) {
        val delta = if (forward) 1L else -1L
        val type = _st.value.periodType
        val start = when (type) {
            SummaryPeriodType.DAY -> _st.value.periodStart.plusDays(delta)
            SummaryPeriodType.WEEK -> _st.value.periodStart.plusWeeks(delta)
            SummaryPeriodType.MONTH -> _st.value.periodStart.plusMonths(delta)
        }
        val end = when (type) {
            SummaryPeriodType.DAY -> start
            SummaryPeriodType.WEEK -> start.plusDays(6)
            SummaryPeriodType.MONTH -> start.endOfMonth()
        }
        _st.value = _st.value.copy(periodStart = start, periodEnd = end)
    }

    fun selectPeriod(start: LocalDate, end: LocalDate) {
        val normalizedEnd = if (end.isBefore(start)) start else end
        _st.value = _st.value.copy(periodStart = start, periodEnd = normalizedEnd)
    }

    fun updateSearchQuery(query: String) {
        _st.value = _st.value.copy(searchQuery = query)
    }

    fun setCalorieSort(order: CalorieSortOrder) {
        _st.value = _st.value.copy(calorieSort = order)
    }

    fun clearMessage() {
        _st.value = _st.value.copy(message = null)
    }

    fun updateItem(token: String, itemId: Long, grams: Int, quantity: Int) {
        if (grams <= 0 || quantity <= 0) {
            _st.value = _st.value.copy(error = "Įveskite teigiamas reikšmes")
            return
        }

        viewModelScope.launch {
            try {
                _st.value = _st.value.copy(updatingItemId = itemId, message = null, error = null)

                val res = RetrofitClient.api.updateFoodLog(
                    "Bearer $token",
                    itemId,
                    FoodLogUpdateReq(grams = grams, quantity = quantity)
                )

                val updated = FoodLogItem(
                    id = res.id,
                    name = res.name,
                    portionCalories = res.portionCalories,
                    caloriesPer100g = res.caloriesPer100g,
                    grams = res.grams,
                    quantity = res.quantity,
                    totalCalories = res.totalCalories,
                    consumedAt = parseDateTime(res.consumedAt),
                    protein = res.protein,
                    fat = res.fat,
                    carbs = res.carbs,
                    totalProtein = res.totalProtein,
                    totalFat = res.totalFat,
                    totalCarbs = res.totalCarbs
                )

                val newGroups = _st.value.groups.map { group ->
                    val updatedItems = group.items.map { if (it.id == itemId) updated else it }
                    if (updatedItems == group.items) {
                        group
                    } else {
                        group.copy(
                            items = updatedItems.sortedBy { it.consumedAt },
                            dayTotal = updatedItems.sumOf { it.totalCalories },
                            proteinTotal = updatedItems.sumOf { it.totalProtein },
                            fatTotal = updatedItems.sumOf { it.totalFat },
                            carbsTotal = updatedItems.sumOf { it.totalCarbs }
                        )
                    }
                }

                _st.value = _st.value.copy(
                    groups = newGroups,
                    updatingItemId = null,
                    message = "Įrašas atnaujintas"
                )
            } catch (e: HttpException) {
                val msg = e.response()?.errorBody()?.string()?.let {
                    try { JSONObject(it).optString("message") } catch (_: Exception) { null }
                } ?: "Nepavyko atnaujinti įrašo"
                _st.value = _st.value.copy(updatingItemId = null, error = msg)
            } catch (_: Exception) {
                _st.value = _st.value.copy(updatingItemId = null, error = "Nepavyko atnaujinti įrašo")
            }
        }
    }
    private fun saveCache(groups: List<DayGroup>) {
        viewModelScope.launch {
            val arr = JSONArray()
            groups.take(7).forEach { group ->
                val obj = JSONObject()
                obj.put("date", group.date.toString())
                val items = JSONArray()
                group.items.forEach { item ->
                    val it = JSONObject()
                    it.put("id", item.id)
                    it.put("name", item.name)
                    it.put("portionCalories", item.portionCalories)
                    it.put("caloriesPer100g", item.caloriesPer100g)
                    it.put("grams", item.grams)
                    it.put("quantity", item.quantity)
                    it.put("totalCalories", item.totalCalories)
                    it.put("consumedAt", item.consumedAt.toString())
                    it.put("protein", item.protein)
                    it.put("fat", item.fat)
                    it.put("carbs", item.carbs)
                    it.put("totalProtein", item.totalProtein)
                    it.put("totalFat", item.totalFat)
                    it.put("totalCarbs", item.totalCarbs)
                    items.put(it)
                }
                obj.put("items", items)
                arr.put(obj)
            }
            prefs.saveSummaryCache(arr.toString())
        }
    }

    private fun parseCachedGroups(json: String): List<DayGroup> {
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { index ->
                val obj = arr.optJSONObject(index) ?: return@mapNotNull null
                val date = LocalDate.parse(obj.getString("date"))
                val itemsArr = obj.optJSONArray("items") ?: return@mapNotNull null
                val items = (0 until itemsArr.length()).mapNotNull { idx ->
                    val itemObj = itemsArr.optJSONObject(idx) ?: return@mapNotNull null
                    val dt = parseDateTime(itemObj.getString("consumedAt"))
                    FoodLogItem(
                        id = itemObj.getLong("id"),
                        name = itemObj.getString("name"),
                        portionCalories = itemObj.getInt("portionCalories"),
                        caloriesPer100g = itemObj.getInt("caloriesPer100g"),
                        grams = itemObj.getInt("grams"),
                        quantity = itemObj.getInt("quantity"),
                        totalCalories = itemObj.getInt("totalCalories"),
                        consumedAt = dt,
                        protein = itemObj.optDouble("protein", 0.0),
                        fat = itemObj.optDouble("fat", 0.0),
                        carbs = itemObj.optDouble("carbs", 0.0),
                        totalProtein = itemObj.optDouble("totalProtein", 0.0),
                        totalFat = itemObj.optDouble("totalFat", 0.0),
                        totalCarbs = itemObj.optDouble("totalCarbs", 0.0)
                    )
                }.sortedBy { it.consumedAt }
                DayGroup(
                    date = date,
                    items = items,
                    dayTotal = items.sumOf { it.totalCalories },
                    proteinTotal = items.sumOf { it.totalProtein },
                    fatTotal = items.sumOf { it.totalFat },
                    carbsTotal = items.sumOf { it.totalCarbs }
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun LocalDate.startOfWeek(): LocalDate {
        val dow = this.dayOfWeek.value
        return this.minusDays(((dow + 6) % 7).toLong())
    }

    private fun LocalDate.endOfMonth(): LocalDate = this.withDayOfMonth(this.lengthOfMonth())

    fun deleteItem(token: String, itemId: Long) {
        viewModelScope.launch {
            try {
                _st.value = _st.value.copy(deletingItemId = itemId, message = null, error = null)

                RetrofitClient.api.deleteFoodLog("Bearer $token", itemId)

                val newGroups = _st.value.groups.mapNotNull { group ->
                    val remaining = group.items.filterNot { it.id == itemId }
                    if (remaining.size == group.items.size) {
                        group
                    } else if (remaining.isEmpty()) {
                        null
                    } else {
                        group.copy(
                            items = remaining,
                            dayTotal = remaining.sumOf { it.totalCalories },
                            proteinTotal = remaining.sumOf { it.totalProtein },
                            fatTotal = remaining.sumOf { it.totalFat },
                            carbsTotal = remaining.sumOf { it.totalCarbs }
                        )
                    }
                }

                _st.value = _st.value.copy(
                    groups = newGroups,
                    deletingItemId = null,
                    message = "Įrašas pašalintas"
                )
                saveCache(newGroups)
            } catch (e: HttpException) {
                val msg = e.response()?.errorBody()?.string()?.let {
                    try { JSONObject(it).optString("message") } catch (_: Exception) { null }
                } ?: "Nepavyko pašalinti įrašo"
                _st.value = _st.value.copy(deletingItemId = null, error = msg)
            } catch (_: Exception) {
                _st.value = _st.value.copy(deletingItemId = null, error = "Nepavyko pašalinti įrašo")
            }
        }
    }
}
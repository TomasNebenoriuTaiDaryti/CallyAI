package com.example.callyaiandroid.ui.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.callyaiandroid.network.RetrofitClient
import com.example.callyaiandroid.network.dto.FoodLogDtos
import com.example.callyaiandroid.network.dto.FoodLogUpdateReq
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class FoodLogItem(
    val id: Long,
    val name: String,
    val portionCalories: Int,
    val caloriesPer100g: Int,
    val grams: Int,
    val quantity: Int,
    val totalCalories: Int,
    val consumedAt: LocalDateTime
)

data class DayGroup(
    val date: LocalDate,
    val items: List<FoodLogItem>,
    val dayTotal: Int
)

data class SummaryState(
    val loading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val updatingItemId: Long? = null,
    val groups: List<DayGroup> = emptyList(),
    val selectedDate: LocalDate = LocalDate.now()
) {
    val selectedGroup: DayGroup? get() = groups.firstOrNull { it.date == selectedDate }
    val overallTotal: Int get() = selectedGroup?.dayTotal ?: 0
    val availableDates: List<LocalDate> get() = groups.map { it.date }
}

class SummaryViewModel : ViewModel() {

    private val _st = MutableStateFlow(SummaryState())
    val st: StateFlow<SummaryState> = _st

    private val formats = listOf(
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ISO_LOCAL_DATE_TIME
    )

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
                        consumedAt = dt
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
                            dayTotal = sorted.sumOf { it.totalCalories }
                        )
                    }

                _st.value = _st.value.copy(loading = false, groups = grouped, updatingItemId = null)
            } catch (e: HttpException) {
                val msg = e.response()?.errorBody()?.string()?.let {
                    try { JSONObject(it).optString("message") } catch (_: Exception) { null }
                } ?: "Nepavyko įkelti suvestinės"
                _st.value = _st.value.copy(loading = false, error = msg, updatingItemId = null)
            } catch (_: Exception) {
                _st.value = _st.value.copy(loading = false, error = "Nepavyko įkelti suvestinės", updatingItemId = null)
            }
        }
    }

    fun selectDate(date: LocalDate) {
        _st.value = _st.value.copy(selectedDate = date)
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
                    consumedAt = parseDateTime(res.consumedAt)
                )

                val newGroups = _st.value.groups.map { group ->
                    val updatedItems = group.items.map { if (it.id == itemId) updated else it }
                    val changed = updatedItems != group.items
                    if (!changed) group else group.copy(
                        items = updatedItems.sortedBy { it.consumedAt },
                        dayTotal = updatedItems.sumOf { it.totalCalories }
                    )
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
}
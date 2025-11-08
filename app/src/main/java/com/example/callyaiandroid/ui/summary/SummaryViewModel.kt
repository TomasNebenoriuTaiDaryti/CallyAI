package com.example.callyaiandroid.ui.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.callyaiandroid.network.RetrofitClient
import com.example.callyaiandroid.network.dto.FoodLogDtos
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
    val calories: Int,
    val quantity: Int,
    val total: Int,
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
    val groups: List<DayGroup> = emptyList()
) {
    val overallTotal: Int get() = groups.sumOf { it.dayTotal }
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
                _st.value = _st.value.copy(loading = true, error = null)

                val list: List<FoodLogDtos> = RetrofitClient.api.diaryAll("Bearer $token")

                val items = list.map { dto ->
                    val dt = parseDateTime(dto.consumedAt)
                    val total = dto.totalCalories ?: (dto.calories * dto.quantity)
                    FoodLogItem(
                        id = dto.id,
                        name = dto.name,
                        calories = dto.calories,
                        quantity = dto.quantity,
                        total = total,
                        consumedAt = dt
                    )
                }

                val grouped = items
                    .groupBy { it.consumedAt.toLocalDate() }
                    .toSortedMap(compareByDescending { it })
                    .map { (date, dayItems) ->
                        val sorted = dayItems.sortedBy { it.consumedAt.toLocalTime() }
                        DayGroup(
                            date = date,
                            items = sorted,
                            dayTotal = sorted.sumOf { it.total }
                        )
                    }

                _st.value = SummaryState(loading = false, groups = grouped)
            } catch (e: HttpException) {
                val msg = e.response()?.errorBody()?.string()?.let {
                    try { JSONObject(it).optString("message") } catch (_: Exception) { null }
                } ?: "Nepavyko įkelti suvestinės"
                _st.value = _st.value.copy(loading = false, error = msg)
            } catch (_: Exception) {
                _st.value = _st.value.copy(loading = false, error = "Nepavyko įkelti suvestinės")
            }
        }
    }
}

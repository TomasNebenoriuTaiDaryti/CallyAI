package com.example.callyaiandroid.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.callyaiandroid.network.RetrofitClient
import com.example.callyaiandroid.network.dto.FoodSearchRes
import com.example.callyaiandroid.network.dto.FoodLogCreateReq
import com.example.callyaiandroid.network.dto.FoodLogItemReq
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import kotlin.math.pow
import kotlin.math.round
data class CartItem(
    val name: String,
    val caloriesPer100g: Int,
    val grams: Int = 100,
    val qty: Int = 1,
    val unit: String = "per 100 g",
    val proteinPer100g: Double = 0.0,
    val fatPer100g: Double = 0.0,
    val carbsPer100g: Double = 0.0,
) {
    val perServingKcal: Int
        get() = ((caloriesPer100g * (grams.coerceAtLeast(0))) / 100.0).roundToInt()
    val totalKcal: Int
        get() = perServingKcal * qty
    val perServingProtein: Double
        get() = ((proteinPer100g * grams.coerceAtLeast(0)) / 100.0).roundTo(1)
    val perServingFat: Double
        get() = ((fatPer100g * grams.coerceAtLeast(0)) / 100.0).roundTo(1)
    val perServingCarbs: Double
        get() = ((carbsPer100g * grams.coerceAtLeast(0)) / 100.0).roundTo(1)
    val totalProtein: Double
        get() = (perServingProtein * qty).roundTo(1)
    val totalFat: Double
        get() = (perServingFat * qty).roundTo(1)
    val totalCarbs: Double
        get() = (perServingCarbs * qty).roundTo(1)
}

data class AddFoodState(
    val loading: Boolean = false,
    val result: FoodSearchRes? = null,
    val message: String? = null,
    val cart: List<CartItem> = emptyList(),
    val consumedAt: LocalDateTime = LocalDateTime.now(),
    val mode: AddFoodMode = AddFoodMode.MANUAL,
    val photoDraft: List<CartItem> = emptyList(),
    val photoInProgress: Boolean = false,
) {
    val cartTotal: Int get() = cart.sumOf { it.totalKcal }
}
enum class AddFoodMode { MANUAL, CAMERA }
class AddFoodViewModel : ViewModel() {
    private val _st = MutableStateFlow(AddFoodState())
    val st: StateFlow<AddFoodState> = _st

    private val dtFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    fun search(token: String, query: String) {
        if (query.isBlank()) {
            _st.value = _st.value.copy(message = "Įveskite maisto pavadinimą")
            return
        }
        viewModelScope.launch {
            try {
                _st.value = _st.value.copy(loading = true, message = null)
                val res = RetrofitClient.api.searchFood("Bearer $token", query)
                _st.value = _st.value.copy(loading = false, result = res)
            } catch (e: HttpException) {
                val msg = e.response()?.errorBody()?.string()?.let { json ->
                    try { JSONObject(json).optString("message") } catch (_: Exception) { null }
                } ?: "Nepavyko gauti duomenų"
                _st.value = _st.value.copy(loading = false, message = msg)
            } catch (_: Exception) {
                _st.value = _st.value.copy(loading = false, message = "Nepavyko gauti duomenų")
            }
        }
    }
    fun addLastResultToCart() {
        val r = _st.value.result ?: return
        val item = CartItem(
            name = r.name ?: "Maistas",
            caloriesPer100g = r.calories,
            grams = 100,
            qty = 1,
            unit = r.unit ?: "per 100 g",
            proteinPer100g = r.protein,
            fatPer100g = r.fat,
            carbsPer100g = r.carbs,
        )
        _st.value = _st.value.copy(cart = _st.value.cart + item, message = null)
    }

    fun incQty(index: Int) {
        val list = _st.value.cart.toMutableList()
        if (index in list.indices) {
            val it = list[index]
            list[index] = it.copy(qty = it.qty + 1)
            _st.value = _st.value.copy(cart = list)
        }
    }

    fun decQty(index: Int) {
        val list = _st.value.cart.toMutableList()
        if (index in list.indices) {
            val it = list[index]
            if (it.qty <= 1) {
                list.removeAt(index)
            } else {
                list[index] = it.copy(qty = it.qty - 1)
            }
            _st.value = _st.value.copy(cart = list)
        }
    }

    fun setGrams(index: Int, gramsText: String) {
        val g = gramsText.filter { it.isDigit() }.toIntOrNull() ?: 0
        val list = _st.value.cart.toMutableList()
        if (index in list.indices) {
            val it = list[index]
            list[index] = it.copy(grams = g.coerceIn(0, 10_000))
            _st.value = _st.value.copy(cart = list)
        }
    }

    fun setMode(mode: AddFoodMode) {
        _st.value = _st.value.copy(mode = mode)
    }

    fun startPhotoCapture() {
        _st.value = _st.value.copy(photoInProgress = true, message = null)
        // This is a placeholder for future camera integration
        _st.value = _st.value.copy(photoInProgress = false, message = "Fotografavimo funkcija dar ruošiama")
    }

    fun setPhotoDraft(items: List<CartItem>) {
        _st.value = _st.value.copy(photoDraft = items, photoInProgress = false)
    }

    fun clearPhotoDraft() {
        _st.value = _st.value.copy(photoDraft = emptyList())
    }

    fun importPhotoDraftToCart() {
        if (_st.value.photoDraft.isEmpty()) return
        _st.value = _st.value.copy(
            cart = _st.value.cart + _st.value.photoDraft,
            photoDraft = emptyList(),
            message = "Produktai pridėti iš fotografijos"
        )
    }

    fun setPhotoDraftGrams(index: Int, gramsText: String) {
        val g = gramsText.filter { it.isDigit() }.toIntOrNull() ?: 0
        val list = _st.value.photoDraft.toMutableList()
        if (index in list.indices) {
            val it = list[index]
            list[index] = it.copy(grams = g.coerceIn(0, 10_000))
            _st.value = _st.value.copy(photoDraft = list)
        }
    }

    fun incPhotoDraftQty(index: Int) {
        val list = _st.value.photoDraft.toMutableList()
        if (index in list.indices) {
            val it = list[index]
            list[index] = it.copy(qty = it.qty + 1)
            _st.value = _st.value.copy(photoDraft = list)
        }
    }

    fun decPhotoDraftQty(index: Int) {
        val list = _st.value.photoDraft.toMutableList()
        if (index in list.indices) {
            val it = list[index]
            if (it.qty <= 1) {
                list.removeAt(index)
            } else {
                list[index] = it.copy(qty = it.qty - 1)
            }
            _st.value = _st.value.copy(photoDraft = list)
        }
    }

    fun remove(index: Int) {
        val list = _st.value.cart.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _st.value = _st.value.copy(cart = list)
        }
    }
    fun setDate(date: LocalDate) {
        val t = _st.value.consumedAt.toLocalTime()
        _st.value = _st.value.copy(consumedAt = LocalDateTime.of(date, t))
    }

    fun setTime(time: LocalTime) {
        val d = _st.value.consumedAt.toLocalDate()
        _st.value = _st.value.copy(consumedAt = LocalDateTime.of(d, time))
    }
    fun save(token: String) {
        val items = _st.value.cart
        if (items.isEmpty()) return

        viewModelScope.launch {
            try {
                _st.value = _st.value.copy(loading = true, message = null)

                val req = FoodLogCreateReq(
                    consumedAt = _st.value.consumedAt.format(dtFmt),
                    items = items.map {
                        FoodLogItemReq(
                            name = it.name,
                            caloriesPer100g = it.caloriesPer100g,
                            grams = it.grams,
                            quantity = it.qty,
                            proteinPer100g = it.proteinPer100g,
                            fatPer100g = it.fatPer100g,
                            carbsPer100g = it.carbsPer100g
                        )
                    }
                )

                RetrofitClient.api.createFoodLog("Bearer $token", req)
                _st.value = _st.value.copy(
                    loading = false,
                    cart = emptyList(),
                    result = null,
                    message = "Įrašas išsaugotas"
                )
            } catch (e: HttpException) {
                val msg = e.response()?.errorBody()?.string()?.let { json ->
                    try { JSONObject(json).optString("message") } catch (_: Exception) { null }
                } ?: "Nepavyko išsaugoti"
                _st.value = _st.value.copy(loading = false, message = msg)
            } catch (_: Exception) {
                _st.value = _st.value.copy(loading = false, message = "Nepavyko išsaugoti")
            }
        }
    }
}

private fun Double.roundTo(decimals: Int): Double {
    if (decimals <= 0) return round(this)
    val factor = 10.0.pow(decimals)
    return round(this * factor) / factor
}
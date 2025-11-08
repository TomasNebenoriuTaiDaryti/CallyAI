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

data class CartItem(
    val name: String,
    val caloriesPer100g: Int,     // bazė skaičiavimui
    val grams: Int = 100,         // redaguojamas kiekis gramais
    val qty: Int = 1,             // vienetų skaičius (kiek porcijų)
    val unit: String = "per 100 g"
) {
    val perServingKcal: Int
        get() = ((caloriesPer100g * (grams.coerceAtLeast(0))) / 100.0).roundToInt()
    val totalKcal: Int
        get() = perServingKcal * qty
}

data class AddFoodState(
    val loading: Boolean = false,
    val result: FoodSearchRes? = null,
    val message: String? = null,
    val cart: List<CartItem> = emptyList(),
    val consumedAt: LocalDateTime = LocalDateTime.now()
) {
    val cartTotal: Int get() = cart.sumOf { it.totalKcal }
}

class AddFoodViewModel : ViewModel() {
    private val _st = MutableStateFlow(AddFoodState())
    val st: StateFlow<AddFoodState> = _st

    private val dtFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

    // --- SEARCH ---
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

    // --- CART ---
    fun addLastResultToCart() {
        val r = _st.value.result ?: return
        val item = CartItem(
            name = r.name ?: "Maistas",
            caloriesPer100g = r.calories,
            grams = 100,
            qty = 1,
            unit = r.unit ?: "per 100 g"
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
                list.removeAt(index)          // 2) ištrinti kai qty==1 ir spaudžiam „–“
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

    fun remove(index: Int) {
        val list = _st.value.cart.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _st.value = _st.value.copy(cart = list)
        }
    }

    // --- DATA / LAIKAS ---
    fun setDate(date: LocalDate) {
        val t = _st.value.consumedAt.toLocalTime()
        _st.value = _st.value.copy(consumedAt = LocalDateTime.of(date, t))
    }

    fun setTime(time: LocalTime) {
        val d = _st.value.consumedAt.toLocalDate()
        _st.value = _st.value.copy(consumedAt = LocalDateTime.of(d, time))
    }

    // --- SAVE ---
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
                            calories = it.perServingKcal, // kcal už pasirinktus gramus
                            quantity = it.qty
                        )
                    }
                )

                RetrofitClient.api.createFoodLog("Bearer $token", req)
                // sėkmė – išvalom krepšelį
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

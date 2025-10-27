package com.example.callyaiandroid.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.callyaiandroid.network.RetrofitClient
import com.example.callyaiandroid.network.dto.FoodSearchRes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException

data class AddFoodState(
    val loading: Boolean = false,
    val result: FoodSearchRes? = null,
    val message: String? = null
)

class AddFoodViewModel : ViewModel() {
    private val _st = MutableStateFlow(AddFoodState())
    val st: StateFlow<AddFoodState> = _st

    fun search(token: String, query: String) {
        if (query.isBlank()) {
            _st.value = _st.value.copy(message = "Įveskite maisto pavadinimą")
            return
        }
        viewModelScope.launch {
            try {
                _st.value = _st.value.copy(loading = true, message = null)
                val res = RetrofitClient.api.searchFood("Bearer $token", query)
                _st.value = AddFoodState(result = res)
            } catch (e: HttpException) {
                val msg = e.response()?.errorBody()?.string()?.let { json ->
                    try { JSONObject(json).optString("message") } catch (_: Exception) { null }
                } ?: "Nepavyko gauti duomenų"
                _st.value = AddFoodState(message = msg)
            } catch (_: Exception) {
                _st.value = AddFoodState(message = "Nepavyko gauti duomenų")
            }
        }
    }
}

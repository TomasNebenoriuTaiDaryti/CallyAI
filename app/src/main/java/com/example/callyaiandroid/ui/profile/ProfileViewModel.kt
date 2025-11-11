package com.example.callyaiandroid.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.callyaiandroid.data.Prefs
import com.example.callyaiandroid.network.RetrofitClient
import com.example.callyaiandroid.network.dto.CaloriePlanReq
import com.example.callyaiandroid.network.dto.UpdateProfileReq
import com.example.callyaiandroid.network.dto.UserMe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException

data class ProfileState(
    val loading: Boolean = false,
    val user: UserMe? = null,
    val message: String? = null,
    val calcLoading: Boolean = false,
    val calcCalories: Int? = null,
)

class ProfileViewModel(private val prefs: Prefs) : ViewModel() {
    private val _st = MutableStateFlow(ProfileState())
    val st: StateFlow<ProfileState> = _st

    init {
        viewModelScope.launch {
            prefs.profileCacheFlow.collect { cached ->
                if (cached != null && _st.value.user == null) {
                    _st.value = _st.value.copy(user = cached)
                }
            }
        }
    }
    fun load(token: String) {
        viewModelScope.launch {
            try {
                _st.value = _st.value.copy(loading = true, message = null)
                val me = RetrofitClient.api.me("Bearer $token")
                val resolved = me.copy(dailyCalories = me.dailyCalories ?: 2000)
                prefs.saveProfileCache(resolved)
                prefs.setDailyKcal(resolved.dailyCalories ?: 2000)
                _st.value = ProfileState(user = resolved)
            } catch (e: Exception) {
                _st.value = ProfileState(message = "Nepavyko įkelti profilio")
            }
        }
    }

    fun save(token: String, u: UserMe) {
        viewModelScope.launch {
            try {
                _st.value = _st.value.copy(loading = true, message = null)
                val updated = RetrofitClient.api.updateMe(
                    "Bearer $token",
                    UpdateProfileReq(u.name, u.email, u.dailyCalories ?: 2000)
                )
                val resolved = updated.copy(dailyCalories = updated.dailyCalories ?: 2000)
                prefs.saveProfileCache(resolved)
                prefs.setDailyKcal(resolved.dailyCalories ?: 2000)
                _st.value = ProfileState(user = resolved, message = "Išsaugota")
            } catch (e: HttpException) {
                val msg = e.response()?.errorBody()?.string()?.let { json ->
                    try { JSONObject(json).optString("message") } catch (_: Exception) { null }
                } ?: "Nepavyko atnaujinti"
                _st.value = _st.value.copy(loading = false, message = msg)
            } catch (e: Exception) {
                _st.value = _st.value.copy(loading = false, message = "Nepavyko atnaujinti")
            }
        }
    }

    fun calculateDailyCalories(
        token: String,
        goal: String,
        weight: Double,
        height: Double,
        gender: String,
        activityLevel: String
    ) {
        viewModelScope.launch {
            try {
                _st.value = _st.value.copy(calcLoading = true, message = null)
                val res = RetrofitClient.api.calculateDailyCalories(
                    "Bearer $token",
                    CaloriePlanReq(goal, weight, height, gender, activityLevel)
                )
                _st.value = _st.value.copy(
                    calcLoading = false,
                    calcCalories = res.dailyCalories,
                )
            } catch (e: HttpException) {
                val msg = e.response()?.errorBody()?.string()?.let { json ->
                    try { JSONObject(json).optString("message") } catch (_: Exception) { null }
                } ?: "Nepavyko gauti rekomendacijos"
                _st.value = _st.value.copy(calcLoading = false, message = msg)
            } catch (_: Exception) {
                _st.value = _st.value.copy(calcLoading = false, message = "Nepavyko gauti rekomendacijos")
            }
        }
    }

    fun logout(token: String, onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            try {
                prefs.saveToken(null)
                onLoggedOut()
            } catch (_: Exception) {
                onLoggedOut()
            }
        }
    }
}
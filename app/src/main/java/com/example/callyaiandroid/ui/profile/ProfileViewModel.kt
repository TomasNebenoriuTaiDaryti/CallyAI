package com.example.callyaiandroid.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.callyaiandroid.data.Prefs
import com.example.callyaiandroid.network.RetrofitClient
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
    val message: String? = null
)

class ProfileViewModel(private val prefs: Prefs) : ViewModel() {
    private val _st = MutableStateFlow(ProfileState())
    val st: StateFlow<ProfileState> = _st

    fun load(token: String) {
        viewModelScope.launch {
            try {
                _st.value = _st.value.copy(loading = true, message = null)
                val me = RetrofitClient.api.me("Bearer $token")
                _st.value = ProfileState(user = me)
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
                    UpdateProfileReq(u.name, u.email, u.dailyCalories)
                )
                _st.value = ProfileState(user = updated, message = "Išsaugota")
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

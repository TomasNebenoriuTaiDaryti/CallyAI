package com.example.callyaiandroid.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.callyaiandroid.data.Prefs
import com.example.callyaiandroid.network.RetrofitClient
import com.example.callyaiandroid.network.dto.LoginReq
import com.example.callyaiandroid.network.dto.RegisterReq
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException

data class AuthUiState(
    val loading: Boolean = false,
    val emailError: String? = null,
    val passError: String? = null,
    val generalError: String? = null
)

class AuthViewModel(private val prefs: Prefs) : ViewModel() {
    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state

    fun clearError() {
        _state.value = _state.value.copy(generalError = null, emailError = null, passError = null)
    }

    fun setError(msg: String) {
        _state.value = _state.value.copy(generalError = msg)
    }

    fun login(email: String, pass: String) {
        val emailErr =
            if (email.isBlank()) "Laukas privalomas"
            else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) "Neteisingas el. paštas" else null
        val passErr = if (pass.isBlank()) "Laukas privalomas" else null
        if (emailErr != null || passErr != null) {
            _state.value = _state.value.copy(emailError = emailErr, passError = passErr); return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, generalError = null)
            try {
                val resp = RetrofitClient.api.login(LoginReq(email, pass))
                val token = resp.token?.trim()
                if (token.isNullOrEmpty()) {
                    _state.value = _state.value.copy(loading = false, generalError = "Neteisingi prisijungimo duomenys")
                    return@launch
                }
                prefs.saveToken(token)
                _state.value = AuthUiState()
            } catch (e: HttpException) {
                val msg = e.response()?.errorBody()?.string()?.let { json ->
                    try { JSONObject(json).optString("message") } catch (_: Exception) { null }
                } ?: "Nepavyko prisijungti"
                _state.value = _state.value.copy(loading = false, generalError = msg)
            } catch (_: Exception) {
                _state.value = _state.value.copy(loading = false, generalError = "Nepavyko prisijungti")
            }
        }
    }

    fun register(
        name: String,
        email: String,
        pass: String,
        confirm: String
    ) {
        val emailErr =
            if (email.isBlank()) "Laukas privalomas"
            else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) "Neteisingas el. paštas" else null
        val passErr = if (pass.isBlank()) "Laukas privalomas" else null
        if (name.isBlank() || emailErr != null || passErr != null) {
            _state.value = _state.value.copy(
                generalError = if (name.isBlank()) "Laukas privalomas" else null,
                emailError = emailErr, passError = passErr
            ); return
        }
        if (pass != confirm) { setError("Slaptažodžiai nesutampa"); return }

        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, generalError = null)
            try {
                val resp = RetrofitClient.api.register(
                    RegisterReq(name, email, pass, confirm)
                )
                val token = resp.token?.trim()
                if (token.isNullOrEmpty()) {
                    _state.value = _state.value.copy(loading = false, generalError = "Registracija nepavyko")
                    return@launch
                }
                prefs.saveToken(token)
                _state.value = AuthUiState()
            } catch (e: HttpException) {
                val msg = e.response()?.errorBody()?.string()?.let { json ->
                    try { JSONObject(json).optString("message") } catch (_: Exception) { null }
                } ?: "Nepavyko sukurti paskyros"
                _state.value = _state.value.copy(loading = false, generalError = msg)
            } catch (_: Exception) {
                _state.value = _state.value.copy(loading = false, generalError = "Nepavyko sukurti paskyros")
            }
        }
    }
}

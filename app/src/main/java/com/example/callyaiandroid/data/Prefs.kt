package com.example.callyaiandroid.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore("prefs")

object Keys {
    val token = stringPreferencesKey("token")
    val theme = stringPreferencesKey("theme")
    val dailyKcal = intPreferencesKey("daily_kcal")
}

class Prefs(private val ctx: Context) {
    /** Emits NULL when token is missing OR blank – blank token is not considered logged-in. */
    val tokenFlow = ctx.dataStore.data.map { prefs ->
        prefs[Keys.token]?.trim()?.takeIf { it.isNotEmpty() }
    }

    /** Saves token only if non-blank; otherwise clears it. */
    suspend fun saveToken(t: String?) = ctx.dataStore.edit {
        val clean = t?.trim()
        if (clean.isNullOrEmpty()) it.remove(Keys.token) else it[Keys.token] = clean
    }

    val themeFlow = ctx.dataStore.data.map { it[Keys.theme] ?: "light" }
    suspend fun setTheme(value: String) = ctx.dataStore.edit { it[Keys.theme] = value }

    val kcalFlow = ctx.dataStore.data.map { it[Keys.dailyKcal] ?: 2000 }
    suspend fun setDailyKcal(value: Int) = ctx.dataStore.edit { it[Keys.dailyKcal] = value }
}

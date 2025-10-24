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
    val tokenFlow = ctx.dataStore.data.map { it[Keys.token] }
    suspend fun saveToken(t: String?) = ctx.dataStore.edit {
        if (t == null) it.remove(Keys.token) else it[Keys.token] = t
    }

    val themeFlow = ctx.dataStore.data.map { it[Keys.theme] ?: "light" }
    suspend fun setTheme(value: String) = ctx.dataStore.edit { it[Keys.theme] = value }

    val kcalFlow = ctx.dataStore.data.map { it[Keys.dailyKcal] ?: 2000 }
    suspend fun setDailyKcal(value: Int) = ctx.dataStore.edit { it[Keys.dailyKcal] = value }
}

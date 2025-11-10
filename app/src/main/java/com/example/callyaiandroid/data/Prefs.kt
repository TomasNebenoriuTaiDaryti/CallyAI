package com.example.callyaiandroid.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import com.example.callyaiandroid.network.dto.UserMe
import org.json.JSONObject

val Context.dataStore by preferencesDataStore("prefs")

object Keys {
    val token = stringPreferencesKey("token")
    val theme = stringPreferencesKey("theme")
    val dailyKcal = intPreferencesKey("daily_kcal")
    val profileCache = stringPreferencesKey("profile_cache")
    val summaryCache = stringPreferencesKey("summary_cache")
}

class Prefs(private val ctx: Context) {
    val tokenFlow = ctx.dataStore.data.map { prefs ->
        prefs[Keys.token]?.trim()?.takeIf { it.isNotEmpty() }
    }
    suspend fun saveToken(t: String?) = ctx.dataStore.edit {
        val clean = t?.trim()
        if (clean.isNullOrEmpty()) it.remove(Keys.token) else it[Keys.token] = clean
    }

    val themeFlow = ctx.dataStore.data.map { it[Keys.theme] ?: "light" }
    suspend fun setTheme(value: String) = ctx.dataStore.edit { it[Keys.theme] = value }

    val kcalFlow = ctx.dataStore.data.map { it[Keys.dailyKcal] ?: 2000 }
    suspend fun setDailyKcal(value: Int) = ctx.dataStore.edit { it[Keys.dailyKcal] = value }

    val profileCacheFlow = ctx.dataStore.data.map { prefs ->
        prefs[Keys.profileCache]?.let { json ->
            try {
                val obj = JSONObject(json)
                UserMe(
                    id = obj.getLong("id"),
                    name = obj.getString("name"),
                    email = obj.getString("email"),
                    dailyCalories = if (obj.has("dailyCalories") && !obj.isNull("dailyCalories")) obj.optInt("dailyCalories") else null,
                    units = obj.takeIf { it.has("units") && !it.isNull("units") }?.optString("units"),
                    theme = obj.takeIf { it.has("theme") && !it.isNull("theme") }?.optString("theme"),
                    autoAddAi = if (obj.has("autoAddAi") && !obj.isNull("autoAddAi")) obj.optBoolean("autoAddAi") else null
                )
            } catch (_: Exception) {
                null
            }
        }
    }

    suspend fun saveProfileCache(user: UserMe) = ctx.dataStore.edit { prefs ->
        val obj = JSONObject()
        obj.put("id", user.id)
        obj.put("name", user.name)
        obj.put("email", user.email)
        obj.put("dailyCalories", user.dailyCalories ?: JSONObject.NULL)
        obj.put("units", user.units ?: JSONObject.NULL)
        obj.put("theme", user.theme ?: JSONObject.NULL)
        obj.put("autoAddAi", user.autoAddAi ?: JSONObject.NULL)
        prefs[Keys.profileCache] = obj.toString()
    }

    val summaryCacheFlow = ctx.dataStore.data.map { it[Keys.summaryCache] }
    suspend fun saveSummaryCache(json: String) = ctx.dataStore.edit { it[Keys.summaryCache] = json }
}

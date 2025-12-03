package com.example.callyaiandroid.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import com.example.callyaiandroid.network.dto.UserMe
import org.json.JSONObject
import androidx.datastore.preferences.core.booleanPreferencesKey
import kotlin.math.max

val Context.dataStore by preferencesDataStore("prefs")
object Keys {
    val token = stringPreferencesKey("token")
    val theme = stringPreferencesKey("theme")
    val dailyKcal = intPreferencesKey("daily_kcal")
    val profileCache = stringPreferencesKey("profile_cache")
    val summaryCache = stringPreferencesKey("summary_cache")
    val notificationsEnabled = booleanPreferencesKey("notifications_enabled")
    val notificationIntervalHours = intPreferencesKey("notification_interval_hours")
    val macroProtein = intPreferencesKey("macro_protein")
    val macroFat = intPreferencesKey("macro_fat")
    val macroCarbs = intPreferencesKey("macro_carbs")
}
class Prefs(private val ctx: Context) : PrefsGateway {
    override val tokenFlow = ctx.dataStore.data.map { prefs ->
        prefs[Keys.token]?.trim()?.takeIf { it.isNotEmpty() }
    }

    override suspend fun saveToken(t: String?) {
        ctx.dataStore.edit {
            val clean = t?.trim()
            if (clean.isNullOrEmpty()) it.remove(Keys.token) else it[Keys.token] = clean
        }
    }
    override val themeFlow = ctx.dataStore.data.map { it[Keys.theme] ?: "light" }
    override suspend fun setTheme(value: String) {
        ctx.dataStore.edit { it[Keys.theme] = value }
    }
    override val kcalFlow = ctx.dataStore.data.map { it[Keys.dailyKcal] ?: 2000 }
    override suspend fun setDailyKcal(value: Int) {
        ctx.dataStore.edit { it[Keys.dailyKcal] = value }
    }
    override val profileCacheFlow = ctx.dataStore.data.map { prefs ->
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
    override suspend fun saveProfileCache(user: UserMe) {
        ctx.dataStore.edit { prefs ->
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
    }
    override val summaryCacheFlow = ctx.dataStore.data.map { it[Keys.summaryCache] }
    override suspend fun saveSummaryCache(json: String) {
        ctx.dataStore.edit { it[Keys.summaryCache] = json }
    }
    override val notificationsEnabledFlow = ctx.dataStore.data.map { it[Keys.notificationsEnabled] ?: false }
    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        ctx.dataStore.edit { it[Keys.notificationsEnabled] = enabled }
    }

    override val notificationIntervalHoursFlow = ctx.dataStore.data.map { it[Keys.notificationIntervalHours] ?: 3 }
    override suspend fun setNotificationIntervalHours(hours: Int) {
        ctx.dataStore.edit { it[Keys.notificationIntervalHours] = max(1, hours) }
    }

    override val macroPercentsFlow = ctx.dataStore.data.map { prefs ->
        MacroPercents(
            protein = prefs[Keys.macroProtein] ?: MacroPercents().protein,
            fat = prefs[Keys.macroFat] ?: MacroPercents().fat,
            carbs = prefs[Keys.macroCarbs] ?: MacroPercents().carbs,
        )
    }

    override suspend fun setMacroPercents(protein: Int, fat: Int, carbs: Int) {
        ctx.dataStore.edit { prefs ->
            prefs[Keys.macroProtein] = protein.coerceIn(0, 100)
            prefs[Keys.macroFat] = fat.coerceIn(0, 100)
            prefs[Keys.macroCarbs] = carbs.coerceIn(0, 100)
        }
    }
}

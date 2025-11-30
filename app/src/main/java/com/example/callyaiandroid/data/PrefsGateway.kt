package com.example.callyaiandroid.data

import com.example.callyaiandroid.network.dto.UserMe
import kotlinx.coroutines.flow.Flow

interface PrefsGateway {
    val tokenFlow: Flow<String?>
    suspend fun saveToken(t: String?)

    val themeFlow: Flow<String>
    suspend fun setTheme(value: String)

    val kcalFlow: Flow<Int>
    suspend fun setDailyKcal(value: Int)

    val profileCacheFlow: Flow<UserMe?>
    suspend fun saveProfileCache(user: UserMe)

    val summaryCacheFlow: Flow<String?>
    suspend fun saveSummaryCache(json: String)

    val notificationsEnabledFlow: Flow<Boolean>
    suspend fun setNotificationsEnabled(enabled: Boolean)

    val notificationIntervalHoursFlow: Flow<Int>
    suspend fun setNotificationIntervalHours(hours: Int)
}
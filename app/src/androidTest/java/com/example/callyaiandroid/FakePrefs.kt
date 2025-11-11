package com.example.callyaiandroid

import com.example.callyaiandroid.data.PrefsGateway
import com.example.callyaiandroid.network.dto.UserMe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakePrefs : PrefsGateway {
    private val tokenState = MutableStateFlow<String?>(null)
    private val themeState = MutableStateFlow("light")
    private val kcalState = MutableStateFlow(2000)
    private val profileState = MutableStateFlow<UserMe?>(null)
    private val summaryState = MutableStateFlow<String?>(null)

    var savedToken: String? = null
        private set
    var savedSummaryCache: String? = null
        private set
    var savedProfile: UserMe? = null
        private set
    var setThemeCalls: MutableList<String> = mutableListOf()
    var dailyKcalSaves: MutableList<Int> = mutableListOf()

    override val tokenFlow: Flow<String?> = tokenState

    override suspend fun saveToken(t: String?) {
        val clean = t?.trim()?.takeIf { it.isNotEmpty() }
        savedToken = clean
        tokenState.value = clean
    }

    override val themeFlow: Flow<String> = themeState

    override suspend fun setTheme(value: String) {
        themeState.value = value
        setThemeCalls.add(value)
    }

    override val kcalFlow: Flow<Int> = kcalState

    override suspend fun setDailyKcal(value: Int) {
        kcalState.value = value
        dailyKcalSaves.add(value)
    }

    override val profileCacheFlow: Flow<UserMe?> = profileState

    override suspend fun saveProfileCache(user: UserMe) {
        savedProfile = user
        profileState.value = user
    }

    override val summaryCacheFlow: Flow<String?> = summaryState

    override suspend fun saveSummaryCache(json: String) {
        savedSummaryCache = json
        summaryState.value = json
    }

    fun emitSummaryCache(value: String?) {
        summaryState.value = value
    }

    fun emitProfileCache(user: UserMe?) {
        profileState.value = user
    }

    fun emitDailyKcal(value: Int) {
        kcalState.value = value
    }
}
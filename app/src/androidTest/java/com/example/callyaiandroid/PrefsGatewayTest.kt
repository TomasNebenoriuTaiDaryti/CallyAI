package com.example.callyaiandroid

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PrefsGatewayTest {
    @Test
    fun updatesNotificationSettings() = runTest {
        val prefs = FakePrefs()

        prefs.setNotificationsEnabled(true)
        prefs.setNotificationIntervalHours(6)

        assertEquals(true, prefs.notificationsEnabledFlow.first())
        assertEquals(6, prefs.notificationIntervalHoursFlow.first())
    }
}
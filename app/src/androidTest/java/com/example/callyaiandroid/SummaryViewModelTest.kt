package com.example.callyaiandroid.ui.summary

import com.example.callyaiandroid.FakePrefs
import com.example.callyaiandroid.network.ApiService
import com.example.callyaiandroid.network.dto.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.time.LocalDate
import com.example.callyaiandroid.data.MacroPercents
import okhttp3.MultipartBody

class SummaryViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var prefs: FakePrefs
    private lateinit var api: FakeSummaryApi
    private var originalDelegate: Any? = null
    private lateinit var vm: SummaryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        prefs = FakePrefs()
        api = FakeSummaryApi()
        swapRetrofitApi(api)
        vm = SummaryViewModel(prefs)
    }

    @After
    fun tearDown() {
        restoreRetrofitApi()
        Dispatchers.resetMain()
    }

    @Test
    fun loadAllGroupsEntriesAndCachesResult() = runTest {
        api.entries = listOf(
            FoodLogDtos(
                id = 1L,
                name = "Avižos",
                portionCalories = 150,
                caloriesPer100g = 350,
                grams = 80,
                quantity = 1,
                totalCalories = 150,
                consumedAt = "2024-11-01T08:15:00",
                protein = 10.0,
                fat = 5.0,
                carbs = 20.0,
                totalProtein = 10.0,
                totalFat = 5.0,
                totalCarbs = 20.0
            ),
            FoodLogDtos(
                id = 2L,
                name = "Kava",
                portionCalories = 5,
                caloriesPer100g = 10,
                grams = 100,
                quantity = 1,
                totalCalories = 5,
                consumedAt = "2024-11-01T09:00:00",
                protein = 0.0,
                fat = 0.0,
                carbs = 1.0,
                totalProtein = 0.0,
                totalFat = 0.0,
                totalCarbs = 1.0
            ),
            FoodLogDtos(
                id = 3L,
                name = "Sriuba",
                portionCalories = 200,
                caloriesPer100g = 90,
                grams = 250,
                quantity = 1,
                totalCalories = 200,
                consumedAt = "2024-11-02T12:30:00",
                protein = 12.0,
                fat = 8.0,
                carbs = 15.0,
                totalProtein = 12.0,
                totalFat = 8.0,
                totalCarbs = 15.0
            )
        )

        vm.loadAll("token-1")
        advanceUntilIdle()

        val state = vm.st.value
        assertEquals(2, state.groups.size)
        val firstGroup = state.groups.first()
        assertEquals(LocalDate.parse("2024-11-02"), firstGroup.date)
        assertEquals(200, firstGroup.dayTotal)
        assertEquals(3, state.groups.sumOf { it.items.size })
        assertTrue(prefs.savedSummaryCache?.contains("Avižos") == true)
    }

    @Test
    fun shiftPeriodForwardUpdatesWindow() = runTest {
        api.entries = listOf(
            FoodLogDtos(
                id = 10L,
                name = "Salotos",
                portionCalories = 120,
                caloriesPer100g = 60,
                grams = 200,
                quantity = 1,
                totalCalories = 120,
                consumedAt = "2024-10-01T10:00:00",
                protein = 6.0,
                fat = 4.0,
                carbs = 12.0,
                totalProtein = 6.0,
                totalFat = 4.0,
                totalCarbs = 12.0
            )
        )

        vm.loadAll("token-2")
        advanceUntilIdle()

        vm.setPeriodType(SummaryPeriodType.WEEK)
        val startBefore = vm.st.value.periodStart

        vm.shiftPeriod(forward = true)
        val state = vm.st.value
        assertEquals(startBefore.plusWeeks(1), state.periodStart)
        assertEquals(startBefore.plusWeeks(1).plusDays(6), state.periodEnd)
    }

    @Test
    fun updateItemHttpErrorSetsMessage() = runTest {
        api.shouldFailUpdate = true
        api.failMessage = "{\"message\":\"Nepavyko\"}"
        api.updateResponse = FoodLogEntryRes(
            id = 99L,
            name = "Košė",
            portionCalories = 120,
            caloriesPer100g = 110,
            grams = 80,
            quantity = 1,
            totalCalories = 120,
            consumedAt = "2024-11-01T08:00:00",
            protein = 7.0,
            fat = 4.0,
            carbs = 15.0,
            totalProtein = 7.0,
            totalFat = 4.0,
            totalCarbs = 15.0
        )
        api.entries = listOf(
            FoodLogDtos(
                id = 99L,
                name = "Košė",
                portionCalories = 120,
                caloriesPer100g = 110,
                grams = 80,
                quantity = 1,
                totalCalories = 120,
                consumedAt = "2024-11-01T08:00:00",
                protein = 7.0,
                fat = 4.0,
                carbs = 15.0,
                totalProtein = 7.0,
                totalFat = 4.0,
                totalCarbs = 15.0
            )
        )

        vm.loadAll("token-1")
        advanceUntilIdle()
        vm.updateItem("token-1", 99L, grams = 50, quantity = 1)
        advanceUntilIdle()

        val state = vm.st.value
        assertEquals("Nepavyko", state.error)
        assertEquals(null, state.updatingItemId)
    }


    private fun retrofitClientInstance(): Any {
        val clazz = com.example.callyaiandroid.network.RetrofitClient::class.java
        val instanceField = clazz.getDeclaredField("INSTANCE").apply { isAccessible = true }
        return instanceField.get(null)
    }

    private fun swapRetrofitApi(service: ApiService) {
        val clazz = com.example.callyaiandroid.network.RetrofitClient::class.java
        val instance = retrofitClientInstance()

        val (field, isDelegate) = try {
            clazz.getDeclaredField("api\$delegate").apply { isAccessible = true } to true
        } catch (_: NoSuchFieldException) {
            clazz.getDeclaredField("api").apply { isAccessible = true } to false
        }

        if (originalDelegate == null) {
            originalDelegate = field.get(instance)
        }
        if (isDelegate) {
            field.set(instance, lazyOf(service))
        } else {
            field.set(instance, service)
        }
    }

    private fun restoreRetrofitApi() {
        val clazz = com.example.callyaiandroid.network.RetrofitClient::class.java
        val instance = retrofitClientInstance()

        val field = try {
            clazz.getDeclaredField("api\$delegate").apply { isAccessible = true }
        } catch (_: NoSuchFieldException) {
            clazz.getDeclaredField("api").apply { isAccessible = true }
        }

        field.set(instance, originalDelegate)
    }


    private class FakeSummaryApi : ApiService {
        var entries: List<FoodLogDtos> = emptyList()
        var shouldFailUpdate: Boolean = false
        var failMessage: String = ""
        var updateResponse: FoodLogEntryRes? = null

        override suspend fun diaryAll(auth: String): List<FoodLogDtos> {
            if (!auth.startsWith("Bearer")) throw IllegalArgumentException("Missing bearer token")
            return entries
        }

        override suspend fun updateFoodLog(
            auth: String,
            id: Long,
            body: FoodLogUpdateReq
        ): FoodLogEntryRes {
            if (shouldFailUpdate) {
                val mediaType = "application/json".toMediaType()
                val responseBody = failMessage.toResponseBody(mediaType)
                throw HttpException(Response.error<Any>(422, responseBody))
            }
            return updateResponse ?: throw IllegalStateException("updateResponse not set")
        }

        override suspend fun deleteFoodLog(auth: String, id: Long) {}

        override suspend fun login(body: LoginReq): AuthResp = throw UnsupportedOperationException()
        override suspend fun register(body: RegisterReq): AuthResp = throw UnsupportedOperationException()
        override suspend fun forgotPassword(body: ForgotPasswordReq): Map<String, String> = throw UnsupportedOperationException()
        override suspend fun logout(bearer: String): Map<String, String> = throw UnsupportedOperationException()
        override suspend fun me(bearer: String): UserMe = throw UnsupportedOperationException()
        override suspend fun updateMe(bearer: String, body: UpdateProfileReq): UserMe = throw UnsupportedOperationException()
        override suspend fun calculateDailyCalories(auth: String, body: CaloriePlanReq): CaloriePlanResp =
            throw UnsupportedOperationException()
        override suspend fun searchFood(auth: String, query: String): FoodSearchRes = throw UnsupportedOperationException()
        override suspend fun createFoodLog(auth: String, body: FoodLogCreateReq) {
            throw UnsupportedOperationException()
        }
        override suspend fun getDayEntries(auth: String, date: String): List<FoodLogEntryRes> = throw UnsupportedOperationException()
        override suspend fun getMacroPercents(bearer: String): MacroPercents = MacroPercents()
        override suspend fun saveMacroPercents(bearer: String, body: MacroPercents): MacroPercents = body
        override suspend fun analyzeFoodPhoto(auth: String, image: MultipartBody.Part): PhotoRecognitionRes {
            throw UnsupportedOperationException()
        }
    }
}

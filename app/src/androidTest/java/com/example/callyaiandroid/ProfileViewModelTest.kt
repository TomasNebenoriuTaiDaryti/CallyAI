package com.example.callyaiandroid.ui.profile

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
import com.example.callyaiandroid.data.MacroPercents
import okhttp3.MultipartBody

class ProfileViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var prefs: FakePrefs
    private lateinit var api: FakeProfileApi
    private var originalDelegate: Any? = null
    private lateinit var vm: ProfileViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        prefs = FakePrefs()
        api = FakeProfileApi()
        swapRetrofitApi(api)
        vm = ProfileViewModel(prefs)
    }

    @After
    fun tearDown() {
        restoreRetrofitApi()
        Dispatchers.resetMain()
    }

    @Test
    fun loadSuccessUpdatesStateAndCache() = runTest {
        val user = UserMe(
            id = 7L,
            name = "Greta",
            email = "greta@example.com",
            dailyCalories = 1800,
            units = "metric",
            theme = "dark",
            autoAddAi = true
        )
        api.meResponse = user

        vm.load("token-xyz")
        advanceUntilIdle()

        val state = vm.st.value
        assertEquals("Greta", state.user?.name)
        assertEquals(user, prefs.savedProfile)
        assertTrue(prefs.dailyKcalSaves.contains(1800))
    }

    @Test
    fun saveHttpErrorSetsMessage() = runTest {
        val existing = UserMe(
            id = 1L,
            name = "Jonas",
            email = "jonas@example.com",
            dailyCalories = 2000,
        )
        prefs.emitProfileCache(existing)
        vm = ProfileViewModel(prefs)
        api.shouldFailUpdate = true
        api.failMessage = "{\"message\":\"Nepavyko\"}"

        vm.save("token", existing.copy(name = "Jonas N."))
        advanceUntilIdle()

        val state = vm.st.value
        assertEquals("Nepavyko", state.message)
        assertEquals(false, state.loading)
    }

    @Test
    fun calculateCaloriesSuccessUpdatesState() = runTest {
        api.caloriePlanResp = CaloriePlanResp(dailyCalories = 2100, advice = "Valgykite subalansuotai")

        vm.calculateDailyCalories("token", goal = "maintain", weight = 70.0, height = 175.0, gender = "male", activityLevel = "low")
        advanceUntilIdle()

        val state = vm.st.value
        assertEquals(2100, state.calcCalories)
        assertEquals(false, state.calcLoading)
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


    private class FakeProfileApi : ApiService {
        var meResponse: UserMe? = null
        var shouldFailUpdate: Boolean = false
        var failMessage: String = ""
        var caloriePlanResp: CaloriePlanResp? = null

        override suspend fun me(bearer: String): UserMe {
            if (!bearer.startsWith("Bearer")) throw IllegalArgumentException("Missing bearer")
            return meResponse ?: throw IllegalStateException("meResponse not set")
        }

        override suspend fun updateMe(bearer: String, body: UpdateProfileReq): UserMe {
            if (shouldFailUpdate) {
                val mediaType = "application/json".toMediaType()
                val responseBody = failMessage.toResponseBody(mediaType)
                throw HttpException(Response.error<Any>(422, responseBody))
            }
            return UserMe(
                id = 1L,
                name = body.name,
                email = body.email,
                dailyCalories = body.dailyCalories
            )
        }

        override suspend fun calculateDailyCalories(auth: String, body: CaloriePlanReq): CaloriePlanResp {
            return caloriePlanResp ?: throw IllegalStateException("caloriePlanResp not set")
        }

        override suspend fun login(body: LoginReq): AuthResp = throw UnsupportedOperationException()
        override suspend fun register(body: RegisterReq): AuthResp = throw UnsupportedOperationException()
        override suspend fun forgotPassword(body: ForgotPasswordReq): Map<String, String> = throw UnsupportedOperationException()
        override suspend fun logout(bearer: String): Map<String, String> = throw UnsupportedOperationException()
        override suspend fun diaryAll(auth: String): List<FoodLogDtos> = throw UnsupportedOperationException()
        override suspend fun createFoodLog(auth: String, body: FoodLogCreateReq) {
            throw UnsupportedOperationException()
        }
        override suspend fun updateFoodLog(auth: String, id: Long, body: FoodLogUpdateReq): FoodLogEntryRes =
            throw UnsupportedOperationException()
        override suspend fun getDayEntries(auth: String, date: String): List<FoodLogEntryRes> = throw UnsupportedOperationException()
        override suspend fun deleteFoodLog(auth: String, id: Long) = throw UnsupportedOperationException()
        override suspend fun searchFood(auth: String, query: String): FoodSearchRes = throw UnsupportedOperationException()
        override suspend fun getMacroPercents(bearer: String): MacroPercents = MacroPercents()
        override suspend fun saveMacroPercents(bearer: String, body: MacroPercents): MacroPercents = body
        override suspend fun analyzeFoodPhoto(auth: String, image: MultipartBody.Part): PhotoRecognitionRes {
            throw UnsupportedOperationException()
        }
    }
}
package com.example.callyaiandroid.auth

import com.example.callyaiandroid.FakePrefs
import com.example.callyaiandroid.network.ApiService
import com.example.callyaiandroid.network.dto.AuthResp
import com.example.callyaiandroid.network.dto.LoginReq
import com.example.callyaiandroid.network.dto.RegisterReq
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
import com.example.callyaiandroid.network.dto.PhotoRecognitionRes
import okhttp3.MultipartBody

class AuthViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var prefs: FakePrefs
    private lateinit var api: FakeAuthApi
    private var originalDelegate: Any? = null
    private lateinit var vm: AuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        prefs = FakePrefs()
        api = FakeAuthApi()
        swapRetrofitApi(api)
        vm = AuthViewModel(prefs)
    }

    @After
    fun tearDown() {
        restoreRetrofitApi()
        Dispatchers.resetMain()
    }

    @Test
    fun loginValidatesInputsAndSetsErrors() {
        vm.login("", "")

        val state = vm.state.value
        assertEquals("Laukas privalomas", state.emailError)
        assertEquals("Laukas privalomas", state.passError)
    }

    @Test
    fun loginSuccessSavesTokenAndClearsErrors() = runTest {
        api.loginResponse = AuthResp(token = "abc123", userId = 1L, name = "Jonas")

        vm.login("jonas@example.com", "slaptas")
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue(state.generalError == null && !state.loading)
        assertEquals("abc123", prefs.savedToken)
    }

    @Test
    fun registerMismatchedPasswordsSetsError() {
        vm.register("Jonas", "jonas@example.com", "aaa", "bbb")

        val state = vm.state.value
        assertEquals("Slaptažodžiai nesutampa", state.generalError)
    }

    @Test
    fun registerHttpErrorPropagatesMessage() = runTest {
        api.shouldFailRegister = true
        api.failMessage = "{\"message\":\"El. paštas užimtas\"}"

        vm.register("Jonas", "jonas@example.com", "slaptas", "slaptas")
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals("El. paštas užimtas", state.generalError)
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


    private class FakeAuthApi : ApiService {
        var loginResponse: AuthResp? = null
        var shouldFailRegister: Boolean = false
        var failMessage: String = ""

        override suspend fun login(body: LoginReq): AuthResp {
            return loginResponse ?: throw IllegalStateException("loginResponse not set")
        }

        override suspend fun register(body: RegisterReq): AuthResp {
            if (shouldFailRegister) {
                val mediaType = "application/json".toMediaType()
                val responseBody = failMessage.toResponseBody(mediaType)
                throw HttpException(Response.error<Any>(409, responseBody))
            }
            return AuthResp(token = "token-${body.email}", userId = 2L, name = body.name)
        }

        override suspend fun forgotPassword(body: com.example.callyaiandroid.network.dto.ForgotPasswordReq): Map<String, String> =
            mapOf("message" to "OK")

        override suspend fun logout(bearer: String): Map<String, String> = emptyMap()
        override suspend fun me(bearer: String): com.example.callyaiandroid.network.dto.UserMe =
            throw UnsupportedOperationException()

        override suspend fun updateMe(
            bearer: String,
            body: com.example.callyaiandroid.network.dto.UpdateProfileReq
        ): com.example.callyaiandroid.network.dto.UserMe = throw UnsupportedOperationException()

        override suspend fun calculateDailyCalories(
            auth: String,
            body: com.example.callyaiandroid.network.dto.CaloriePlanReq
        ): com.example.callyaiandroid.network.dto.CaloriePlanResp = throw UnsupportedOperationException()

        override suspend fun searchFood(
            auth: String,
            query: String
        ): com.example.callyaiandroid.network.dto.FoodSearchRes = throw UnsupportedOperationException()

        override suspend fun createFoodLog(
            auth: String,
            body: com.example.callyaiandroid.network.dto.FoodLogCreateReq
        ) = throw UnsupportedOperationException()

        override suspend fun updateFoodLog(
            auth: String,
            id: Long,
            body: com.example.callyaiandroid.network.dto.FoodLogUpdateReq
        ): com.example.callyaiandroid.network.dto.FoodLogEntryRes = throw UnsupportedOperationException()

        override suspend fun getDayEntries(
            auth: String,
            date: String
        ): List<com.example.callyaiandroid.network.dto.FoodLogEntryRes> = throw UnsupportedOperationException()

        override suspend fun diaryAll(auth: String): List<com.example.callyaiandroid.network.dto.FoodLogDtos> =
            throw UnsupportedOperationException()

        override suspend fun deleteFoodLog(auth: String, id: Long) = throw UnsupportedOperationException()

        override suspend fun getMacroPercents(bearer: String): MacroPercents = MacroPercents()

        override suspend fun saveMacroPercents(bearer: String, body: MacroPercents): MacroPercents = body

        override suspend fun analyzeFoodPhoto(auth: String, image: MultipartBody.Part): PhotoRecognitionRes {
            throw UnsupportedOperationException()
        }
    }
}
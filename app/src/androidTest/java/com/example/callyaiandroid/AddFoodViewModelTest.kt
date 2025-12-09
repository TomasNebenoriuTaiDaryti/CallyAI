package com.example.callyaiandroid.ui.add

import com.example.callyaiandroid.network.ApiService
import com.example.callyaiandroid.network.RetrofitClient
import com.example.callyaiandroid.network.dto.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import com.example.callyaiandroid.data.MacroPercents
import okhttp3.MultipartBody

class AddFoodViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: AddFoodViewModel
    private lateinit var fakeApi: FakeApiService
    private var originalDelegate: Any? = null

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeApi = FakeApiService()
        swapRetrofitApi(fakeApi)
        viewModel = AddFoodViewModel()
    }

    @After
    fun tearDown() {
        restoreRetrofitApi()
        Dispatchers.resetMain()
    }

    @Test
    fun cartItemCalculatesDerivedValues() {
        val item = CartItem(
            name = "Obuolys",
            caloriesPer100g = 52,
            grams = 135,
            qty = 3,
            proteinPer100g = 0.3,
            fatPer100g = 0.2,
            carbsPer100g = 13.0,
        )

        assertEquals(70, item.perServingKcal)
        assertEquals(210, item.totalKcal)
        assertEquals(0.4, item.perServingProtein, 0.0001)
        assertEquals(1.2, item.totalProtein, 0.0001)
        assertEquals(0.3, item.perServingFat, 0.0001)
        assertEquals(0.9, item.totalFat, 0.0001)
        assertEquals(17.6, item.perServingCarbs, 0.0001)
        assertEquals(52.8, item.totalCarbs, 0.0001)
    }

    @Test
    fun addLastResultAddsItemToCart() {
        setState(AddFoodState(result = FoodSearchRes(calories = 90, name = "Varške", unit = "per 100 g", protein = 10.0)))

        viewModel.addLastResultToCart()

        val state = viewModel.st.value
        assertEquals(1, state.cart.size)
        assertEquals("Varške", state.cart.first().name)
        assertEquals(90, state.cart.first().caloriesPer100g)
    }

    @Test
    fun saveClearsCartOnSuccess() = runTest {
        setState(
            AddFoodState(
                cart = listOf(
                    CartItem(name = "Ryžiai", caloriesPer100g = 110, grams = 90, qty = 1),
                    CartItem(name = "Vištiena", caloriesPer100g = 165, grams = 120, qty = 2)
                )
            )
        )

        viewModel.save("token-123")
        advanceUntilIdle()

        val state = viewModel.st.value
        assertTrue(fakeApi.createFoodLogCalled)
        assertEquals(0, state.cart.size)
        assertEquals("Įrašas išsaugotas", state.message)
    }

    @Test
    fun savePropagatesHttpErrorMessage() = runTest {
        fakeApi.failMessage = "{\"message\":\"Nepavyko\"}"
        fakeApi.shouldFailCreate = true
        setState(AddFoodState(cart = listOf(CartItem(name = "Jogurtas", caloriesPer100g = 60, grams = 100, qty = 1))))

        viewModel.save("token")
        advanceUntilIdle()

        val state = viewModel.st.value
        assertEquals("Nepavyko", state.message)
        assertEquals(1, state.cart.size)
    }

    @Test
    fun setGramsSanitizesInput() {
        val initial = AddFoodState(cart = listOf(CartItem(name = "Sultys", caloriesPer100g = 40, grams = 100, qty = 1)))
        setState(initial)

        viewModel.setGrams(0, "12a34")

        val state = viewModel.st.value
        assertEquals(1234, state.cart.first().grams)
    }

    @Test
    fun importPhotoDraftMovesItemsToCart() {
        val draftItems = listOf(CartItem(name = "Obuolys", caloriesPer100g = 52), CartItem(name = "Kriaušė", caloriesPer100g = 57))
        setState(AddFoodState(cart = emptyList(), photoDraft = draftItems))

        viewModel.importPhotoDraftToCart()

        val state = viewModel.st.value
        assertEquals(2, state.cart.size)
        assertTrue(state.photoDraft.isEmpty())
        assertEquals("Produktai pridėti iš fotografijos", state.message)
    }

    @Test
    fun searchBlankQuerySetsMessage() {
        viewModel.search("token", "   ")
        assertEquals("Įveskite maisto pavadinimą", viewModel.st.value.message)
    }

    private fun setState(state: AddFoodState) {
        val field = AddFoodViewModel::class.java.getDeclaredField("_st")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val flow = field.get(viewModel) as MutableStateFlow<AddFoodState>
        flow.value = state
    }


    private fun retrofitClientInstance(): Any {
        val clazz = RetrofitClient::class.java
        val instanceField = clazz.getDeclaredField("INSTANCE").apply { isAccessible = true }
        return instanceField.get(null)
    }

    private fun swapRetrofitApi(api: ApiService) {
        val clazz = RetrofitClient::class.java
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
            field.set(instance, lazyOf(api))
        } else {
            field.set(instance, api)
        }
    }

    private fun restoreRetrofitApi() {
        val clazz = RetrofitClient::class.java
        val instance = retrofitClientInstance()

        val field = try {
            clazz.getDeclaredField("api\$delegate").apply { isAccessible = true }
        } catch (_: NoSuchFieldException) {
            clazz.getDeclaredField("api").apply { isAccessible = true }
        }

        field.set(instance, originalDelegate)
    }


    private class FakeApiService : ApiService {
        var shouldFailCreate: Boolean = false
        var failMessage: String = ""
        var createFoodLogCalled = false

        override suspend fun createFoodLog(auth: String, body: FoodLogCreateReq) {
            createFoodLogCalled = true
            if (shouldFailCreate) {
                val mediaType = "application/json".toMediaType()
                val responseBody = failMessage.toResponseBody(mediaType)
                throw HttpException(Response.error<Any>(422, responseBody))
            }
        }

        override suspend fun searchFood(auth: String, query: String): FoodSearchRes {
            throw UnsupportedOperationException("Not used in tests")
        }

        override suspend fun register(body: RegisterReq): AuthResp = throw UnsupportedOperationException()
        override suspend fun login(body: LoginReq): AuthResp = throw UnsupportedOperationException()
        override suspend fun logout(bearer: String): Map<String, String> = throw UnsupportedOperationException()
        override suspend fun me(bearer: String): UserMe = throw UnsupportedOperationException()
        override suspend fun updateMe(bearer: String, body: UpdateProfileReq): UserMe = throw UnsupportedOperationException()
        override suspend fun calculateDailyCalories(auth: String, body: CaloriePlanReq): CaloriePlanResp = throw UnsupportedOperationException()
        override suspend fun updateFoodLog(auth: String, id: Long, body: FoodLogUpdateReq): FoodLogEntryRes = throw UnsupportedOperationException()
        override suspend fun getDayEntries(auth: String, date: String): List<FoodLogEntryRes> = throw UnsupportedOperationException()
        override suspend fun diaryAll(auth: String): List<FoodLogDtos> = throw UnsupportedOperationException()
        override suspend fun forgotPassword(body: ForgotPasswordReq): Map<String, String> = throw UnsupportedOperationException()
        override suspend fun deleteFoodLog(auth: String, id: Long) {
            throw UnsupportedOperationException()
        }
        override suspend fun getMacroPercents(bearer: String): MacroPercents = MacroPercents()
        override suspend fun saveMacroPercents(bearer: String, body: MacroPercents): MacroPercents = body
        override suspend fun analyzeFoodPhoto(auth: String, image: MultipartBody.Part): PhotoRecognitionRes {
            throw UnsupportedOperationException()
        }
    }
}
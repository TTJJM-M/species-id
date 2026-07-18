package com.ttjjm.speciesid.ui.camera

import com.ttjjm.speciesid.data.RecognitionResponse
import com.ttjjm.speciesid.data.guide.GuideRepository
import com.ttjjm.speciesid.data.guide.RecognitionRecord
import com.ttjjm.speciesid.net.ApiService
import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MultipartBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CameraViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val sampleImage = ByteArray(10) { 0 }

    @Test
    fun `success when backend recognizes`() = runTest {
        val fakeApi = FakeApiService(
            RecognitionResponse(
                recognized = true,
                domain = "动物",
                species = "柯基",
                description = "短腿长身",
                confidence = 88,
            )
        )
        val viewModel = CameraViewModel(apiServiceProvider = { fakeApi })

        viewModel.uiState.test {
            assertTrue(awaitItem() is RecognitionUiState.Idle)

            viewModel.recognizeImage(sampleImage)
            assertTrue(awaitItem() is RecognitionUiState.Loading)
            assertTrue(awaitItem() is RecognitionUiState.Success)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `unrecognized when recognized is false`() = runTest {
        val fakeApi = FakeApiService(
            RecognitionResponse(recognized = false, message = "认不准")
        )
        val viewModel = CameraViewModel(apiServiceProvider = { fakeApi })

        viewModel.uiState.test {
            viewModel.recognizeImage(sampleImage)
            awaitItem() // Idle
            awaitItem() // Loading
            assertTrue(awaitItem() is RecognitionUiState.Unrecognized)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `error when api throws`() = runTest {
        val fakeApi = FakeApiService(throwOnCall = RuntimeException("network down"))
        val viewModel = CameraViewModel(apiServiceProvider = { fakeApi })

        viewModel.uiState.test {
            viewModel.recognizeImage(sampleImage)
            awaitItem() // Idle
            awaitItem() // Loading
            val state = awaitItem()
            assertTrue(state is RecognitionUiState.Error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `error when backend not configured`() = runTest {
        val viewModel = CameraViewModel(apiServiceProvider = { null })

        viewModel.uiState.test {
            awaitItem() // Idle
            viewModel.recognizeImage(sampleImage)
            awaitItem() // Loading
            val state = awaitItem()
            assertTrue(state is RecognitionUiState.Error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `retry reuses last bytes`() = runTest {
        val fakeApi = FakeApiService(
            RecognitionResponse(recognized = false, message = "认不准")
        )
        val viewModel = CameraViewModel(apiServiceProvider = { fakeApi })

        viewModel.recognizeImage(sampleImage)

        viewModel.uiState.test {
            // After retry, state should settle on the new result
            viewModel.retryWithLastBytes()
            skipItems(2) // Loading + result
            assertTrue(awaitItem() is RecognitionUiState.Unrecognized)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `recognition success is saved to guide`() = runTest {
        val response = RecognitionResponse(
            recognized = true,
            domain = "菜品",
            species = "卤鸭腿",
            description = "卤制的鸭腿",
            confidence = 95,
        )
        val fakeRepo = FakeGuideRepository()
        val viewModel = CameraViewModel(
            apiServiceProvider = { FakeApiService(response) },
            guideRepositoryProvider = { fakeRepo },
        )

        viewModel.recognizeImage(sampleImage)

        assertEquals(1, fakeRepo.saved.size)
        assertEquals(sampleImage, fakeRepo.saved[0].first)
        assertEquals(response, fakeRepo.saved[0].second)
    }

    @Test
    fun `unrecognized result is not saved to guide`() = runTest {
        val fakeRepo = FakeGuideRepository()
        val viewModel = CameraViewModel(
            apiServiceProvider = {
                FakeApiService(RecognitionResponse(recognized = false, message = "认不准"))
            },
            guideRepositoryProvider = { fakeRepo },
        )

        viewModel.recognizeImage(sampleImage)

        assertTrue(fakeRepo.saved.isEmpty())
    }

    @Test
    fun `guide save failure does not break recognition result`() = runTest {
        val response = RecognitionResponse(
            recognized = true,
            domain = "植物",
            species = "银杏",
            description = "落叶乔木",
            confidence = 80,
        )
        val failingRepo = FakeGuideRepository(throwOnSave = RuntimeException("disk full"))
        val viewModel = CameraViewModel(
            apiServiceProvider = { FakeApiService(response) },
            guideRepositoryProvider = { failingRepo },
        )

        viewModel.uiState.test {
            awaitItem() // Idle
            viewModel.recognizeImage(sampleImage)
            awaitItem() // Loading
            assertTrue(awaitItem() is RecognitionUiState.Success)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

private class FakeApiService(
    private val response: RecognitionResponse? = null,
    private val throwOnCall: Throwable? = null,
) : ApiService {
    override suspend fun recognize(image: MultipartBody.Part): RecognitionResponse {
        if (throwOnCall != null) throw throwOnCall
        return response!!
    }
}

private class FakeGuideRepository(
    private val throwOnSave: Throwable? = null,
) : GuideRepository {
    val saved = mutableListOf<Pair<ByteArray, RecognitionResponse>>()

    override suspend fun saveRecognition(imageBytes: ByteArray, response: RecognitionResponse) {
        if (throwOnSave != null) throw throwOnSave
        saved += imageBytes to response
    }

    override fun observe(domain: String?, query: String): Flow<List<RecognitionRecord>> =
        flowOf(emptyList())

    override suspend fun delete(record: RecognitionRecord) = Unit
}
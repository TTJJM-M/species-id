package com.ttjjm.speciesid.ui.camera

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ttjjm.speciesid.data.RecognitionResponse
import com.ttjjm.speciesid.data.GuideGraph
import com.ttjjm.speciesid.data.guide.GuideRepository
import com.ttjjm.speciesid.net.ApiService
import com.ttjjm.speciesid.net.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class CameraViewModel(
    private val apiServiceProvider: () -> ApiService? = { RetrofitClient.getApiService() },
    private val guideRepositoryProvider: () -> GuideRepository? = { GuideGraph.repository },
) : ViewModel() {

    private val _uiState = MutableStateFlow<RecognitionUiState>(RecognitionUiState.Idle)
    val uiState: StateFlow<RecognitionUiState> = _uiState.asStateFlow()

    private val _pendingBytes = MutableStateFlow<ByteArray?>(null)
    val pendingBytes: StateFlow<ByteArray?> = _pendingBytes.asStateFlow()

    fun recognizeImage(imageBytes: ByteArray) {
        _pendingBytes.value = imageBytes
        _uiState.value = RecognitionUiState.Loading
        viewModelScope.launch { callApi(imageBytes) }
    }

    private suspend fun callApi(imageBytes: ByteArray) {
        val service = apiServiceProvider()
        if (service == null) {
            _uiState.value =
                RecognitionUiState.Error("未设置后端地址，请先在设置中填写")
            return
        }

        val body = imageBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("image", "photo.jpg", body)

        val result = runCatching { service.recognize(part) }

        result
            .onSuccess { response ->
                _uiState.value = if (response.recognized) {
                    // 写图鉴失败不影响识别结果展示,但要留下线索
                    runCatching { guideRepositoryProvider()?.saveRecognition(imageBytes, response) }
                        .onFailure { Log.w("SpeciesId", "识别结果写入图鉴失败", it) }
                    RecognitionUiState.Success(response)
                } else {
                    RecognitionUiState.Unrecognized
                }
            }
            .onFailure { error ->
                _uiState.value = RecognitionUiState.Error(error.message ?: "连接失败")
            }
    }

    fun retryWithLastBytes() {
        val bytes = _pendingBytes.value
        if (bytes != null) {
            _uiState.value = RecognitionUiState.Loading
            viewModelScope.launch { callApi(bytes) }
        }
    }

    fun reset() {
        _uiState.value = RecognitionUiState.Idle
        _pendingBytes.value = null
    }
}
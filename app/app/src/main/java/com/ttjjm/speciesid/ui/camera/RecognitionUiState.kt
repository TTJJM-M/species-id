package com.ttjjm.speciesid.ui.camera

import com.ttjjm.speciesid.data.RecognitionResponse

sealed interface RecognitionUiState {
    data object Idle : RecognitionUiState
    data object Loading : RecognitionUiState
    data class Success(val result: RecognitionResponse) : RecognitionUiState
    data object Unrecognized : RecognitionUiState
    data class Error(val message: String) : RecognitionUiState
}
package com.ttjjm.speciesid.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RecognitionResponse(
    val recognized: Boolean = false,
    val domain: String? = null,
    val species: String? = null,
    val description: String? = null,
    val confidence: Int? = null,
    val message: String? = null,
)
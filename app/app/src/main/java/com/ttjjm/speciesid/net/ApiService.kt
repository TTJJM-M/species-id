package com.ttjjm.speciesid.net

import com.ttjjm.speciesid.data.RecognitionResponse
import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {

    @Multipart
    @POST("recognize")
    suspend fun recognize(@Part image: MultipartBody.Part): RecognitionResponse
}
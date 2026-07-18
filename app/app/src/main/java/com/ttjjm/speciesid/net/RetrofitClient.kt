package com.ttjjm.speciesid.net

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object RetrofitClient {

    private var apiService: ApiService? = null
    private var currentBaseUrl: String? = null

    private const val DEFAULT_BASE_URL = "http://192.168.1.3:8000/"

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences("species_id", Context.MODE_PRIVATE)
        val savedUrl = prefs.getString("backend_url", null)
        if (savedUrl != null) {
            updateBaseUrl(savedUrl)
        } else {
            // 首次启动直接用默认地址，无需用户配置
            updateBaseUrl(DEFAULT_BASE_URL)
        }
    }

    fun updateBaseUrl(baseUrl: String): Boolean {
        var normalized = baseUrl.trim()
        if (normalized.isEmpty()) return false
        if (!normalized.endsWith("/")) normalized += "/"

        return try {
            val client = OkHttpClient.Builder()
                .addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                })
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(normalized)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()

            apiService = retrofit.create(ApiService::class.java)
            currentBaseUrl = normalized
            true
        } catch (e: Exception) {
            false
        }
    }

    fun saveBaseUrl(context: Context, baseUrl: String): Boolean {
        val ok = updateBaseUrl(baseUrl)
        if (ok) {
            context.getSharedPreferences("species_id", Context.MODE_PRIVATE)
                .edit()
                .putString("backend_url", currentBaseUrl)
                .apply()
        }
        return ok
    }

    fun getCurrentBaseUrl(): String? = currentBaseUrl

    fun getApiService(): ApiService? = apiService
}
package com.example.chat_box_android.data.network

import com.example.chat_box_android.data.local.TokenManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    
    @Volatile
    private var retrofit: Retrofit? = null
    
    fun getInstance(tokenManager: TokenManager): Retrofit {
        return retrofit ?: synchronized(this) {
            retrofit ?: buildRetrofit(tokenManager).also { retrofit = it }
        }
    }
    
    private fun buildRetrofit(tokenManager: TokenManager): Retrofit {
        val okHttpClientBuilder = OkHttpClient.Builder()
            .connectTimeout(ApiConfig.CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(ApiConfig.READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(ApiConfig.WRITE_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(tokenManager))
        
        // Add logging interceptor (you can enable/disable this manually for debugging)
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        okHttpClientBuilder.addInterceptor(loggingInterceptor)
        
        return Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(okHttpClientBuilder.build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}

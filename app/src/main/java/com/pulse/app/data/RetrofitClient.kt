package com.pulse.app.data

import com.pulse.app.BuildConfig
import com.pulse.app.SupabaseClient
import io.github.jan.supabase.auth.auth
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    val api: ApiService by lazy {
        val httpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val token = SupabaseClient.client.auth.currentAccessTokenOrNull()
                val request = chain.request().newBuilder().apply {
                    if (!token.isNullOrBlank()) {
                        header("Authorization", "Bearer $token")
                    }
                }.build()
                chain.proceed(request)
            }
            .build()

        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

package com.myoutdoor.agent.retrofit

import android.annotation.SuppressLint
import appentus.datasource.api.ApiInterface
import com.google.gson.GsonBuilder
import com.trax.app.retrofit.Constants
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    //==============================================================================
    // Network Client Builders
    //==============================================================================

    //--------------------------------------------------
    // What it does: Configures and returns standard OkHttpClient and Retrofit client instances.
    // When it is called: Triggered when user login / registration operations are dispatching.
    // Why it is required: Builds unauthenticated service interfaces.
    //--------------------------------------------------
    fun getApiClient(): ApiInterface? {
        val gson = GsonBuilder().setLenient().create()

        val httpClient = OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.MINUTES)
            .writeTimeout(3, TimeUnit.MINUTES)
            .readTimeout(3, TimeUnit.MINUTES)

        httpClient.addInterceptor { chain ->
            val newRequest = chain.request().newBuilder()
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .build()
            chain.proceed(newRequest)
        }

        val interceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        httpClient.addInterceptor(interceptor)

        val retrofit = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create(gson))
            .baseUrl(Constants.BASE_URL)
            .client(httpClient.build())
            .build()

        return retrofit.create(ApiInterface::class.java)
    }

    //--------------------------------------------------
    // What it does: Configures and returns OkHttpClient and Retrofit clients holding active bearer auth tokens.
    // When it is called: Triggered when invoking authenticated calls (e.g. tracks, profile detail operations).
    // Why it is required: Submits token headers matching active logged user sessions.
    //--------------------------------------------------
    @SuppressLint("SuspiciousIndentation")
    fun getApiClientWithHeader(token: String): ApiInterface? {
        val gson = GsonBuilder().setLenient().create()

        val httpClient = OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.MINUTES)
            .writeTimeout(3, TimeUnit.MINUTES)
            .readTimeout(3, TimeUnit.MINUTES)

        httpClient.addInterceptor { chain ->
            val newRequest = chain.request().newBuilder()
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .addHeader("Authorization", "Bearer $token")
                .build()
            chain.proceed(newRequest)
        }

        val interceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        httpClient.addInterceptor(interceptor)

        val retrofit = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create(gson))
            .baseUrl(Constants.BASE_URL)
            .client(httpClient.build())
            .build()

        return retrofit.create(ApiInterface::class.java)
    }
}
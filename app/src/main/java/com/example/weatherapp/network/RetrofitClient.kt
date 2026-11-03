package com.example.weatherapp.network

import android.content.Context
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

// RetryInterceptor: automatically retries failed network calls up to maxRetries times
class RetryInterceptor(private val maxRetries: Int = 3) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var lastException: IOException? = null

        repeat(maxRetries) { attempt ->
            try {
                return chain.proceed(chain.request())
            } catch (e: IOException) {
                // IOException = network failure (no internet, timeout, DNS error)
                // NOT thrown for HTTP 404 or 401 -- those are valid responses, not exceptions
                lastException = e
            }
        }
        throw lastException ?: IOException("Request failed after $maxRetries retries")
    }
}

object RetrofitClient {

    private lateinit var appContext: Context
    private lateinit var cache: Cache
    // Hoisted to object level so clearCache() below can reach the same Cache instance

    // Call once from MainActivity.onCreate(), before setContent { }
    fun init(context: Context) {
        appContext = context.applicationContext
        // .applicationContext = the global app context, not tied to any specific Activity
        // Using applicationContext prevents memory leaks
    }

    fun clearCache() {
        if (::cache.isInitialized) {
            cache.evictAll()
            // evictAll() = OkHttp API that deletes every cached response immediately
        }
    }

    val weatherApiService: WeatherApiService by lazy {
        val cacheDir = File(appContext.cacheDir, "http_cache")
        // appContext.cacheDir = the app's private cache directory on disk
        cache = Cache(cacheDir, 5L * 1024 * 1024)
        // Assigns the hoisted property (not "val cache =") -- 5MB max disk space

        val okHttpClient = OkHttpClient.Builder()
            .cache(cache)
            .addNetworkInterceptor { chain ->
                chain.proceed(chain.request()).newBuilder()
                    .header("Cache-Control", "public, max-age=300")
                    // max-age=300 = cache is valid for 300 seconds (5 minutes)
                    .build()
            }
            .addInterceptor(RetryInterceptor(maxRetries = 3))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(AppConstants.WEATHER_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApiService::class.java)
    }

    val feedbackApiService: FeedbackApiService by lazy {
        // Feedback service -- no cache (POST requests should not be cached)
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(RetryInterceptor(maxRetries = 3))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(AppConstants.FEEDBACK_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FeedbackApiService::class.java)
    }
}
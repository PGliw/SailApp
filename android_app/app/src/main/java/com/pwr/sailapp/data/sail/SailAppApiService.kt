package com.pwr.sailapp.data.sail

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.pwr.sailapp.data.sail.response.CentresResponse
import kotlinx.coroutines.Deferred
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

/*
getCentres: https://0e4682b3-c081-4689-b8c5-51f3f0a7ae09.mock.pstmn.io/getCentres
getAllCentreGear: https://0e4682b3-c081-4689-b8c5-51f3f0a7ae09.mock.pstmn.io/getAllCentreGear?centre_id=1
 */

const val SERVER_URL = "https://0e4682b3-c081-4689-b8c5-51f3f0a7ae09.mock.pstmn.io"

interface SailAppApiService {

    @GET("getCentres")
    fun getCentres(): Deferred<CentresResponse> // defer - odraczać

    companion object{
        // syntax: SailAppApiService()
        operator fun invoke(): SailAppApiService{
            // Interceptors are a powerful mechanism that can monitor, rewrite, and retry calls (OkHttp3)
            val requestInterceptor = Interceptor{
                val request = it.request()
                    .newBuilder()
                    .build()

                return@Interceptor it.proceed(request)
            }

            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(requestInterceptor)
                .build()

            return Retrofit.Builder()
                .client(okHttpClient)
                .baseUrl(SERVER_URL)
                .addCallAdapterFactory(CoroutineCallAdapterFactory())
                .addConverterFactory(GsonConverterFactory.create()) // use Gson to parse Json
                .build()
                .create(SailAppApiService::class.java)
        }
    }
}
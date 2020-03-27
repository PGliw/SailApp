package com.pwr.sailapp.data.network.sail

import com.pwr.sailapp.internal.ErrorCodeException
import okhttp3.Interceptor
import okhttp3.Response
import kotlin.Exception

class ErrorHandlingInterceptor : Interceptor{

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        try {
            val response = chain.proceed(request)
            if(response.isSuccessful) return response
            else throw ErrorCodeException(response.code(), response.message())
        } catch (e: Exception){
            throw ErrorCodeException(-1, e.message?: "Unknown error occurred")
        }
    }

}
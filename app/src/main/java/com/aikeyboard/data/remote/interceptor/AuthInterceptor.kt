package com.aikeyboard.data.remote.interceptor

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Authentication interceptor for API requests
 * 
 * Adds authorization header to requests that require authentication.
 */
class AuthInterceptor(
    private val apiKey: String,
    private val authType: AuthType = AuthType.BEARER
) : Interceptor {

    enum class AuthType {
        BEARER,
        API_KEY,
        CUSTOM
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .apply {
                when (authType) {
                    AuthType.BEARER -> addHeader("Authorization", "Bearer $apiKey")
                    AuthType.API_KEY -> addHeader("X-API-Key", apiKey)
                    AuthType.CUSTOM -> addHeader("Authorization", apiKey)
                }
            }
            .build()
        
        return chain.proceed(request)
    }

    companion object {
        /**
         * Create a Bearer token interceptor
         */
        fun bearer(apiKey: String): AuthInterceptor {
            return AuthInterceptor(apiKey, AuthType.BEARER)
        }

        /**
         * Create an API key interceptor
         */
        fun apiKey(apiKey: String): AuthInterceptor {
            return AuthInterceptor(apiKey, AuthType.API_KEY)
        }

        /**
         * Create a custom auth interceptor
         */
        fun custom(authHeader: String): AuthInterceptor {
            return AuthInterceptor(authHeader, AuthType.CUSTOM)
        }
    }
}

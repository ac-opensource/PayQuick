package com.payquick.data.network

import com.payquick.data.session.SessionManager
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Interceptor
import okhttp3.Response

@Singleton
class AuthInterceptor @Inject constructor(
    private val sessionManager: SessionManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val session = sessionManager.session.value

        val shouldAttachHeader = session?.accessToken?.isNotBlank() == true &&
            !original.url.encodedPath.endsWith("/v1/login")

        val request = if (shouldAttachHeader) {
            original.newBuilder()
                .addHeader("Authorization", "Bearer ${session.accessToken}")
                .build()
        } else {
            original
        }

        return chain.proceed(request)
    }
}

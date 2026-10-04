package com.gridsync.mobile.data.remote

import com.gridsync.mobile.data.remote.dto.ApiMessageDto
import com.google.gson.Gson
import retrofit2.HttpException
import java.io.IOException

class ApiException(message: String) : Exception(message)

fun Throwable.toUserMessage(): String {
    return when (this) {
        is ApiException -> message ?: "Request failed"
        is HttpException -> {
            val body = response()?.errorBody()?.string()
            val parsed = runCatching {
                Gson().fromJson(body, ApiMessageDto::class.java)?.message
            }.getOrNull()
            parsed
                ?: when (code()) {
                    401 -> "Invalid email or password."
                    400 -> "Bad request. Check your details and try again."
                    409 -> "This account already exists."
                    else -> "Request failed (${code()})"
                }
        }
        is IOException -> {
            val detail = message?.takeIf { it.isNotBlank() }
            val base = ApiConfig.BASE_URL.trimEnd('/')
            if (detail != null) {
                "Cannot reach the server at $base ($detail). On a physical phone use your Mac LAN IP in local.properties (API_BASE_URL), not 10.0.2.2."
            } else {
                "Cannot reach the server at $base. Is the API running, and is API_BASE_URL correct for emulator vs phone?"
            }
        }
        else -> message ?: "Something went wrong"
    }
}

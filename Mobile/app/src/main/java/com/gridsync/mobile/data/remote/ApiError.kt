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
            if (detail != null) {
                "Cannot reach the server ($detail). Is the API running on port 5269?"
            } else {
                "Cannot reach the server. Check that the API is running on port 5269."
            }
        }
        else -> message ?: "Something went wrong"
    }
}

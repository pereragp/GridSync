package com.gridsync.mobile.data.remote

import com.gridsync.mobile.BuildConfig

/**
 * Backend base URL from `Mobile/local.properties` → `API_BASE_URL`.
 *
 * - Emulator: `http://10.0.2.2:5269/` (special alias for the host machine)
 * - Physical phone on same Wi‑Fi: `http://<your-mac-lan-ip>:5269/`
 *
 * API must listen on all interfaces (`http://0.0.0.0:5269`).
 */
object ApiConfig {
    val BASE_URL: String = BuildConfig.API_BASE_URL.let { url ->
        if (url.endsWith("/")) url else "$url/"
    }
}

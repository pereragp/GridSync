package com.gridsync.mobile.data.remote

/**
 * Emulator → host machine (special Android alias for your Mac's localhost).
 *
 * Requires the API to listen on all interfaces, e.g. http://0.0.0.0:5269
 * (see GridSync.Api launchSettings "http" profile).
 *
 * Physical device on the same Wi‑Fi: use your Mac LAN IP instead, e.g.
 * http://192.168.1.4:5269/ — and allow GridSync.Api through macOS Firewall.
 */
object ApiConfig {
    const val BASE_URL = "http://10.0.2.2:5269/"
}

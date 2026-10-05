package com.gridsync.mobile.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

data class LatLngPoint(
    val latitude: Double,
    val longitude: Double,
)

data class ResolvedLocation(
    val point: LatLngPoint,
    /** True when GPS/permission was unavailable and Colombo fallback was used. */
    val isFallback: Boolean,
)

/**
 * Resolves the device location for nearby-station queries.
 * Falls back to Colombo city center when permission is missing or GPS is unavailable.
 */
class UserLocationProvider(
    private val context: Context,
) {
    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    /** System location toggle (GPS / network). Permission alone is not enough. */
    fun isLocationEnabled(): Boolean {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return false
        return runCatching {
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }.getOrDefault(false)
    }

    @SuppressLint("MissingPermission")
    suspend fun resolve(
        preferFresh: Boolean = true,
        highAccuracy: Boolean = false,
    ): ResolvedLocation {
        if (!hasLocationPermission()) {
            return ResolvedLocation(FALLBACK_COLOMBO, isFallback = true)
        }
        if (!isLocationEnabled()) {
            return ResolvedLocation(FALLBACK_COLOMBO, isFallback = true)
        }

        val priority = if (highAccuracy) {
            Priority.PRIORITY_HIGH_ACCURACY
        } else {
            Priority.PRIORITY_BALANCED_POWER_ACCURACY
        }
        val timeoutMs = if (highAccuracy) 12_000L else 8_000L

        val current = if (preferFresh) {
            withTimeoutOrNull(timeoutMs) {
                val token = CancellationTokenSource()
                try {
                    fusedClient.getCurrentLocation(priority, token.token).await()
                } finally {
                    token.cancel()
                }
            }
        } else {
            null
        }

        val location: Location? = current ?: withTimeoutOrNull(4_000L) {
            fusedClient.lastLocation.await()
        }

        return if (location != null) {
            ResolvedLocation(location.toLatLngPoint(), isFallback = false)
        } else {
            ResolvedLocation(FALLBACK_COLOMBO, isFallback = true)
        }
    }

    private fun Location.toLatLngPoint() = LatLngPoint(latitude, longitude)

    companion object {
        /** Same default as the web LocationPicker. */
        val FALLBACK_COLOMBO = LatLngPoint(latitude = 6.9271, longitude = 79.8612)
    }
}

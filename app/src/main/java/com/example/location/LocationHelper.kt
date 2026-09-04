package com.example.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class Coordinates(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float = 0f,
    val isRealLocation: Boolean = false
) {
    companion object {
        // Sensible default location (e.g., Mexico City historic center / vibrant urban area)
        val DEFAULT = Coordinates(
            latitude = 19.432608,
            longitude = -99.133209,
            accuracy = 10f,
            isRealLocation = false
        )
    }
}

class LocationHelper(private val context: Context) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    fun hasLocationPermission(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocation || coarseLocation
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Coordinates {
        if (!hasLocationPermission()) {
            return Coordinates.DEFAULT
        }

        return try {
            suspendCancellableCoroutine { continuation ->
                val cts = CancellationTokenSource()
                continuation.invokeOnCancellation {
                    cts.cancel()
                }

                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    cts.token
                ).addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        continuation.resume(
                            Coordinates(
                                latitude = location.latitude,
                                longitude = location.longitude,
                                accuracy = location.accuracy,
                                isRealLocation = true
                            )
                        )
                    } else {
                        // Try last known location as fallback
                        fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc: Location? ->
                            if (lastLoc != null) {
                                continuation.resume(
                                    Coordinates(
                                        latitude = lastLoc.latitude,
                                        longitude = lastLoc.longitude,
                                        accuracy = lastLoc.accuracy,
                                        isRealLocation = true
                                    )
                                )
                            } else {
                                continuation.resume(Coordinates.DEFAULT)
                            }
                        }.addOnFailureListener {
                            continuation.resume(Coordinates.DEFAULT)
                        }
                    }
                }.addOnFailureListener {
                    continuation.resume(Coordinates.DEFAULT)
                }
            }
        } catch (e: Exception) {
            Coordinates.DEFAULT
        }
    }
}

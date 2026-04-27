package com.sailapp.util

import android.location.Location

object SpeedFilter {

    /**
     * Returns true if this location fix should be accepted (not filtered out).
     */
    fun isValid(location: Location, lastLocation: Location?): Boolean {
        // Filter poor accuracy
        if (location.hasAccuracy() && location.accuracy > GeoUtils.MAX_ACCURACY_M) return false

        // Filter impossible speed
        if (location.hasSpeed() && location.speed > GeoUtils.MAX_SPEED_MPS) return false

        // Filter micro-movements
        if (lastLocation != null) {
            val dist = GeoUtils.distanceMeters(
                lastLocation.latitude, lastLocation.longitude,
                location.latitude, location.longitude
            )
            if (dist < GeoUtils.MIN_DISTANCE_M) return false
        }

        return true
    }
}

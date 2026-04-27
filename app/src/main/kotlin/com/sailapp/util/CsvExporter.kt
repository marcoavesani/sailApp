package com.sailapp.util

import com.sailapp.data.db.entities.TrackPoint
import java.text.SimpleDateFormat
import java.util.*

object CsvExporter {

    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    val HEADER = "trip_id,segment_id,timestamp_utc,latitude,longitude,altitude_m," +
            "sog_mps,cog_deg,heading_deg,horizontal_accuracy_m,battery_percent,is_valid_for_stats"

    fun export(points: List<TrackPoint>): String {
        val sb = StringBuilder()
        sb.appendLine(HEADER)
        for (pt in points) {
            sb.appendLine(
                "${pt.tripId},${pt.segmentId}," +
                "${isoFormat.format(Date(pt.timestampUtcMs))}," +
                "${pt.latitude},${pt.longitude}," +
                "${pt.altitudeM ?: ""}," +
                "${pt.sogMps ?: ""}," +
                "${pt.cogDeg ?: ""}," +
                "${pt.headingDeg ?: ""}," +
                "${pt.horizontalAccuracyM ?: ""}," +
                "${pt.batteryPercent ?: ""}," +
                "${pt.isValidForStats}"
            )
        }
        return sb.toString()
    }
}

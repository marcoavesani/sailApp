package com.sailapp.util

import com.sailapp.data.db.entities.TrackPoint
import com.sailapp.data.db.entities.TripSegment
import com.sailapp.data.db.entities.Waypoint
import java.text.SimpleDateFormat
import java.util.*

object GpxExporter {

    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun export(
        tripName: String,
        segments: List<TripSegment>,
        pointsBySegment: Map<Long, List<TrackPoint>>,
        waypoints: List<Waypoint>
    ): String {
        val sb = StringBuilder()
        sb.appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        sb.appendLine("""<gpx version="1.1" creator="SailApp" xmlns="http://www.topografix.com/GPX/1/1">""")
        sb.appendLine("""  <metadata><name>${escapeXml(tripName)}</name></metadata>""")

        // Waypoints
        for (wpt in waypoints) {
            sb.appendLine("""  <wpt lat="${wpt.latitude}" lon="${wpt.longitude}">""")
            sb.appendLine("""    <name>${escapeXml(wpt.name)}</name>""")
            sb.appendLine("""    <time>${isoFormat.format(Date(wpt.createdAtUtcMs))}</time>""")
            sb.appendLine("""    <type>${escapeXml(wpt.type)}</type>""")
            if (wpt.notes.isNotBlank()) {
                sb.appendLine("""    <desc>${escapeXml(wpt.notes)}</desc>""")
            }
            sb.appendLine("""  </wpt>""")
        }

        // Track
        sb.appendLine("""  <trk><name>${escapeXml(tripName)}</name>""")
        for (segment in segments) {
            val points = pointsBySegment[segment.id] ?: continue
            if (points.isEmpty()) continue
            sb.appendLine("""    <trkseg>""")
            for (pt in points) {
                sb.append("""      <trkpt lat="${pt.latitude}" lon="${pt.longitude}">""")
                if (pt.altitudeM != null) sb.append("""<ele>${pt.altitudeM}</ele>""")
                sb.append("""<time>${isoFormat.format(Date(pt.timestampUtcMs))}</time>""")
                if (pt.sogMps != null) {
                    sb.append("""<extensions><speed>${pt.sogMps}</speed></extensions>""")
                }
                sb.appendLine("""</trkpt>""")
            }
            sb.appendLine("""    </trkseg>""")
        }
        sb.appendLine("""  </trk>""")
        sb.appendLine("""</gpx>""")
        return sb.toString()
    }

    private fun escapeXml(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}

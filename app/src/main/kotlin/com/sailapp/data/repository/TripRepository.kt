package com.sailapp.data.repository

import com.sailapp.data.db.dao.*
import com.sailapp.data.db.entities.*
import com.sailapp.util.GeoUtils
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TripRepository @Inject constructor(
    private val tripDao: TripDao,
    private val trackPointDao: TrackPointDao,
    private val waypointDao: WaypointDao,
    private val tripEventDao: TripEventDao,
    private val regattaDao: RegattaDao
) {
    fun getAllTrips(): Flow<List<Trip>> = tripDao.getAllTrips()

    suspend fun getTripById(id: Long): Trip? = tripDao.getTripById(id)

    suspend fun getActiveTrips(): List<Trip> = tripDao.getActiveTrips()

    suspend fun createTrip(name: String): Long {
        val trip = Trip(
            name = name,
            createdAtUtcMs = System.currentTimeMillis(),
            status = "planned"
        )
        return tripDao.insertTrip(trip)
    }

    suspend fun startTrip(tripId: Long): Long {
        val now = System.currentTimeMillis()
        val trip = tripDao.getTripById(tripId) ?: return -1
        tripDao.updateTrip(trip.copy(status = "recording", startedAtUtcMs = now))
        val segment = TripSegment(tripId = tripId, startedAtUtcMs = now, reason = "normal")
        return tripDao.insertSegment(segment)
    }

    suspend fun pauseTrip(tripId: Long, segmentId: Long) {
        val now = System.currentTimeMillis()
        tripDao.updateTripStatus(tripId, "paused")
        tripDao.endSegment(segmentId, now)
    }

    suspend fun resumeTrip(tripId: Long): Long {
        val now = System.currentTimeMillis()
        tripDao.updateTripStatus(tripId, "recording")
        val segment = TripSegment(tripId = tripId, startedAtUtcMs = now, reason = "pause_resume")
        return tripDao.insertSegment(segment)
    }

    suspend fun stopTrip(tripId: Long, segmentId: Long) {
        val now = System.currentTimeMillis()
        tripDao.endSegment(segmentId, now)
        tripDao.finishTrip(tripId, now, "completed")
        recalculateTripDistance(tripId)
    }

    suspend fun markTripInterrupted(tripId: Long) {
        tripDao.updateTripStatus(tripId, "interrupted")
    }

    suspend fun discardTrip(tripId: Long) {
        val trip = tripDao.getTripById(tripId) ?: return
        tripDao.updateTrip(trip.copy(status = "discarded"))
    }

    suspend fun recoverTrip(tripId: Long): Long {
        val now = System.currentTimeMillis()
        tripDao.updateTripStatus(tripId, "recording")
        val segment = TripSegment(tripId = tripId, startedAtUtcMs = now, reason = "app_recovery")
        return tripDao.insertSegment(segment)
    }

    suspend fun addTrackPoint(point: TrackPoint) {
        trackPointDao.insertTrackPoint(point)
    }

    suspend fun getLastTrackPoint(tripId: Long): TrackPoint? =
        trackPointDao.getLastPointForTrip(tripId)

    fun getTrackPointsForTrip(tripId: Long): Flow<List<TrackPoint>> =
        trackPointDao.getPointsForTrip(tripId)

    suspend fun getTrackPointsSync(tripId: Long): List<TrackPoint> =
        trackPointDao.getPointsForTripSync(tripId)

    suspend fun getSegmentsForTrip(tripId: Long): List<TripSegment> =
        tripDao.getSegmentsForTripSync(tripId)

    suspend fun addWaypoint(waypoint: Waypoint): Long =
        waypointDao.insertWaypoint(waypoint)

    fun getWaypointsForTrip(tripId: Long): Flow<List<Waypoint>> =
        waypointDao.getWaypointsForTrip(tripId)

    suspend fun getWaypointsForTripSync(tripId: Long): List<Waypoint> =
        waypointDao.getWaypointsForTripSync(tripId)

    suspend fun getWaypointById(id: Long): Waypoint? = waypointDao.getWaypointById(id)

    suspend fun addTripEvent(event: TripEvent): Long =
        tripEventDao.insertEvent(event)

    fun getEventsForTrip(tripId: Long): Flow<List<TripEvent>> =
        tripEventDao.getEventsForTrip(tripId)

    // Regatta
    fun getAllRegattaSessions(): Flow<List<RegattaSession>> = regattaDao.getAllSessions()

    suspend fun getRegattaSessionById(id: Long): RegattaSession? = regattaDao.getSessionById(id)

    suspend fun createRegattaSession(name: String, tripId: Long? = null): Long {
        val session = RegattaSession(tripId = tripId, name = name)
        return regattaDao.insertSession(session)
    }

    suspend fun updateRegattaSession(session: RegattaSession) = regattaDao.updateSession(session)

    suspend fun getStartLineForSession(sessionId: Long): StartLine? =
        regattaDao.getStartLineForSession(sessionId)

    suspend fun upsertStartLine(startLine: StartLine): Long =
        regattaDao.insertStartLine(startLine)

    suspend fun updateStartLine(startLine: StartLine) = regattaDao.updateStartLine(startLine)

    // Statistics helpers
    suspend fun getTripStats(tripId: Long): TripStats {
        val points = trackPointDao.getPointsForTripSync(tripId)
        val validPoints = points.filter { it.isValidForStats }
        val trip = tripDao.getTripById(tripId)

        val totalDistanceM = if (validPoints.size >= 2) {
            var dist = 0.0
            for (i in 1 until validPoints.size) {
                dist += GeoUtils.distanceMeters(
                    validPoints[i - 1].latitude, validPoints[i - 1].longitude,
                    validPoints[i].latitude, validPoints[i].longitude
                )
            }
            dist
        } else 0.0

        val maxSpeedMps = validPoints.mapNotNull { it.sogMps }.maxOrNull() ?: 0f
        val avgSpeedMps = validPoints.mapNotNull { it.sogMps }
            .takeIf { it.isNotEmpty() }?.average()?.toFloat() ?: 0f

        val elapsedMs = if (trip?.startedAtUtcMs != null && trip.endedAtUtcMs != null)
            trip.endedAtUtcMs - trip.startedAtUtcMs
        else if (trip?.startedAtUtcMs != null)
            System.currentTimeMillis() - trip.startedAtUtcMs
        else 0L

        val movingPoints = validPoints.filter { (it.sogMps ?: 0f) > 0.5f }
        val movingTimeMs = if (movingPoints.size >= 2) {
            movingPoints.last().timestampUtcMs - movingPoints.first().timestampUtcMs
        } else 0L

        return TripStats(
            totalDistanceM = totalDistanceM,
            elapsedMs = elapsedMs,
            movingTimeMs = movingTimeMs,
            maxSpeedMps = maxSpeedMps,
            avgSpeedMps = avgSpeedMps,
            pointCount = points.size
        )
    }

    private suspend fun recalculateTripDistance(tripId: Long) {
        val points = trackPointDao.getPointsForTripSync(tripId).filter { it.isValidForStats }
        var dist = 0.0
        for (i in 1 until points.size) {
            dist += GeoUtils.distanceMeters(
                points[i - 1].latitude, points[i - 1].longitude,
                points[i].latitude, points[i].longitude
            )
        }
        tripDao.updateTripDistance(tripId, dist)
    }
}

data class TripStats(
    val totalDistanceM: Double,
    val elapsedMs: Long,
    val movingTimeMs: Long,
    val maxSpeedMps: Float,
    val avgSpeedMps: Float,
    val pointCount: Int
)

package com.sailapp.data.db.dao

import androidx.room.*
import com.sailapp.data.db.entities.TrackPoint
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackPointDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackPoint(point: TrackPoint): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackPoints(points: List<TrackPoint>)

    @Query("SELECT * FROM track_points WHERE tripId = :tripId ORDER BY timestampUtcMs ASC")
    fun getPointsForTrip(tripId: Long): Flow<List<TrackPoint>>

    @Query("SELECT * FROM track_points WHERE tripId = :tripId ORDER BY timestampUtcMs ASC")
    suspend fun getPointsForTripSync(tripId: Long): List<TrackPoint>

    @Query("SELECT * FROM track_points WHERE segmentId = :segmentId ORDER BY timestampUtcMs ASC")
    suspend fun getPointsForSegment(segmentId: Long): List<TrackPoint>

    @Query("SELECT COUNT(*) FROM track_points WHERE tripId = :tripId")
    suspend fun getPointCountForTrip(tripId: Long): Int

    @Query("SELECT * FROM track_points WHERE tripId = :tripId ORDER BY timestampUtcMs DESC LIMIT 1")
    suspend fun getLastPointForTrip(tripId: Long): TrackPoint?

    @Query("SELECT * FROM track_points WHERE tripId = :tripId AND isValidForStats = 1 ORDER BY sogMps DESC LIMIT 1")
    suspend fun getMaxSpeedPoint(tripId: Long): TrackPoint?

    @Query("""
        SELECT AVG(sogMps) FROM track_points 
        WHERE tripId = :tripId AND isValidForStats = 1 AND sogMps IS NOT NULL
    """)
    suspend fun getAverageSpeedMps(tripId: Long): Float?

    @Query("DELETE FROM track_points WHERE tripId = :tripId")
    suspend fun deletePointsForTrip(tripId: Long)
}

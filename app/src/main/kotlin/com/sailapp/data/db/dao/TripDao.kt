package com.sailapp.data.db.dao

import androidx.room.*
import com.sailapp.data.db.entities.Trip
import com.sailapp.data.db.entities.TripSegment
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {

    @Query("SELECT * FROM trips ORDER BY createdAtUtcMs DESC")
    fun getAllTrips(): Flow<List<Trip>>

    @Query("SELECT * FROM trips WHERE id = :tripId")
    suspend fun getTripById(tripId: Long): Trip?

    @Query("SELECT * FROM trips WHERE status IN ('recording', 'paused', 'interrupted')")
    suspend fun getActiveTrips(): List<Trip>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: Trip): Long

    @Update
    suspend fun updateTrip(trip: Trip)

    @Query("UPDATE trips SET status = :status WHERE id = :tripId")
    suspend fun updateTripStatus(tripId: Long, status: String)

    @Query("UPDATE trips SET totalDistanceM = :distanceM WHERE id = :tripId")
    suspend fun updateTripDistance(tripId: Long, distanceM: Double)

    @Query("UPDATE trips SET endedAtUtcMs = :endedAtMs, status = :status WHERE id = :tripId")
    suspend fun finishTrip(tripId: Long, endedAtMs: Long, status: String)

    @Delete
    suspend fun deleteTrip(trip: Trip)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSegment(segment: TripSegment): Long

    @Query("SELECT * FROM trip_segments WHERE tripId = :tripId ORDER BY startedAtUtcMs ASC")
    fun getSegmentsForTrip(tripId: Long): Flow<List<TripSegment>>

    @Query("SELECT * FROM trip_segments WHERE tripId = :tripId ORDER BY startedAtUtcMs ASC")
    suspend fun getSegmentsForTripSync(tripId: Long): List<TripSegment>

    @Query("UPDATE trip_segments SET endedAtUtcMs = :endedAtMs WHERE id = :segmentId")
    suspend fun endSegment(segmentId: Long, endedAtMs: Long)
}

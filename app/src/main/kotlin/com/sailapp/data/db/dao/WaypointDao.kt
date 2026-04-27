package com.sailapp.data.db.dao

import androidx.room.*
import com.sailapp.data.db.entities.Waypoint
import kotlinx.coroutines.flow.Flow

@Dao
interface WaypointDao {

    @Query("SELECT * FROM waypoints ORDER BY createdAtUtcMs DESC")
    fun getAllWaypoints(): Flow<List<Waypoint>>

    @Query("SELECT * FROM waypoints WHERE tripId = :tripId ORDER BY createdAtUtcMs ASC")
    fun getWaypointsForTrip(tripId: Long): Flow<List<Waypoint>>

    @Query("SELECT * FROM waypoints WHERE tripId = :tripId ORDER BY createdAtUtcMs ASC")
    suspend fun getWaypointsForTripSync(tripId: Long): List<Waypoint>

    @Query("SELECT * FROM waypoints WHERE id = :id")
    suspend fun getWaypointById(id: Long): Waypoint?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaypoint(waypoint: Waypoint): Long

    @Update
    suspend fun updateWaypoint(waypoint: Waypoint)

    @Delete
    suspend fun deleteWaypoint(waypoint: Waypoint)
}

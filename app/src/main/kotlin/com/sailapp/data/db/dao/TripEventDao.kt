package com.sailapp.data.db.dao

import androidx.room.*
import com.sailapp.data.db.entities.TripEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface TripEventDao {

    @Query("SELECT * FROM trip_events WHERE tripId = :tripId ORDER BY timestampUtcMs ASC")
    fun getEventsForTrip(tripId: Long): Flow<List<TripEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: TripEvent): Long

    @Delete
    suspend fun deleteEvent(event: TripEvent)

    @Query("DELETE FROM trip_events WHERE tripId = :tripId")
    suspend fun deleteEventsForTrip(tripId: Long)
}

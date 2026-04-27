package com.sailapp.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sailapp.data.db.dao.*
import com.sailapp.data.db.entities.*

@Database(
    entities = [
        Trip::class,
        TripSegment::class,
        TrackPoint::class,
        Waypoint::class,
        TripEvent::class,
        RegattaSession::class,
        StartLine::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SailDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun trackPointDao(): TrackPointDao
    abstract fun waypointDao(): WaypointDao
    abstract fun tripEventDao(): TripEventDao
    abstract fun regattaDao(): RegattaDao
}

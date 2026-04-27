package com.sailapp.di

import android.content.Context
import androidx.room.Room
import com.sailapp.data.db.SailDatabase
import com.sailapp.data.db.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideSailDatabase(@ApplicationContext context: Context): SailDatabase {
        return Room.databaseBuilder(context, SailDatabase::class.java, "sail_database.db")
            .setJournalMode(androidx.room.RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .build()
    }

    @Provides
    fun provideTripDao(db: SailDatabase): TripDao = db.tripDao()

    @Provides
    fun provideTrackPointDao(db: SailDatabase): TrackPointDao = db.trackPointDao()

    @Provides
    fun provideWaypointDao(db: SailDatabase): WaypointDao = db.waypointDao()

    @Provides
    fun provideTripEventDao(db: SailDatabase): TripEventDao = db.tripEventDao()

    @Provides
    fun provideRegattaDao(db: SailDatabase): RegattaDao = db.regattaDao()
}

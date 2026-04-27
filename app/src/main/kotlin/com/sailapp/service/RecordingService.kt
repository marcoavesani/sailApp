package com.sailapp.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.content.Context
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.sailapp.MainActivity
import com.sailapp.R
import com.sailapp.data.db.entities.TrackPoint
import com.sailapp.data.repository.TripRepository
import com.sailapp.util.GeoUtils
import com.sailapp.util.SpeedFilter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

sealed class RecordingState {
    object Idle : RecordingState()
    data class Recording(
        val tripId: Long,
        val segmentId: Long,
        val startMs: Long,
        val distanceM: Double,
        val pointCount: Int
    ) : RecordingState()
    data class Paused(
        val tripId: Long,
        val startMs: Long,
        val distanceM: Double
    ) : RecordingState()
}

@AndroidEntryPoint
class RecordingService : Service() {

    companion object {
        const val ACTION_START = "com.sailapp.action.START_RECORDING"
        const val ACTION_PAUSE = "com.sailapp.action.PAUSE_RECORDING"
        const val ACTION_RESUME = "com.sailapp.action.RESUME_RECORDING"
        const val ACTION_STOP = "com.sailapp.action.STOP_RECORDING"
        const val EXTRA_TRIP_ID = "trip_id"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "recording_channel"

        private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
        val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

        private val _currentLocation = MutableStateFlow<Location?>(null)
        val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()
    }

    @Inject
    lateinit var repository: TripRepository

    @Inject
    lateinit var fusedLocationClient: FusedLocationProviderClient

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val binder = RecordingBinder()
    private var lastLocation: Location? = null
    private var batteryReceiver: BroadcastReceiver? = null
    private var currentBatteryPercent: Int? = null

    inner class RecordingBinder : Binder() {
        fun getService(): RecordingService = this@RecordingService
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { handleLocation(it) }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        registerBatteryReceiver()
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val tripId = intent.getLongExtra(EXTRA_TRIP_ID, -1L)
                if (tripId != -1L) startRecording(tripId)
            }
            ACTION_PAUSE -> pauseRecording()
            ACTION_RESUME -> resumeRecording()
            ACTION_STOP -> stopRecording()
        }
        return START_STICKY
    }

    private fun startRecording(tripId: Long) {
        serviceScope.launch {
            val segmentId = repository.startTrip(tripId)
            val now = System.currentTimeMillis()
            _recordingState.value = RecordingState.Recording(
                tripId = tripId,
                segmentId = segmentId,
                startMs = now,
                distanceM = 0.0,
                pointCount = 0
            )
            startForeground(NOTIFICATION_ID, buildNotification(0, 0.0))
            startLocationUpdates()
        }
    }

    private fun pauseRecording() {
        val state = _recordingState.value as? RecordingState.Recording ?: return
        serviceScope.launch {
            repository.pauseTrip(state.tripId, state.segmentId)
            _recordingState.value = RecordingState.Paused(
                tripId = state.tripId,
                startMs = state.startMs,
                distanceM = state.distanceM
            )
            stopLocationUpdates()
            updateNotification(state.startMs, state.distanceM)
        }
    }

    private fun resumeRecording() {
        val state = _recordingState.value as? RecordingState.Paused ?: return
        serviceScope.launch {
            val segmentId = repository.resumeTrip(state.tripId)
            _recordingState.value = RecordingState.Recording(
                tripId = state.tripId,
                segmentId = segmentId,
                startMs = state.startMs,
                distanceM = state.distanceM,
                pointCount = 0
            )
            startLocationUpdates()
        }
    }

    private fun stopRecording() {
        val state = _recordingState.value
        serviceScope.launch {
            when (state) {
                is RecordingState.Recording -> {
                    repository.stopTrip(state.tripId, state.segmentId)
                }
                is RecordingState.Paused -> {
                    val trip = repository.getTripById(state.tripId)
                    if (trip != null) {
                        val segs = repository.getSegmentsForTrip(state.tripId)
                        val lastSeg = segs.lastOrNull()
                        if (lastSeg != null) repository.stopTrip(state.tripId, lastSeg.id)
                    }
                }
                else -> {}
            }
            _recordingState.value = RecordingState.Idle
            stopLocationUpdates()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun handleLocation(location: Location) {
        val state = _recordingState.value as? RecordingState.Recording ?: return
        _currentLocation.value = location

        if (!SpeedFilter.isValid(location, lastLocation)) return

        val distDelta = if (lastLocation != null)
            GeoUtils.distanceMeters(
                lastLocation!!.latitude, lastLocation!!.longitude,
                location.latitude, location.longitude
            )
        else 0.0

        lastLocation = location
        val newDistance = state.distanceM + distDelta
        val newPointCount = state.pointCount + 1

        _recordingState.value = state.copy(distanceM = newDistance, pointCount = newPointCount)

        val point = TrackPoint(
            tripId = state.tripId,
            segmentId = state.segmentId,
            timestampUtcMs = location.time,
            latitude = location.latitude,
            longitude = location.longitude,
            altitudeM = if (location.hasAltitude()) location.altitude else null,
            sogMps = if (location.hasSpeed()) location.speed else null,
            cogDeg = if (location.hasBearing()) location.bearing else null,
            horizontalAccuracyM = if (location.hasAccuracy()) location.accuracy else null,
            verticalAccuracyM = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && location.hasVerticalAccuracy())
                location.verticalAccuracyMeters else null,
            speedAccuracyMps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && location.hasSpeedAccuracy())
                location.speedAccuracyMetersPerSecond else null,
            bearingAccuracyDeg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && location.hasBearingAccuracy())
                location.bearingAccuracyDegrees else null,
            provider = location.provider,
            batteryPercent = currentBatteryPercent,
            isValidForStats = true
        )

        serviceScope.launch {
            repository.addTrackPoint(point)
            updateNotification(state.startMs, newDistance)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateIntervalMillis(500L)
            .build()
        fusedLocationClient.requestLocationUpdates(request, locationCallback, mainLooper)
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.recording_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(elapsedMs: Long, distanceM: Double): Notification {
        val elapsedMin = elapsedMs / 60_000
        val distKm = distanceM / 1000.0
        val intent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle(getString(R.string.recording_notification_title))
            .setContentText("${elapsedMin} min · ${"%.2f".format(distKm)} km")
            .setContentIntent(pi)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateNotification(startMs: Long, distanceM: Double) {
        val elapsed = System.currentTimeMillis() - startMs
        val notification = buildNotification(elapsed, distanceM)
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, notification)
    }

    private fun registerBatteryReceiver() {
        batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val level = intent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1)
                if (level >= 0 && scale > 0) {
                    currentBatteryPercent = (level * 100 / scale)
                }
            }
        }
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        stopLocationUpdates()
        batteryReceiver?.let { unregisterReceiver(it) }
        if (_recordingState.value !is RecordingState.Idle) {
            val state = _recordingState.value
            if (state is RecordingState.Recording) {
                runBlocking {
                    repository.markTripInterrupted(state.tripId)
                }
            }
            _recordingState.value = RecordingState.Idle
        }
    }
}

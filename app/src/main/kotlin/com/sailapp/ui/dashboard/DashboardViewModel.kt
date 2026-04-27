package com.sailapp.ui.dashboard

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sailapp.data.db.entities.Trip
import com.sailapp.data.repository.TripRepository
import com.sailapp.service.RecordingService
import com.sailapp.service.RecordingState
import com.sailapp.util.GeoUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val trips: List<Trip> = emptyList(),
    val recordingState: RecordingState = RecordingState.Idle,
    val currentLocation: Location? = null,
    val elapsedMs: Long = 0L,
    val distanceM: Double = 0.0,
    val maxSpeedKn: Float = 0f
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: TripRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var maxSpeedKn = 0f

    init {
        observeRecordingState()
        observeLocation()
        observeTrips()
        tickElapsed()
    }

    private fun observeTrips() {
        viewModelScope.launch {
            repository.getAllTrips().collect { trips ->
                _uiState.update { it.copy(trips = trips) }
            }
        }
    }

    private fun observeRecordingState() {
        viewModelScope.launch {
            RecordingService.recordingState.collect { state ->
                if (state is RecordingState.Idle) maxSpeedKn = 0f
                _uiState.update { it.copy(recordingState = state) }
            }
        }
    }

    private fun observeLocation() {
        viewModelScope.launch {
            RecordingService.currentLocation.collect { loc ->
                loc?.let {
                    val speedKn = if (loc.hasSpeed()) GeoUtils.mpsToKnots(loc.speed) else 0f
                    if (speedKn > maxSpeedKn) maxSpeedKn = speedKn
                }
                _uiState.update { it.copy(currentLocation = loc, maxSpeedKn = maxSpeedKn) }
            }
        }
    }

    private fun tickElapsed() {
        viewModelScope.launch {
            while (true) {
                val state = _uiState.value.recordingState
                val elapsed = when (state) {
                    is RecordingState.Recording -> System.currentTimeMillis() - state.startMs
                    is RecordingState.Paused -> System.currentTimeMillis() - state.startMs
                    else -> 0L
                }
                val dist = when (state) {
                    is RecordingState.Recording -> state.distanceM
                    is RecordingState.Paused -> state.distanceM
                    else -> 0.0
                }
                _uiState.update { it.copy(elapsedMs = elapsed, distanceM = dist) }
                kotlinx.coroutines.delay(1000L)
            }
        }
    }

    fun startNewTrip(name: String) {
        viewModelScope.launch {
            repository.createTrip(name)
        }
    }

    suspend fun createTripAndGetId(name: String): Long = repository.createTrip(name)

    fun markWaypoint(name: String) {
        val loc = _uiState.value.currentLocation ?: return
        val state = _uiState.value.recordingState
        val tripId = when (state) {
            is RecordingState.Recording -> state.tripId
            is RecordingState.Paused -> state.tripId
            else -> null
        }
        viewModelScope.launch {
            repository.addWaypoint(
                com.sailapp.data.db.entities.Waypoint(
                    tripId = tripId,
                    name = name,
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    createdAtUtcMs = System.currentTimeMillis(),
                    type = "generic"
                )
            )
        }
    }
}

package com.sailapp.ui.track

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sailapp.data.db.entities.TrackPoint
import com.sailapp.data.db.entities.Trip
import com.sailapp.data.db.entities.Waypoint
import com.sailapp.data.repository.TripRepository
import com.sailapp.service.RecordingService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrackUiState(
    val trip: Trip? = null,
    val trackPoints: List<TrackPoint> = emptyList(),
    val waypoints: List<Waypoint> = emptyList(),
    val currentLocation: Location? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class TrackViewModel @Inject constructor(
    private val repository: TripRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrackUiState())
    val uiState: StateFlow<TrackUiState> = _uiState.asStateFlow()

    fun loadTrip(tripId: Long) {
        viewModelScope.launch {
            val trip = repository.getTripById(tripId)
            _uiState.update { it.copy(trip = trip, isLoading = false) }
        }

        viewModelScope.launch {
            repository.getTrackPointsForTrip(tripId).collect { pts ->
                _uiState.update { it.copy(trackPoints = pts) }
            }
        }

        viewModelScope.launch {
            repository.getWaypointsForTrip(tripId).collect { wpts ->
                _uiState.update { it.copy(waypoints = wpts) }
            }
        }

        viewModelScope.launch {
            RecordingService.currentLocation.collect { loc ->
                _uiState.update { it.copy(currentLocation = loc) }
            }
        }
    }

    suspend fun getTrackPointsSync(tripId: Long): List<TrackPoint> =
        repository.getTrackPointsSync(tripId)

    suspend fun getSegmentsForTrip(tripId: Long) =
        repository.getSegmentsForTrip(tripId)

    suspend fun getWaypointsSync(tripId: Long) =
        repository.getWaypointsForTripSync(tripId)
}

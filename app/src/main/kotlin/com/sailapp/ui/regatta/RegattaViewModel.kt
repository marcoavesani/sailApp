package com.sailapp.ui.regatta

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sailapp.data.db.entities.RegattaSession
import com.sailapp.data.db.entities.StartLine
import com.sailapp.data.db.entities.Waypoint
import com.sailapp.data.repository.TripRepository
import com.sailapp.service.RecordingService
import com.sailapp.util.GeoUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegattaUiState(
    val session: RegattaSession? = null,
    val startLine: StartLine? = null,
    val pinWaypoint: Waypoint? = null,
    val committeeWaypoint: Waypoint? = null,
    val countdownMs: Long = 300_000L,
    val timerState: TimerState = TimerState.Idle,
    val currentLocation: Location? = null,
    val distanceToLineM: Double? = null,
    val bearingToLineDeg: Double? = null
)

enum class TimerState { Idle, Running, Stopped }

@HiltViewModel
class RegattaViewModel @Inject constructor(
    private val repository: TripRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegattaUiState())
    val uiState: StateFlow<RegattaUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var timerEndMs: Long = 0L

    init {
        viewModelScope.launch {
            RecordingService.currentLocation.collect { loc ->
                _uiState.update { it.copy(currentLocation = loc) }
                loc?.let { recalcLineDistance(it) }
            }
        }
        loadOrCreateSession()
    }

    private fun loadOrCreateSession() {
        viewModelScope.launch {
            val sessions = repository.getAllRegattaSessions().first()
            val session = sessions.firstOrNull() ?: run {
                val id = repository.createRegattaSession("Regatta")
                repository.getRegattaSessionById(id)!!
            }
            _uiState.update { it.copy(session = session, countdownMs = session.countdownDurationS * 1000L) }
            loadStartLine(session.id)
        }
    }

    private suspend fun loadStartLine(sessionId: Long) {
        val startLine = repository.getStartLineForSession(sessionId)
        if (startLine != null) {
            val pin = startLine.pinWaypointId?.let { repository.getWaypointById(it) }
            val committee = startLine.committeeWaypointId?.let { repository.getWaypointById(it) }
            _uiState.update { it.copy(startLine = startLine, pinWaypoint = pin, committeeWaypoint = committee) }
        }
    }

    fun startTimer() {
        val state = _uiState.value
        timerEndMs = System.currentTimeMillis() + state.countdownMs
        _uiState.update { it.copy(timerState = TimerState.Running) }
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                val remaining = timerEndMs - System.currentTimeMillis()
                if (remaining <= 0) {
                    _uiState.update { it.copy(countdownMs = 0, timerState = TimerState.Stopped) }
                    break
                }
                _uiState.update { it.copy(countdownMs = remaining) }
                delay(100L)
            }
        }
    }

    fun pauseTimer() {
        timerJob?.cancel()
        _uiState.update { it.copy(timerState = TimerState.Stopped) }
    }

    fun syncTimer() {
        // Round to nearest minute
        val remaining = _uiState.value.countdownMs
        val roundedMs = ((remaining + 30_000L) / 60_000L) * 60_000L
        timerEndMs = System.currentTimeMillis() + roundedMs
        _uiState.update { it.copy(countdownMs = roundedMs) }
        if (_uiState.value.timerState == TimerState.Running) {
            startTimer()
        }
    }

    fun resetTimer(durationS: Int) {
        timerJob?.cancel()
        val ms = durationS * 1000L
        _uiState.update { it.copy(countdownMs = ms, timerState = TimerState.Idle) }
        saveSessionDuration(durationS)
    }

    private fun saveSessionDuration(durationS: Int) {
        viewModelScope.launch {
            val session = _uiState.value.session ?: return@launch
            repository.updateRegattaSession(session.copy(countdownDurationS = durationS))
        }
    }

    fun setPin() {
        val loc = _uiState.value.currentLocation ?: return
        viewModelScope.launch {
            val session = _uiState.value.session ?: return@launch
            val pinId = repository.addWaypoint(
                Waypoint(
                    name = "Start Pin",
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    createdAtUtcMs = System.currentTimeMillis(),
                    type = "start_pin"
                )
            )
            val existingLine = _uiState.value.startLine
            if (existingLine == null) {
                val lineId = repository.upsertStartLine(StartLine(regattaSessionId = session.id, pinWaypointId = pinId))
                loadStartLine(session.id)
            } else {
                repository.updateStartLine(existingLine.copy(pinWaypointId = pinId))
                loadStartLine(session.id)
            }
        }
    }

    fun setCommitteeBoat() {
        val loc = _uiState.value.currentLocation ?: return
        viewModelScope.launch {
            val session = _uiState.value.session ?: return@launch
            val committeeId = repository.addWaypoint(
                Waypoint(
                    name = "Committee Boat",
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    createdAtUtcMs = System.currentTimeMillis(),
                    type = "committee_boat"
                )
            )
            val existingLine = _uiState.value.startLine
            if (existingLine == null) {
                repository.upsertStartLine(StartLine(regattaSessionId = session.id, committeeWaypointId = committeeId))
                loadStartLine(session.id)
            } else {
                repository.updateStartLine(existingLine.copy(committeeWaypointId = committeeId))
                loadStartLine(session.id)
            }
        }
    }

    private fun recalcLineDistance(loc: Location) {
        val pin = _uiState.value.pinWaypoint
        val committee = _uiState.value.committeeWaypoint
        if (pin == null || committee == null) return
        val midLat = (pin.latitude + committee.latitude) / 2
        val midLon = (pin.longitude + committee.longitude) / 2
        val dist = GeoUtils.distanceMeters(loc.latitude, loc.longitude, midLat, midLon)
        val bearing = GeoUtils.bearingDeg(loc.latitude, loc.longitude, midLat, midLon)
        _uiState.update { it.copy(distanceToLineM = dist, bearingToLineDeg = bearing) }
    }
}

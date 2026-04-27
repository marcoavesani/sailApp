package com.sailapp.ui.recovery

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sailapp.data.db.entities.Trip
import com.sailapp.data.repository.TripRepository
import com.sailapp.service.RecordingServiceConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecoveryViewModel @Inject constructor(
    private val repository: TripRepository
) : ViewModel() {

    private val _activeTrips = MutableStateFlow<List<Trip>>(emptyList())
    val activeTrips: StateFlow<List<Trip>> = _activeTrips.asStateFlow()

    init {
        viewModelScope.launch {
            _activeTrips.value = repository.getActiveTrips()
        }
    }

    fun resumeTrip(tripId: Long, serviceConn: RecordingServiceConnection) {
        viewModelScope.launch {
            val segmentId = repository.recoverTrip(tripId)
            serviceConn.startRecording(tripId)
            _activeTrips.value = emptyList()
        }
    }

    fun saveAsInterrupted(tripId: Long) {
        viewModelScope.launch {
            repository.markTripInterrupted(tripId)
            _activeTrips.value = _activeTrips.value.filter { it.id != tripId }
        }
    }

    fun discardTrip(tripId: Long) {
        viewModelScope.launch {
            repository.discardTrip(tripId)
            _activeTrips.value = _activeTrips.value.filter { it.id != tripId }
        }
    }

    fun dismiss() {
        _activeTrips.value = emptyList()
    }
}

@Composable
fun RecoveryDialog(
    viewModel: RecoveryViewModel = hiltViewModel()
) {
    val activeTrips by viewModel.activeTrips.collectAsState()
    var showDiscardConfirmId by remember { mutableStateOf<Long?>(null) }

    if (showDiscardConfirmId != null) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirmId = null },
            title = { Text("Discard Trip?") },
            text = { Text("This will permanently discard the trip. Are you sure?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.discardTrip(showDiscardConfirmId!!)
                    showDiscardConfirmId = null
                }) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirmId = null }) { Text("Cancel") }
            }
        )
        return
    }

    val trip = activeTrips.firstOrNull() ?: return

    AlertDialog(
        onDismissRequest = { viewModel.dismiss() },
        title = { Text("Recover Trip") },
        text = {
            Text("Found an unfinished trip: \"${trip.name}\" (${trip.status}). What would you like to do?")
        },
        confirmButton = {
            // No service connection here — UI will handle via intent on next start
            TextButton(onClick = { viewModel.saveAsInterrupted(trip.id) }) {
                Text("Save as Interrupted")
            }
        },
        dismissButton = {
            TextButton(onClick = { showDiscardConfirmId = trip.id }) {
                Text("Discard")
            }
        }
    )
}

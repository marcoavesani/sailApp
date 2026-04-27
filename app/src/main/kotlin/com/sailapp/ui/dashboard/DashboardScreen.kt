package com.sailapp.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sailapp.service.RecordingServiceConnection
import com.sailapp.service.RecordingState
import com.sailapp.util.GeoUtils
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    contentPadding: PaddingValues,
    onNavigateToTrack: (Long) -> Unit,
    onNavigateToStats: (Long) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val serviceConn = remember { RecordingServiceConnection(context) }
    var showNameDialog by remember { mutableStateOf(false) }
    var showWaypointDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { serviceConn.bind() }
    DisposableEffect(Unit) { onDispose { serviceConn.unbind() } }

    if (showNameDialog) {
        TripNameDialog(
            onConfirm = { name ->
                showNameDialog = false
                scope.launch {
                    val tripId = viewModel.createTripAndGetId(name)
                    serviceConn.startRecording(tripId)
                }
            },
            onDismiss = { showNameDialog = false }
        )
    }

    if (showWaypointDialog) {
        WaypointNameDialog(
            onConfirm = { name ->
                showWaypointDialog = false
                viewModel.markWaypoint(name)
            },
            onDismiss = { showWaypointDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(12.dp)
    ) {
        Text("Dashboard", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 8.dp))

        val loc = uiState.currentLocation
        val state = uiState.recordingState
        val sog = if (loc?.hasSpeed() == true) GeoUtils.mpsToKnots(loc.speed) else 0f
        val cog = if (loc?.hasBearing() == true) loc.bearing else 0f
        val accuracy = if (loc?.hasAccuracy() == true) loc.accuracy else 0f
        val fixAge = if (loc != null) System.currentTimeMillis() - loc.time else -1L

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { DataTile("SOG", "%.1f kn".format(sog)) }
            item { DataTile("COG", "%.0f°".format(cog)) }
            item { DataTile("Lat", if (loc != null) "%.5f°".format(loc.latitude) else "---") }
            item { DataTile("Lon", if (loc != null) "%.5f°".format(loc.longitude) else "---") }
            item { DataTile("Accuracy", "%.1f m".format(accuracy)) }
            item { DataTile("Fix Age", if (fixAge >= 0) "${fixAge / 1000}s" else "---") }
            item { DataTile("Elapsed", GeoUtils.formatDuration(uiState.elapsedMs)) }
            item { DataTile("Distance", "%.2f nm".format(GeoUtils.metersToNauticalMiles(uiState.distanceM))) }
            item { DataTile("Max Speed", "%.1f kn".format(uiState.maxSpeedKn)) }
            item { DataTile("Status", when (state) {
                is RecordingState.Idle -> "Idle"
                is RecordingState.Recording -> "Recording"
                is RecordingState.Paused -> "Paused"
            }) }
        }

        Spacer(Modifier.height(12.dp))

        // Recording controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (state) {
                is RecordingState.Idle -> {
                    Button(onClick = { showNameDialog = true }, modifier = Modifier.weight(1f)) {
                        Text("Start")
                    }
                }
                is RecordingState.Recording -> {
                    Button(onClick = { serviceConn.pauseRecording() }, modifier = Modifier.weight(1f)) {
                        Text("Pause")
                    }
                    OutlinedButton(onClick = { serviceConn.stopRecording() }, modifier = Modifier.weight(1f)) {
                        Text("Stop")
                    }
                }
                is RecordingState.Paused -> {
                    Button(onClick = { serviceConn.resumeRecording() }, modifier = Modifier.weight(1f)) {
                        Text("Resume")
                    }
                    OutlinedButton(onClick = { serviceConn.stopRecording() }, modifier = Modifier.weight(1f)) {
                        Text("Stop")
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { showWaypointDialog = true },
                modifier = Modifier.weight(1f),
                enabled = loc != null
            ) { Text("Mark Waypoint") }

            val activeTripId = when (state) {
                is RecordingState.Recording -> state.tripId
                is RecordingState.Paused -> state.tripId
                else -> null
            }
            if (activeTripId != null) {
                OutlinedButton(
                    onClick = { onNavigateToTrack(activeTripId) },
                    modifier = Modifier.weight(1f)
                ) { Text("Map") }
                OutlinedButton(
                    onClick = { onNavigateToStats(activeTripId) },
                    modifier = Modifier.weight(1f)
                ) { Text("Stats") }
            }
        }

        // Recent trips
        if (uiState.trips.isNotEmpty()) {
            Text("Recent Trips", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
            uiState.trips.take(3).forEach { trip ->
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    onClick = { onNavigateToStats(trip.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(trip.name, fontWeight = FontWeight.Medium)
                        Text(trip.status, color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun DataTile(label: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TripNameDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("Trip ${java.text.SimpleDateFormat("MMdd-HHmm", java.util.Locale.US).format(java.util.Date())}") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Trip") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Trip name") },
                singleLine = true
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(name) }) { Text("Start") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun WaypointNameDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("Waypoint") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mark Waypoint") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Waypoint name") },
                singleLine = true
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(name) }) { Text("Mark") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

package com.sailapp.ui.regatta

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sailapp.util.GeoUtils

@Composable
fun RegattaScreen(
    contentPadding: PaddingValues,
    viewModel: RegattaViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Regatta Timer", style = MaterialTheme.typography.headlineMedium)

        // Countdown display
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val totalSeconds = uiState.countdownMs / 1000
                val minutes = totalSeconds / 60
                val seconds = totalSeconds % 60
                Text(
                    text = "%02d:%02d".format(minutes, seconds),
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        uiState.countdownMs <= 0 -> MaterialTheme.colorScheme.error
                        uiState.countdownMs <= 60_000 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
                Text(
                    text = when (uiState.timerState) {
                        TimerState.Idle -> "Ready"
                        TimerState.Running -> "Running"
                        TimerState.Stopped -> if (uiState.countdownMs <= 0) "GUN!" else "Paused"
                    },
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        // Timer controls
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            when (uiState.timerState) {
                TimerState.Idle, TimerState.Stopped -> {
                    Button(
                        onClick = { viewModel.startTimer() },
                        modifier = Modifier.weight(1f)
                    ) { Text("Start") }
                }
                TimerState.Running -> {
                    Button(
                        onClick = { viewModel.pauseTimer() },
                        modifier = Modifier.weight(1f)
                    ) { Text("Pause") }
                    OutlinedButton(
                        onClick = { viewModel.syncTimer() },
                        modifier = Modifier.weight(1f)
                    ) { Text("Sync") }
                }
            }
        }

        // Duration presets
        Text("Sequence", style = MaterialTheme.typography.titleMedium)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(300, 240, 60, 0).forEach { seconds ->
                OutlinedButton(
                    onClick = { viewModel.resetTimer(seconds) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (seconds == 0) "0" else "${seconds / 60}min", fontSize = 12.sp)
                }
            }
        }

        HorizontalDivider()

        // Start line
        Text("Start Line", style = MaterialTheme.typography.titleMedium)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { viewModel.setPin() },
                modifier = Modifier.weight(1f),
                enabled = uiState.currentLocation != null
            ) { Text("Set Pin") }
            OutlinedButton(
                onClick = { viewModel.setCommitteeBoat() },
                modifier = Modifier.weight(1f),
                enabled = uiState.currentLocation != null
            ) { Text("Set Committee") }
        }

        // Line info
        val pin = uiState.pinWaypoint
        val committee = uiState.committeeWaypoint
        if (pin != null || committee != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Pin: ${if (pin != null) "%.5f, %.5f".format(pin.latitude, pin.longitude) else "Not set"}")
                    Text("Committee: ${if (committee != null) "%.5f, %.5f".format(committee.latitude, committee.longitude) else "Not set"}")
                    if (pin != null && committee != null) {
                        val lineLen = GeoUtils.distanceMeters(pin.latitude, pin.longitude, committee.latitude, committee.longitude)
                        Text("Line length: %.0f m".format(lineLen))
                    }
                    uiState.distanceToLineM?.let { dist ->
                        Text("Distance to line: %.0f m (%.2f nm)".format(
                            dist, GeoUtils.metersToNauticalMiles(dist)
                        ))
                    }
                    uiState.bearingToLineDeg?.let { bearing ->
                        Text("Bearing to line: %.0f°".format(bearing))
                    }
                }
            }
        }
    }
}

package com.sailapp.ui.statistics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sailapp.util.GeoUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    tripId: Long,
    onBack: () -> Unit,
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(tripId) { viewModel.loadTrip(tripId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.trip?.name ?: "Statistics") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val stats = uiState.stats
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatRow("Total Distance", "%.2f nm / %.2f km".format(
                    GeoUtils.metersToNauticalMiles(stats?.totalDistanceM ?: 0.0),
                    (stats?.totalDistanceM ?: 0.0) / 1000
                ))
                StatRow("Total Elapsed Time", GeoUtils.formatDuration(stats?.elapsedMs ?: 0L))
                StatRow("Moving Time", GeoUtils.formatDuration(stats?.movingTimeMs ?: 0L))
                StatRow("Average Speed", "%.1f kn".format(GeoUtils.mpsToKnots(stats?.avgSpeedMps ?: 0f)))
                StatRow("Max Speed", "%.1f kn".format(GeoUtils.mpsToKnots(stats?.maxSpeedMps ?: 0f)))
                StatRow("Track Points", "${stats?.pointCount ?: 0}")
                StatRow("Status", uiState.trip?.status ?: "---")
                StatRow("Notes", uiState.trip?.notes?.ifBlank { "---" } ?: "---")
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

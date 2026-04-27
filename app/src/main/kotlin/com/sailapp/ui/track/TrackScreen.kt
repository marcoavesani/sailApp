package com.sailapp.ui.track

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sailapp.util.CsvExporter
import com.sailapp.util.GpxExporter
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackScreen(
    tripId: Long,
    onBack: () -> Unit,
    viewModel: TrackViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var mapView: MapView? by remember { mutableStateOf(null) }

    LaunchedEffect(tripId) { viewModel.loadTrip(tripId) }

    // Update map when track points change
    LaunchedEffect(uiState.trackPoints, uiState.currentLocation) {
        mapView?.let { mv ->
            updateMap(mv, uiState, context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.trip?.name ?: "Track") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                },
                actions = {
                    TextButton(onClick = {
                        scope.launch { exportGpx(context, tripId, viewModel) }
                    }) { Text("GPX") }
                    TextButton(onClick = {
                        scope.launch { exportCsv(context, tripId, viewModel) }
                    }) { Text("CSV") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(14.0)
                        mapView = this
                    }
                },
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
            Text(
                "${uiState.trackPoints.size} track points · ${uiState.waypoints.size} waypoints",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

private fun updateMap(mv: MapView, uiState: TrackUiState, context: Context) {
    mv.overlays.clear()

    // Draw track polyline
    if (uiState.trackPoints.size >= 2) {
        val polyline = Polyline(mv).apply {
            setPoints(uiState.trackPoints.map { GeoPoint(it.latitude, it.longitude) })
            outlinePaint.color = android.graphics.Color.BLUE
            outlinePaint.strokeWidth = 5f
        }
        mv.overlays.add(polyline)

        // Center map on first point if not yet moved
        val first = uiState.trackPoints.first()
        mv.controller.setCenter(GeoPoint(first.latitude, first.longitude))
    }

    // Draw current location
    uiState.currentLocation?.let { loc ->
        val marker = Marker(mv).apply {
            position = GeoPoint(loc.latitude, loc.longitude)
            title = "Current Position"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        mv.overlays.add(marker)
        mv.controller.setCenter(GeoPoint(loc.latitude, loc.longitude))
    }

    // Draw waypoints
    for (wpt in uiState.waypoints) {
        val marker = Marker(mv).apply {
            position = GeoPoint(wpt.latitude, wpt.longitude)
            title = wpt.name
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        mv.overlays.add(marker)
    }

    mv.invalidate()
}

private suspend fun exportGpx(context: Context, tripId: Long, viewModel: TrackViewModel) {
    val points = viewModel.getTrackPointsSync(tripId)
    val segments = viewModel.getSegmentsForTrip(tripId)
    val waypoints = viewModel.getWaypointsSync(tripId)
    val trip = viewModel.uiState.value.trip
    val pointsBySegment = points.groupBy { it.segmentId }
    val gpxContent = GpxExporter.export(
        tripName = trip?.name ?: "Trip $tripId",
        segments = segments,
        pointsBySegment = pointsBySegment,
        waypoints = waypoints
    )
    shareText(context, gpxContent, "trip_${tripId}.gpx", "application/gpx+xml")
}

private suspend fun exportCsv(context: Context, tripId: Long, viewModel: TrackViewModel) {
    val points = viewModel.getTrackPointsSync(tripId)
    val csvContent = CsvExporter.export(points)
    shareText(context, csvContent, "trip_${tripId}.csv", "text/csv")
}

private fun shareText(context: Context, content: String, fileName: String, mimeType: String) {
    val exportsDir = File(context.filesDir, "exports").apply { mkdirs() }
    val file = File(exportsDir, fileName)
    file.writeText(content)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Export $fileName"))
}

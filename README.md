# Sailing Dashboard Android App

A robust Android sailing companion app for dashboard instrumentation, trip recording, trip statistics, and regatta-start assistance.

The main design goal is **not losing trip data**. The app should write data incrementally to durable local storage during recording, recover unfinished trips after crashes or Android battery/process interruptions, and allow multiple partial recordings to be merged into one logical journey.

---

## Project status

Initial product and technical specification phase.

Planned MVP:

- Real-time sailing dashboard
- GPS trip recorder
- Track/map view
- Journey statistics
- Regatta start assistant
- Waypoints and start-line tools
- Crash-safe local persistence
- Interrupted-trip recovery
- Trip/file merge workflow
- GPX and CSV import/export

---

## Core user goals

1. See key sailing parameters clearly while underway.
2. Record a journey trace with position, speed, heading, and timestamps.
3. Review distance, duration, speed, and other statistics after the trip.
4. Use a dedicated regatta tab for start countdown, start-line setup, waypoints, and timing support.
5. Avoid losing recordings if Android kills the app, the battery saver interferes, the UI closes, or the phone is interrupted.
6. Merge multiple partial recordings into one complete trip when an interruption happens.

---

## Main app sections

Use a bottom navigation layout with four primary tabs.

### 1. Dashboard

Displays real-time sailing metrics in large, high-contrast tiles suitable for outdoor use.

Required metrics:

- SOG: speed over ground
- COG: course over ground
- Heading from compass/sensors when available
- Latitude and longitude
- GPS accuracy
- Last GPS fix age
- Trip elapsed time
- Trip distance
- Average SOG
- Max SOG
- Recording status
- Battery level
- GPS quality indicator

Primary actions:

- Start trip
- Pause trip
- Resume trip
- Stop/save trip
- Mark waypoint
- Man overboard marker, optional but recommended
- Keep screen awake while recording
- Switch day/night/high-contrast mode

---

### 2. Track / Map

Shows the current position, the recorded track, and waypoints.

Required features:

- Current boat position
- Recorded polyline
- Start and stop markers
- Waypoints
- Segment gaps after interruptions
- Center-on-boat control
- North-up / course-up orientation option
- Export current trip as GPX
- Export current trip as CSV
- Import GPX or CSV as a trip

Map implementation may use OpenStreetMap/OSMDroid or another Android map provider. Trip recording must not depend on network connectivity or map tile availability.

---

### 3. Statistics

Shows trip-level and segment-level statistics.

Required statistics:

- Total elapsed time
- Moving time
- Paused time
- Total distance
- Average speed
- Average moving speed
- Max speed
- Speed-over-time chart
- Distance-over-time chart
- COG/heading distribution if feasible
- Number of interruptions/recoveries
- GPS quality summary

Optional later statistics:

- VMG to waypoint
- VMC to course
- Tacks/gybes detection
- Polar comparison
- Speed histogram
- Replay mode

---

### 4. Regatta

Provides race-start and waypoint tools.

Required MVP features:

- Regatta countdown timer
- Configurable start sequence, for example 5 min / 4 min / 1 min / start
- Audio/vibration alerts
- Large countdown display
- Sync timer to nearest minute
- Start line defined by two waypoints: pin and committee boat
- Distance to start line
- Time to line estimate using current SOG
- Start-line side indicator
- Waypoint list
- Bearing and distance to selected waypoint

Optional later features:

- Layline estimation
- Wind direction input
- VMG to windward/leeward mark
- Race course templates
- Route replay
- Fleet sharing

---

## Data-loss prevention requirement

This is the most important technical requirement.

The app must never keep the only copy of an active recording in memory.

Required behavior:

- Use an Android foreground service while recording.
- Use the foreground service type for location.
- Show a persistent notification while recording.
- Keep recording independent from the Activity/UI lifecycle.
- Write every accepted track point immediately, or in very small batches.
- Target maximum data loss after process death: no more than 2 seconds of points.
- Store points in Room/SQLite as the canonical source of truth.
- Enable SQLite write-ahead logging.
- Use database transactions for point and event batches.
- Detect unfinished trips on app startup.
- Mark killed/interrupted trips as interrupted, not corrupted.
- Resume interrupted trips by creating a new segment in the same trip.
- Never wait until the user presses “Stop” to save the trip.

Recommended extra recovery layer:

- Append each point/event to a newline-delimited JSON journal during active recording.
- Compact/synchronize journal data into Room.
- On startup, replay any uncommitted journal entries before showing recovery options.

---

## Trip recovery workflow

On app startup:

1. Query trips with status `recording`, `paused`, or `interrupted` and no clean stop event.
2. Show a recovery banner.
3. Offer actions:
   - Resume trip
   - Save as interrupted
   - Merge with another trip/file
   - Export partial trip
   - Discard, only after explicit confirmation
4. If resumed, create a new segment in the same trip.
5. Preserve all original data.

---

## Trip merge workflow

Users must be able to merge multiple partial recordings into one logical trip.

Inputs:

- Internal interrupted trips
- Internal completed trips
- Imported GPX files
- Imported CSV files created by this app

Merge requirements:

- Select two or more trips/files.
- Show chronological preview.
- Display segment boundaries and gaps.
- Warn about overlapping time ranges.
- Warn about duplicate points.
- Warn about impossible jumps.
- Allow automatic ordering by timestamp.
- Allow manual reordering.
- Allow trimming start/end of each segment.
- Deduplicate by timestamp and proximity.
- Save merged result as a new trip by default.
- Never destroy source trips/files automatically.

---

## Suggested Android architecture

Recommended stack for a modern native Android implementation:

- Language: Kotlin
- UI: Jetpack Compose
- Architecture: MVVM or unidirectional state flow
- Async/state: Kotlin Coroutines + Flow
- Persistence: Room over SQLite
- Location: Fused Location Provider or Android location APIs
- Background recording: foreground service with persistent notification
- Maps: OSMDroid/OpenStreetMap or Google Maps, depending on licensing and offline needs
- Serialization/import/export: GPX and CSV modules
- Dependency injection: Hilt or Koin
- Charts: Compose-compatible chart library or custom Canvas charts
- Testing: JUnit, Robolectric, instrumentation tests, and fake location providers

---

## Proposed data model

### Trip

- `id`
- `name`
- `created_at_utc_ms`
- `started_at_utc_ms`
- `ended_at_utc_ms`
- `status`: planned, recording, paused, completed, interrupted, merged, discarded
- `total_distance_m`
- `notes`

### TripSegment

- `id`
- `trip_id`
- `started_at_utc_ms`
- `ended_at_utc_ms`
- `reason`: normal, pause_resume, app_recovery, import, merge

### TrackPoint

- `id`
- `trip_id`
- `segment_id`
- `timestamp_utc_ms`
- `latitude`
- `longitude`
- `altitude_m`
- `sog_mps`
- `cog_deg`
- `heading_deg`
- `horizontal_accuracy_m`
- `vertical_accuracy_m`
- `speed_accuracy_mps`
- `bearing_accuracy_deg`
- `provider`
- `battery_percent`
- `is_valid_for_stats`
- `raw_json`

### Waypoint

- `id`
- `trip_id`
- `name`
- `latitude`
- `longitude`
- `created_at_utc_ms`
- `type`: generic, start_pin, committee_boat, mark, mob
- `notes`

### TripEvent

- `id`
- `trip_id`
- `segment_id`
- `timestamp_utc_ms`
- `type`
- `message`
- `metadata_json`

### RegattaSession

- `id`
- `trip_id`
- `name`
- `start_time_utc_ms`
- `countdown_duration_s`
- `timer_state`

### StartLine

- `id`
- `regatta_session_id`
- `pin_waypoint_id`
- `committee_waypoint_id`

---

## Important calculations

### Distance

Use the WGS84/geodesic distance between consecutive valid points. For MVP, Android `Location.distanceBetween` or equivalent haversine/geodesic calculation is acceptable.

### Speed

Prefer GPS-derived speed when available and accuracy is acceptable. Otherwise compute speed from distance and timestamp difference between valid points.

### Course over ground

Prefer GPS bearing/course when available. Otherwise compute bearing from consecutive valid points.

### Filtering

Reject points from statistics, but preserve them in raw storage, when:

- Horizontal accuracy is worse than the configured threshold.
- The implied speed exceeds a configurable maximum plausible boat speed.
- Timestamp ordering is invalid.
- Coordinates are invalid.

Default thresholds:

- Maximum accepted horizontal accuracy: 50 m
- Maximum plausible speed: 35 knots
- Default recording rate: 1 Hz
- Default distance filter: 1-3 m

---

## Permissions

Expected Android permissions/capabilities:

- Fine location
- Coarse location, optional fallback
- Notification permission on Android versions that require it
- Foreground service permission
- Foreground service location type declaration
- Ignore battery optimization request flow, optional but recommended
- Wake lock or keep-screen-on behavior while recording, optional

The app must degrade gracefully if permissions are denied and must explain clearly which functions are unavailable.

---

## Import/export

Required export formats:

- GPX
- CSV

Recommended GPX behavior:

- Export each trip segment as a GPX track segment.
- Export waypoints as GPX waypoints.
- Preserve timestamps in UTC.
- Include speed/course as extensions where appropriate.

Recommended CSV columns:

```csv
trip_id,segment_id,timestamp_utc,latitude,longitude,altitude_m,sog_mps,cog_deg,heading_deg,horizontal_accuracy_m,battery_percent,is_valid_for_stats
```

---

## Non-goals for MVP

The MVP should not try to replace a full chartplotter.

Non-goals for the first version:

- Certified marine navigation
- Official nautical chart subscription system
- AIS receiver integration
- NMEA 0183 / NMEA 2000 integration
- Weather routing
- Tide/current prediction
- Cloud synchronization
- Fleet tracking
- Handicap scoring

These may be considered after the core recorder and regatta tools are reliable.

---

## Open-source projects to evaluate

These projects are useful references, but license compatibility must be checked before copying code.

### GPSLogger by mendhak

Repository: https://github.com/mendhak/gpslogger

Why it is useful:

- Mature Android GPS logging application
- GPX, KML, CSV, NMEA export concepts
- Background logging concepts
- Upload/export workflows
- Strong reference for robust GPS logging behavior

License note: GPL v2. Copying or forking it likely means your derived app must comply with GPL v2.

### BasicAirData GPS Logger

Repository: https://github.com/BasicAirData/GPSLogger

Why it is useful:

- Open-source Android GPS logger
- Focused on accuracy and power saving
- GPX/KML/TXT export
- Track list and sharing workflows
- Useful reference for storage, recording, and export UX

License note: GPL v3. Copying or forking it likely means your derived app must comply with GPL v3.

### OpenCPN

Repository: https://github.com/OpenCPN/OpenCPN
Android components: https://github.com/bdbcat/OpenCPN-Android

Why it is useful:

- Full marine chartplotter/navigation reference
- Waypoints, routes, AIS, charts, GPS input concepts
- Useful for understanding mature marine-navigation UX

License note: GPL-family licensing. It is likely too large and complex as a base for this MVP, but very useful as a conceptual reference.

### RegattApp

Repository: https://github.com/bmlrX3k/RegattApp

Why it is useful:

- Android sailing/regatta assistant example
- Includes GPS, buoy mapping, weather, and race-course import ideas
- MIT license is more permissive than GPL projects

Caution: the author marks it as educational/legacy and not production-ready.

### Breezy

Repository: https://github.com/alexanno/breezy

Why it is useful:

- Minimal offline-first sailing PWA
- Real-time navigation, tracking, waypoints, GPX/GeoJSON export
- Good conceptual prototype for UI and feature flow

Caution: it is a PWA, not a native Android/Kotlin project.

---

## Recommended starting strategy

Recommended approach: **start a new native Kotlin/Jetpack Compose app**, then study existing open-source projects for implementation ideas.

Reasoning:

- The key differentiator is robust Android-native persistence and recovery.
- GPL projects are useful references but may constrain your licensing if forked or copied.
- Existing sailing/regatta open-source Android projects are either too large, too old, educational, or not native Android.
- A clean app can be designed around Room, foreground service recording, recovery, and merge from day one.

Suggested implementation order:

1. Create app skeleton with Compose bottom navigation.
2. Implement Room schema for trips, segments, points, waypoints, and events.
3. Implement foreground location recording service.
4. Persist track points incrementally.
5. Implement unfinished-trip recovery on startup.
6. Build dashboard UI.
7. Build trip list and basic statistics.
8. Add GPX/CSV export.
9. Add map/track visualization.
10. Add merge workflow.
11. Add regatta timer and start-line tools.
12. Add GPX/CSV import.
13. Add advanced statistics and optional VMG/waypoint tools.

---

## Safety disclaimer

This app is intended as an aid for sailing, training, and trip recording. It is not certified navigation equipment and must not be the only navigation or safety system on board.
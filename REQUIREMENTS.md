# Android Sailing Dashboard App - Product & Technical Requirements

Version: 0.1
Date: 2026-04-27
Target platform: Android phone/tablet
Primary goal: provide a reliable sailing dashboard, trip recorder, trip statistics, and regatta-start assistant, with crash/battery-kill-safe persistence and trip recovery/merge.

---

## 1. Product summary

Build an Android sailing companion app for recreational sailing, training, and club regattas. The app must show real-time sailing parameters, record the trip trace with position/speed, compute journey statistics, and provide a regatta tab with waypoints, start-line tools, and countdown timers. A core differentiator is robust persistence: trip data must be written incrementally to disk so that Android process death, battery optimization, app crash, screen lock, or accidental app interruption does not lose the voyage.

This app is not intended to replace certified navigation equipment or official nautical charts. It may show maps/charts if feasible, but the MVP should focus on instrumentation, recording, statistics, and regatta-start workflow.

---

## 2. Competitive research summary

Observed features in existing sailing and marine apps:

- SailFreeGPS: COG, SOG, position, max speed, trip distance, average speed, magnetic compass, COG/SOG history, GPS filtering, track display, GPX/KML import/export, regatta countdown, start line, waypoint/GOTO with ETA, anchor/course alarms.
- Boatspeed: speed-focused sailing display, waypoints, trip recording, GPX/CSV export, regatta countdown, VMG with weather/wind input, on-device data ownership.
- SailAlign: digital logbook, regatta timers, route replay, GPS tracking, vessel/crew management, maintenance logs, weather/NavTex warnings.
- Regatta Racer / Regatta Hero / SailRacer / KWINDOO: start countdowns, line/start assistance, race tracking, performance analysis, handicap scoring or club/fleet management in some cases.
- Navionics / OpenCPN / SailGrib WR: more complete marine navigation including charts, routes, AIS, NMEA, weather, tides/currents, routing, and offline charts.

Implications for this app:

- MVP should include: SOG, COG, heading, GPS accuracy, position, trip distance, elapsed time, max/average speed, track recording, GPX export/import, waypoints, GOTO/ETA, regatta timer, start line, distance/time-to-line, and statistics.
- High-value differentiator: crash-safe and battery-kill-safe trip persistence, automatic recovery of interrupted sessions, and user-controlled merge of partial tracks.
- Nice-to-have after MVP: NMEA/AIS, wind input, VMG/VMC, polar-performance tools, weather/tides, anchor/course alarms, route replay, cloud backup, and crew sharing.

---

## 3. User personas

### 3.1 Recreational sailor
Wants a simple, readable dashboard with speed, direction, position, trip distance, and track recording.

### 3.2 Training sailor
Wants speed history, course history, statistics, route replay, exportable data, and comparison between sessions.

### 3.3 Regatta sailor
Wants start countdown, waypoints, start-line management, distance/time-to-line, and ideally layline/VMG support later.

---

## 4. Core navigation structure

Use a bottom navigation bar with four primary tabs:

1. Dashboard
2. Track / Map
3. Statistics
4. Regatta

Add a fifth optional tab or overflow section for Trips / Files / Settings if UI clarity requires it.

---

## 5. Functional requirements

### 5.1 Dashboard tab

Must display real-time metrics in large, high-contrast tiles suitable for outdoor use:

- SOG: speed over ground, selectable units: knots default, km/h, m/s.
- COG: course over ground, degrees true.
- Heading: compass heading when available, degrees magnetic/true as configured.
- Position: latitude/longitude in decimal degrees and degrees-minutes format.
- GPS accuracy: horizontal accuracy in meters.
- Altitude optional; hide by default for sailing.
- Trip elapsed time.
- Trip distance.
- Average SOG.
- Max SOG.
- Current recording status: Not recording / Recording / Paused / Recoverable interrupted trip.
- Battery level and GPS quality indicator.
- Last fix age, e.g. "GPS fix 2 s ago".

Should include:

- Start trip button.
- Pause/resume trip button.
- Stop/save trip button.
- Mark waypoint button.
- Man overboard quick marker button as optional high-priority safety feature.
- Screen-awake toggle during recording.
- Day/night/high-contrast display mode.

Dashboard calculations:

- Prefer GPS-derived speed and bearing when accuracy is good.
- Smooth speed and COG using a configurable moving median/average filter.
- Display raw and filtered values only in advanced/debug mode.
- Avoid using very inaccurate fixes in statistics unless the user explicitly chooses to include them.

### 5.2 Track / Map tab

Must show:

- Current location.
- Recorded track polyline.
- Waypoints.
- Start/stop markers.
- Color-coded track by speed if feasible.
- Basic map view using an offline-capable or cache-capable map provider if available.

MVP map choices:

- Use OpenStreetMap/OSMDroid or Google Maps depending on licensing constraints.
- Do not require network access for trip recording; map tiles may be unavailable offline, but the track must still be recorded.

Track controls:

- Center on boat.
- Lock orientation: north-up / course-up.
- Show/hide waypoints.
- Export current trip as GPX and CSV.
- Import GPX/CSV as a trip.

### 5.3 Trip recording

A trip is a logical collection of ordered track points, waypoints, events, and derived statistics.

Each track point must include:

- trip_id
- segment_id
- timestamp_utc_ms
- latitude
- longitude
- altitude_m nullable
- sog_mps nullable
- cog_deg nullable
- heading_deg nullable
- horizontal_accuracy_m nullable
- vertical_accuracy_m nullable
- speed_accuracy_mps nullable
- bearing_accuracy_deg nullable
- provider/source
- battery_percent nullable
- is_valid_for_stats boolean
- raw_json optional for platform-specific metadata

Trip events must include:

- trip_started
- trip_paused
- trip_resumed
- trip_stopped
- app_recovered_after_crash
- gps_lost
- gps_restored
- waypoint_created
- manual_note optional
- export_created
- file_merged

Recording interval:

- Default: 1 Hz while recording.
- Configurable: 0.2 Hz, 0.5 Hz, 1 Hz, 2 Hz if supported.
- Distance filter default: 1-3 meters, configurable.
- Low-power cruise mode optional: lower update rate.

Filtering rules:

- Reject impossible jumps based on maximum plausible boat speed threshold, default 35 knots configurable.
- Reject points with horizontal accuracy worse than threshold, default 50 m configurable.
- Keep rejected points in raw storage but flag them as invalid for statistics if practical.

### 5.4 Robust persistence and data-loss prevention

This is a top-level requirement.

The app must never keep the only copy of a recording in memory. It must write new trip data to durable local storage incrementally during recording.

Required behavior:

- Use a foreground service of type location while recording.
- Show a persistent notification during active recording with trip name, elapsed time, distance, and stop action.
- Acquire location updates through the Fused Location Provider or equivalent Android location API.
- Persist each accepted point immediately or in very small batches.
- Use an append-safe local database and/or append-only journal file.
- Flush data frequently enough that process death loses at most the latest point batch, target <= 2 seconds of data loss.
- On app start, detect unfinished trips and offer recovery.
- If recording was active when the process was killed, mark the trip as interrupted, not corrupted.
- Create new segment_id after every interruption/recovery so gaps are explicit.
- Maintain trip metadata independently from track points so partially recorded trips are discoverable.
- Add database transactions for point batches and event writes.
- Avoid waiting until "Stop trip" to save data.

Recommended storage design:

- Room database over SQLite as the canonical local store.
- Write-ahead logging enabled.
- Tables: trips, trip_segments, track_points, waypoints, trip_events, regatta_sessions, start_lines, app_settings.
- Optional: append-only newline-delimited JSON journal for extra recovery. The journal can be compacted into Room after successful import.
- Export files generated from Room, not used as primary storage during active recording.

Crash recovery flow:

1. App launches.
2. Query trips where status in [recording, paused, interrupted] and no clean stop event exists.
3. Show recovery banner: "An unfinished trip was found. Resume, Save as interrupted, Merge, or Discard."
4. If Resume: create a new segment in the same trip and continue recording.
5. If Save as interrupted: close the trip with interrupted status.
6. If Merge: open merge workflow.
7. If Discard: soft-delete only after explicit confirmation.

Battery-kill behavior:

- Use foreground service and notification.
- Ask user to exempt app from battery optimization, but app must still recover if killed.
- Show settings checklist for permissions: fine location, notification permission, background/foreground service requirements, battery optimization status.
- Keep recording independent from UI lifecycle.

### 5.5 Trip merge workflow

Users must be able to merge multiple partial trip files or interrupted trip records into one logical trip.

Inputs:

- Internal interrupted trips.
- Internal completed trips.
- Imported GPX files.
- Imported CSV files created by this app.

Merge UI:

- Select two or more trips/files.
- Show chronological preview with segments and gaps.
- Show warnings for overlapping time ranges, duplicate points, large gaps, inconsistent coordinate formats, or impossible jumps.
- Allow reorder by time or manual reorder.
- Allow trimming start/end of each segment before merge.
- Allow deduplication by timestamp and proximity.
- Save merged output as a new trip by default; never destroy originals automatically.

Merge rules:

- Sort points by timestamp unless user chooses manual order.
- Preserve segment boundaries.
- For duplicate points with same timestamp and very close coordinates, keep one.
- If points overlap but differ, keep both but flag conflict or ask user.
- Recompute statistics after merge.
- Add a trip_event recording source trip IDs/files and merge time.

Export after merge:

- GPX with track segments.
- CSV with all point fields.
- Optional JSON backup preserving full metadata.

### 5.6 Statistics tab

Statistics for current and saved trips:

- Elapsed time.
- Moving time.
- Stopped time.
- Total distance.
- Average SOG overall.
- Average SOG while moving.
- Max SOG.
- Median SOG.
- Percentiles: P50, P90, P95 speed.
- Average COG optional.
- Distance by speed bands.
- Time by speed bands.
- Track gap count and total gap duration.
- GPS quality summary: average accuracy, percentage of invalid points.
- Best 10 s / 30 s / 1 min / 5 min average speed.

Charts:

- Speed over time.
- COG over time.
- Speed histogram.
- Optional map replay timeline.

Stats must be recomputable from raw track points. Store cached stats for performance, but never rely only on cached values.

### 5.7 Regatta tab

MVP requirements:

- Countdown timer with common presets: 5 min, 4 min, 3 min, 1 min, custom.
- Audible beeps/vibration at configurable marks, e.g. every minute, 1 min, 30 s, 10 s, 5-4-3-2-1-start.
- Large display optimized for sunlight and wet hands.
- Start/pause/sync/reset controls.
- Quick +/- 1 s and +/- 10 s sync corrections.
- Store race session with timestamp and optional associated trip.

Start line:

- Set port end from current GPS position or map tap/manual coordinates.
- Set starboard end from current GPS position or map tap/manual coordinates.
- Display line length.
- Display distance to line.
- Display side of line.
- Display time-to-line using current SOG.
- Display burn time: time-to-line minus countdown remaining.
- Show whether the boat is early/late at current speed.

Waypoints:

- Add waypoint from current position, map tap, or manual coordinates.
- Name waypoint.
- Show bearing and distance to active waypoint.
- Show ETA to active waypoint based on SOG.
- Support GOTO waypoint.
- GPX import/export of waypoints.

Post-MVP regatta features:

- Wind direction input and lift/header detection.
- VMG to waypoint and VMG to wind.
- Laylines.
- Preferred end calculation based on wind direction and start line angle.
- Polar file import and target speed.
- Race course builder.
- Fleet sharing/live tracking, only if privacy and network design are addressed.

### 5.8 Alarms and safety features

MVP or near-MVP:

- Anchor alarm: alert when distance from anchor point exceeds radius.
- Course alarm: alert if COG deviates from target by threshold.
- GPS lost alarm during recording.
- Low battery alert during recording.
- Man overboard marker and navigation back to MOB point.

All alarms must be configurable and work when screen is off if the relevant service is active.

### 5.9 Import/export and interoperability

Required export formats:

- GPX 1.1 for tracks, segments, and waypoints.
- CSV for data analysis.
- JSON backup for full app-native data including metadata/events/settings.

Required import formats:

- GPX track import.
- CSV import from this app's schema.
- JSON restore from this app's backup format.

Optional later:

- FIT import/export.
- KML export.
- NMEA log import.

CSV columns should include at minimum:

timestamp_utc_iso, timestamp_utc_ms, trip_id, segment_id, latitude, longitude, sog_kn, sog_mps, cog_deg, heading_deg, horizontal_accuracy_m, battery_percent, valid_for_stats, event_marker

### 5.10 Settings

Settings should include:

- Units: speed, distance, coordinate format.
- GPS update interval.
- GPS accuracy threshold.
- Speed smoothing window.
- Max plausible speed filter.
- Keep screen on during recording.
- Battery optimization warning enable/disable.
- Export folder.
- Privacy: keep all data local by default.
- Map provider and offline cache options.
- Regatta timer presets and sound/vibration preferences.
- Theme: day/night/high contrast.

---

## 6. Non-functional requirements

### 6.1 Reliability

- Target data loss after crash/process kill: <= 2 seconds of samples.
- App must recover unfinished trips on next launch.
- App must tolerate lack of network connection.
- Recording must continue when UI is backgrounded, subject to Android permission constraints.
- Database migrations must preserve user trips.

### 6.2 Performance

- UI updates at 1 Hz minimum during recording.
- Map must remain usable for trips with at least 50,000 points by decimating displayed points while preserving raw data.
- Statistics computation for a 10-hour 1 Hz trip should complete in a few seconds on a mid-range Android device.

### 6.3 Battery

- Default recording mode should balance accuracy and battery.
- Provide high-accuracy regatta/training mode and lower-power cruising mode.
- Warn user that high-frequency GPS recording consumes battery.
- Avoid unnecessary network, wake locks, and sensors.

### 6.4 Privacy

- All trip data stored locally by default.
- No account required for MVP.
- No telemetry unless explicitly opt-in.
- Location data export/share must require explicit user action.

### 6.5 Accessibility and marine usability

- Large touch targets.
- High contrast outdoor mode.
- Usable with wet hands: minimize small controls.
- Orientation support: portrait and landscape.
- Tablet layout support.
- Audible/vibration feedback for regatta timer.

---

## 7. Recommended Android architecture

Language and UI:

- Kotlin.
- Jetpack Compose.
- Material 3, customized for high-contrast marine dashboard.

Architecture pattern:

- MVVM or MVI.
- Repository layer for trips, points, waypoints, settings.
- Foreground recording service independent of Activity lifecycle.
- Dependency injection with Hilt or Koin.

Persistence:

- Room database over SQLite.
- DataStore for settings.
- Optional append-only journal file for additional recovery.

Location:

- Fused Location Provider Client.
- Foreground service with location service type.
- Runtime permissions for fine/coarse location and notifications as needed by Android version.

Map:

- OSMDroid/OpenStreetMap for open-source-friendly MVP, or Google Maps if acceptable.
- Abstract map provider behind an interface if future swap is desired.

Export/import:

- Android Storage Access Framework for user-selected export/import location.
- Share sheet for GPX/CSV.

Testing:

- Unit tests for distance, bearing, ETA, time-to-line, merge, deduplication, speed filtering, statistics.
- Instrumented tests for Room migrations.
- Manual tests for process kill during recording.
- GPS simulation tests with GPX replay.

---

## 8. Data model proposal

### trips

- id UUID primary key
- name text
- status enum: draft, recording, paused, completed, interrupted, merged, deleted
- created_at_utc_ms
- started_at_utc_ms nullable
- ended_at_utc_ms nullable
- total_distance_m cached nullable
- notes text nullable
- source enum: native, imported_gpx, imported_csv, merged

### trip_segments

- id UUID primary key
- trip_id UUID foreign key
- index int
- started_at_utc_ms
- ended_at_utc_ms nullable
- reason enum: initial, resumed_after_pause, recovered_after_crash, imported, merged

### track_points

- id integer auto primary key
- trip_id UUID indexed
- segment_id UUID indexed
- timestamp_utc_ms indexed
- latitude real
- longitude real
- altitude_m real nullable
- sog_mps real nullable
- cog_deg real nullable
- heading_deg real nullable
- horizontal_accuracy_m real nullable
- vertical_accuracy_m real nullable
- speed_accuracy_mps real nullable
- bearing_accuracy_deg real nullable
- provider text nullable
- battery_percent int nullable
- is_valid_for_stats boolean
- raw_json text nullable

### waypoints

- id UUID primary key
- trip_id UUID nullable
- name text
- latitude real
- longitude real
- created_at_utc_ms
- type enum: generic, start_port, start_starboard, mark, mob, anchor
- notes text nullable

### trip_events

- id UUID primary key
- trip_id UUID
- timestamp_utc_ms
- type text
- payload_json text nullable

### regatta_sessions

- id UUID primary key
- linked_trip_id UUID nullable
- name text
- countdown_duration_s int
- start_time_utc_ms nullable
- status enum

### start_lines

- id UUID primary key
- regatta_session_id UUID nullable
- port_lat real
- port_lon real
- starboard_lat real
- starboard_lon real
- created_at_utc_ms

---

## 9. Core calculations

Use WGS84 geodesic or haversine approximation for MVP, but prefer a tested geodesic library for accuracy.

Required functions:

- distanceMeters(pointA, pointB)
- bearingDegrees(pointA, pointB)
- normalizeBearing(deg)
- crossTrackDistanceToLine(current, lineA, lineB)
- sideOfLine(current, lineA, lineB)
- distanceToStartLine(current, lineA, lineB)
- timeToLine(distance, sog)
- burnTime(timeToLine, countdownRemaining)
- tripDistance(points, ignoreInvalid = true)
- movingTime(points, speedThreshold)
- speedStats(points)
- deduplicatePoints(points)
- mergeTrips(trips, strategy)

---

## 10. MVP scope

Build the first version with:

1. Dashboard with SOG, COG, heading, position, GPS accuracy, trip time, distance, average/max speed.
2. Start/pause/resume/stop recording.
3. Foreground recording service.
4. Incremental Room persistence of all points.
5. Recovery of unfinished/interrupted trips.
6. Track/map display.
7. Statistics tab.
8. GPX and CSV export.
9. GPX import.
10. Merge internal trips and imported GPX files.
11. Regatta countdown timer.
12. Start line with distance/time-to-line/burn-time.
13. Waypoints with GOTO distance/bearing/ETA.
14. Settings for units and GPS interval.
15. High-contrast outdoor UI.

Explicitly out of MVP:

- Full nautical chart subscription system.
- AIS/NMEA integration.
- Weather/tides/currents.
- Cloud sync/live tracking.
- Polar performance and advanced laylines.
- Club regatta management/scoring.

---

## 11. Acceptance criteria

### Recording reliability

- Start a trip, record for 10 minutes, force-kill the app process via Android developer tools, relaunch app. The app must detect unfinished trip and show recorded points up to within approximately 2 seconds of the kill.
- Lock screen for 30 minutes while recording. Trip must continue or explicitly show interruption, with no silent data loss.
- Disable network. Recording, statistics, and export must still work.

### Merge

- Import two GPX files from the same journey. Merge creates a new trip with two segments, correct chronological order, recomputed stats, and originals preserved.
- Merge overlapping files. Duplicate timestamps are handled according to configured deduplication rules.

### Dashboard

- On a moving test track, SOG, COG, distance, max speed, and average speed update live.
- GPS accuracy and last fix age are visible.

### Regatta

- User can define a start line using two current-position captures or map/manual coordinates.
- Timer counts down with sound/vibration cues.
- Distance to line, time-to-line, and burn time update live.

### Export

- GPX export opens in common GPX viewers.
- CSV export contains all required fields and can be opened in spreadsheet/data-analysis tools.

---

## 12. Prompt for implementation LLM

Use this section as the direct instruction to another LLM/code agent:

"Build an Android application in Kotlin with Jetpack Compose named SailTrace. Implement the MVP described in this requirements file. Prioritize robust foreground GPS recording and incremental durable persistence over advanced UI polish. Use Room as the canonical trip database and a foreground location service independent from Activity lifecycle. Write each point immediately or in small transactions so that crashes/process kills lose at most around 2 seconds of samples. On app startup, detect unfinished trips and offer recovery/resume. Implement dashboard, track/map, statistics, and regatta tabs. Implement GPX/CSV export, GPX import, and merge of multiple trips/files into a new trip preserving segment boundaries. Provide unit tests for calculations and merge logic, and instrumented tests for Room schema/migrations. Do not add cloud sync, account login, ads, or subscription logic in MVP."

---

## 13. Future roadmap

Phase 2:

- Anchor alarm and course alarm.
- Route replay.
- Speed-colored tracks.
- KML/FIT export.
- Better offline map tile management.
- Backup/restore JSON.

Phase 3:

- NMEA 0183 over Wi-Fi/Bluetooth.
- AIS target display and alarms.
- External wind data.
- VMG/VMC, laylines, lift/header detection.
- Polar import and target speed.

Phase 4:

- Weather, tides, currents.
- Cloud backup and optional live sharing.
- Crew/vessel logbook.
- Regatta course management and scoring.

---

## 14. Safety disclaimer text for app

"This app is a sailing aid for recording, training, and situational awareness. It is not a certified navigation system and must not be used as the sole means of navigation or safety decision-making. Always carry appropriate nautical charts, instruments, and safety equipment."
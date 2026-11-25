package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.model.AircraftTarget
import com.example.domain.model.CallidusUiState
import com.example.domain.model.SatelliteTarget
import com.example.domain.model.SelectedTarget
import com.example.domain.model.TelemetryDashboardTab
import com.example.engine.DeviceTilt
import com.example.engine.Globe3DEngine
import com.example.ui.theme.CallidusCrimson
import com.example.ui.theme.CallidusEmerald
import com.example.ui.theme.CallidusGold
import com.example.ui.theme.ObsidianPanel
import com.example.ui.theme.ObsidianPanelElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Pure telemetry analytics helpers shared between the floating HUD overlay and unit tests.
 */
object TelemetryCalculator {

    fun resolveTrackedAircraft(
        aircraft: List<AircraftTarget>,
        selectedTarget: SelectedTarget,
        trackedIcao: String?,
        centerLat: Double,
        centerLon: Double
    ): AircraftTarget? {
        if (selectedTarget is SelectedTarget.Flight) {
            return aircraft.firstOrNull { it.icao24 == selectedTarget.data.icao24 } ?: selectedTarget.data
        }
        if (aircraft.isEmpty()) return null
        if (!trackedIcao.isNullOrBlank()) {
            aircraft.firstOrNull { it.icao24 == trackedIcao }?.let { return it }
        }
        return aircraft.minByOrNull {
            Globe3DEngine.haversineKm(centerLat, centerLon, it.lat, it.lon)
        }
    }

    fun resolveTrackedSatellite(
        satellites: List<SatelliteTarget>,
        selectedTarget: SelectedTarget,
        trackedNorad: Int?,
        centerLat: Double,
        centerLon: Double
    ): SatelliteTarget? {
        if (selectedTarget is SelectedTarget.Satellite) {
            return satellites.firstOrNull { it.element.noradId == selectedTarget.data.element.noradId }
                ?: selectedTarget.data
        }
        if (satellites.isEmpty()) return null
        if (trackedNorad != null) {
            satellites.firstOrNull { it.element.noradId == trackedNorad }?.let { return it }
        }
        return satellites.firstOrNull { it.element.name.contains("ISS", ignoreCase = true) }
            ?: satellites.minByOrNull {
                Globe3DEngine.haversineKm(centerLat, centerLon, it.lat, it.lon)
            }
    }

    fun compassCardinal(deg: Float): String {
        val norm = ((deg % 360f) + 360f) % 360f
        return when (((norm + 22.5f) / 45f).toInt() % 8) {
            0 -> "N"
            1 -> "NE"
            2 -> "E"
            3 -> "SE"
            4 -> "S"
            5 -> "SW"
            6 -> "W"
            else -> "NW"
        }
    }

    fun initialBearingDeg(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val dLon = Math.toRadians(Globe3DEngine.shortestLonDelta(lon1, lon2))
        val y = sin(dLon) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(dLon)
        val raw = Math.toDegrees(atan2(y, x)).toFloat()
        return ((raw % 360f) + 360f) % 360f
    }

    fun satelliteTrackHeadingDeg(satellite: SatelliteTarget): Float {
        val track = satellite.groundTrack
        if (track.size >= 2) {
            val mid = track.size / 2
            val p0 = track[(mid - 1).coerceAtLeast(0)]
            val p1 = track[(mid + 1).coerceAtMost(track.lastIndex)]
            return initialBearingDeg(p0.lat, p0.lon, p1.lat, p1.lon)
        }
        return (satellite.element.inclinationDeg.toFloat() % 360f + 360f) % 360f
    }

    fun orbitRegimeLabel(altitudeKm: Double): String = when {
        altitudeKm < 350.0 -> "VLEO"
        altitudeKm < 2_000.0 -> "LEO"
        altitudeKm < 35_000.0 -> "MEO"
        altitudeKm <= 36_500.0 -> "GEO"
        else -> "HEO"
    }

    fun orbitalPeriodMinutes(meanMotionRevPerDay: Double): Double {
        if (meanMotionRevPerDay <= 0.01) return 0.0
        return 1440.0 / meanMotionRevPerDay
    }

    fun flightLevelLabel(altitudeFeet: Int): String {
        val fl = (altitudeFeet.coerceAtLeast(0) / 100.0).roundToInt()
        return String.format(Locale.US, "FL%03d", fl)
    }
}

@Composable
fun FloatingTelemetryDashboard(
    state: CallidusUiState,
    deviceTilt: DeviceTilt,
    maxAvailableHeight: Dp,
    onSelectTab: (TelemetryDashboardTab) -> Unit,
    onToggleExpanded: () -> Unit,
    onHide: () -> Unit,
    onCycleAircraft: (Int) -> Unit,
    onCycleSatellite: (Int) -> Unit,
    onLockAircraft: (AircraftTarget) -> Unit,
    onLockSatellite: (SatelliteTarget) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = state.opticMode.palette.hudAccent
    val aircraftColor = state.opticMode.palette.aircraftColor
    val satelliteColor = state.opticMode.palette.satelliteColor
    val density = LocalDensity.current.density

    val activeAircraft = remember(
        state.aircraft,
        state.selectedTarget,
        state.trackedAircraftIcao,
        state.camera.centerLat,
        state.camera.centerLon
    ) {
        TelemetryCalculator.resolveTrackedAircraft(
            aircraft = state.aircraft,
            selectedTarget = state.selectedTarget,
            trackedIcao = state.trackedAircraftIcao,
            centerLat = state.camera.centerLat,
            centerLon = state.camera.centerLon
        )
    }

    val activeSatellite = remember(
        state.satellites,
        state.selectedTarget,
        state.trackedSatelliteNorad,
        state.camera.centerLat,
        state.camera.centerLon
    ) {
        TelemetryCalculator.resolveTrackedSatellite(
            satellites = state.satellites,
            selectedTarget = state.selectedTarget,
            trackedNorad = state.trackedSatelliteNorad,
            centerLat = state.camera.centerLat,
            centerLon = state.camera.centerLon
        )
    }

    val pulseTransition = rememberInfiniteTransition(label = "telemetry_beacon_pulse")
    val beaconAlpha by pulseTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(950), RepeatMode.Reverse),
        label = "beacon_alpha"
    )
    val radarSweepDeg by pulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing), RepeatMode.Restart),
        label = "radar_sweep"
    )

    AnimatedVisibility(
        visible = state.isTelemetryDashboardVisible,
        modifier = modifier,
        enter = fadeIn() + expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
        exit = fadeOut() + shrinkVertically()
    ) {
        Surface(
            modifier = Modifier
                .widthIn(min = 216.dp, max = 266.dp)
                .heightIn(max = maxAvailableHeight.coerceAtLeast(140.dp))
                .graphicsLayer {
                    rotationY = deviceTilt.x * 3.5f
                    rotationX = -deviceTilt.y * 2.5f
                    cameraDistance = 14f * density
                }
                .testTag("floating_telemetry_dashboard"),
            color = ObsidianPanel,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(
                1.dp,
                when (state.telemetryDashboardTab) {
                    TelemetryDashboardTab.AIRCRAFT -> aircraftColor.copy(alpha = 0.55f)
                    TelemetryDashboardTab.SATELLITE -> satelliteColor.copy(alpha = 0.55f)
                }
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header Row: Live indicator + Mode Title + Expand/Minimize + Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(CallidusEmerald.copy(alpha = beaconAlpha))
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "TELEMETRY HUD",
                            style = MaterialTheme.typography.labelMedium,
                            color = accent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onToggleExpanded,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("telemetry_dash_toggle_expand")
                        ) {
                            Icon(
                                imageVector = if (state.isTelemetryDashboardExpanded) {
                                    Icons.Default.ExpandLess
                                } else {
                                    Icons.Default.ExpandMore
                                },
                                contentDescription = if (state.isTelemetryDashboardExpanded) {
                                    "Collapse telemetry dashboard"
                                } else {
                                    "Expand telemetry dashboard"
                                },
                                tint = accent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onHide,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("telemetry_dash_close")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Hide telemetry dashboard",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Tab Switcher: AIRCRAFT | SATELLITE
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TelemetryDashboardTab.entries.forEach { tab ->
                        val selected = tab == state.telemetryDashboardTab
                        val tabColor = if (tab == TelemetryDashboardTab.AIRCRAFT) aircraftColor else satelliteColor
                        val tabIcon = if (tab == TelemetryDashboardTab.AIRCRAFT) {
                            Icons.Default.Flight
                        } else {
                            Icons.Default.SatelliteAlt
                        }
                        val count = if (tab == TelemetryDashboardTab.AIRCRAFT) {
                            state.aircraft.size
                        } else {
                            state.satellites.size
                        }
                        Surface(
                            onClick = { onSelectTab(tab) },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("telemetry_tab_${tab.name.lowercase()}"),
                            color = if (selected) tabColor.copy(alpha = 0.2f) else Color(0x55060D1A),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                width = if (selected) 1.2.dp else 0.75.dp,
                                color = if (selected) tabColor else tabColor.copy(alpha = 0.25f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = tabIcon,
                                    contentDescription = tab.label,
                                    tint = if (selected) tabColor else TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "${if (tab == TelemetryDashboardTab.AIRCRAFT) "AIR" else "SAT"} $count",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) tabColor else TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                if (!state.isTelemetryDashboardExpanded) {
                    TelemetryCompactSummary(
                        tab = state.telemetryDashboardTab,
                        aircraft = activeAircraft,
                        satellite = activeSatellite,
                        aircraftColor = aircraftColor,
                        satelliteColor = satelliteColor,
                        onExpand = onToggleExpanded
                    )
                } else {
                    when (state.telemetryDashboardTab) {
                        TelemetryDashboardTab.AIRCRAFT -> AircraftTelemetrySection(
                            aircraft = activeAircraft,
                            totalCount = state.aircraft.size,
                            isLocked = state.selectedTarget is SelectedTarget.Flight,
                            centerLat = state.camera.centerLat,
                            centerLon = state.camera.centerLon,
                            accent = aircraftColor,
                            onPrev = { onCycleAircraft(-1) },
                            onNext = { onCycleAircraft(1) },
                            onFlyLock = { activeAircraft?.let(onLockAircraft) }
                        )

                        TelemetryDashboardTab.SATELLITE -> SatelliteTelemetrySection(
                            satellite = activeSatellite,
                            totalCount = state.satellites.size,
                            isLocked = state.selectedTarget is SelectedTarget.Satellite,
                            centerLat = state.camera.centerLat,
                            centerLon = state.camera.centerLon,
                            radarSweepDeg = radarSweepDeg,
                            accent = satelliteColor,
                            onPrev = { onCycleSatellite(-1) },
                            onNext = { onCycleSatellite(1) },
                            onFlyLock = { activeSatellite?.let(onLockSatellite) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryCompactSummary(
    tab: TelemetryDashboardTab,
    aircraft: AircraftTarget?,
    satellite: SatelliteTarget?,
    aircraftColor: Color,
    satelliteColor: Color,
    onExpand: () -> Unit
) {
    Surface(
        onClick = onExpand,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag("telemetry_compact_summary"),
        color = Color(0x66060D1A),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(0.75.dp, TextMuted.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (tab == TelemetryDashboardTab.AIRCRAFT) {
                if (aircraft != null) {
                    Text(
                        text = "${aircraft.callsign} • ${TelemetryCalculator.flightLevelLabel(aircraft.altitudeFeet)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = aircraftColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${aircraft.groundSpeedKnots} kts • HDG ${aircraft.headingDeg.roundToInt()}° ${TelemetryCalculator.compassCardinal(aircraft.headingDeg)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary,
                        maxLines = 1
                    )
                } else {
                    Text(
                        text = "Scanning regional ADS-B airspace...",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            } else {
                if (satellite != null) {
                    Text(
                        text = "${satellite.element.name} • ${TelemetryCalculator.orbitRegimeLabel(satellite.altitudeKm)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = satelliteColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${String.format(Locale.US, "%.0f", satellite.altitudeKm)} km • ${String.format(Locale.US, "%.2f", satellite.velocityKmS)} km/s",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary,
                        maxLines = 1
                    )
                } else {
                    Text(
                        text = "Propagating orbital constellations...",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun AircraftTelemetrySection(
    aircraft: AircraftTarget?,
    totalCount: Int,
    isLocked: Boolean,
    centerLat: Double,
    centerLon: Double,
    accent: Color,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onFlyLock: () -> Unit
) {
    if (aircraft == null) {
        TelemetryEmptyCard(
            title = "Awaiting ADS-B Telemetry",
            subtitle = "Pan globe to a flight corridor or tap Sync to fetch live aircraft altitude, speed & heading.",
            accent = accent
        )
        return
    }

    val rangeKm = Globe3DEngine.haversineKm(centerLat, centerLon, aircraft.lat, aircraft.lon)
    val speedKmh = (aircraft.groundSpeedKnots * 1.852).roundToInt()
    val machEstimate = (aircraft.groundSpeedKnots / 661.47).coerceAtLeast(0.0)
    val vsLabel = when {
        aircraft.verticalRateFpm > 120 -> "▲ +${aircraft.verticalRateFpm}"
        aircraft.verticalRateFpm < -120 -> "▼ ${aircraft.verticalRateFpm}"
        else -> "— LEVEL"
    }
    val vsColor = when {
        aircraft.verticalRateFpm > 120 -> CallidusEmerald
        aircraft.verticalRateFpm < -120 -> CallidusGold
        else -> TextSecondary
    }

    // Target selector strip
    TargetCycleHeader(
        title = aircraft.callsign,
        badge = if (isLocked) "LOCKED" else "AUTO",
        subtitle = "${aircraft.aircraftType} • ICAO ${aircraft.icao24.uppercase(Locale.US)}",
        accent = accent,
        prevTag = "telemetry_aircraft_prev",
        nextTag = "telemetry_aircraft_next",
        flyTag = "telemetry_aircraft_fly",
        onPrev = onPrev,
        onNext = onNext,
        onFlyLock = onFlyLock
    )

    // Altitude + Heading Compass side-by-side
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Altitude Instrument + Ladder
        Surface(
            modifier = Modifier
                .weight(1f)
                .testTag("telemetry_aircraft_altitude"),
            color = Color(0x66060D1A),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(0.8.dp, accent.copy(alpha = 0.35f))
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AltitudeTapeGauge(
                    altitudeFeet = aircraft.altitudeFeet,
                    accent = accent,
                    modifier = Modifier.size(width = 10.dp, height = 64.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("ALTITUDE", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text(
                        text = String.format(Locale.US, "%,d ft", aircraft.altitudeFeet),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = TelemetryCalculator.flightLevelLabel(aircraft.altitudeFeet),
                            style = MaterialTheme.typography.labelSmall,
                            color = accent
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f km", aircraft.altitudeKm),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    Text(
                        text = vsLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = vsColor,
                        maxLines = 1
                    )
                }
            }
        }

        // Rotating Avionics Heading Compass Rose
        Surface(
            modifier = Modifier
                .size(width = 88.dp, height = 88.dp)
                .testTag("telemetry_aircraft_heading"),
            color = Color(0x66060D1A),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(0.8.dp, accent.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "HDG ${aircraft.headingDeg.roundToInt()}° ${TelemetryCalculator.compassCardinal(aircraft.headingDeg)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                    maxLines = 1
                )
                CompassRose3DGauge(
                    headingDeg = aircraft.headingDeg,
                    accent = accent,
                    modifier = Modifier.size(54.dp)
                )
            }
        }
    }

    // Airspeed Gauge Bar Card
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("telemetry_aircraft_speed"),
        color = Color(0x66060D1A),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.8.dp, accent.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("GROUND SPEED", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Text(
                    text = "${aircraft.groundSpeedKnots} kts • $speedKmh km/h • M${String.format(Locale.US, "%.2f", machEstimate)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextPrimary,
                    maxLines = 1
                )
            }
            val speedNorm = (aircraft.groundSpeedKnots / 620f).coerceIn(0.05f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0x44FFFFFF))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(speedNorm)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(Brush.horizontalGradient(listOf(accent.copy(alpha = 0.6f), accent)))
                )
            }
        }
    }

    // Sector Footer
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "SQK ${aircraft.squawk} • ${String.format(Locale.US, "%.0f", rangeKm)} km",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
        Text(
            text = "$totalCount ACTIVE",
            style = MaterialTheme.typography.labelSmall,
            color = CallidusEmerald
        )
    }
}

@Composable
private fun SatelliteTelemetrySection(
    satellite: SatelliteTarget?,
    totalCount: Int,
    isLocked: Boolean,
    centerLat: Double,
    centerLon: Double,
    radarSweepDeg: Float,
    accent: Color,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onFlyLock: () -> Unit
) {
    if (satellite == null) {
        TelemetryEmptyCard(
            title = "Awaiting Orbital Ephemeris",
            subtitle = "CelesTrak GP & ISS propagators initializing live satellite orbit vectors.",
            accent = accent
        )
        return
    }

    val trackHeading = remember(satellite) { TelemetryCalculator.satelliteTrackHeadingDeg(satellite) }
    val regime = remember(satellite.altitudeKm) { TelemetryCalculator.orbitRegimeLabel(satellite.altitudeKm) }
    val periodMin = remember(satellite.element.meanMotionRevPerDay) {
        TelemetryCalculator.orbitalPeriodMinutes(satellite.element.meanMotionRevPerDay)
    }
    val velocityKmh = (satellite.velocityKmS * 3600.0).roundToInt()
    val groundRangeKm = Globe3DEngine.haversineKm(centerLat, centerLon, satellite.lat, satellite.lon)

    TargetCycleHeader(
        title = satellite.element.name,
        badge = if (isLocked) "LOCKED" else regime,
        subtitle = "NORAD ${satellite.element.noradId} • ${satellite.element.category.label}",
        accent = accent,
        prevTag = "telemetry_satellite_prev",
        nextTag = "telemetry_satellite_next",
        flyTag = "telemetry_satellite_fly",
        onPrev = onPrev,
        onNext = onNext,
        onFlyLock = onFlyLock
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("telemetry_satellite_metrics"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.weight(1f),
            color = Color(0x66060D1A),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(0.8.dp, accent.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MetricLine("ALT", String.format(Locale.US, "%.1f km", satellite.altitudeKm), accent)
                MetricLine("VEL", String.format(Locale.US, "%.2f km/s", satellite.velocityKmS), CallidusEmerald)
                MetricLine("TRK", "${trackHeading.roundToInt()}° ${TelemetryCalculator.compassCardinal(trackHeading)}", TextPrimary)
                MetricLine("INC", String.format(Locale.US, "%.1f°", satellite.element.inclinationDeg), TextSecondary)
            }
        }

        Surface(
            modifier = Modifier.size(88.dp),
            color = Color(0x66060D1A),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(0.8.dp, accent.copy(alpha = 0.35f))
        ) {
            OrbitalRadarScopeCanvas(
                satellite = satellite,
                trackHeadingDeg = trackHeading,
                radarSweepDeg = radarSweepDeg,
                accent = accent,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp)
            )
        }
    }

    // Orbital Period & Live Sub-Satellite Position
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0x66060D1A),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(0.75.dp, accent.copy(alpha = 0.28f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = String.format(Locale.US, "SUB %.2f°, %.2f°", satellite.lat, satellite.lon),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextPrimary
                )
                Text(
                    text = String.format(Locale.US, "%.0f km/h", velocityKmh.toDouble()),
                    style = MaterialTheme.typography.labelSmall,
                    color = accent
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = String.format(Locale.US, "ORB %.1f min • RNG %.0f km", periodMin, groundRangeKm),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Text(
                    text = "$totalCount ORBITS",
                    style = MaterialTheme.typography.labelSmall,
                    color = CallidusEmerald
                )
            }
        }
    }
}

@Composable
private fun TargetCycleHeader(
    title: String,
    badge: String,
    subtitle: String,
    accent: Color,
    prevTag: String,
    nextTag: String,
    flyTag: String,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onFlyLock: () -> Unit
) {
    Surface(
        color = ObsidianPanelElevated,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(0.75.dp, accent.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Surface(
                            color = accent.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = badge,
                                style = MaterialTheme.typography.labelSmall,
                                color = accent,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = accent.copy(alpha = 0.16f)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onPrev,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag(prevTag),
                    color = Color(0x55060D1A),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(0.75.dp, accent.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous target",
                            tint = TextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Surface(
                    onClick = onFlyLock,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag(flyTag),
                    color = accent.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(0.9.dp, accent)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "Lock and fly to target",
                            tint = accent,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "LOCK",
                            style = MaterialTheme.typography.labelSmall,
                            color = accent
                        )
                    }
                }

                Surface(
                    onClick = onNext,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag(nextTag),
                    color = Color(0x55060D1A),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(0.75.dp, accent.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next target",
                            tint = TextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricLine(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Text(value, style = MaterialTheme.typography.labelSmall, color = valueColor, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AltitudeTapeGauge(
    altitudeFeet: Int,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val norm by animateFloatAsState(
        targetValue = (altitudeFeet / 45_000f).coerceIn(0.05f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "alt_norm"
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            color = Color(0x33FFFFFF),
            topLeft = Offset.Zero,
            size = Size(w, h),
            cornerRadius = CornerRadius(4f, 4f)
        )
        val fillH = h * norm
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(accent, accent.copy(alpha = 0.35f))),
            topLeft = Offset(0f, h - fillH),
            size = Size(w, fillH),
            cornerRadius = CornerRadius(4f, 4f)
        )
        for (i in 1..4) {
            val ty = h * (i / 5f)
            drawLine(
                color = Color(0x8802050B),
                start = Offset(0f, ty),
                end = Offset(w * 0.65f, ty),
                strokeWidth = 1.4f
            )
        }
    }
}

@Composable
private fun CompassRose3DGauge(
    headingDeg: Float,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val animHeading by animateFloatAsState(
        targetValue = headingDeg,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
        label = "compass_heading"
    )
    Canvas(modifier = modifier) {
        val r = size.minDimension * 0.44f
        val c = Offset(size.width * 0.5f, size.height * 0.5f)
        drawCircle(color = accent.copy(alpha = 0.25f), radius = r, center = c, style = Stroke(width = 1.5f))
        drawCircle(color = accent.copy(alpha = 0.1f), radius = r * 0.62f, center = c, style = Stroke(width = 1f))

        for (step in 0 until 12) {
            val angleRad = step * (PI / 6.0)
            val tickLen = if (step % 3 == 0) 5f else 3f
            val p1 = Offset(
                c.x + (r - tickLen) * sin(angleRad).toFloat(),
                c.y - (r - tickLen) * cos(angleRad).toFloat()
            )
            val p2 = Offset(
                c.x + r * sin(angleRad).toFloat(),
                c.y - r * cos(angleRad).toFloat()
            )
            drawLine(
                color = if (step == 0) CallidusCrimson else accent.copy(alpha = 0.65f),
                start = p1,
                end = p2,
                strokeWidth = if (step % 3 == 0) 2f else 1f
            )
        }

        // Directional aircraft heading vector needle
        rotate(degrees = animHeading, pivot = c) {
            val needle = Path().apply {
                moveTo(c.x, c.y - r * 0.72f)
                lineTo(c.x - 5.5f, c.y + r * 0.35f)
                lineTo(c.x, c.y + r * 0.16f)
                lineTo(c.x + 5.5f, c.y + r * 0.35f)
                close()
            }
            drawPath(path = needle, color = accent)
        }
        drawCircle(color = TextPrimary, radius = 2.4f, center = c)
    }
}

@Composable
private fun OrbitalRadarScopeCanvas(
    satellite: SatelliteTarget,
    trackHeadingDeg: Float,
    radarSweepDeg: Float,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val r = size.minDimension * 0.45f
        val c = Offset(size.width * 0.5f, size.height * 0.5f)

        // Concentric orbital rings
        drawCircle(color = accent.copy(alpha = 0.25f), radius = r, center = c, style = Stroke(width = 1.3f))
        drawCircle(color = accent.copy(alpha = 0.18f), radius = r * 0.62f, center = c, style = Stroke(width = 1f))
        drawLine(accent.copy(alpha = 0.18f), Offset(c.x - r, c.y), Offset(c.x + r, c.y), strokeWidth = 1f)
        drawLine(accent.copy(alpha = 0.18f), Offset(c.x, c.y - r), Offset(c.x, c.y + r), strokeWidth = 1f)

        // Animated radar sweep line
        rotate(degrees = radarSweepDeg, pivot = c) {
            drawLine(
                color = accent.copy(alpha = 0.45f),
                start = c,
                end = Offset(c.x, c.y - r),
                strokeWidth = 1.6f,
                cap = StrokeCap.Round
            )
        }

        // Ground track azimuth chord through scope center
        rotate(degrees = trackHeadingDeg, pivot = c) {
            drawLine(
                color = CallidusEmerald.copy(alpha = 0.85f),
                start = Offset(c.x, c.y + r * 0.85f),
                end = Offset(c.x, c.y - r * 0.85f),
                strokeWidth = 1.8f,
                cap = StrokeCap.Round
            )
            // Satellite heading chevron
            val chevron = Path().apply {
                moveTo(c.x, c.y - r * 0.46f)
                lineTo(c.x - 4.5f, c.y - r * 0.2f)
                lineTo(c.x + 4.5f, c.y - r * 0.2f)
                close()
            }
            drawPath(chevron, color = accent)
        }

        // Earth core indicator
        val orbitNorm = (satellite.altitudeKm / 20_000.0).toFloat().coerceIn(0.15f, 0.7f)
        drawCircle(color = Color(0x4400E5FF), radius = r * (0.45f - orbitNorm * 0.2f), center = c)
        drawCircle(color = CallidusGold, radius = 3.2f, center = c)
    }
}

@Composable
private fun TelemetryEmptyCard(title: String, subtitle: String, accent: Color) {
    Surface(
        color = Color(0x66060D1A),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(0.75.dp, accent.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = accent)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}

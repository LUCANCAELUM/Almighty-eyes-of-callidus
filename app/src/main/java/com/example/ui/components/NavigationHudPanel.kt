package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.CallidusUiState
import com.example.domain.model.GeoPoint3D
import com.example.domain.model.GlobalLandmarks
import com.example.domain.model.NavigationTravelMode
import com.example.domain.model.ViewportMode
import com.example.engine.Globe3DEngine
import com.example.engine.LiveGpsCompassState
import com.example.engine.OfflineSteeringCalculator
import com.example.ui.theme.CallidusCrimson
import com.example.ui.theme.CallidusEmerald
import com.example.ui.theme.CallidusGold
import com.example.ui.theme.ObsidianPanelElevated
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

private data class OfflinePinWaypoint(
    val id: String,
    val label: String,
    val lat: Double,
    val lon: Double
)

@Composable
fun NavigationBottomBanner(
    state: CallidusUiState,
    gpsCompassState: LiveGpsCompassState,
    accent: Color,
    onOpenNavSheet: () -> Unit,
    onStopNavigation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val route = state.activeRoute ?: return
    val curLat = if (gpsCompassState.hasGpsFix) gpsCompassState.lat else route.originLat
    val curLon = if (gpsCompassState.hasGpsFix) gpsCompassState.lon else route.originLon
    val steering = OfflineSteeringCalculator.computeSteering(
        currentLat = curLat,
        currentLon = curLon,
        compassHeadingDeg = gpsCompassState.compassHeadingDeg,
        destLat = route.destinationLat,
        destLon = route.destinationLon
    )
    val nextInstruction = route.steps.firstOrNull()?.instruction ?: steering.steerInstruction

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .testTag("navigation_active_banner"),
        color = ObsidianPanelElevated,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.3.dp, CallidusEmerald.copy(alpha = 0.8f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Navigation, contentDescription = "Active Route", tint = CallidusEmerald, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = "${route.destinationLabel} • ${steering.steerInstruction}",
                        style = MaterialTheme.typography.labelMedium,
                        color = CallidusEmerald,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${OfflineSteeringCalculator.formatDistance(steering.distanceMeters)} • " +
                            "${OfflineSteeringCalculator.formatDuration(route.durationSeconds)} • $nextInstruction",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedButton(
                    onClick = onOpenNavSheet,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    border = BorderStroke(1.dp, accent),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("OPEN", style = MaterialTheme.typography.labelSmall, color = accent)
                }
                IconButton(onClick = onStopNavigation, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Stop Navigation", tint = CallidusCrimson, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun NavigationPlannerSheet(
    visible: Boolean,
    state: CallidusUiState,
    gpsCompassState: LiveGpsCompassState,
    accent: Color,
    onSearchPlaces: (String) -> Unit,
    onSelectTravelMode: (NavigationTravelMode) -> Unit,
    onNavigateTo: (destLat: Double, destLon: Double, destLabel: String, originLat: Double?, originLon: Double?) -> Unit,
    onClearRoute: () -> Unit,
    onSwitchViewport: (ViewportMode) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = OFFLINE GPS & COMPASS, 1 = ROUTE PLANNER
    var query by remember { mutableStateOf("") }
    var useGpsOrigin by remember { mutableStateOf(gpsCompassState.hasGpsFix) }
    val offlinePins = remember { mutableStateListOf<OfflinePinWaypoint>() }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .widthIn(max = 620.dp)
                .navigationBarsPadding()
                .imePadding()
                .padding(10.dp),
            color = ObsidianPanelElevated,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.3.dp, accent.copy(alpha = 0.65f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Explore, contentDescription = null, tint = accent)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("NAVIGATION & OFFLINE COMPASS", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text("Hardware GPS + Magnetometer • Zero-Internet Ready", style = MaterialTheme.typography.labelSmall, color = CallidusEmerald)
                        }
                    }
                    IconButton(onClick = onClose, modifier = Modifier.size(48.dp).testTag("close_nav_sheet_button")) {
                        Icon(Icons.Default.Close, contentDescription = "Close Navigation", tint = TextSecondary)
                    }
                }

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = accent
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("OFFLINE COMPASS", style = MaterialTheme.typography.labelMedium) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("ROUTE PLANNER", style = MaterialTheme.typography.labelMedium) }
                    )
                }

                Spacer(Modifier.height(10.dp))

                if (selectedTab == 0) {
                    val curLat = if (gpsCompassState.hasGpsFix) gpsCompassState.lat else state.camera.centerLat
                    val curLon = if (gpsCompassState.hasGpsFix) gpsCompassState.lon else state.camera.centerLon
                    val route = state.activeRoute
                    val steering = route?.let {
                        OfflineSteeringCalculator.computeSteering(
                            currentLat = curLat,
                            currentLon = curLon,
                            compassHeadingDeg = gpsCompassState.compassHeadingDeg,
                            destLat = it.destinationLat,
                            destLon = it.destinationLon
                        )
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 12.dp)
                    ) {
                        // Offline Hardware GPS Readout
                        item {
                            Surface(
                                color = Color(0x66060D1A),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, (if (gpsCompassState.hasGpsFix) CallidusEmerald else CallidusGold).copy(alpha = 0.6f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.WifiOff, contentDescription = null, tint = CallidusEmerald, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = if (gpsCompassState.hasGpsFix) "GPS HARDWARE LOCK (${gpsCompassState.provider})" else "USING GLOBE COORDINATES (ACQUIRING GPS...)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (gpsCompassState.hasGpsFix) CallidusEmerald else CallidusGold,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Text(
                                        text = String.format(
                                            Locale.US,
                                            "LAT %+.5f°  LON %+.5f°  |  ALT %.0fm  |  %.1f km/h  |  ±%.0fm",
                                            curLat,
                                            curLon,
                                            gpsCompassState.altitudeMeters,
                                            gpsCompassState.speedMetersPerSec * 3.6f,
                                            gpsCompassState.accuracyMeters
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "BREADCRUMB TRAIL RECORDED: ${gpsCompassState.breadcrumbTrail.size} POINTS",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = accent
                                    )
                                }
                            }
                        }

                        // Large Live Hardware Compass Rose & Target Bearing Needle
                        item {
                            Surface(
                                color = Color(0x77040812),
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, accent.copy(alpha = 0.45f))
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    OfflineCompassRose(
                                        headingDeg = gpsCompassState.compassHeadingDeg,
                                        targetBearingDeg = steering?.targetBearingDeg,
                                        accent = accent,
                                        modifier = Modifier.size(190.dp)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = String.format(
                                            Locale.US,
                                            "HEADING %03.0f° %s",
                                            gpsCompassState.compassHeadingDeg,
                                            TelemetryCalculator.compassCardinal(gpsCompassState.compassHeadingDeg)
                                        ),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (steering != null && route != null) {
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = "TO ${route.destinationLabel}: ${steering.steerInstruction}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = CallidusEmerald,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "TRUE BEARING ${steering.targetBearingDeg.toInt()}° ${steering.cardinalDirection} • ${OfflineSteeringCalculator.formatDistance(steering.distanceMeters)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CallidusGold
                                        )
                                    } else {
                                        Text(
                                            text = "Select any offline landmark, bookmark, or drop a pin below to steer without internet.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary
                                        )
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                val idx = offlinePins.size + 1
                                                offlinePins.add(
                                                    0,
                                                    OfflinePinWaypoint(
                                                        id = "pin_$idx",
                                                        label = "OFFLINE PIN #$idx",
                                                        lat = curLat,
                                                        lon = curLon
                                                    )
                                                )
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = CallidusGold, contentColor = ObsidianVoid),
                                            modifier = Modifier.height(40.dp)
                                        ) {
                                            Icon(Icons.Default.AddLocationAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(5.dp))
                                            Text("DROP PIN HERE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        }
                                        if (route != null) {
                                            OutlinedButton(
                                                onClick = onClearRoute,
                                                border = BorderStroke(1.dp, CallidusCrimson),
                                                modifier = Modifier.height(40.dp)
                                            ) {
                                                Text("STOP STEERING", style = MaterialTheme.typography.labelSmall, color = CallidusCrimson)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (offlinePins.isNotEmpty()) {
                            item { Text("YOUR OFFLINE FIELD PINS", style = MaterialTheme.typography.labelMedium, color = CallidusGold) }
                            items(offlinePins, key = { it.id }) { pin ->
                                OfflineTargetRow(
                                    title = pin.label,
                                    subtitle = String.format(Locale.US, "%.4f°, %.4f°", pin.lat, pin.lon),
                                    accent = CallidusGold,
                                    onSteer = {
                                        onNavigateTo(pin.lat, pin.lon, pin.label, curLat, curLon)
                                    }
                                )
                            }
                        }

                        item { Text("PRELOADED GLOBAL OFFLINE TARGETS", style = MaterialTheme.typography.labelMedium, color = accent) }
                        items(GlobalLandmarks, key = { it.id }) { lm ->
                            val dist = Globe3DEngine.haversineKm(curLat, curLon, lm.lat, lm.lon) * 1000.0
                            OfflineTargetRow(
                                title = "${lm.name} (${lm.city})",
                                subtitle = "${lm.code} • ${OfflineSteeringCalculator.formatDistance(dist)}",
                                accent = accent,
                                onSteer = {
                                    onNavigateTo(lm.lat, lm.lon, lm.name, curLat, curLon)
                                }
                            )
                        }
                    }
                } else {
                    // Online + Geodesic Turn-by-Turn Route Planner Tab
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = query,
                                onValueChange = { query = it },
                                placeholder = { Text("Destination city, street, airport...", style = MaterialTheme.typography.bodyMedium) },
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("nav_destination_search"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accent,
                                    unfocusedBorderColor = accent.copy(alpha = 0.4f),
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )
                            Button(
                                onClick = { onSearchPlaces(query) },
                                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = ObsidianVoid),
                                modifier = Modifier.height(52.dp).testTag("nav_search_submit")
                            ) {
                                Icon(Icons.Default.Search, contentDescription = "Find Destination")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = useGpsOrigin && gpsCompassState.hasGpsFix,
                                onClick = { useGpsOrigin = !useGpsOrigin },
                                label = {
                                    Text(
                                        text = if (useGpsOrigin && gpsCompassState.hasGpsFix) "FROM: MY GPS" else "FROM: GLOBE CENTER",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            )
                            NavigationTravelMode.entries.forEach { m ->
                                FilterChip(
                                    selected = state.navTravelMode == m,
                                    onClick = { onSelectTravelMode(m) },
                                    label = { Text(m.label, style = MaterialTheme.typography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = accent.copy(alpha = 0.24f),
                                        selectedLabelColor = accent
                                    )
                                )
                            }
                        }

                        if (state.isRouteLoading) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = accent, strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Computing route geometry...", style = MaterialTheme.typography.labelMedium, color = accent)
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 12.dp)
                        ) {
                            if (state.geocodeResults.isNotEmpty()) {
                                item {
                                    Text("SEARCH RESULTS — TAP TO ROUTE", style = MaterialTheme.typography.labelSmall, color = CallidusGold)
                                }
                                items(state.geocodeResults, key = { "${it.lat}_${it.lon}_${it.name}" }) { place ->
                                    OfflineTargetRow(
                                        title = place.name,
                                        subtitle = place.displayName,
                                        accent = CallidusGold,
                                        onSteer = {
                                            val oLat = if (useGpsOrigin && gpsCompassState.hasGpsFix) gpsCompassState.lat else null
                                            val oLon = if (useGpsOrigin && gpsCompassState.hasGpsFix) gpsCompassState.lon else null
                                            onNavigateTo(place.lat, place.lon, place.name, oLat, oLon)
                                        }
                                    )
                                }
                            }

                            val route = state.activeRoute
                            if (route != null) {
                                item {
                                    Surface(
                                        color = Color(0x66060D1A),
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.dp, CallidusEmerald)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(
                                                text = "${route.originLabel} → ${route.destinationLabel}",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = TextPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${OfflineSteeringCalculator.formatDistance(route.distanceMeters)} • ${OfflineSteeringCalculator.formatDuration(route.durationSeconds)} • ${route.sourceLabel}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = CallidusEmerald
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Button(
                                                    onClick = { onSwitchViewport(ViewportMode.SAT_3D_BUILDINGS) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = ObsidianVoid),
                                                    modifier = Modifier.weight(1f).height(40.dp)
                                                ) {
                                                    Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(15.dp))
                                                    Spacer(Modifier.width(4.dp))
                                                    Text("3D BUILDINGS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                                }
                                                OutlinedButton(
                                                    onClick = {
                                                        val navUri = Uri.parse(
                                                            String.format(
                                                                Locale.US,
                                                                "google.navigation:q=%.5f,%.5f",
                                                                route.destinationLat,
                                                                route.destinationLon
                                                            )
                                                        )
                                                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, navUri)) }
                                                    },
                                                    border = BorderStroke(1.dp, CallidusGold),
                                                    modifier = Modifier.weight(1f).height(40.dp)
                                                ) {
                                                    Icon(Icons.Default.Directions, contentDescription = null, tint = CallidusGold, modifier = Modifier.size(15.dp))
                                                    Spacer(Modifier.width(4.dp))
                                                    Text("MAPS TURN", color = CallidusGold, style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }
                                    }
                                }

                                items(route.steps) { step ->
                                    Surface(
                                        color = Color(0x55060D1A),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(0.75.dp, accent.copy(alpha = 0.25f))
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = step.instruction,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextPrimary,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = OfflineSteeringCalculator.formatDistance(step.distanceMeters),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = accent
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OfflineCompassRose(
    headingDeg: Float,
    targetBearingDeg: Float?,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cx = size.width * 0.5f
        val cy = size.height * 0.5f
        val r = minOf(size.width, size.height) * 0.44f

        drawCircle(color = accent.copy(alpha = 0.22f), radius = r, center = Offset(cx, cy), style = Stroke(width = 2.4f))
        drawCircle(color = accent.copy(alpha = 0.45f), radius = r * 0.72f, center = Offset(cx, cy), style = Stroke(width = 1.2f))

        // Rotating compass ring ticks
        rotate(degrees = -headingDeg, pivot = Offset(cx, cy)) {
            for (deg in 0 until 360 step 15) {
                val rad = Math.toRadians(deg.toDouble() - 90.0)
                val innerR = if (deg % 90 == 0) r * 0.8f else if (deg % 45 == 0) r * 0.85f else r * 0.91f
                val x0 = cx + (cos(rad) * innerR).toFloat()
                val y0 = cy + (sin(rad) * innerR).toFloat()
                val x1 = cx + (cos(rad) * r).toFloat()
                val y1 = cy + (sin(rad) * r).toFloat()
                drawLine(
                    color = if (deg == 0) Color(0xFFFF5252) else accent.copy(alpha = 0.75f),
                    start = Offset(x0, y0),
                    end = Offset(x1, y1),
                    strokeWidth = if (deg % 90 == 0) 3.2f else 1.6f,
                    cap = StrokeCap.Round
                )
            }
        }

        // Fixed device lubber line at top
        drawLine(Color.White, Offset(cx, cy - r - 8f), Offset(cx, cy - r + 10f), strokeWidth = 3f)

        // Target True Bearing Steering Needle (if destination active)
        if (targetBearingDeg != null) {
            val relAngle = targetBearingDeg - headingDeg
            rotate(degrees = relAngle, pivot = Offset(cx, cy)) {
                val needle = Path().apply {
                    moveTo(cx, cy - r * 0.82f)
                    lineTo(cx - 11f, cy + 12f)
                    lineTo(cx, cy + 4f)
                    lineTo(cx + 11f, cy + 12f)
                    close()
                }
                drawPath(needle, color = CallidusEmerald)
            }
        }
        drawCircle(color = accent, radius = 6f, center = Offset(cx, cy))
    }
}

@Composable
private fun OfflineTargetRow(
    title: String,
    subtitle: String,
    accent: Color,
    onSteer: () -> Unit
) {
    Surface(
        onClick = onSteer,
        color = Color(0x66060D1A),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.8.dp, accent.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = accent, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.Navigation, contentDescription = "Steer Here", tint = CallidusEmerald, modifier = Modifier.size(18.dp))
        }
    }
}

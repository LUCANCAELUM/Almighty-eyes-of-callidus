package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.domain.model.ArTargetFilter
import com.example.domain.model.CallidusUiState
import com.example.domain.model.OpticMode
import com.example.domain.model.SelectedTarget
import com.example.engine.ArScreenProjection
import com.example.engine.ArSkyProjectionEngine
import com.example.engine.LiveGpsCompassState
import com.example.ui.theme.CallidusEmerald
import com.example.ui.theme.CallidusGold
import com.example.ui.theme.ObsidianPanel
import com.example.ui.theme.ObsidianPanelElevated
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private sealed interface ArProjectedBadge {
    val projection: ArScreenProjection
    val title: String
    val subtitle: String
    val badgeColor: Color
    val isFlight: Boolean
    val target: SelectedTarget

    data class FlightBadge(
        override val projection: ArScreenProjection,
        override val title: String,
        override val subtitle: String,
        override val badgeColor: Color,
        override val target: SelectedTarget
    ) : ArProjectedBadge {
        override val isFlight: Boolean = true
    }

    data class SatelliteBadge(
        override val projection: ArScreenProjection,
        override val title: String,
        override val subtitle: String,
        override val badgeColor: Color,
        override val target: SelectedTarget
    ) : ArProjectedBadge {
        override val isFlight: Boolean = false
    }
}

@Composable
fun ArSkyHudViewport(
    state: CallidusUiState,
    gpsCompassState: LiveGpsCompassState,
    accent: Color,
    onSelectFilter: (ArTargetFilter) -> Unit,
    onSelectTarget: (SelectedTarget) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var cameraBindError by remember { mutableStateOf<String?>(null) }
    var manualPanAzOffset by remember { mutableFloatStateOf(0f) }
    var manualPanElOffset by remember { mutableFloatStateOf(0f) }

    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasCameraPermission = results[Manifest.permission.CAMERA] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        hasLocationPermission = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission || !hasLocationPermission) {
            permLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }
    }

    val effectiveAz = (((gpsCompassState.compassHeadingDeg + manualPanAzOffset) % 360f) + 360f) % 360f
    val effectiveEl = (gpsCompassState.cameraPitchDeg + manualPanElOffset).coerceIn(-65f, 88f)
    val obsLat = if (gpsCompassState.hasGpsFix) gpsCompassState.lat else state.camera.centerLat
    val obsLon = if (gpsCompassState.hasGpsFix) gpsCompassState.lon else state.camera.centerLon
    val obsAltKm = if (gpsCompassState.hasGpsFix) gpsCompassState.altitudeMeters / 1000.0 else 0.05

    val tintOverlay = when (state.opticMode) {
        OpticMode.FLIR_THERMAL -> Color(0x30FF5722)
        OpticMode.NVG_PHOSPHOR -> Color(0x3600E676)
        OpticMode.CRT_MONITOR -> Color(0x2800B0FF)
        OpticMode.NOIR_SURVEILLANCE -> Color(0x33000000)
        OpticMode.WIREFRAME_HOLO -> Color(0x2E00E5FF)
        else -> Color(0x1404070D)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianVoid)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    manualPanAzOffset -= dragAmount.x * 0.16f
                    manualPanElOffset = (manualPanElOffset + dragAmount.y * 0.14f).coerceIn(-75f, 75f)
                }
            }
    ) {
        if (hasCameraPermission && cameraBindError == null) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                    val future = ProcessCameraProvider.getInstance(ctx)
                    future.addListener(
                        {
                            runCatching {
                                val provider = future.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }
                                provider.unbindAll()
                                provider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview
                                )
                            }.onFailure { err ->
                                cameraBindError = err.localizedMessage ?: "Camera sensor unavailable"
                            }
                        },
                        ContextCompat.getMainExecutor(ctx)
                    )
                    previewView
                }
            )
            DisposableEffect(lifecycleOwner) {
                onDispose {
                    runCatching {
                        ProcessCameraProvider.getInstance(context).get().unbindAll()
                    }
                }
            }
        } else {
            // Optical Synth Sky Backdrop when camera permission or hardware sensor isn't attached
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF020714),
                                Color(0xFF08192F),
                                Color(0xFF040A16)
                            )
                        )
                    )
            )
        }

        // Optic tint veil
        Box(modifier = Modifier.fillMaxSize().background(tintOverlay))

        // AR Horizon Compass Bar, Pitch Ladder & Crosshair Reticle
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w * 0.5f
            val cy = h * 0.5f

            // Subtle pitch ladder lines
            for (deg in listOf(-30f, -15f, 0f, 15f, 30f, 45f, 60f)) {
                val dy = (effectiveEl - deg) * (h / 52f)
                val yPos = cy + dy
                if (yPos in (h * 0.18f)..(h * 0.82f)) {
                    val alpha = if (deg == 0f) 0.55f else 0.25f
                    val halfSpan = if (deg == 0f) w * 0.34f else w * 0.16f
                    drawLine(
                        color = accent.copy(alpha = alpha),
                        start = Offset(cx - halfSpan, yPos),
                        end = Offset(cx - 28f, yPos),
                        strokeWidth = if (deg == 0f) 2.2f else 1.4f
                    )
                    drawLine(
                        color = accent.copy(alpha = alpha),
                        start = Offset(cx + 28f, yPos),
                        end = Offset(cx + halfSpan, yPos),
                        strokeWidth = if (deg == 0f) 2.2f else 1.4f
                    )
                }
            }

            // Center targeting reticle
            val arm = 22f
            drawCircle(
                color = accent.copy(alpha = 0.36f),
                radius = 44f,
                center = Offset(cx, cy),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.6f)
            )
            drawLine(accent, Offset(cx - arm, cy), Offset(cx - 7f, cy), 2.2f)
            drawLine(accent, Offset(cx + 7f, cy), Offset(cx + arm, cy), 2.2f)
            drawLine(accent, Offset(cx, cy - arm), Offset(cx, cy - 7f), 2.2f)
            drawLine(accent, Offset(cx, cy + 7f), Offset(cx, cy + arm), 2.2f)
        }

        // AR Sky Targets Overlay (Aircraft + Orbital Satellites)
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val widthPx = with(density) { maxWidth.toPx() }
            val heightPx = with(density) { maxHeight.toPx() }

            val badges = remember(
                state.aircraft,
                state.satellites,
                state.arTargetFilter,
                obsLat,
                obsLon,
                obsAltKm,
                effectiveAz,
                effectiveEl,
                widthPx,
                heightPx
            ) {
                val out = mutableListOf<ArProjectedBadge>()
                if (state.arTargetFilter == ArTargetFilter.ALL || state.arTargetFilter == ArTargetFilter.AIRCRAFT) {
                    state.aircraft.take(36).forEach { fl ->
                        val proj = ArSkyProjectionEngine.projectToCameraViewport(
                            obsLat = obsLat,
                            obsLon = obsLon,
                            obsAltKm = obsAltKm,
                            tgtLat = fl.lat,
                            tgtLon = fl.lon,
                            tgtAltKm = fl.altitudeKm.coerceAtLeast(0.3),
                            camAzimuthDeg = effectiveAz,
                            camElevationDeg = effectiveEl,
                            viewportWidth = widthPx,
                            viewportHeight = heightPx
                        )
                        out.add(
                            ArProjectedBadge.FlightBadge(
                                projection = proj,
                                title = "${fl.callsign} • FL${fl.altitudeFeet / 100}",
                                subtitle = String.format(
                                    Locale.US,
                                    "%dkts • %.0fkm • AZ%.0f°",
                                    fl.groundSpeedKnots,
                                    proj.polar.slantRangeKm,
                                    proj.polar.azimuthDeg
                                ),
                                badgeColor = Color(0xFFFFC107),
                                target = SelectedTarget.Flight(fl)
                            )
                        )
                    }
                }
                if (state.arTargetFilter == ArTargetFilter.ALL || state.arTargetFilter == ArTargetFilter.SATELLITES) {
                    state.satellites.take(28).forEach { sat ->
                        val proj = ArSkyProjectionEngine.projectToCameraViewport(
                            obsLat = obsLat,
                            obsLon = obsLon,
                            obsAltKm = obsAltKm,
                            tgtLat = sat.lat,
                            tgtLon = sat.lon,
                            tgtAltKm = sat.altitudeKm,
                            camAzimuthDeg = effectiveAz,
                            camElevationDeg = effectiveEl,
                            viewportWidth = widthPx,
                            viewportHeight = heightPx
                        )
                        if (proj.polar.elevationDeg > -8f) {
                            out.add(
                                ArProjectedBadge.SatelliteBadge(
                                    projection = proj,
                                    title = sat.element.name,
                                    subtitle = String.format(
                                        Locale.US,
                                        "ALT %.0fkm • %.1fkm/s • EL%.0f°",
                                        sat.altitudeKm,
                                        sat.velocityKmS,
                                        proj.polar.elevationDeg
                                    ),
                                    badgeColor = accent,
                                    target = SelectedTarget.Satellite(sat)
                                )
                            )
                        }
                    }
                }
                out
            }

            // Render visible in-FOV AR floating labels
            badges.filter { it.projection.isInFov }.take(18).forEach { badge ->
                val xPx = badge.projection.screenX.roundToInt() - 120
                val yPx = badge.projection.screenY.roundToInt() - 48
                Surface(
                    onClick = { onSelectTarget(badge.target) },
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                xPx.coerceIn(12, (widthPx.roundToInt() - 220).coerceAtLeast(12)),
                                yPx.coerceIn(220, (heightPx.roundToInt() - 260).coerceAtLeast(220))
                            )
                        }
                        .testTag("ar_target_badge"),
                    color = Color(0xCC040912),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.2.dp, badge.badgeColor.copy(alpha = 0.85f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (badge.isFlight) Icons.Default.Flight else Icons.Default.SatelliteAlt,
                            contentDescription = badge.title,
                            tint = badge.badgeColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Column {
                            Text(
                                text = badge.title,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = badge.subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = badge.badgeColor,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Off-screen directional lock indicators (closest target cue)
            val nearestOffscreen = badges
                .filter { !it.projection.isInFov }
                .minByOrNull { abs(it.projection.deltaAzimuthDeg) + abs(it.projection.deltaElevationDeg) }

            if (nearestOffscreen != null) {
                val arrow = if (nearestOffscreen.projection.deltaAzimuthDeg > 0f) "▶" else "◀"
                Surface(
                    onClick = {
                        manualPanAzOffset += nearestOffscreen.projection.deltaAzimuthDeg
                        manualPanElOffset = (manualPanElOffset + nearestOffscreen.projection.deltaElevationDeg)
                            .coerceIn(-75f, 75f)
                    },
                    color = Color(0xBB050B16),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, nearestOffscreen.badgeColor.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = (-80).dp)
                ) {
                    Text(
                        text = "$arrow NEXT TARGET: ${nearestOffscreen.title} (${abs(nearestOffscreen.projection.deltaAzimuthDeg).toInt()}°)",
                        style = MaterialTheme.typography.labelSmall,
                        color = nearestOffscreen.badgeColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        // Top AR Telemetry & Target Filter Strip (positioned safely below CallidusTopTelemetryBar)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 148.dp, start = 12.dp, end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                color = ObsidianPanel,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = String.format(
                            Locale.US,
                            "HDG %03.0f° (%s) • PITCH %+.0f° • %s",
                            effectiveAz,
                            TelemetryCalculator.compassCardinal(effectiveAz),
                            effectiveEl,
                            if (gpsCompassState.hasGpsFix) "GPS FIX" else "GRID POS"
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                        fontWeight = FontWeight.Bold
                    )
                    if (abs(manualPanAzOffset) > 1f || abs(manualPanElOffset) > 1f) {
                        OutlinedButton(
                            onClick = {
                                manualPanAzOffset = 0f
                                manualPanElOffset = 0f
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            border = BorderStroke(0.9.dp, CallidusGold),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = "Re-center AR Compass", tint = CallidusGold, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("RECENTER", style = MaterialTheme.typography.labelSmall, color = CallidusGold)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ArTargetFilter.entries.forEach { f ->
                    FilterChip(
                        selected = state.arTargetFilter == f,
                        onClick = { onSelectFilter(f) },
                        label = { Text(f.label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accent.copy(alpha = 0.26f),
                            selectedLabelColor = accent
                        )
                    )
                }
            }

            if (!hasCameraPermission || cameraBindError != null) {
                Surface(
                    color = ObsidianPanelElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CallidusGold.copy(alpha = 0.7f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = cameraBindError ?: "Enable Camera for live AR pass-through (drag sky to pan)",
                            style = MaterialTheme.typography.labelSmall,
                            color = CallidusGold,
                            modifier = Modifier.weight(1f)
                        )
                        if (!hasCameraPermission) {
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    permLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.CAMERA,
                                            Manifest.permission.ACCESS_FINE_LOCATION
                                        )
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = ObsidianVoid),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("ENABLE", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        // Bottom helper hint above main command carousel
        Surface(
            color = Color(0xAA050B16),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(0.8.dp, CallidusEmerald.copy(alpha = 0.45f)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 132.dp)
        ) {
            Text(
                text = "POINT SKYWARD OR DRAG VIEW • TAP ANY AIRCRAFT/SATELLITE TAG TO LOCK",
                style = MaterialTheme.typography.labelSmall,
                color = CallidusEmerald,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

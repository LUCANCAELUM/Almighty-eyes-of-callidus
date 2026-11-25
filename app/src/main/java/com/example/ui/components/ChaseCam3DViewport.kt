package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.CallidusUiState
import com.example.domain.model.OpticMode
import com.example.domain.model.SelectedTarget
import com.example.engine.DeviceTilt
import com.example.engine.Globe3DEngine
import com.example.ui.theme.ObsidianPanel
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos

/**
 * First-Person 3D Ride-Along Cockpit, Orbital Chase-Cam, Maritime Bow-Cam & CCTV Viewshed Fly-Over.
 * Preserves all 8 OpticMode sensor styles (EO, FLIR, NVG, CRT, Noir, Snow, Holo, Solar).
 */
@Composable
fun ChaseCam3DViewport(
    state: CallidusUiState,
    deviceTilt: DeviceTilt,
    onFreeLookDrag: (dx: Float, dy: Float) -> Unit,
    onExitChase: () -> Unit,
    modifier: Modifier = Modifier
) {
    val target = state.selectedTarget
    if (target !is SelectedTarget.Flight &&
        target !is SelectedTarget.Satellite &&
        target !is SelectedTarget.Vessel &&
        target !is SelectedTarget.Cctv
    ) return

    val palette = state.opticMode.palette
    val accent = palette.hudAccent

    val infinite = rememberInfiniteTransition(label = "chase_cam")
    val gridScroll by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "grid_scroll"
    )
    val bankingWave by infinite.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bank_wave"
    )

    val targetTitle = when (target) {
        is SelectedTarget.Flight -> target.data.callsign
        is SelectedTarget.Satellite -> target.data.element.name
        is SelectedTarget.Vessel -> target.data.name
        is SelectedTarget.Cctv -> target.data.name
        else -> ""
    }
    val headingDeg = when (target) {
        is SelectedTarget.Flight -> target.data.headingDeg
        is SelectedTarget.Satellite -> 48f
        is SelectedTarget.Vessel -> target.data.headingDeg
        is SelectedTarget.Cctv -> target.data.azimuthDeg
        else -> 0f
    }
    val speedLabel = when (target) {
        is SelectedTarget.Flight -> "${target.data.groundSpeedKnots} KTS"
        is SelectedTarget.Satellite -> String.format(Locale.US, "%.2f KM/S", target.data.velocityKmS)
        is SelectedTarget.Vessel -> String.format(Locale.US, "%.1f KTS", target.data.speedKnots)
        is SelectedTarget.Cctv -> "FOV ${target.data.fovDeg.toInt()}°"
        else -> ""
    }
    val altLabel = when (target) {
        is SelectedTarget.Flight -> "${target.data.altitudeFeet} FT"
        is SelectedTarget.Satellite -> String.format(Locale.US, "%.0f KM", target.data.altitudeKm)
        is SelectedTarget.Vessel -> "SEA LEVEL"
        is SelectedTarget.Cctv -> String.format(Locale.US, "%.2f KM", target.data.rangeKm)
        else -> ""
    }
    val curLat = when (target) {
        is SelectedTarget.Flight -> target.data.lat
        is SelectedTarget.Satellite -> target.data.lat
        is SelectedTarget.Vessel -> target.data.lat
        is SelectedTarget.Cctv -> target.data.lat
        else -> 0.0
    }
    val curLon = when (target) {
        is SelectedTarget.Flight -> target.data.lon
        is SelectedTarget.Satellite -> target.data.lon
        is SelectedTarget.Vessel -> target.data.lon
        is SelectedTarget.Cctv -> target.data.lon
        else -> 0.0
    }

    val liveBankDeg = (
        (if (target is SelectedTarget.Cctv || target is SelectedTarget.Vessel) 0f else bankingWave * 5.5f) +
            deviceTilt.x * 8f - state.camera.chaseLookYawDeg * 0.12f
        ).coerceIn(-22f, 22f)
    val livePitchDeg = (state.camera.chaseLookPitchDeg + deviceTilt.y * 5f).coerceIn(-30f, 30f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianVoid)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onFreeLookDrag(dragAmount.x, dragAmount.y)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w * 0.5f
            val horizonY = h * (0.44f + livePitchDeg / 90f)

            // 1. Sky / Deep Orbital Atmosphere Dome
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        palette.spaceBgTop,
                        palette.atmosphereGlow.copy(alpha = 0.28f),
                        palette.globeOceanNight
                    ),
                    startY = 0f,
                    endY = h
                )
            )

            // 2. Curved 3D Planetary Horizon Glow + Starfield
            rotate(degrees = liveBankDeg, pivot = Offset(cx, horizonY)) {
                for (i in 0 until 45) {
                    val sx = ((i * 197 + state.camera.chaseLookYawDeg * 6f) % w + w) % w
                    val sy = ((i * 89) % (horizonY * 0.85f).coerceAtLeast(100f))
                    drawCircle(
                        color = Color.White.copy(alpha = 0.35f + (i % 4) * 0.15f),
                        radius = 1.4f,
                        center = Offset(sx, sy)
                    )
                }

                val limbPath = Path().apply {
                    moveTo(-w * 0.5f, horizonY + 120f)
                    quadraticTo(cx, horizonY - 36f, w * 1.5f, horizonY + 120f)
                }
                drawPath(
                    path = limbPath,
                    color = palette.atmosphereGlow.copy(alpha = 0.55f),
                    style = Stroke(width = 4f)
                )

                // 3. Rushing 3D Perspective Terrain / Orbital Lattice Grid
                val groundTop = horizonY + 8f
                val groundH = (h - groundTop).coerceAtLeast(80f)
                val numRows = 14
                for (r in 0 until numRows) {
                    val t = ((r + gridScroll) / numRows).coerceIn(0f, 1f)
                    val depthY = groundTop + groundH * (t * t)
                    drawLine(
                        color = palette.coastlineStroke.copy(alpha = (t * 0.55f).coerceIn(0.08f, 0.6f)),
                        start = Offset(-w * 0.4f, depthY),
                        end = Offset(w * 1.4f, depthY),
                        strokeWidth = 1.1f + t * 1.2f
                    )
                }

                for (col in -8..8) {
                    val topX = cx + col * (w * 0.032f)
                    val botX = cx + col * (w * 0.24f)
                    drawLine(
                        color = palette.graticuleStroke.copy(alpha = 0.55f),
                        start = Offset(topX, groundTop),
                        end = Offset(botX, h + 120f),
                        strokeWidth = 1.3f
                    )
                }

                // If in CCTV Viewshed Fly-Over, draw forward 3D Viewshed Frustum Cone on terrain!
                if (target is SelectedTarget.Cctv) {
                    val halfFovRatio = (target.data.fovDeg / 90f).coerceIn(0.25f, 1.1f)
                    val coneLeft = cx - w * 0.36f * halfFovRatio
                    val coneRight = cx + w * 0.36f * halfFovRatio
                    val viewshedPath = Path().apply {
                        moveTo(cx, h * 0.72f)
                        lineTo(coneLeft, horizonY + 24f)
                        lineTo(coneRight, horizonY + 24f)
                        close()
                    }
                    drawPath(path = viewshedPath, color = palette.cctvColor.copy(alpha = 0.18f))
                    drawPath(path = viewshedPath, color = palette.cctvColor.copy(alpha = 0.78f), style = Stroke(width = 2.2f))
                }

                // 4. 3D Artificial Horizon Pitch Ladder
                for (pitchMark in listOf(-20, -10, 0, 10, 20)) {
                    val py = horizonY - pitchMark * 6.5f
                    val barW = if (pitchMark == 0) w * 0.28f else w * 0.13f
                    val gap = w * 0.055f
                    drawLine(
                        color = accent.copy(alpha = if (pitchMark == 0) 0.9f else 0.55f),
                        start = Offset(cx - barW, py),
                        end = Offset(cx - gap, py),
                        strokeWidth = if (pitchMark == 0) 2.5f else 1.5f
                    )
                    drawLine(
                        color = accent.copy(alpha = if (pitchMark == 0) 0.9f else 0.55f),
                        start = Offset(cx + gap, py),
                        end = Offset(cx + barW, py),
                        strokeWidth = if (pitchMark == 0) 2.5f else 1.5f
                    )
                }
            }

            // 5. Project Nearby Live Aircraft, Maritime Vessels & CCTV Cones into 3D Forward Frustum
            state.aircraft.filter {
                Globe3DEngine.haversineKm(curLat, curLon, it.lat, it.lon) in 3.0..450.0
            }.take(12).forEach { other ->
                val bearing = Math.toDegrees(atan2((other.lon - curLon) * cos(Math.toRadians(curLat)), other.lat - curLat)).toFloat()
                var relAngle = bearing - (headingDeg + state.camera.chaseLookYawDeg)
                while (relAngle > 180f) relAngle -= 360f
                while (relAngle < -180f) relAngle += 360f
                if (abs(relAngle) < 55f) {
                    val distKm = Globe3DEngine.haversineKm(curLat, curLon, other.lat, other.lon).toFloat()
                    val sx = cx + (relAngle / 55f) * (w * 0.45f)
                    val sy = horizonY + (1f - (distKm / 450f).coerceIn(0f, 1f)) * 90f - 25f
                    val boxSize = (18f - distKm / 35f).coerceIn(8f, 20f)
                    drawRect(
                        color = palette.aircraftColor.copy(alpha = 0.85f),
                        topLeft = Offset(sx - boxSize / 2f, sy - boxSize / 2f),
                        size = Size(boxSize, boxSize),
                        style = Stroke(width = 1.8f)
                    )
                }
            }

            state.cctvCameras.filter {
                Globe3DEngine.haversineKm(curLat, curLon, it.lat, it.lon) in 0.2..35.0
            }.take(10).forEach { cam ->
                val bearing = Math.toDegrees(atan2((cam.lon - curLon) * cos(Math.toRadians(curLat)), cam.lat - curLat)).toFloat()
                var relAngle = bearing - (headingDeg + state.camera.chaseLookYawDeg)
                while (relAngle > 180f) relAngle -= 360f
                while (relAngle < -180f) relAngle += 360f
                if (abs(relAngle) < 55f) {
                    val distKm = Globe3DEngine.haversineKm(curLat, curLon, cam.lat, cam.lon).toFloat()
                    val sx = cx + (relAngle / 55f) * (w * 0.45f)
                    val sy = horizonY + (1f - (distKm / 35f).coerceIn(0f, 1f)) * 120f + 16f
                    drawCircle(color = palette.cctvColor, radius = 5.5f, center = Offset(sx, sy))
                }
            }

            // 6. Center 3D Bore-Sight Velocity Vector Reticle
            val bore = Offset(cx, h * 0.45f)
            drawCircle(color = accent, radius = 12f, center = bore, style = Stroke(width = 2.2f))
            drawLine(accent, Offset(bore.x - 24f, bore.y), Offset(bore.x - 12f, bore.y), 2.2f, StrokeCap.Round)
            drawLine(accent, Offset(bore.x + 12f, bore.y), Offset(bore.x + 24f, bore.y), 2.2f, StrokeCap.Round)
            drawLine(accent, Offset(bore.x, bore.y - 22f), Offset(bore.x, bore.y - 12f), 2.2f, StrokeCap.Round)

            // 7. Persistent Optical Sensor Post-FX across Chase Cam (matching gods-eye-view)
            if (palette.scanlineAlpha > 0.02f) {
                val step = if (state.opticMode == OpticMode.CRT_MONITOR) 7f else 10f
                var y = 0f
                while (y < h) {
                    drawLine(
                        color = Color.Black.copy(alpha = palette.scanlineAlpha),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 2.2f
                    )
                    y += step
                }
            }
            if (state.opticMode == OpticMode.NVG_PHOSPHOR || state.opticMode == OpticMode.CRT_MONITOR) {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.58f)),
                        center = Offset(cx, h * 0.5f),
                        radius = maxOf(w, h) * 0.74f
                    )
                )
            }
        }

        // HUD Overlay Top Telemetry Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = ObsidianPanel,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val icon = when (target) {
                        is SelectedTarget.Satellite -> Icons.Default.SatelliteAlt
                        is SelectedTarget.Vessel -> Icons.Default.DirectionsBoat
                        is SelectedTarget.Cctv -> Icons.Default.Videocam
                        else -> Icons.Default.Flight
                    }
                    Icon(imageVector = icon, contentDescription = null, tint = accent)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "${state.opticMode.code} CHASE // $targetTitle",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = String.format(Locale.US, "HDG %03.0f°  •  %.3f°, %.3f°", headingDeg, curLat, curLon),
                            style = MaterialTheme.typography.labelSmall,
                            color = accent
                        )
                    }
                }
            }

            Button(
                onClick = onExitChase,
                modifier = Modifier.testTag("exit_chase_cam_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent.copy(alpha = 0.2f),
                    contentColor = accent
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, accent)
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Return to Orbit")
                Spacer(Modifier.width(6.dp))
                Text("ORBIT", style = MaterialTheme.typography.labelLarge)
            }
        }

        // Bottom Speed & Altitude Tapes + Free-Look Hint
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Surface(
                color = ObsidianPanel,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(if (target is SelectedTarget.Cctv) "OPTIC FOV" else "VELOCITY", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(speedLabel, style = MaterialTheme.typography.headlineMedium, color = accent)
                }
            }

            Text(
                text = "DRAG TO FREE-LOOK IN 3D",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary.copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 10.dp)
            )

            Surface(
                color = ObsidianPanel,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(if (target is SelectedTarget.Cctv) "VIEWSHED" else "ALTITUDE", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(altLabel, style = MaterialTheme.typography.headlineMedium, color = accent)
                }
            }
        }
    }
}

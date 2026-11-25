package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.firebase.SavedBookmark
import com.example.domain.model.CallidusUiState
import com.example.domain.model.FeedStatus
import com.example.domain.model.GlobeImageryStyle
import com.example.domain.model.IntelLayer
import com.example.domain.model.OpticMode
import com.example.domain.model.SeismicConnectionStatus
import com.example.domain.model.SelectedTarget
import com.example.domain.model.StrategicTheaters
import com.example.domain.model.TheaterPreset
import com.example.domain.model.ViewportMode
import com.example.engine.DeviceTilt
import com.example.ui.theme.CallidusCrimson
import com.example.ui.theme.CallidusEmerald
import com.example.ui.theme.CallidusGold
import com.example.ui.theme.ObsidianPanel
import com.example.ui.theme.ObsidianPanelElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun CallidusTopTelemetryBar(
    state: CallidusUiState,
    deviceTilt: DeviceTilt,
    onRefresh: () -> Unit,
    onToggleAutoRotate: () -> Unit,
    onToggleSound: () -> Unit,
    onToggleCleanHud: () -> Unit,
    onOpenNavigation: () -> Unit,
    onVoiceClick: () -> Unit,
    onSelectViewport: (ViewportMode) -> Unit,
    onOpenWorldometers: () -> Unit,
    onOpenSeismic: () -> Unit,
    onOpenAircraftTelemetry: () -> Unit,
    onOpenSatelliteTelemetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = state.opticMode.palette.hudAccent
    val density = LocalDensity.current.density
    var utcText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("HH:mm:ss 'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        while (true) {
            utcText = sdf.format(Date())
            delay(1000L)
        }
    }

    val isAnySyncing = state.feedStatuses.values.any { it is FeedStatus.Syncing }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .graphicsLayer {
                rotationY = deviceTilt.x * 4f
                rotationX = -deviceTilt.y * 3f
                cameraDistance = 14f * density
            },
        color = ObsidianPanel,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Animated3DCallidusLogo(
                        size = 34.dp,
                        accent = if (isAnySyncing) CallidusGold else accent,
                        secondaryAccent = CallidusGold,
                        dramaticEntrance = false
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "SYS // CALLIDUS",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Surface(
                                color = CallidusGold.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(0.75.dp, CallidusGold.copy(alpha = 0.55f))
                            ) {
                                Text(
                                    text = "PANOPTIC",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CallidusGold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = String.format(
                                Locale.US,
                                "%+.2f°, %+.2f° • %.1fx • %s",
                                state.camera.centerLat,
                                state.camera.centerLon,
                                state.camera.zoom,
                                utcText
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = accent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onOpenNavigation,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("top_nav_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = "Navigation & Offline Compass",
                            tint = if (state.activeRoute != null) CallidusEmerald else accent
                        )
                    }
                    IconButton(
                        onClick = onToggleSound,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("toggle_sound_button")
                    ) {
                        Icon(
                            imageVector = if (state.isSoundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "Toggle Ops Room Sound",
                            tint = if (state.isSoundEnabled) CallidusEmerald else TextMuted
                        )
                    }
                    IconButton(
                        onClick = onToggleCleanHud,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("toggle_clean_hud_button")
                    ) {
                        Icon(
                            imageVector = if (state.isCleanHudMode) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Declutter HUD",
                            tint = TextSecondary
                        )
                    }
                    IconButton(
                        onClick = onVoiceClick,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("voice_command_button")
                    ) {
                        Icon(
                            imageVector = if (state.isVoiceListening) Icons.Default.Mic else Icons.Default.MicNone,
                            contentDescription = "Voice Command",
                            tint = if (state.isVoiceListening) CallidusCrimson else accent
                        )
                    }
                    IconButton(
                        onClick = onToggleAutoRotate,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("toggle_autorotate_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RotateRight,
                            contentDescription = "Toggle 3D Auto-Orbit",
                            tint = if (state.autoRotateGlobe) accent else TextMuted
                        )
                    }
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("sync_telemetry_button")
                    ) {
                        Icon(
                            imageVector = if (isAnySyncing) Icons.Default.Sync else Icons.Default.Refresh,
                            contentDescription = "Sync Live Feeds",
                            tint = accent
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // Compact live telemetry strip: Worldometers, Air, Orbit, CCTV, Maritime, Seismic, Kp, Local Weather
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val worldPop = state.worldometersStats.firstOrNull { it.key == "current_population" }?.value
                val worldColor = if (worldPop != null) CallidusEmerald else CallidusGold
                Surface(
                    onClick = onOpenWorldometers,
                    color = worldColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(0.9.dp, worldColor.copy(alpha = 0.6f)),
                    modifier = Modifier.testTag("open_worldometers_pill")
                ) {
                    Text(
                        text = if (worldPop != null) "WORLD POP $worldPop" else "WORLDOMETERS ${state.worldometersStatus}",
                        style = MaterialTheme.typography.labelSmall,
                        color = worldColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                val seismicLive = state.seismicStream.status == SeismicConnectionStatus.LIVE_STREAM
                val seismicColor = if (seismicLive) CallidusCrimson else Color(0xFFFF7043)
                Surface(
                    onClick = onOpenSeismic,
                    color = seismicColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(0.9.dp, seismicColor.copy(alpha = 0.65f)),
                    modifier = Modifier.testTag("open_seismic_pill")
                ) {
                    Text(
                        text = if (seismicLive) "SEISMIC ${state.earthquakes.size} • LIVE" else "QUAKE ${state.earthquakes.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = seismicColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                val airColor = Color(0xFFFFC107)
                Surface(
                    onClick = onOpenAircraftTelemetry,
                    color = airColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(0.9.dp, airColor.copy(alpha = 0.55f)),
                    modifier = Modifier.testTag("open_air_telemetry_pill")
                ) {
                    Text(
                        text = "AIR ${state.aircraft.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = airColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Surface(
                    onClick = onOpenSatelliteTelemetry,
                    color = accent.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(0.9.dp, accent.copy(alpha = 0.55f)),
                    modifier = Modifier.testTag("open_sat_telemetry_pill")
                ) {
                    Text(
                        text = "SAT ${state.satellites.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                TelemetryPill("CCTV ${state.cctvCameras.size}", state.opticMode.palette.cctvColor)
                TelemetryPill("SHIP ${state.maritimeVessels.size}", state.opticMode.palette.shipColor)
                TelemetryPill("EONET ${state.naturalEvents.size}", Color(0xFFFF7043))
                TelemetryPill("Kp ${String.format("%.1f", state.spaceWeather.kpIndex)}", CallidusEmerald)
                state.localWeather?.let { wx ->
                    TelemetryPill(
                        "${String.format("%.0f", wx.temperatureC)}°C • ${String.format("%.0f", wx.windSpeedKmh)}km/h",
                        TextSecondary
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Viewport Switcher: 3D Globe | 3D Buildings | AR Camera | Google Sat | Three.js 3D
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ViewportMode.entries.forEach { mode ->
                    val active = state.viewportMode == mode
                    Surface(
                        onClick = { onSelectViewport(mode) },
                        modifier = Modifier
                            .widthIn(min = 94.dp)
                            .height(34.dp)
                            .testTag("viewport_mode_${mode.name.lowercase()}"),
                        color = if (active) accent.copy(alpha = 0.24f) else Color(0x55060D1A),
                        shape = RoundedCornerShape(9.dp),
                        border = BorderStroke(
                            width = if (active) 1.2.dp else 0.75.dp,
                            color = if (active) accent else accent.copy(alpha = 0.25f)
                        )
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        ) {
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (active) accent else TextSecondary,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryPill(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(0.75.dp, color.copy(alpha = 0.4f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun Pitch3DGimbalControl(
    pitchDeg: Float,
    zoom: Float,
    accent: Color,
    onPitchChange: (Float) -> Unit,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .padding(end = 10.dp)
            .width(56.dp),
        color = ObsidianPanel,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(
                onClick = { onZoomChange((zoom * 1.38f).coerceAtMost(24f)) },
                modifier = Modifier
                    .size(44.dp)
                    .testTag("zoom_in_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = accent)
            }
            Text(
                text = String.format("%.1fx", zoom),
                style = MaterialTheme.typography.labelSmall,
                color = TextPrimary
            )
            IconButton(
                onClick = { onZoomChange((zoom / 1.35f).coerceAtLeast(0.85f)) },
                modifier = Modifier
                    .size(44.dp)
                    .testTag("zoom_out_button")
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = accent)
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp),
                color = accent.copy(alpha = 0.25f)
            )

            Text(
                text = "3D",
                style = MaterialTheme.typography.labelSmall,
                color = accent
            )
            // Compact vertical-style 3D tilt presets (0°, 32°, 58°) for crisp 1-tap 3D oblique switching!
            for (preset in listOf(60f, 34f, 0f)) {
                val selected = kotlin.math.abs(pitchDeg - preset) < 12f
                Surface(
                    onClick = { onPitchChange(preset) },
                    modifier = Modifier
                        .padding(vertical = 3.dp)
                        .size(width = 44.dp, height = 34.dp),
                    color = if (selected) accent.copy(alpha = 0.28f) else Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(
                        1.dp,
                        if (selected) accent else accent.copy(alpha = 0.2f)
                    )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${preset.toInt()}°",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) accent else TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Optic3DCarouselBar(
    activeOptic: OpticMode,
    deviceTilt: DeviceTilt,
    showOpticRow: Boolean,
    onSelectOptic: (OpticMode) -> Unit,
    onOpenLayers: () -> Unit,
    onOpenTheaters: () -> Unit,
    onOpenCctvWall: () -> Unit,
    onOpenNavigation: () -> Unit,
    onOpenWorldometers: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenDonations: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = activeOptic.palette.hudAccent
    val density = LocalDensity.current.density

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .graphicsLayer {
                rotationY = deviceTilt.x * 3.5f
                rotationX = deviceTilt.y * 2.5f
                cameraDistance = 14f * density
            },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (showOpticRow) {
            // 3D Fan Carousel of Optical Modes
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OpticMode.entries.forEachIndexed { idx, mode ->
                    val isSelected = mode == activeOptic
                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.05f else 0.94f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "optic_scale"
                    )
                    val rotY by animateFloatAsState(
                        targetValue = if (isSelected) 0f else (idx - activeOptic.ordinal) * 9f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "optic_rot"
                    )
                    val modeAccent = mode.palette.hudAccent

                    Surface(
                        onClick = { onSelectOptic(mode) },
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("optic_mode_${mode.name.lowercase()}")
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                rotationY = rotY
                                cameraDistance = 12f * density
                            },
                        color = if (isSelected) modeAccent.copy(alpha = 0.22f) else ObsidianPanel,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) modeAccent else modeAccent.copy(alpha = 0.25f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = modeAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = mode.displayName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) TextPrimary else TextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = mode.code,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = modeAccent
                                )
                            }
                        }
                    }
                }
            }
        }

        // Primary Command Bar Row 1: Layers Deck + Strategic Theaters + Live CCTV Wall + Nav/GPS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = onOpenLayers,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("open_layers_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ObsidianPanelElevated,
                    contentColor = TextPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
                contentPadding = PaddingValues(horizontal = 6.dp)
            ) {
                Icon(Icons.Default.Layers, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("LAYERS", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            Button(
                onClick = onOpenTheaters,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("open_theaters_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent.copy(alpha = 0.18f),
                    contentColor = accent
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, accent),
                contentPadding = PaddingValues(horizontal = 6.dp)
            ) {
                Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("THEATERS", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            Button(
                onClick = onOpenCctvWall,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("open_cctv_wall_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ObsidianPanelElevated,
                    contentColor = CallidusGold
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CallidusGold.copy(alpha = 0.65f)),
                contentPadding = PaddingValues(horizontal = 6.dp)
            ) {
                Icon(Icons.Default.Videocam, contentDescription = null, tint = CallidusGold, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("CCTV", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            Button(
                onClick = onOpenNavigation,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("open_nav_dock_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CallidusEmerald.copy(alpha = 0.16f),
                    contentColor = CallidusEmerald
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CallidusEmerald.copy(alpha = 0.7f)),
                contentPadding = PaddingValues(horizontal = 6.dp)
            ) {
                Icon(Icons.Default.Explore, contentDescription = null, tint = CallidusEmerald, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("NAV/GPS", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        // Primary Command Bar Row 2: Worldometers + Gemini AI Analyst + Donations (Paybill 522522)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onOpenWorldometers,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("open_worldometers_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ObsidianPanelElevated,
                    contentColor = CallidusEmerald
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CallidusEmerald.copy(alpha = 0.7f)),
                contentPadding = PaddingValues(horizontal = 6.dp)
            ) {
                Icon(Icons.Default.Public, contentDescription = null, tint = CallidusEmerald, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("WORLD DATA", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            Button(
                onClick = onOpenChat,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("open_chat_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ObsidianPanelElevated,
                    contentColor = accent
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.7f)),
                contentPadding = PaddingValues(horizontal = 6.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("GEMINI AI", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            Button(
                onClick = onOpenDonations,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("open_donations_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CallidusGold.copy(alpha = 0.18f),
                    contentColor = CallidusGold
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CallidusGold),
                contentPadding = PaddingValues(horizontal = 6.dp)
            ) {
                Icon(Icons.Default.VolunteerActivism, contentDescription = null, tint = CallidusGold, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("DONATE 522522", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun Target3DDossierCard(
    selected: SelectedTarget,
    opticMode: OpticMode,
    isPinned: Boolean,
    accent: Color,
    deviceTilt: DeviceTilt,
    fetchCctvSnapshot: suspend (String) -> ByteArray?,
    onCalibrateCctv: (cameraId: String, azimuthDeg: Float, fovDeg: Float, rangeKm: Float) -> Unit,
    onTogglePin: (String) -> Unit,
    onEngageChaseCam: () -> Unit,
    onNavigateTarget: (Double, Double, String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val visible = selected !is SelectedTarget.None
    val density = LocalDensity.current.density

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(
            initialOffsetY = { it / 2 },
            animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)
        ) + scaleIn(initialScale = 0.88f) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it / 2 }) + scaleOut(targetScale = 0.9f) + fadeOut()
    ) {
        val targetId = selected.targetId.orEmpty()
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .navigationBarsPadding()
                .padding(horizontal = 12.dp)
                .graphicsLayer {
                    rotationX = 4f - deviceTilt.y * 4f
                    rotationY = deviceTilt.x * 5f
                    cameraDistance = 12f * density
                },
            color = ObsidianPanelElevated,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.5.dp, accent.copy(alpha = 0.7f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val icon = when (selected) {
                            is SelectedTarget.Flight -> Icons.Default.Flight
                            is SelectedTarget.Satellite -> Icons.Default.SatelliteAlt
                            is SelectedTarget.Cctv -> Icons.Default.Videocam
                            is SelectedTarget.Vessel -> Icons.Default.DirectionsBoat
                            is SelectedTarget.Quake -> Icons.Default.Sensors
                            is SelectedTarget.Event -> Icons.Default.LocalFireDepartment
                            SelectedTarget.None -> Icons.Default.Explore
                        }
                        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            val title = when (selected) {
                                is SelectedTarget.Flight -> selected.data.callsign
                                is SelectedTarget.Satellite -> selected.data.element.name
                                is SelectedTarget.Cctv -> selected.data.name
                                is SelectedTarget.Vessel -> selected.data.name
                                is SelectedTarget.Quake -> "M${String.format("%.1f", selected.data.magnitude)} EARTHQUAKE"
                                is SelectedTarget.Event -> selected.data.category.uppercase()
                                SelectedTarget.None -> ""
                            }
                            val sub = when (selected) {
                                is SelectedTarget.Flight -> "ICAO ${selected.data.icao24} • ${selected.data.aircraftType.ifEmpty { "ADS-B" }}"
                                is SelectedTarget.Satellite -> "NORAD ${selected.data.element.noradId} • ${selected.data.element.category.label}"
                                is SelectedTarget.Cctv -> "${selected.data.city} • ${selected.data.status}"
                                is SelectedTarget.Vessel -> "MMSI ${selected.data.mmsi} • ${selected.data.shipType}"
                                is SelectedTarget.Quake -> selected.data.place
                                is SelectedTarget.Event -> selected.data.title
                                SelectedTarget.None -> ""
                            }
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = sub,
                                style = MaterialTheme.typography.labelSmall,
                                color = accent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row {
                        IconButton(
                            onClick = { if (targetId.isNotEmpty()) onTogglePin(targetId) },
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("pin_target_button")
                        ) {
                            Icon(
                                imageVector = if (isPinned) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Pin Target",
                                tint = if (isPinned) CallidusGold else TextSecondary
                            )
                        }
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("close_dossier_button")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close Dossier", tint = TextSecondary)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Telemetry Metrics Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (selected) {
                        is SelectedTarget.Flight -> {
                            MetricBox("ALTITUDE", "${selected.data.altitudeFeet} ft", accent, Modifier.weight(1f))
                            MetricBox("SPEED", "${selected.data.groundSpeedKnots} kts", accent, Modifier.weight(1f))
                            MetricBox("HEADING", "${selected.data.headingDeg.toInt()}°", accent, Modifier.weight(1f))
                        }
                        is SelectedTarget.Satellite -> {
                            MetricBox("ORBIT ALT", "${selected.data.altitudeKm.toInt()} km", accent, Modifier.weight(1f))
                            MetricBox("VELOCITY", "${String.format("%.2f", selected.data.velocityKmS)} km/s", accent, Modifier.weight(1f))
                            MetricBox("INCLIN", "${String.format("%.1f", selected.data.element.inclinationDeg)}°", accent, Modifier.weight(1f))
                        }
                        is SelectedTarget.Cctv -> {
                            MetricBox("AZIMUTH", "${selected.data.azimuthDeg.toInt()}°", accent, Modifier.weight(1f))
                            MetricBox("FOV", "${selected.data.fovDeg.toInt()}°", accent, Modifier.weight(1f))
                            MetricBox("RANGE", "${String.format("%.1f", selected.data.rangeKm)} km", CallidusGold, Modifier.weight(1f))
                        }
                        is SelectedTarget.Vessel -> {
                            MetricBox("SPEED", "${String.format("%.1f", selected.data.speedKnots)} kts", accent, Modifier.weight(1f))
                            MetricBox("HEADING", "${selected.data.headingDeg.toInt()}°", accent, Modifier.weight(1f))
                            MetricBox("MMSI", "${selected.data.mmsi}", CallidusGold, Modifier.weight(1f))
                        }
                        is SelectedTarget.Quake -> {
                            MetricBox("MAGNITUDE", "M ${String.format("%.1f", selected.data.magnitude)}", CallidusCrimson, Modifier.weight(1f))
                            MetricBox("DEPTH", "${String.format("%.1f", selected.data.depthKm)} km", accent, Modifier.weight(1f))
                            MetricBox("TSUNAMI", if (selected.data.tsunamiAlert) "ALERT" else "NONE", CallidusGold, Modifier.weight(1f))
                        }
                        is SelectedTarget.Event -> {
                            MetricBox("LAT", String.format("%.2f°", selected.data.lat), accent, Modifier.weight(1f))
                            MetricBox("LON", String.format("%.2f°", selected.data.lon), accent, Modifier.weight(1f))
                            MetricBox("SOURCE", selected.data.sourceLabel, CallidusGold, Modifier.weight(1f))
                        }
                        SelectedTarget.None -> Unit
                    }
                }

                // Live CCTV Snapshot Preview + Interactive 3D Viewshed Calibration Sliders
                if (selected is SelectedTarget.Cctv) {
                    val cam = selected.data
                    Spacer(Modifier.height(10.dp))
                    LiveCctvVideoOrSnapshotBox(
                        cam = cam,
                        opticMode = opticMode,
                        accent = accent,
                        fetchSnapshotBytes = fetchCctvSnapshot,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(142.dp)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("3D VIEWSHED CALIBRATION (LIVE ON GLOBE)", style = MaterialTheme.typography.labelSmall, color = CallidusGold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("AZ ${cam.azimuthDeg.toInt()}°", style = MaterialTheme.typography.labelSmall, color = TextSecondary, modifier = Modifier.width(56.dp))
                        Slider(
                            value = cam.azimuthDeg,
                            onValueChange = { onCalibrateCctv(cam.id, it, cam.fovDeg, cam.rangeKm) },
                            valueRange = 0f..359f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("FOV ${cam.fovDeg.toInt()}°", style = MaterialTheme.typography.labelSmall, color = TextSecondary, modifier = Modifier.width(56.dp))
                        Slider(
                            value = cam.fovDeg,
                            onValueChange = { onCalibrateCctv(cam.id, cam.azimuthDeg, it, cam.rangeKm) },
                            valueRange = 20f..120f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(thumbColor = CallidusGold, activeTrackColor = CallidusGold)
                        )
                    }
                }

                val targetCoords: Triple<Double, Double, String>? = when (selected) {
                    is SelectedTarget.Flight -> Triple(selected.data.lat, selected.data.lon, selected.data.callsign)
                    is SelectedTarget.Satellite -> Triple(selected.data.lat, selected.data.lon, selected.data.element.name)
                    is SelectedTarget.Cctv -> Triple(selected.data.lat, selected.data.lon, selected.data.name)
                    is SelectedTarget.Vessel -> Triple(selected.data.lat, selected.data.lon, selected.data.name)
                    is SelectedTarget.Quake -> Triple(selected.data.lat, selected.data.lon, selected.data.place)
                    is SelectedTarget.Event -> Triple(selected.data.lat, selected.data.lon, selected.data.title)
                    SelectedTarget.None -> null
                }

                if (selected is SelectedTarget.Flight || selected is SelectedTarget.Satellite ||
                    selected is SelectedTarget.Cctv || selected is SelectedTarget.Vessel ||
                    targetCoords != null
                ) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (selected is SelectedTarget.Flight || selected is SelectedTarget.Satellite ||
                            selected is SelectedTarget.Cctv || selected is SelectedTarget.Vessel
                        ) {
                            Button(
                                onClick = onEngageChaseCam,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("engage_chase_cam_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = accent,
                                    contentColor = Color(0xFF04070D)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.ViewInAr, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(5.dp))
                                val btnText = when (selected) {
                                    is SelectedTarget.Satellite -> "ORBITAL CHASE"
                                    is SelectedTarget.Cctv -> "3D VIEWSHED"
                                    is SelectedTarget.Vessel -> "3D BOW CAM"
                                    else -> "3D COCKPIT"
                                }
                                Text(btnText, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }

                        if (targetCoords != null) {
                            OutlinedButton(
                                onClick = {
                                    onNavigateTarget(targetCoords.first, targetCoords.second, targetCoords.third)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("navigate_target_button"),
                                border = BorderStroke(1.dp, CallidusEmerald),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Directions, contentDescription = "Navigate to Target", tint = CallidusEmerald, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("NAVIGATE", color = CallidusEmerald, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (selected is SelectedTarget.Cctv && !selected.data.videoUrl.isNullOrBlank()) {
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(selected.data.videoUrl))
                                    runCatching { context.startActivity(intent) }
                                },
                                modifier = Modifier.height(48.dp),
                                border = BorderStroke(1.dp, CallidusGold),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.PlayCircle, contentDescription = "Open Stream", tint = CallidusGold, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(3.dp))
                                Text("STREAM", color = CallidusGold, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricBox(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color(0x66060D1A),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(0.75.dp, accent.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun BasemapQuickSwitchBar(
    activeStyle: GlobeImageryStyle,
    accent: Color,
    onSelectStyle: (GlobeImageryStyle) -> Unit,
    onOpenLayerManager: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .padding(horizontal = 12.dp)
            .widthIn(max = 520.dp),
        color = ObsidianPanel,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            GlobeImageryStyle.entries.forEach { style ->
                val selected = style == activeStyle
                val icon = when (style) {
                    GlobeImageryStyle.STANDARD -> Icons.Default.Map
                    GlobeImageryStyle.SATELLITE -> Icons.Default.SatelliteAlt
                    GlobeImageryStyle.TERRAIN -> Icons.Default.Terrain
                }
                Surface(
                    onClick = { onSelectStyle(style) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("quick_basemap_${style.id}"),
                    color = if (selected) accent.copy(alpha = 0.22f) else Color.Transparent,
                    shape = RoundedCornerShape(10.dp),
                    border = if (selected) BorderStroke(1.dp, accent) else null
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = style.label,
                            tint = if (selected) accent else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = style.shortTitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) accent else TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Surface(
                onClick = onOpenLayerManager,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("quick_open_layer_manager"),
                color = accent.copy(alpha = 0.14f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.45f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = "Open 3D Globe Layer Manager",
                        tint = accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ImageryStylePreviewCard(
    style: GlobeImageryStyle,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.02f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
        label = "imagery_card_scale"
    )
    val styleIcon = when (style) {
        GlobeImageryStyle.STANDARD -> Icons.Default.Map
        GlobeImageryStyle.SATELLITE -> Icons.Default.SatelliteAlt
        GlobeImageryStyle.TERRAIN -> Icons.Default.Terrain
    }

    Surface(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 104.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .testTag("imagery_style_${style.id}"),
        color = if (selected) accent.copy(alpha = 0.18f) else Color(0x66060D1A),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = if (selected) 1.6.dp else 0.8.dp,
            color = if (selected) accent else accent.copy(alpha = 0.28f)
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(9.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    when (style) {
                        GlobeImageryStyle.STANDARD -> {
                            drawRect(Color(0xFFE9E5D8))
                            drawRect(
                                color = Color(0xFFAAD3DF),
                                topLeft = Offset(0f, h * 0.58f),
                                size = androidx.compose.ui.geometry.Size(w * 0.48f, h * 0.42f)
                            )
                            drawLine(Color(0xFFF6CF65), Offset(0f, h * 0.35f), Offset(w, h * 0.28f), strokeWidth = 3.2f)
                            drawLine(Color(0xFFFFFFFF), Offset(w * 0.25f, 0f), Offset(w * 0.62f, h), strokeWidth = 2.4f)
                            drawLine(Color(0xFF9EB5A3), Offset(w * 0.56f, 0f), Offset(w, h * 0.55f), strokeWidth = 2f)
                        }
                        GlobeImageryStyle.SATELLITE -> {
                            drawRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFF071A2F), Color(0xFF0E3A47), Color(0xFF1F4B36))
                                )
                            )
                            drawCircle(
                                color = Color(0xFF2E6A4A),
                                radius = w * 0.36f,
                                center = Offset(w * 0.68f, h * 0.44f)
                            )
                            drawCircle(
                                color = Color(0x44FFFFFF),
                                radius = w * 0.28f,
                                center = Offset(w * 0.34f, h * 0.28f)
                            )
                        }
                        GlobeImageryStyle.TERRAIN -> {
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color(0xFF28483C), Color(0xFF6B7B4A), Color(0xFFA88E68))
                                )
                            )
                            drawCircle(
                                color = Color(0x88E7DFCF),
                                radius = w * 0.34f,
                                center = Offset(w * 0.52f, h * 0.55f),
                                style = Stroke(width = 1.6f)
                            )
                            drawCircle(
                                color = Color(0xAAEFE8DA),
                                radius = w * 0.21f,
                                center = Offset(w * 0.52f, h * 0.55f),
                                style = Stroke(width = 1.4f)
                            )
                            drawCircle(
                                color = Color(0xCCD5C8AE),
                                radius = w * 0.1f,
                                center = Offset(w * 0.52f, h * 0.55f),
                                style = Stroke(width = 1.2f)
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(if (selected) accent else Color(0x99050B16)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = styleIcon,
                        contentDescription = style.label,
                        tint = if (selected) Color(0xFF02060E) else TextPrimary,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }

            Text(
                text = style.shortTitle,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) accent else TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (selected) "ACTIVE" else "SELECT",
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) CallidusEmerald else TextMuted,
                maxLines = 1
            )
        }
    }
}

@Composable
fun LayerAndWatchlist3DDrawer(
    state: CallidusUiState,
    onToggleLayer: (IntelLayer) -> Unit,
    onSelectImageryStyle: (GlobeImageryStyle) -> Unit,
    onImageryOpacityChange: (Float) -> Unit,
    onToggleGraticule: () -> Unit,
    onToggleAtmosphere: () -> Unit,
    onSearchChange: (String) -> Unit,
    onSelectTarget: (SelectedTarget) -> Unit,
    onFlyToBookmark: (SavedBookmark) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = state.opticMode.palette.hudAccent
    val density = LocalDensity.current.density

    AnimatedVisibility(
        visible = state.isLayerDrawerOpen,
        modifier = modifier,
        enter = slideInHorizontally(
            initialOffsetX = { -it },
            animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)
        ) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
    ) {
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 360.dp)
                .fillMaxWidth(0.86f)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(12.dp)
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0f, 0.5f)
                    rotationY = 4f
                    cameraDistance = 14f * density
                },
            color = ObsidianPanelElevated,
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.2.dp, accent.copy(alpha = 0.55f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "3D GLOBE LAYER MANAGER",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = accent
                    )
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("close_layers_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close Deck", tint = TextSecondary)
                    }
                }

                // Live Target Search (Flights, Satellites, Quakes, Events)
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = onSearchChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("target_search_input"),
                    placeholder = { Text("Search callsign, ISS, quake...", style = MaterialTheme.typography.bodyMedium) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = accent) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent,
                        unfocusedBorderColor = accent.copy(alpha = 0.35f),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Search Results if typing
                    if (state.searchQuery.isNotBlank()) {
                        val q = state.searchQuery.trim()
                        val matchingFlights = state.aircraft.filter {
                            it.callsign.contains(q, true) || it.icao24.contains(q, true)
                        }.take(5)
                        val matchingSats = state.satellites.filter {
                            it.element.name.contains(q, true)
                        }.take(5)
                        val matchingCams = state.cctvCameras.filter {
                            it.name.contains(q, true) || it.city.contains(q, true)
                        }.take(5)
                        val matchingShips = state.maritimeVessels.filter {
                            it.name.contains(q, true) || it.mmsi.toString().contains(q)
                        }.take(4)
                        val matchingQuakes = state.earthquakes.filter {
                            it.place.contains(q, true)
                        }.take(4)

                        items(matchingCams, key = { "scam_${it.id}" }) { cam ->
                            SearchResultRow(
                                title = cam.name,
                                subtitle = "CCTV • ${cam.city} • Az ${cam.azimuthDeg.toInt()}°",
                                accent = state.opticMode.palette.cctvColor,
                                onClick = { onSelectTarget(SelectedTarget.Cctv(cam)) }
                            )
                        }
                        items(matchingFlights, key = { "sf_${it.icao24}" }) { f ->
                            SearchResultRow(
                                title = f.callsign,
                                subtitle = "FLIGHT • ${f.altitudeFeet}ft • ${f.groundSpeedKnots}kts",
                                accent = Color(0xFFFFC107),
                                onClick = { onSelectTarget(SelectedTarget.Flight(f)) }
                            )
                        }
                        items(matchingSats, key = { "ss_${it.element.noradId}" }) { s ->
                            SearchResultRow(
                                title = s.element.name,
                                subtitle = "ORBIT • ${s.altitudeKm.toInt()}km • NORAD ${s.element.noradId}",
                                accent = accent,
                                onClick = { onSelectTarget(SelectedTarget.Satellite(s)) }
                            )
                        }
                        items(matchingShips, key = { "sship_${it.mmsi}" }) { v ->
                            SearchResultRow(
                                title = v.name,
                                subtitle = "AIS SHIP • ${String.format("%.1f", v.speedKnots)} kts • Hdg ${v.headingDeg.toInt()}°",
                                accent = state.opticMode.palette.shipColor,
                                onClick = { onSelectTarget(SelectedTarget.Vessel(v)) }
                            )
                        }
                        items(matchingQuakes, key = { "sq_${it.id}" }) { eq ->
                            SearchResultRow(
                                title = "M${String.format("%.1f", eq.magnitude)} — ${eq.place}",
                                subtitle = "SEISMIC • Depth ${eq.depthKm.toInt()}km",
                                accent = CallidusCrimson,
                                onClick = { onSelectTarget(SelectedTarget.Quake(eq)) }
                            )
                        }
                    } else {
                        // 3D Globe Basemap Imagery Selector
                        item {
                            Text(
                                text = "BASEMAP IMAGERY",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    GlobeImageryStyle.entries.forEach { style ->
                                        ImageryStylePreviewCard(
                                            style = style,
                                            selected = style == state.imageryStyle,
                                            accent = accent,
                                            onClick = { onSelectImageryStyle(style) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                Surface(
                                    color = Color(0x55060D1A),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(0.75.dp, accent.copy(alpha = 0.25f))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "PROVIDER • ${state.imageryStyle.shortTitle}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = accent
                                            )
                                            Text(
                                                text = state.imageryStyle.subtitle,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextMuted,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        HorizontalDivider(color = accent.copy(alpha = 0.16f))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "BASEMAP OPACITY",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSecondary
                                            )
                                            Text(
                                                text = "${(state.imageryOpacity * 100).toInt()}%",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = accent
                                            )
                                        }
                                        Slider(
                                            value = state.imageryOpacity,
                                            onValueChange = onImageryOpacityChange,
                                            valueRange = 0.15f..1.0f,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("imagery_opacity_slider"),
                                            colors = SliderDefaults.colors(
                                                thumbColor = accent,
                                                activeTrackColor = accent,
                                                inactiveTrackColor = accent.copy(alpha = 0.25f)
                                            )
                                        )

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 48.dp)
                                                .clickable { onToggleGraticule() },
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "3D Graticule Grid",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = TextPrimary
                                                )
                                                Text(
                                                    text = "Lat/Lon spherical meridians",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextMuted
                                                )
                                            }
                                            Switch(
                                                checked = state.showGraticuleGrid,
                                                onCheckedChange = { onToggleGraticule() },
                                                modifier = Modifier.testTag("toggle_graticule_switch"),
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = accent,
                                                    checkedTrackColor = accent.copy(alpha = 0.3f)
                                                )
                                            )
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 48.dp)
                                                .clickable { onToggleAtmosphere() },
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Atmospheric Glow",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = TextPrimary
                                                )
                                                Text(
                                                    text = "Rayleigh limb scattering",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextMuted
                                                )
                                            }
                                            Switch(
                                                checked = state.showAtmosphereGlow,
                                                onCheckedChange = { onToggleAtmosphere() },
                                                modifier = Modifier.testTag("toggle_atmosphere_switch"),
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = accent,
                                                    checkedTrackColor = accent.copy(alpha = 0.3f)
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Layer Switches with Live Feed Status
                        item {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "LIVE TELEMETRY FEEDS",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(IntelLayer.entries, key = { it.id }) { layer ->
                            val enabled = layer in state.enabledLayers
                            val status = state.feedStatuses[layer] ?: FeedStatus.Idle
                            val statusLabel = when (status) {
                                FeedStatus.Idle -> "STANDBY"
                                FeedStatus.Syncing -> "SYNCING..."
                                is FeedStatus.Live -> "LIVE (${status.count})"
                                is FeedStatus.Error -> "OFFLINE"
                            }
                            val statusColor = when (status) {
                                is FeedStatus.Live -> CallidusEmerald
                                FeedStatus.Syncing -> CallidusGold
                                is FeedStatus.Error -> CallidusCrimson
                                FeedStatus.Idle -> TextMuted
                            }
                            Surface(
                                color = Color(0x55060D1A),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(0.75.dp, accent.copy(alpha = 0.25f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onToggleLayer(layer) }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(layer.title, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(layer.sourceLabel, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                            Spacer(Modifier.width(8.dp))
                                            Text(statusLabel, style = MaterialTheme.typography.labelSmall, color = statusColor)
                                        }
                                    }
                                    Switch(
                                        checked = enabled,
                                        onCheckedChange = { onToggleLayer(layer) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = accent,
                                            checkedTrackColor = accent.copy(alpha = 0.3f)
                                        )
                                    )
                                }
                            }
                        }

                        // Live Quick-Track Notable Targets (ISS + Flight + CCTV + Ship + Quake)
                        if (state.satellites.isNotEmpty() || state.aircraft.isNotEmpty() || state.cctvCameras.isNotEmpty()) {
                            item {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "PRIORITY LIVE TRACKS",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary
                                )
                            }
                            val iss = state.satellites.firstOrNull { it.element.name.contains("ISS", true) }
                                ?: state.satellites.firstOrNull()
                            if (iss != null) {
                                item(key = "prio_iss") {
                                    SearchResultRow(
                                        title = iss.element.name,
                                        subtitle = "ORBITAL LOCK • ${iss.altitudeKm.toInt()}km • ${String.format("%.2f", iss.velocityKmS)} km/s",
                                        accent = CallidusGold,
                                        onClick = { onSelectTarget(SelectedTarget.Satellite(iss)) }
                                    )
                                }
                            }
                            state.cctvCameras.firstOrNull()?.let { firstCam ->
                                item(key = "prio_cctv") {
                                    SearchResultRow(
                                        title = "CCTV ${firstCam.name}",
                                        subtitle = "${firstCam.city} • LIVE VIEWSHED CONE",
                                        accent = state.opticMode.palette.cctvColor,
                                        onClick = { onSelectTarget(SelectedTarget.Cctv(firstCam)) }
                                    )
                                }
                            }
                            state.aircraft.firstOrNull()?.let { firstFlight ->
                                item(key = "prio_flight") {
                                    SearchResultRow(
                                        title = "FLIGHT ${firstFlight.callsign}",
                                        subtitle = "${firstFlight.altitudeFeet}ft • ${firstFlight.groundSpeedKnots}kts",
                                        accent = Color(0xFFFFC107),
                                        onClick = { onSelectTarget(SelectedTarget.Flight(firstFlight)) }
                                    )
                                }
                            }
                            state.maritimeVessels.firstOrNull()?.let { firstShip ->
                                item(key = "prio_ship") {
                                    SearchResultRow(
                                        title = firstShip.name,
                                        subtitle = "AIS SHIP • ${String.format("%.1f", firstShip.speedKnots)} kts",
                                        accent = state.opticMode.palette.shipColor,
                                        onClick = { onSelectTarget(SelectedTarget.Vessel(firstShip)) }
                                    )
                                }
                            }
                            state.earthquakes.firstOrNull()?.let { topQuake ->
                                item(key = "prio_quake") {
                                    SearchResultRow(
                                        title = "M${String.format("%.1f", topQuake.magnitude)} ${topQuake.place}",
                                        subtitle = "STRONGEST 24H TREMOR • Depth ${topQuake.depthKm.toInt()}km",
                                        accent = CallidusCrimson,
                                        onClick = { onSelectTarget(SelectedTarget.Quake(topQuake)) }
                                    )
                                }
                            }
                        }

                        if (state.pinnedTargetIds.isNotEmpty()) {
                            val pinnedFlights = state.aircraft.filter { "flight_${it.icao24}" in state.pinnedTargetIds }
                            val pinnedSats = state.satellites.filter { "sat_${it.element.noradId}" in state.pinnedTargetIds }
                            val pinnedCams = state.cctvCameras.filter { "cctv_${it.id}" in state.pinnedTargetIds }
                            val pinnedShips = state.maritimeVessels.filter { "ship_${it.mmsi}" in state.pinnedTargetIds }
                            if (pinnedFlights.isNotEmpty() || pinnedSats.isNotEmpty() || pinnedCams.isNotEmpty() || pinnedShips.isNotEmpty()) {
                                item {
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = "PINNED WATCHLIST",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = CallidusGold
                                    )
                                }
                                items(pinnedCams, key = { "pin_c_${it.id}" }) { cam ->
                                    SearchResultRow(
                                        title = cam.name,
                                        subtitle = "PINNED CCTV • ${cam.city}",
                                        accent = CallidusGold,
                                        onClick = { onSelectTarget(SelectedTarget.Cctv(cam)) }
                                    )
                                }
                                items(pinnedSats, key = { "pin_s_${it.element.noradId}" }) { sat ->
                                    SearchResultRow(
                                        title = sat.element.name,
                                        subtitle = "PINNED ORBIT • ${sat.altitudeKm.toInt()}km",
                                        accent = CallidusGold,
                                        onClick = { onSelectTarget(SelectedTarget.Satellite(sat)) }
                                    )
                                }
                                items(pinnedFlights, key = { "pin_f_${it.icao24}" }) { fl ->
                                    SearchResultRow(
                                        title = fl.callsign,
                                        subtitle = "PINNED FLIGHT • ${fl.altitudeFeet}ft",
                                        accent = CallidusGold,
                                        onClick = { onSelectTarget(SelectedTarget.Flight(fl)) }
                                    )
                                }
                                items(pinnedShips, key = { "pin_v_${it.mmsi}" }) { ship ->
                                    SearchResultRow(
                                        title = ship.name,
                                        subtitle = "PINNED SHIP • ${String.format("%.1f", ship.speedKnots)} kts",
                                        accent = CallidusGold,
                                        onClick = { onSelectTarget(SelectedTarget.Vessel(ship)) }
                                    )
                                }
                            }
                        }

                        if (state.bookmarks.isNotEmpty()) {
                            item {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "CLOUD BOOKMARKS (${state.bookmarks.size})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = accent
                                )
                            }
                            items(state.bookmarks, key = { "cloud_bm_${it.id.ifEmpty { it.targetKey }}" }) { bm ->
                                SearchResultRow(
                                    title = bm.title,
                                    subtitle = "${bm.targetType} • ${bm.subtitle}",
                                    accent = accent,
                                    onClick = { onFlyToBookmark(bm) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color(0x660A1526),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.75.dp, accent.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun StrategicTheaters3DSheet(
    visible: Boolean,
    activeTheaterId: String,
    accent: Color,
    onSelectTheater: (TheaterPreset) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current.density

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(dampingRatio = 0.76f, stiffness = Spring.StiffnessMediumLow)
        ) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .heightIn(max = 460.dp)
                .navigationBarsPadding()
                .padding(12.dp)
                .graphicsLayer {
                    rotationX = 3f
                    cameraDistance = 14f * density
                },
            color = ObsidianPanelElevated,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.3.dp, accent.copy(alpha = 0.55f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("STRATEGIC THEATERS", style = MaterialTheme.typography.headlineMedium, color = accent)
                        Text("Tap to initiate 3D parabolic orbital flight", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("close_theaters_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close Theaters", tint = TextSecondary)
                    }
                }
                Spacer(Modifier.height(10.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(StrategicTheaters, key = { it.id }) { theater ->
                        val isActive = theater.id == activeTheaterId
                        Surface(
                            onClick = { onSelectTheater(theater) },
                            modifier = Modifier.testTag("theater_card_${theater.id}"),
                            color = if (isActive) accent.copy(alpha = 0.16f) else Color(0x55060D1A),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                width = if (isActive) 1.4.dp else 0.8.dp,
                                color = if (isActive) accent else accent.copy(alpha = 0.25f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = accent.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = theater.regionCode,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = accent,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(theater.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                    Text(
                                        text = theater.briefing,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
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

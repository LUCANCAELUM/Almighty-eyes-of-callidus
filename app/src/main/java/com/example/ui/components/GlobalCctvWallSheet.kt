package com.example.ui.components

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.model.CctvCameraTarget
import com.example.domain.model.OpticMode
import com.example.engine.Globe3DEngine
import com.example.ui.theme.CallidusCrimson
import com.example.ui.theme.CallidusEmerald
import com.example.ui.theme.CallidusGold
import com.example.ui.theme.ObsidianPanelElevated
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Plays live MP4 (TfL JamCams) or HLS .m3u8 (Caltrans DOT) video directly in-app
 * with seamless automatic fallback to LiveCctvSnapshotBox if stream fails or is still buffering.
 */
@Composable
fun LiveCctvVideoOrSnapshotBox(
    cam: CctvCameraTarget,
    opticMode: OpticMode,
    accent: Color,
    fetchSnapshotBytes: suspend (String) -> ByteArray?,
    preferLiveVideo: Boolean = true,
    modifier: Modifier = Modifier
) {
    val hasVideo = !cam.videoUrl.isNullOrBlank() && preferLiveVideo
    var videoFailed by remember(cam.id, cam.videoUrl) { mutableStateOf(false) }

    if (hasVideo && !videoFailed) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF040811))
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    VideoView(ctx).apply {
                        setVideoURI(Uri.parse(cam.videoUrl))
                        setOnPreparedListener { mp ->
                            mp.isLooping = true
                            mp.setVolume(0f, 0f)
                            mp.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                            start()
                        }
                        setOnErrorListener { _, _, _ ->
                            videoFailed = true
                            true
                        }
                    }
                }
            )
            Surface(
                color = Color(0xCC04070D),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(0.9.dp, CallidusCrimson),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                Text(
                    text = "● ${cam.streamType}",
                    style = MaterialTheme.typography.labelSmall,
                    color = CallidusCrimson,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }
        }
    } else {
        LiveCctvSnapshotBox(
            imageUrl = cam.imageUrl,
            cameraName = cam.name,
            opticMode = opticMode,
            accent = accent,
            fetchSnapshotBytes = fetchSnapshotBytes,
            modifier = modifier
        )
    }
}

@Composable
fun LiveCctvWallSheet(
    visible: Boolean,
    cameras: List<CctvCameraTarget>,
    centerLat: Double,
    centerLon: Double,
    opticMode: OpticMode,
    accent: Color,
    fetchSnapshotBytes: suspend (String) -> ByteArray?,
    onRefreshFeeds: () -> Unit,
    onSelectCamera3D: (CctvCameraTarget) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var regionFilter by remember { mutableStateOf("ALL") }
    var filterQuery by remember { mutableStateOf("") }
    var gridColumns by remember { mutableIntStateOf(1) }
    var sortByNearest by remember { mutableStateOf(false) }
    var fullscreenCam by remember { mutableStateOf<CctvCameraTarget?>(null) }

    val videoCount = remember(cameras) { cameras.count { !it.videoUrl.isNullOrBlank() } }
    val americasCount = remember(cameras) { cameras.count { it.region == "AMERICAS" } }
    val europeCount = remember(cameras) { cameras.count { it.region == "EUROPE" } }
    val apacCount = remember(cameras) { cameras.count { it.region == "ASIA-PACIFIC" } }

    val filteredCameras = remember(cameras, regionFilter, filterQuery, sortByNearest, centerLat, centerLon) {
        val base = cameras.filter { cam ->
            val matchesRegion = when (regionFilter) {
                "VIDEO" -> !cam.videoUrl.isNullOrBlank()
                "EUROPE" -> cam.region == "EUROPE"
                "AMERICAS" -> cam.region == "AMERICAS"
                "ASIA-PACIFIC" -> cam.region == "ASIA-PACIFIC"
                "LONDON" -> cam.city.contains("London", true)
                "NYC" -> cam.city.contains("New York", true)
                "CALTRANS" -> cam.city.contains("Caltrans", true)
                "AUSTIN" -> cam.city.contains("Austin", true)
                "SINGAPORE" -> cam.city.contains("Singapore", true)
                "FINLAND" -> cam.city.contains("Finland", true)
                else -> true
            }
            val matchesQuery = filterQuery.isBlank() ||
                cam.name.contains(filterQuery.trim(), true) ||
                cam.city.contains(filterQuery.trim(), true) ||
                cam.region.contains(filterQuery.trim(), true)
            matchesRegion && matchesQuery
        }
        val sorted = if (sortByNearest) {
            base.sortedBy { Globe3DEngine.haversineKm(centerLat, centerLon, it.lat, it.lon) }
        } else {
            base
        }
        sorted.take(40)
    }

    fullscreenCam?.let { cam ->
        Dialog(
            onDismissRequest = { fullscreenCam = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize().background(ObsidianVoid),
                color = ObsidianVoid
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(cam.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                            Text("${cam.city} • ${cam.streamType} • ${opticMode.displayName}", style = MaterialTheme.typography.labelSmall, color = accent)
                        }
                        IconButton(onClick = { fullscreenCam = null }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close Fullscreen", tint = TextPrimary)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    LiveCctvVideoOrSnapshotBox(
                        cam = cam,
                        opticMode = opticMode,
                        accent = accent,
                        fetchSnapshotBytes = fetchSnapshotBytes,
                        preferLiveVideo = true,
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                fullscreenCam = null
                                onSelectCamera3D(cam)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = ObsidianVoid),
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) {
                            Icon(Icons.Default.ViewInAr, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("FLY TO 3D VIEWSHED", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                        if (!cam.videoUrl.isNullOrBlank()) {
                            OutlinedButton(
                                onClick = {
                                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(cam.videoUrl))) }
                                },
                                border = BorderStroke(1.dp, CallidusGold),
                                modifier = Modifier.height(46.dp)
                            ) {
                                Icon(Icons.Default.OpenInNew, contentDescription = null, tint = CallidusGold, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("PLAYER", color = CallidusGold, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)
        ) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.90f)
                .widthIn(max = 680.dp)
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
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Videocam, contentDescription = null, tint = accent)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("GLOBAL PUBLIC CCTV VIDEO WALL", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${cameras.size} Worldwide Feeds ($videoCount Video Streams) • ${opticMode.code}",
                                style = MaterialTheme.typography.labelSmall,
                                color = accent
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { gridColumns = if (gridColumns == 1) 2 else 1 },
                            modifier = Modifier.size(42.dp).testTag("cctv_grid_toggle")
                        ) {
                            Icon(
                                imageVector = if (gridColumns == 1) Icons.Default.GridView else Icons.Default.ViewAgenda,
                                contentDescription = "Toggle 1x/2x Grid",
                                tint = CallidusGold
                            )
                        }
                        IconButton(onClick = onRefreshFeeds, modifier = Modifier.size(42.dp)) {
                            Icon(Icons.Default.Refresh, contentDescription = "Sync Worldwide CCTVs", tint = accent)
                        }
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.size(46.dp).testTag("close_cctv_wall_button")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close CCTV Wall", tint = TextSecondary)
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                // Global Regions + Live Video Filter Strip
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "ALL" to "ALL WORLD (${cameras.size})",
                        "VIDEO" to "LIVE VIDEO ($videoCount)",
                        "AMERICAS" to "AMERICAS ($americasCount)",
                        "EUROPE" to "EUROPE ($europeCount)",
                        "ASIA-PACIFIC" to "ASIA-PAC ($apacCount)",
                        "LONDON" to "LONDON",
                        "NYC" to "NEW YORK",
                        "CALTRANS" to "CALIFORNIA",
                        "AUSTIN" to "AUSTIN TX",
                        "SINGAPORE" to "SINGAPORE",
                        "FINLAND" to "NORDICS"
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = regionFilter == key,
                            onClick = { regionFilter = key },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (key == "VIDEO") CallidusCrimson.copy(alpha = 0.25f) else accent.copy(alpha = 0.24f),
                                selectedLabelColor = if (key == "VIDEO") CallidusCrimson else accent
                            )
                        )
                    }
                    FilterChip(
                        selected = sortByNearest,
                        onClick = { sortByNearest = !sortByNearest },
                        label = { Text("NEAR CROSSHAIR", style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CallidusEmerald.copy(alpha = 0.24f),
                            selectedLabelColor = CallidusEmerald
                        )
                    )
                }

                OutlinedTextField(
                    value = filterQuery,
                    onValueChange = { filterQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .testTag("cctv_wall_search"),
                    placeholder = { Text("Search city, highway, bridge, airport, or country...", style = MaterialTheme.typography.bodyMedium) },
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

                if (filteredCameras.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Syncing worldwide public CCTV & live video feeds (London, NYC, California, Austin, Singapore, Finland)...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(gridColumns),
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        items(filteredCameras, key = { it.id }) { cam ->
                            Surface(
                                color = Color(0x66060D1A),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (!cam.videoUrl.isNullOrBlank()) CallidusCrimson.copy(alpha = 0.55f) else accent.copy(alpha = 0.35f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Play inline video in 1-col mode or show fast live refreshing stream in 2-col grid
                                    LiveCctvVideoOrSnapshotBox(
                                        cam = cam,
                                        opticMode = opticMode,
                                        accent = accent,
                                        fetchSnapshotBytes = fetchSnapshotBytes,
                                        preferLiveVideo = gridColumns == 1,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(if (gridColumns == 1) 172.dp else 118.dp)
                                    )

                                    Text(
                                        text = cam.name,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${cam.city} • ${cam.streamType}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (!cam.videoUrl.isNullOrBlank()) CallidusGold else accent,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Button(
                                            onClick = { onSelectCamera3D(cam) },
                                            modifier = Modifier.weight(1f).height(38.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = ObsidianVoid),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 6.dp)
                                        ) {
                                            Icon(Icons.Default.ViewInAr, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("3D LOC", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        }
                                        OutlinedButton(
                                            onClick = { fullscreenCam = cam },
                                            modifier = Modifier.height(38.dp),
                                            border = BorderStroke(1.dp, if (!cam.videoUrl.isNullOrBlank()) CallidusCrimson else CallidusGold),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (!cam.videoUrl.isNullOrBlank()) Icons.Default.PlayCircle else Icons.Default.Fullscreen,
                                                contentDescription = "Fullscreen Live Feed",
                                                tint = if (!cam.videoUrl.isNullOrBlank()) CallidusCrimson else CallidusGold,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            if (gridColumns == 1) {
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    text = if (!cam.videoUrl.isNullOrBlank()) "LIVE STREAM" else "EXPAND",
                                                    color = if (!cam.videoUrl.isNullOrBlank()) CallidusCrimson else CallidusGold,
                                                    style = MaterialTheme.typography.labelSmall
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
}

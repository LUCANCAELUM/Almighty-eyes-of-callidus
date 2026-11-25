package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Streetview
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.ViewInAr
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.domain.model.AmenityCategory
import com.example.domain.model.CallidusUiState
import com.example.domain.model.CctvCameraTarget
import com.example.domain.model.GlobalLandmarks
import com.example.domain.model.LandmarkNode
import com.example.domain.model.MapAmenity
import com.example.domain.model.OpticMode
import com.example.engine.Globe3DEngine
import com.example.engine.LiveGpsCompassState
import com.example.engine.OfflineSteeringCalculator
import com.example.ui.theme.CallidusCrimson
import com.example.ui.theme.CallidusEmerald
import com.example.ui.theme.CallidusGold
import com.example.ui.theme.ObsidianPanel
import com.example.ui.theme.ObsidianPanelElevated
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

private enum class GoogleMapLayerType(val code: String, val label: String) {
    SATELLITE("k", "SATELLITE"),
    HYBRID("h", "HYBRID"),
    TERRAIN("p", "TERRAIN"),
    ROADMAP("m", "ROAD")
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RealBuildingsSatelliteViewport(
    state: CallidusUiState,
    gpsCompassState: LiveGpsCompassState,
    accent: Color,
    fetchCctvSnapshot: suspend (String) -> ByteArray?,
    onFlyToLandmark: (LandmarkNode) -> Unit,
    onCenterGps: () -> Unit,
    onSelectAmenityCategory: (AmenityCategory) -> Unit,
    onScanAmenitiesHere: (Double, Double) -> Unit,
    onSelectAmenity: (MapAmenity?) -> Unit,
    onNavigateToCoordinates: (Double, Double, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var showLabels by remember { mutableStateOf(true) }
    var showAmenityDrawer by remember { mutableStateOf(false) }
    var activeMapCctv by remember { mutableStateOf<CctvCameraTarget?>(null) }

    val nearbyCams = remember(state.cctvCameras, state.camera.centerLat, state.camera.centerLon) {
        state.cctvCameras
            .sortedBy { Globe3DEngine.haversineKm(state.camera.centerLat, state.camera.centerLon, it.lat, it.lon) }
            .take(12)
    }

    LaunchedEffect(Unit) {
        if (state.mapAmenities.isEmpty()) {
            onScanAmenitiesHere(state.camera.centerLat, state.camera.centerLon)
        }
    }

    LaunchedEffect(state.camera.centerLat, state.camera.centerLon) {
        webViewRef?.evaluateJavascript(
            String.format(
                Locale.US,
                "window.flyToLocation3D && window.flyToLocation3D(%.5f, %.5f, 16.0, 58, -18);",
                state.camera.centerLat,
                state.camera.centerLon
            ),
            null
        )
    }

    LaunchedEffect(state.opticMode) {
        webViewRef?.evaluateJavascript(
            "window.setOpticMode && window.setOpticMode('${state.opticMode.name}');",
            null
        )
    }

    LaunchedEffect(state.activeRoute) {
        val route = state.activeRoute
        if (route != null && route.coordinates.size >= 2) {
            val coordsArr = JSONArray()
            route.coordinates.forEach { pt ->
                coordsArr.put(JSONArray().put(pt.lon).put(pt.lat))
            }
            val fc = JSONObject()
                .put("type", "FeatureCollection")
                .put(
                    "features",
                    JSONArray().put(
                        JSONObject()
                            .put("type", "Feature")
                            .put(
                                "geometry",
                                JSONObject().put("type", "LineString").put("coordinates", coordsArr)
                            )
                    )
                )
            val escaped = JSONObject.quote(fc.toString())
            webViewRef?.evaluateJavascript("window.setRouteGeoJson && window.setRouteGeoJson($escaped);", null)
        } else {
            val emptyFc = JSONObject.quote("""{"type":"FeatureCollection","features":[]}""")
            webViewRef?.evaluateJavascript("window.setRouteGeoJson && window.setRouteGeoJson($emptyFc);", null)
        }
    }

    LaunchedEffect(state.aircraft, state.cctvCameras, state.earthquakes) {
        val arr = JSONArray()
        state.aircraft.take(24).forEach { fl ->
            arr.put(
                JSONObject()
                    .put("type", "flight")
                    .put("lat", fl.lat)
                    .put("lon", fl.lon)
                    .put("label", "✈ ${fl.callsign} ${fl.altitudeFeet}ft")
                    .put("color", "#FFC107")
            )
        }
        state.cctvCameras.take(80).forEach { cam ->
            arr.put(
                JSONObject()
                    .put("type", "cctv")
                    .put("id", cam.id)
                    .put("lat", cam.lat)
                    .put("lon", cam.lon)
                    .put("label", "📷 ${cam.name.take(18)}")
                    .put("color", if (!cam.videoUrl.isNullOrBlank()) "#FF5252" else "#00E5FF")
            )
        }
        state.earthquakes.take(12).forEach { q ->
            arr.put(
                JSONObject()
                    .put("type", "quake")
                    .put("lat", q.lat)
                    .put("lon", q.lon)
                    .put("label", "M${String.format(Locale.US, "%.1f", q.magnitude)}")
                    .put("color", "#FF5252")
            )
        }
        val escaped = JSONObject.quote(arr.toString())
        webViewRef?.evaluateJavascript("window.updateLiveEntities && window.updateLiveEntities($escaped);", null)
    }

    LaunchedEffect(state.mapAmenities) {
        val arr = JSONArray()
        state.mapAmenities.forEach { am ->
            val hexColor = String.format("#%06X", (0xFFFFFF and am.category.colorHex.toInt()))
            arr.put(
                JSONObject()
                    .put("id", am.id)
                    .put("lat", am.lat)
                    .put("lon", am.lon)
                    .put("name", am.name)
                    .put("subType", am.subType)
                    .put("color", hexColor)
            )
        }
        val escaped = JSONObject.quote(arr.toString())
        webViewRef?.evaluateJavascript("window.updateAmenityMarkers && window.updateAmenityMarkers($escaped);", null)
    }

    Box(modifier = modifier.fillMaxSize().background(ObsidianVoid)) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(top = 148.dp, bottom = 122.dp),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = true
                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                    webChromeClient = WebChromeClient()
                    addJavascriptInterface(
                        object {
                            @JavascriptInterface
                            fun onAmenitySelected(id: String) {
                                post {
                                    val found = state.mapAmenities.firstOrNull { it.id == id }
                                    if (found != null) {
                                        activeMapCctv = null
                                        onSelectAmenity(found)
                                    }
                                }
                            }

                            @JavascriptInterface
                            fun onCctvSelected(camId: String) {
                                post {
                                    val found = state.cctvCameras.firstOrNull { it.id == camId }
                                    if (found != null) {
                                        onSelectAmenity(null)
                                        activeMapCctv = found
                                    }
                                }
                            }

                            @JavascriptInterface
                            fun onMapMoved(lat: Double, lon: Double, zoom: Double) {}
                        },
                        "CallidusMapBridge"
                    )
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            view?.evaluateJavascript(
                                String.format(
                                    Locale.US,
                                    "window.flyToLocation3D && window.flyToLocation3D(%.5f, %.5f, 16.0, 58, -18);" +
                                        "window.setOpticMode && window.setOpticMode('${state.opticMode.name}');",
                                    state.camera.centerLat,
                                    state.camera.centerLon
                                ),
                                null
                            )
                        }
                    }
                    loadUrl("file:///android_asset/callidus_3d_buildings.html")
                    webViewRef = this
                }
            }
        )

        // Top Control Strip: Landmarks + Zoom Presets + Amenity Filters + Map CCTV Live Selector
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 152.dp, start = 10.dp, end = 10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    "ORBIT Z4" to 4.2f,
                    "REGION Z9" to 9.2f,
                    "CITY Z13" to 13.2f,
                    "STREET Z16" to 16.2f,
                    "ROOFTOP Z19" to 18.8f
                ).forEach { (zLabel, zVal) ->
                    Surface(
                        onClick = {
                            webViewRef?.evaluateJavascript(
                                String.format(Locale.US, "window.setMapZoomExact && window.setMapZoomExact(%.1f);", zVal),
                                null
                            )
                        },
                        color = ObsidianPanel,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(0.9.dp, CallidusGold.copy(alpha = 0.55f))
                    ) {
                        Text(
                            text = zLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = CallidusGold,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }
                GlobalLandmarks.forEach { lm ->
                    Surface(
                        onClick = {
                            onFlyToLandmark(lm)
                            onScanAmenitiesHere(lm.lat, lm.lon)
                        },
                        color = ObsidianPanel,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(0.9.dp, accent.copy(alpha = 0.55f)),
                        modifier = Modifier.testTag("landmark_chip_${lm.id}")
                    ) {
                        Text(
                            text = "${lm.code} • ${lm.city}",
                            style = MaterialTheme.typography.labelSmall,
                            color = accent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // Amenities Filter + Map CCTV Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (nearbyCams.isNotEmpty()) {
                    Surface(
                        onClick = {
                            val next = if (activeMapCctv != null) null else nearbyCams.first()
                            activeMapCctv = next
                            if (next != null) {
                                webViewRef?.evaluateJavascript(
                                    String.format(
                                        Locale.US,
                                        "window.flyToLocation3D && window.flyToLocation3D(%.5f, %.5f, 17.0, 58, -18);",
                                        next.lat,
                                        next.lon
                                    ),
                                    null
                                )
                            }
                        },
                        color = if (activeMapCctv != null) CallidusCrimson.copy(alpha = 0.25f) else ObsidianPanel,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (activeMapCctv != null) CallidusCrimson else CallidusGold)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = "Toggle Live CCTV in Map", tint = CallidusGold, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "MAP CCTV (${nearbyCams.size})",
                                style = MaterialTheme.typography.labelSmall,
                                color = CallidusGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Surface(
                    onClick = { onScanAmenitiesHere(state.camera.centerLat, state.camera.centerLon) },
                    color = CallidusEmerald.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, CallidusEmerald),
                    modifier = Modifier.testTag("scan_amenities_3d_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (state.isLoadingAmenities) {
                            CircularProgressIndicator(modifier = Modifier.size(12.dp), color = CallidusEmerald, strokeWidth = 1.6.dp)
                        } else {
                            Icon(Icons.Default.Radar, contentDescription = "Scan Amenities Here", tint = CallidusEmerald, modifier = Modifier.size(14.dp))
                        }
                        Spacer(Modifier.width(4.dp))
                        Text("AMENITIES (${state.mapAmenities.size})", style = MaterialTheme.typography.labelSmall, color = CallidusEmerald, fontWeight = FontWeight.Bold)
                    }
                }

                Surface(
                    onClick = { showAmenityDrawer = !showAmenityDrawer },
                    color = ObsidianPanel,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(0.9.dp, accent.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = if (showAmenityDrawer) "HIDE LIST" else "LIST",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }

                AmenityCategory.entries.forEach { cat ->
                    val active = state.selectedAmenityCategory == cat
                    val cColor = Color(cat.colorHex)
                    Surface(
                        onClick = { onSelectAmenityCategory(cat) },
                        color = if (active) cColor.copy(alpha = 0.24f) else ObsidianPanel,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(if (active) 1.2.dp else 0.75.dp, cColor.copy(alpha = if (active) 0.9f else 0.4f))
                    ) {
                        Text(
                            text = cat.shortCode,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (active) cColor else TextSecondary,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        // Right-side 3D Map Controls
        Surface(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 10.dp),
            color = ObsidianPanel,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(
                    onClick = { webViewRef?.evaluateJavascript("window.adjustMapZoom && window.adjustMapZoom(1.2);", null) },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = accent)
                }
                IconButton(
                    onClick = { webViewRef?.evaluateJavascript("window.adjustMapZoom && window.adjustMapZoom(-1.2);", null) },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = accent)
                }
                IconButton(
                    onClick = { webViewRef?.evaluateJavascript("window.togglePitch3D && window.togglePitch3D();", null) },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Default.ViewInAr, contentDescription = "Toggle 2D/3D Pitch", tint = CallidusGold)
                }
                IconButton(
                    onClick = {
                        showLabels = !showLabels
                        webViewRef?.evaluateJavascript("window.toggleLabels && window.toggleLabels($showLabels);", null)
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Default.Layers, contentDescription = "Toggle Street Labels", tint = if (showLabels) accent else TextMuted)
                }
                IconButton(
                    onClick = {
                        onCenterGps()
                        if (gpsCompassState.hasGpsFix) {
                            onScanAmenitiesHere(gpsCompassState.lat, gpsCompassState.lon)
                        }
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Center GPS", tint = CallidusEmerald)
                }
            }
        }

        // Floating In-Map Live CCTV Picture-in-Picture Card (view CCTV directly inside Map!)
        val liveCam = activeMapCctv
        if (liveCam != null) {
            InMapCctvFeedCard(
                cam = liveCam,
                nearbyCams = nearbyCams,
                opticMode = state.opticMode,
                accent = accent,
                fetchCctvSnapshot = fetchCctvSnapshot,
                onSelectCam = { next ->
                    activeMapCctv = next
                    webViewRef?.evaluateJavascript(
                        String.format(
                            Locale.US,
                            "window.flyToLocation3D && window.flyToLocation3D(%.5f, %.5f, 17.0, 58, -18);",
                            next.lat,
                            next.lon
                        ),
                        null
                    )
                },
                onNavigateToCam = { onNavigateToCoordinates(liveCam.lat, liveCam.lon, liveCam.name) },
                onClose = { activeMapCctv = null },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 12.dp, end = 12.dp, bottom = 180.dp)
            )
        } else if (showAmenityDrawer && state.mapAmenities.isNotEmpty()) {
            AmenitiesOverlayPanel(
                amenities = state.mapAmenities,
                refLat = state.camera.centerLat,
                refLon = state.camera.centerLon,
                accent = accent,
                onFocusAmenity = { am ->
                    onSelectAmenity(am)
                    showAmenityDrawer = false
                    webViewRef?.evaluateJavascript(
                        String.format(
                            Locale.US,
                            "window.highlightAmenity && window.highlightAmenity(%.5f, %.5f, %s);",
                            am.lat,
                            am.lon,
                            JSONObject.quote(am.name)
                        ),
                        null
                    )
                },
                onNavigateAmenity = { am ->
                    showAmenityDrawer = false
                    onNavigateToCoordinates(am.lat, am.lon, am.name)
                },
                onClose = { showAmenityDrawer = false },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 12.dp, end = 12.dp, bottom = 180.dp)
            )
        } else if (state.selectedAmenity != null) {
            SelectedAmenityCard(
                amenity = state.selectedAmenity,
                refLat = state.camera.centerLat,
                refLon = state.camera.centerLon,
                onNavigate = {
                    onNavigateToCoordinates(
                        state.selectedAmenity.lat,
                        state.selectedAmenity.lon,
                        state.selectedAmenity.name
                    )
                },
                onDismiss = { onSelectAmenity(null) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 12.dp, end = 12.dp, bottom = 180.dp)
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 132.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    onNavigateToCoordinates(state.camera.centerLat, state.camera.centerLon, "3D Target")
                },
                colors = ButtonDefaults.buttonColors(containerColor = CallidusEmerald, contentColor = ObsidianVoid),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(42.dp).testTag("navigate_3d_buildings_btn")
            ) {
                Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(5.dp))
                Text("NAVIGATE HERE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GoogleMapsNativeViewport(
    state: CallidusUiState,
    gpsCompassState: LiveGpsCompassState,
    accent: Color,
    fetchCctvSnapshot: suspend (String) -> ByteArray?,
    onSearchPlaces: (String) -> Unit,
    onSelectCoordinates: (Double, Double, String) -> Unit,
    onSelectAmenityCategory: (AmenityCategory) -> Unit,
    onScanAmenitiesHere: (Double, Double) -> Unit,
    onSelectAmenity: (MapAmenity?) -> Unit,
    onNavigateHere: (Double, Double, String) -> Unit,
    onAskGeminiAboutLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchInput by remember { mutableStateOf("") }
    var layerType by remember { mutableStateOf(GoogleMapLayerType.SATELLITE) }
    var streetViewMode by remember { mutableStateOf(false) }
    var zoomLevel by remember { mutableIntStateOf(16) }
    var pinLat by remember(state.camera.centerLat) { mutableStateOf(state.camera.centerLat) }
    var pinLon by remember(state.camera.centerLon) { mutableStateOf(state.camera.centerLon) }
    var showAmenityList by remember { mutableStateOf(false) }
    var activeMapCctv by remember { mutableStateOf<CctvCameraTarget?>(null) }

    val nearbyCams = remember(state.cctvCameras, pinLat, pinLon) {
        state.cctvCameras
            .sortedBy { Globe3DEngine.haversineKm(pinLat, pinLon, it.lat, it.lon) }
            .take(12)
    }

    LaunchedEffect(Unit) {
        if (state.mapAmenities.isEmpty()) {
            onScanAmenitiesHere(pinLat, pinLon)
        }
    }

    val iframeHtml = remember(pinLat, pinLon, layerType, zoomLevel, streetViewMode) {
        val embedUrl = if (streetViewMode) {
            String.format(
                Locale.US,
                "https://maps.google.com/maps?q=&layer=c&cbll=%.5f,%.5f&cbp=11,0,0,0,0&output=svembed",
                pinLat,
                pinLon
            )
        } else {
            String.format(
                Locale.US,
                "https://maps.google.com/maps?q=%.5f,%.5f&z=%d&t=%s&output=embed",
                pinLat,
                pinLon,
                zoomLevel,
                layerType.code
            )
        }
        """
        <!DOCTYPE html>
        <html>
        <head>
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
          <style>
            html, body { margin: 0; padding: 0; width: 100%; height: 100%; overflow: hidden; background: #04070d; }
            iframe { width: 100%; height: 100%; border: 0; display: block; }
          </style>
        </head>
        <body>
          <iframe
            src="$embedUrl"
            allow="geolocation *; accelerometer *; gyroscope *; magnetometer *"
            allowfullscreen
            referrerpolicy="no-referrer-when-downgrade">
          </iframe>
        </body>
        </html>
        """.trimIndent()
    }

    Box(modifier = modifier.fillMaxSize().background(ObsidianVoid)) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(top = 148.dp, bottom = 122.dp),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.setGeolocationEnabled(true)
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    android.webkit.CookieManager.getInstance().setAcceptCookie(true)
                    android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    webChromeClient = WebChromeClient()
                    webViewClient = WebViewClient()
                    tag = iframeHtml
                    loadDataWithBaseURL("https://callidus.local/", iframeHtml, "text/html", "UTF-8", null)
                }
            },
            update = { webView ->
                if (webView.tag != iframeHtml) {
                    webView.tag = iframeHtml
                    webView.loadDataWithBaseURL("https://callidus.local/", iframeHtml, "text/html", "UTF-8", null)
                }
            }
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 152.dp, start = 10.dp, end = 10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Surface(
                color = ObsidianPanelElevated,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchInput,
                        onValueChange = { searchInput = it },
                        placeholder = { Text("Search address, landmark, or amenity...", style = MaterialTheme.typography.labelSmall) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("gmaps_search_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    IconButton(
                        onClick = { onSearchPlaces(searchInput) },
                        modifier = Modifier.size(42.dp).testTag("gmaps_search_btn")
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Search Maps", tint = accent)
                    }
                    IconButton(
                        onClick = {
                            if (gpsCompassState.hasGpsFix) {
                                pinLat = gpsCompassState.lat
                                pinLon = gpsCompassState.lon
                                onSelectCoordinates(pinLat, pinLon, "My GPS")
                                onScanAmenitiesHere(pinLat, pinLon)
                            }
                        },
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = "My GPS Location", tint = CallidusEmerald)
                    }
                }
            }

            if (state.geocodeResults.isNotEmpty() && searchInput.isNotBlank()) {
                Surface(
                    color = ObsidianPanelElevated,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.45f))
                ) {
                    Column(modifier = Modifier.padding(6.dp)) {
                        state.geocodeResults.take(3).forEach { place ->
                            Surface(
                                onClick = {
                                    pinLat = place.lat
                                    pinLon = place.lon
                                    searchInput = ""
                                    onSelectCoordinates(place.lat, place.lon, place.name)
                                    onScanAmenitiesHere(place.lat, place.lon)
                                },
                                color = Color.Transparent
                            ) {
                                Text(
                                    text = "📍 ${place.displayName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Style + Street View + Live CCTV on Map Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                GoogleMapLayerType.entries.forEach { t ->
                    FilterChip(
                        selected = !streetViewMode && layerType == t,
                        onClick = {
                            streetViewMode = false
                            layerType = t
                        },
                        label = { Text(t.label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accent.copy(alpha = 0.26f),
                            selectedLabelColor = accent
                        )
                    )
                }
                FilterChip(
                    selected = streetViewMode,
                    onClick = { streetViewMode = !streetViewMode },
                    leadingIcon = { Icon(Icons.Default.Streetview, contentDescription = null, modifier = Modifier.size(14.dp)) },
                    label = { Text("STREET 360°", style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CallidusGold.copy(alpha = 0.25f),
                        selectedLabelColor = CallidusGold
                    )
                )
                if (nearbyCams.isNotEmpty()) {
                    FilterChip(
                        selected = activeMapCctv != null,
                        onClick = {
                            val next = if (activeMapCctv != null) null else nearbyCams.first()
                            activeMapCctv = next
                            if (next != null) {
                                pinLat = next.lat
                                pinLon = next.lon
                                zoomLevel = 17
                            }
                        },
                        leadingIcon = { Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        label = { Text("LIVE CCTV (${nearbyCams.size})", style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CallidusCrimson.copy(alpha = 0.26f),
                            selectedLabelColor = CallidusCrimson
                        )
                    )
                }
            }

            // Amenities Filter Strip inside Native Google Maps section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = {
                        onScanAmenitiesHere(pinLat, pinLon)
                        showAmenityList = true
                    },
                    color = CallidusEmerald.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, CallidusEmerald),
                    modifier = Modifier.testTag("gmaps_amenities_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (state.isLoadingAmenities) {
                            CircularProgressIndicator(modifier = Modifier.size(12.dp), color = CallidusEmerald, strokeWidth = 1.6.dp)
                        } else {
                            Icon(Icons.Default.Place, contentDescription = null, tint = CallidusEmerald, modifier = Modifier.size(14.dp))
                        }
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "AMENITIES (${state.mapAmenities.size})",
                            style = MaterialTheme.typography.labelSmall,
                            color = CallidusEmerald,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                AmenityCategory.entries.forEach { cat ->
                    val active = state.selectedAmenityCategory == cat
                    val cColor = Color(cat.colorHex)
                    Surface(
                        onClick = {
                            onSelectAmenityCategory(cat)
                            showAmenityList = true
                        },
                        color = if (active) cColor.copy(alpha = 0.24f) else ObsidianPanel,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(if (active) 1.2.dp else 0.75.dp, cColor.copy(alpha = if (active) 0.9f else 0.45f))
                    ) {
                        Text(
                            text = cat.shortCode,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (active) cColor else TextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        // Right-hand Zoom Rail
        Surface(
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 10.dp),
            color = ObsidianPanel,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(
                    onClick = { zoomLevel = (zoomLevel + 2).coerceAtMost(21) },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = accent)
                }
                Text("Z$zoomLevel", style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                IconButton(
                    onClick = { zoomLevel = (zoomLevel - 2).coerceAtLeast(2) },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = accent)
                }
            }
        }

        val liveCam = activeMapCctv
        if (liveCam != null) {
            InMapCctvFeedCard(
                cam = liveCam,
                nearbyCams = nearbyCams,
                opticMode = state.opticMode,
                accent = accent,
                fetchCctvSnapshot = fetchCctvSnapshot,
                onSelectCam = { next ->
                    activeMapCctv = next
                    pinLat = next.lat
                    pinLon = next.lon
                    zoomLevel = 17
                },
                onNavigateToCam = { onNavigateHere(liveCam.lat, liveCam.lon, liveCam.name) },
                onClose = { activeMapCctv = null },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 12.dp, end = 12.dp, bottom = 180.dp)
            )
        } else {
            AnimatedVisibility(
                visible = showAmenityList,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 12.dp, end = 12.dp, bottom = 180.dp)
            ) {
                AmenitiesOverlayPanel(
                    amenities = state.mapAmenities,
                    refLat = pinLat,
                    refLon = pinLon,
                    accent = accent,
                    onFocusAmenity = { am ->
                        pinLat = am.lat
                        pinLon = am.lon
                        zoomLevel = 18
                        streetViewMode = false
                        onSelectAmenity(am)
                        showAmenityList = false
                    },
                    onNavigateAmenity = { am ->
                        showAmenityList = false
                        onNavigateHere(am.lat, am.lon, am.name)
                    },
                    onClose = { showAmenityList = false }
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 132.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onNavigateHere(pinLat, pinLon, "Maps Pin") },
                colors = ButtonDefaults.buttonColors(containerColor = CallidusEmerald, contentColor = ObsidianVoid),
                modifier = Modifier.height(42.dp)
            ) {
                Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("NAVIGATE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = {
                    val uri = Uri.parse(String.format(Locale.US, "geo:%.5f,%.5f?q=%.5f,%.5f", pinLat, pinLon, pinLat, pinLon))
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    runCatching { context.startActivity(intent) }
                },
                border = BorderStroke(1.dp, accent),
                modifier = Modifier.height(42.dp)
            ) {
                Icon(Icons.Default.OpenInNew, contentDescription = null, tint = accent, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text("MAPS APP", color = accent, style = MaterialTheme.typography.labelSmall)
            }

            Button(
                onClick = onAskGeminiAboutLocation,
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = ObsidianVoid),
                modifier = Modifier.height(42.dp)
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text("GEMINI", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun InMapCctvFeedCard(
    cam: CctvCameraTarget,
    nearbyCams: List<CctvCameraTarget>,
    opticMode: OpticMode,
    accent: Color,
    fetchCctvSnapshot: suspend (String) -> ByteArray?,
    onSelectCam: (CctvCameraTarget) -> Unit,
    onNavigateToCam: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val curIdx = nearbyCams.indexOfFirst { it.id == cam.id }
    Surface(
        modifier = modifier.fillMaxWidth().testTag("in_map_cctv_card"),
        color = ObsidianPanelElevated,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.3.dp, CallidusGold.copy(alpha = 0.8f))
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "LIVE MAP CCTV • ${cam.name}",
                        style = MaterialTheme.typography.labelMedium,
                        color = CallidusGold,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${cam.city} • ${cam.streamType}",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (nearbyCams.size > 1) {
                        OutlinedButton(
                            onClick = {
                                val nextIdx = if (curIdx < 0) 0 else (curIdx + 1) % nearbyCams.size
                                onSelectCam(nearbyCams[nextIdx])
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            border = BorderStroke(1.dp, accent),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("NEXT CAM", style = MaterialTheme.typography.labelSmall, color = accent)
                        }
                    }
                    OutlinedButton(
                        onClick = onNavigateToCam,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        border = BorderStroke(1.dp, CallidusEmerald),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("NAV", style = MaterialTheme.typography.labelSmall, color = CallidusEmerald)
                    }
                    IconButton(onClick = onClose, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close Map CCTV", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            }
            LiveCctvVideoOrSnapshotBox(
                cam = cam,
                opticMode = opticMode,
                accent = accent,
                fetchSnapshotBytes = fetchCctvSnapshot,
                preferLiveVideo = true,
                modifier = Modifier.fillMaxWidth().height(145.dp)
            )
        }
    }
}

@Composable
private fun AmenitiesOverlayPanel(
    amenities: List<MapAmenity>,
    refLat: Double,
    refLon: Double,
    accent: Color,
    onFocusAmenity: (MapAmenity) -> Unit,
    onNavigateAmenity: (MapAmenity) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 260.dp),
        color = ObsidianPanelElevated,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.2.dp, accent.copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NEARBY AMENITIES (${amenities.size})",
                    style = MaterialTheme.typography.labelMedium,
                    color = accent,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onClose, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close Amenities", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            }
            if (amenities.isEmpty()) {
                Text(
                    text = "No amenities found in 3.5km radius. Pan to a city/neighborhood and tap AMENITIES.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(8.dp)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(amenities, key = { it.id }) { am ->
                        val catColor = Color(am.category.colorHex)
                        val distMeters = Globe3DEngine.haversineKm(refLat, refLon, am.lat, am.lon) * 1000.0
                        Surface(
                            onClick = { onFocusAmenity(am) },
                            color = Color(0x66060D1A),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(0.8.dp, catColor.copy(alpha = 0.45f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = am.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${am.subType} • ${OfflineSteeringCalculator.formatDistance(distMeters)}" +
                                            if (am.address.isNotEmpty()) " • ${am.address}" else "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = catColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = { onNavigateAmenity(am) },
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(Icons.Default.Directions, contentDescription = "Navigate to ${am.name}", tint = CallidusEmerald, modifier = Modifier.size(18.dp))
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
private fun SelectedAmenityCard(
    amenity: MapAmenity,
    refLat: Double,
    refLon: Double,
    onNavigate: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val catColor = Color(amenity.category.colorHex)
    val distMeters = Globe3DEngine.haversineKm(refLat, refLon, amenity.lat, amenity.lon) * 1000.0
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = ObsidianPanelElevated,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.2.dp, catColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(amenity.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = "${amenity.subType} • ${OfflineSteeringCalculator.formatDistance(distMeters)}" +
                        if (amenity.openingHours.isNotEmpty()) " • ${amenity.openingHours}" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = catColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(
                    onClick = onNavigate,
                    colors = ButtonDefaults.buttonColors(containerColor = CallidusEmerald, contentColor = ObsidianVoid),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.Explore, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("GO", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Dismiss Amenity", tint = TextSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

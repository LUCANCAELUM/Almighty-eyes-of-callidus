package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.firebase.onGoogleSignInClicked
import com.example.domain.model.SelectedTarget
import com.example.domain.model.StrategicTheaters
import com.example.domain.model.TelemetryDashboardTab
import com.example.domain.model.ViewportMode
import com.example.engine.rememberDeviceTilt
import com.example.engine.rememberOfflineGpsCompassState
import com.example.ui.CallidusViewModel
import com.example.ui.components.Animated3DCallidusLogo
import com.example.ui.components.ArSkyHudViewport
import com.example.ui.components.BasemapQuickSwitchBar
import com.example.ui.components.CallidusTopTelemetryBar
import com.example.ui.components.ChaseCam3DViewport
import com.example.ui.components.DonationPaybillSheet
import com.example.ui.components.FloatingTelemetryDashboard
import com.example.ui.components.GeminiChatbotSheet
import com.example.ui.components.Globe3DCanvas
import com.example.ui.components.GoogleMapsNativeViewport
import com.example.ui.components.LayerAndWatchlist3DDrawer
import com.example.ui.components.LiveCctvWallSheet
import com.example.ui.components.NavigationBottomBanner
import com.example.ui.components.NavigationPlannerSheet
import com.example.ui.components.Optic3DCarouselBar
import com.example.ui.components.Pitch3DGimbalControl
import com.example.ui.components.RealBuildingsSatelliteViewport
import com.example.ui.components.SeismicMonitorSheet
import com.example.ui.components.StrategicTheaters3DSheet
import com.example.ui.components.Target3DDossierCard
import com.example.ui.components.ThreeJsTacticalViewport
import com.example.ui.components.WorldometersLiveBridge
import com.example.ui.components.WorldometersLiveSheet
import com.example.ui.theme.CallidusEmerald
import com.example.ui.theme.CallidusGold
import com.example.ui.theme.ObsidianPanel
import com.example.ui.theme.ObsidianPanelElevated
import com.example.ui.theme.TextPrimary
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun CallidusCommandScreen(
    viewModel: CallidusViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val deviceTilt by rememberDeviceTilt()
    val density = LocalDensity.current.density
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasLocationPermission = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val gpsCompassState by rememberOfflineGpsCompassState(
        locationPermissionGranted = hasLocationPermission,
        onFixUpdated = { lat, lon -> viewModel.updateObserverLocation(lat, lon) }
    )

    val speechRecognizer = remember(context) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else null
    }

    val fallbackVoiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.setVoiceListening(false)
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val best = matches?.firstOrNull()
            if (!best.isNullOrBlank()) {
                viewModel.handleVoiceCommand(best)
            }
        }
    }

    DisposableEffect(speechRecognizer) {
        if (speechRecognizer != null) {
            speechRecognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(error: Int) {
                    viewModel.setVoiceListening(false)
                }
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val best = matches?.firstOrNull()
                    if (!best.isNullOrBlank()) {
                        viewModel.handleVoiceCommand(best)
                    } else {
                        viewModel.setVoiceListening(false)
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
        onDispose {
            speechRecognizer?.destroy()
        }
    }

    fun launchVoiceCapture() {
        val voiceIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak a theater, optic mode, CCTV wall, or question...")
        }
        if (speechRecognizer != null) {
            viewModel.setVoiceListening(true)
            runCatching { speechRecognizer.startListening(voiceIntent) }
                .onFailure {
                    viewModel.setVoiceListening(false)
                    runCatching { fallbackVoiceLauncher.launch(voiceIntent) }
                }
        } else {
            runCatching { fallbackVoiceLauncher.launch(voiceIntent) }
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchVoiceCapture()
        } else {
            viewModel.setVoiceListening(false)
        }
    }

    fun onVoiceButtonClicked() {
        if (state.isVoiceListening) {
            speechRecognizer?.stopListening()
            viewModel.setVoiceListening(false)
            return
        }
        val hasPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPerm) {
            launchVoiceCapture()
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(state.actionBannerMessage) {
        if (state.actionBannerMessage != null) {
            delay(3600L)
            viewModel.clearActionBanner()
        }
    }

    // Handle system Back cleanly for sheets, 3D sub-modes, drawers, viewports & selection
    BackHandler(
        enabled = state.isCleanHudMode ||
            state.isNavigationSheetOpen ||
            state.isChatOpen ||
            state.isDonationsOpen ||
            state.isWorldometersOpen ||
            state.isSeismicSheetOpen ||
            state.isCctvWallOpen ||
            state.camera.chaseMode ||
            state.isLayerDrawerOpen ||
            state.isTheatersSheetOpen ||
            state.selectedTarget !is SelectedTarget.None ||
            state.viewportMode != ViewportMode.GLOBE_3D
    ) {
        when {
            state.isCleanHudMode -> viewModel.toggleCleanHudMode()
            state.isNavigationSheetOpen -> viewModel.toggleNavigationSheet(false)
            state.isChatOpen -> viewModel.toggleChatSheet(false)
            state.isDonationsOpen -> viewModel.toggleDonationsSheet(false)
            state.isWorldometersOpen -> viewModel.toggleWorldometersSheet(false)
            state.isSeismicSheetOpen -> viewModel.toggleSeismicSheet(false)
            state.isCctvWallOpen -> viewModel.toggleCctvWall(false)
            state.camera.chaseMode -> viewModel.exitChaseCam()
            state.isLayerDrawerOpen -> viewModel.toggleLayerDrawer(false)
            state.isTheatersSheetOpen -> viewModel.toggleTheatersSheet(false)
            state.selectedTarget !is SelectedTarget.None -> viewModel.clearSelection()
            state.viewportMode != ViewportMode.GLOBE_3D -> viewModel.setViewportMode(ViewportMode.GLOBE_3D)
        }
    }

    // Spatial 3D push-back when Layer Drawer or Sheets open
    val anyOverlayOpen = state.isLayerDrawerOpen ||
        state.isTheatersSheetOpen ||
        state.isCctvWallOpen ||
        state.isWorldometersOpen ||
        state.isSeismicSheetOpen ||
        state.isChatOpen ||
        state.isDonationsOpen ||
        state.isNavigationSheetOpen
    val globeDepthScale by animateFloatAsState(
        targetValue = when {
            state.isLayerDrawerOpen -> 0.88f
            anyOverlayOpen -> 0.92f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
        label = "globe_depth_scale"
    )
    val globeDepthRotY by animateFloatAsState(
        targetValue = if (state.isLayerDrawerOpen) -10f else 0f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
        label = "globe_depth_rot"
    )
    val globeShiftX by animateFloatAsState(
        targetValue = if (state.isLayerDrawerOpen) 110f else 0f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
        label = "globe_shift_x"
    )

    val accent = state.opticMode.palette.hudAccent
    val activeTheaterName = remember(state.activeTheaterId) {
        StrategicTheaters.firstOrNull { it.id == state.activeTheaterId }?.name ?: "Global Orbital View"
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Persistent live real-time DOM telemetry bridge for https://www.worldometers.info/
        WorldometersLiveBridge(
            onStatsUpdated = viewModel::updateWorldometersData,
            modifier = Modifier.align(Alignment.TopStart)
        )

        AnimatedContent(
            targetState = state.camera.chaseMode,
            transitionSpec = {
                (scaleIn(initialScale = 0.86f) + fadeIn()) togetherWith
                    (scaleOut(targetScale = 1.12f) + fadeOut())
            },
            label = "orbit_chase_3d_transition"
        ) { inChaseMode ->
            if (inChaseMode) {
                ChaseCam3DViewport(
                    state = state,
                    deviceTilt = deviceTilt,
                    onFreeLookDrag = { dx, dy -> viewModel.onGlobeDrag(dx, dy, 1000f) },
                    onExitChase = { viewModel.exitChaseCam() }
                )
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Main Viewport Switcher: 3D Globe | 3D Buildings | AR Sky HUD | Google Maps | Three.js 3D
                    AnimatedContent(
                        targetState = state.viewportMode,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "viewport_mode_transition"
                    ) { mode ->
                        when (mode) {
                            ViewportMode.GLOBE_3D -> {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Globe3DCanvas(
                                        state = state,
                                        onDrag = viewModel::onGlobeDrag,
                                        onPinchTilt = viewModel::onGlobePinchAndTilt,
                                        onTargetSelected = viewModel::selectTarget,
                                        gpsCompassState = gpsCompassState,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer {
                                                scaleX = globeDepthScale
                                                scaleY = globeDepthScale
                                                rotationY = globeDepthRotY
                                                translationX = globeShiftX
                                                cameraDistance = 16f * density
                                            }
                                    )
                                    CrosshairReticleOverlay(accent = accent)
                                }
                            }
                            ViewportMode.SAT_3D_BUILDINGS -> {
                                RealBuildingsSatelliteViewport(
                                    state = state,
                                    gpsCompassState = gpsCompassState,
                                    accent = accent,
                                    fetchCctvSnapshot = viewModel::fetchCctvSnapshot,
                                    onFlyToLandmark = viewModel::flyToLandmark,
                                    onCenterGps = {
                                        if (gpsCompassState.hasGpsFix) {
                                            viewModel.centerGlobeOnGps(gpsCompassState.lat, gpsCompassState.lon)
                                            viewModel.scanAmenitiesAt(gpsCompassState.lat, gpsCompassState.lon)
                                        }
                                    },
                                    onSelectAmenityCategory = viewModel::setAmenityCategory,
                                    onScanAmenitiesHere = viewModel::scanAmenitiesAt,
                                    onSelectAmenity = viewModel::selectAmenity,
                                    onNavigateToCoordinates = { lat, lon, label ->
                                        viewModel.navigateToDestination(lat, lon, label)
                                    }
                                )
                            }
                            ViewportMode.AR_SKY_HUD -> {
                                ArSkyHudViewport(
                                    state = state,
                                    gpsCompassState = gpsCompassState,
                                    accent = accent,
                                    onSelectFilter = viewModel::setArTargetFilter,
                                    onSelectTarget = viewModel::selectTarget
                                )
                            }
                            ViewportMode.THREEJS_HANGAR -> {
                                ThreeJsTacticalViewport(
                                    targetLat = state.camera.centerLat,
                                    targetLon = state.camera.centerLon,
                                    theaterName = activeTheaterName,
                                    opticMode = state.opticMode,
                                    accent = accent
                                )
                            }
                            ViewportMode.GOOGLE_MAPS -> {
                                GoogleMapsNativeViewport(
                                    state = state,
                                    gpsCompassState = gpsCompassState,
                                    accent = accent,
                                    fetchCctvSnapshot = viewModel::fetchCctvSnapshot,
                                    onSearchPlaces = viewModel::searchNavigationPlaces,
                                    onSelectCoordinates = { lat, lon, _ ->
                                        viewModel.centerGlobeOnGps(lat, lon)
                                    },
                                    onSelectAmenityCategory = viewModel::setAmenityCategory,
                                    onScanAmenitiesHere = viewModel::scanAmenitiesAt,
                                    onSelectAmenity = viewModel::selectAmenity,
                                    onNavigateHere = { lat, lon, label ->
                                        viewModel.navigateToDestination(lat, lon, label)
                                    },
                                    onAskGeminiAboutLocation = {
                                        viewModel.toggleChatSheet(true)
                                        viewModel.sendGeminiChat(
                                            String.format(
                                                Locale.US,
                                                "Identify key landmarks, infrastructure, airports, and geographic features around %.4f, %.4f using Google Maps spatial intelligence.",
                                                state.camera.centerLat,
                                                state.camera.centerLon
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }

                    // Clean Cinema Mode Floating Restore HUD Button
                    AnimatedVisibility(
                        visible = state.isCleanHudMode,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .statusBarsPadding()
                            .padding(top = 10.dp, end = 12.dp)
                    ) {
                        OutlinedButton(
                            onClick = viewModel::toggleCleanHudMode,
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = ObsidianPanel,
                                contentColor = accent
                            ),
                            border = BorderStroke(1.dp, accent),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.testTag("restore_hud_button")
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = "Restore HUD", tint = accent)
                            Spacer(Modifier.height(0.dp))
                            Text(" SHOW HUD", style = MaterialTheme.typography.labelSmall, color = accent)
                        }
                    }

                    // Non-overlapping Edge-to-Edge Adaptive HUD Overlay Layout
                    AnimatedVisibility(
                        visible = !state.isCleanHudMode,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Top 3D Telemetry + Quick Basemap Switcher + Action Banner
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CallidusTopTelemetryBar(
                                    state = state,
                                    deviceTilt = deviceTilt,
                                    onRefresh = viewModel::refreshAllTelemetryNow,
                                    onToggleAutoRotate = viewModel::toggleAutoRotate,
                                    onToggleSound = viewModel::toggleSound,
                                    onToggleCleanHud = viewModel::toggleCleanHudMode,
                                    onOpenNavigation = { viewModel.toggleNavigationSheet(true) },
                                    onVoiceClick = ::onVoiceButtonClicked,
                                    onSelectViewport = viewModel::setViewportMode,
                                    onOpenWorldometers = { viewModel.toggleWorldometersSheet(true) },
                                    onOpenSeismic = { viewModel.toggleSeismicSheet(true) },
                                    onOpenAircraftTelemetry = { viewModel.openTelemetryDashboard(TelemetryDashboardTab.AIRCRAFT) },
                                    onOpenSatelliteTelemetry = { viewModel.openTelemetryDashboard(TelemetryDashboardTab.SATELLITE) }
                                )

                                AnimatedVisibility(
                                    visible = state.viewportMode == ViewportMode.GLOBE_3D &&
                                        !anyOverlayOpen &&
                                        state.selectedTarget is SelectedTarget.None,
                                    enter = fadeIn(),
                                    exit = fadeOut()
                                ) {
                                    BasemapQuickSwitchBar(
                                        activeStyle = state.imageryStyle,
                                        accent = accent,
                                        onSelectStyle = viewModel::setImageryStyle,
                                        onOpenLayerManager = { viewModel.toggleLayerDrawer(true) },
                                        modifier = Modifier.padding(top = 6.dp)
                                    )
                                }

                                AnimatedVisibility(
                                    visible = state.actionBannerMessage != null,
                                    enter = fadeIn(),
                                    exit = fadeOut(),
                                    modifier = Modifier.padding(top = 6.dp, start = 16.dp, end = 16.dp)
                                ) {
                                    Surface(
                                        color = ObsidianPanelElevated,
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, CallidusEmerald)
                                    ) {
                                        Text(
                                            text = state.actionBannerMessage.orEmpty(),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = TextPrimary,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                        )
                                    }
                                }
                            }

                            // Middle Globe Workspace: Floating Telemetry Dashboard + Right 3D Gimbal Rail
                            BoxWithConstraints(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                if (state.viewportMode == ViewportMode.GLOBE_3D && !anyOverlayOpen) {
                                    FloatingTelemetryDashboard(
                                        state = state,
                                        deviceTilt = deviceTilt,
                                        maxAvailableHeight = maxHeight - 12.dp,
                                        onSelectTab = viewModel::setTelemetryDashboardTab,
                                        onToggleExpanded = viewModel::toggleTelemetryDashboardExpanded,
                                        onHide = { viewModel.toggleTelemetryDashboardVisible(false) },
                                        onCycleAircraft = viewModel::cycleTrackedAircraft,
                                        onCycleSatellite = viewModel::cycleTrackedSatellite,
                                        onLockAircraft = { flight ->
                                            viewModel.selectTarget(SelectedTarget.Flight(flight))
                                        },
                                        onLockSatellite = { sat ->
                                            viewModel.selectTarget(SelectedTarget.Satellite(sat))
                                        },
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(start = 12.dp, top = 8.dp)
                                    )

                                    Pitch3DGimbalControl(
                                        pitchDeg = state.camera.pitchDeg,
                                        zoom = state.camera.zoom,
                                        accent = accent,
                                        onPitchChange = viewModel::setCameraPitch,
                                        onZoomChange = viewModel::setCameraZoom,
                                        modifier = Modifier.align(Alignment.CenterEnd)
                                    )
                                }
                            }

                            if (!anyOverlayOpen && state.activeRoute != null) {
                                NavigationBottomBanner(
                                    state = state,
                                    gpsCompassState = gpsCompassState,
                                    accent = accent,
                                    onOpenNavSheet = { viewModel.toggleNavigationSheet(true) },
                                    onStopNavigation = viewModel::clearNavigationRoute
                                )
                            }

                            // Bottom Section: Either 3D Hinged Target Dossier OR 3D Optic Carousel + Command Dock
                            if (state.viewportMode == ViewportMode.GLOBE_3D &&
                                state.selectedTarget !is SelectedTarget.None &&
                                !anyOverlayOpen
                            ) {
                                val isPinned = state.selectedTarget.targetId in state.pinnedTargetIds
                                Target3DDossierCard(
                                    selected = state.selectedTarget,
                                    opticMode = state.opticMode,
                                    isPinned = isPinned,
                                    accent = accent,
                                    deviceTilt = deviceTilt,
                                    fetchCctvSnapshot = viewModel::fetchCctvSnapshot,
                                    onCalibrateCctv = viewModel::calibrateCctvViewshed,
                                    onTogglePin = viewModel::togglePinTarget,
                                    onEngageChaseCam = viewModel::engageChaseCam,
                                    onNavigateTarget = { lat, lon, label ->
                                        viewModel.navigateToDestination(lat, lon, label)
                                    },
                                    onClose = viewModel::clearSelection,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp)
                                )
                            } else if (!anyOverlayOpen) {
                                Optic3DCarouselBar(
                                    activeOptic = state.opticMode,
                                    deviceTilt = deviceTilt,
                                    showOpticRow = state.viewportMode == ViewportMode.GLOBE_3D,
                                    onSelectOptic = viewModel::setOpticMode,
                                    onOpenLayers = { viewModel.toggleLayerDrawer(true) },
                                    onOpenTheaters = { viewModel.toggleTheatersSheet(true) },
                                    onOpenCctvWall = { viewModel.toggleCctvWall(true) },
                                    onOpenNavigation = { viewModel.toggleNavigationSheet(true) },
                                    onOpenWorldometers = { viewModel.toggleWorldometersSheet(true) },
                                    onOpenChat = { viewModel.toggleChatSheet(true) },
                                    onOpenDonations = { viewModel.toggleDonationsSheet(true) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    // 3D Spatial Layer, Basemap Imagery, Bookmarks & Search Deck
                    LayerAndWatchlist3DDrawer(
                        state = state,
                        onToggleLayer = viewModel::toggleLayer,
                        onSelectImageryStyle = viewModel::setImageryStyle,
                        onImageryOpacityChange = viewModel::setImageryOpacity,
                        onToggleGraticule = viewModel::toggleGraticuleGrid,
                        onToggleAtmosphere = viewModel::toggleAtmosphereGlow,
                        onSearchChange = viewModel::setSearchQuery,
                        onSelectTarget = viewModel::selectTarget,
                        onFlyToBookmark = viewModel::flyToBookmark,
                        onClose = { viewModel.toggleLayerDrawer(false) },
                        modifier = Modifier.align(Alignment.CenterStart)
                    )

                    // 3D Strategic Theaters Selector
                    StrategicTheaters3DSheet(
                        visible = state.isTheatersSheetOpen,
                        activeTheaterId = state.activeTheaterId,
                        accent = accent,
                        onSelectTheater = viewModel::selectTheater,
                        onClose = { viewModel.toggleTheatersSheet(false) },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )

                    // Navigation & Offline Hardware GPS Compass Sheet
                    NavigationPlannerSheet(
                        visible = state.isNavigationSheetOpen,
                        state = state,
                        gpsCompassState = gpsCompassState,
                        accent = accent,
                        onSearchPlaces = viewModel::searchNavigationPlaces,
                        onSelectTravelMode = viewModel::setNavTravelMode,
                        onNavigateTo = { destLat, destLon, destLabel, originLat, originLon ->
                            viewModel.navigateToDestination(destLat, destLon, destLabel, originLat, originLon)
                        },
                        onClearRoute = viewModel::clearNavigationRoute,
                        onSwitchViewport = { mode ->
                            viewModel.toggleNavigationSheet(false)
                            viewModel.setViewportMode(mode)
                        },
                        onClose = { viewModel.toggleNavigationSheet(false) },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )

                    // Live Multi-Camera Surveillance Wall Sheet (Worldwide Feeds)
                    LiveCctvWallSheet(
                        visible = state.isCctvWallOpen,
                        cameras = state.cctvCameras,
                        centerLat = state.camera.centerLat,
                        centerLon = state.camera.centerLon,
                        opticMode = state.opticMode,
                        accent = accent,
                        fetchSnapshotBytes = viewModel::fetchCctvSnapshot,
                        onRefreshFeeds = viewModel::refreshAllTelemetryNow,
                        onSelectCamera3D = viewModel::flyToCctvCamera,
                        onClose = { viewModel.toggleCctvWall(false) },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )

                    // Real-Time Worldometers.info Global Statistics Sheet
                    WorldometersLiveSheet(
                        visible = state.isWorldometersOpen,
                        stats = state.worldometersStats,
                        status = state.worldometersStatus,
                        accent = accent,
                        onAskGemini = { prompt ->
                            viewModel.toggleChatSheet(true)
                            viewModel.sendGeminiChat(prompt)
                        },
                        onClose = { viewModel.toggleWorldometersSheet(false) },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )

                    // Real-Time Seismic Listener & Pulsating Nodes Monitor Sheet
                    SeismicMonitorSheet(
                        visible = state.isSeismicSheetOpen,
                        earthquakes = state.earthquakes,
                        streamTelemetry = state.seismicStream,
                        accent = accent,
                        onFlyToQuake = { quake ->
                            viewModel.selectTarget(SelectedTarget.Quake(quake))
                        },
                        onAskGemini = { prompt ->
                            viewModel.toggleChatSheet(true)
                            viewModel.sendGeminiChat(prompt)
                        },
                        onClose = { viewModel.toggleSeismicSheet(false) },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )

                    // Multi-Turn Gemini AI Analyst Sheet
                    GeminiChatbotSheet(
                        visible = state.isChatOpen,
                        messages = state.chatMessages,
                        streamingReply = state.streamingChatReply,
                        isSending = state.isAiSending,
                        errorText = state.aiError,
                        selectedModel = state.selectedAiModel,
                        targetLat = state.camera.centerLat,
                        targetLon = state.camera.centerLon,
                        accent = accent,
                        onSelectModel = viewModel::setAiModel,
                        onSendPrompt = viewModel::sendGeminiChat,
                        onClearHistory = viewModel::clearChatHistory,
                        onOpenInMaps = viewModel::openCoordinatesInGoogleMaps,
                        onClose = { viewModel.toggleChatSheet(false) },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )

                    // Paybill 522522 Account 1355643775 Donations Sheet + Optional Google Sign-In / Sign-Out
                    DonationPaybillSheet(
                        visible = state.isDonationsOpen,
                        donations = state.donations,
                        sqlSnapshotCount = state.sqlSnapshotCount,
                        isAuthenticated = state.isAuthenticated,
                        accent = accent,
                        onRecordDonation = viewModel::recordDonation,
                        onSignIn = {
                            onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = { viewModel.toggleDonationsSheet(false) },
                                onAuthError = {},
                                scope = scope
                            )
                        },
                        onSignOut = {
                            viewModel.toggleDonationsSheet(false)
                            viewModel.signOut(credentialManager)
                        },
                        onClose = { viewModel.toggleDonationsSheet(false) },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )

                    // Dramatic 3D Holographic Logo Boot Acquisition Transition Overlay
                    AnimatedVisibility(
                        visible = state.bootProgress < 0.88f,
                        enter = fadeIn() + scaleIn(initialScale = 0.6f),
                        exit = fadeOut() + scaleOut(targetScale = 1.65f),
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x8802050B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Animated3DCallidusLogo(
                                    size = 160.dp,
                                    accent = accent,
                                    secondaryAccent = CallidusGold,
                                    dramaticEntrance = true
                                )
                                Spacer(Modifier.height(14.dp))
                                Text(
                                    text = "CALLIDUS ORBITAL ACQUISITION",
                                    style = MaterialTheme.typography.labelLarge,
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

@Composable
private fun CrosshairReticleOverlay(accent: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width * 0.5f
        val cy = size.height * 0.5f
        val arm = 12f
        val gap = 6f
        val c = accent.copy(alpha = 0.35f)
        drawLine(c, Offset(cx - arm - gap, cy), Offset(cx - gap, cy), 1.4f)
        drawLine(c, Offset(cx + gap, cy), Offset(cx + arm + gap, cy), 1.4f)
        drawLine(c, Offset(cx, cy - arm - gap), Offset(cx, cy - gap), 1.4f)
        drawLine(c, Offset(cx, cy + gap), Offset(cx, cy + arm + gap), 1.4f)
    }
}

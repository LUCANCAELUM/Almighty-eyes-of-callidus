package com.example.ui

import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.AiStreamChunk
import com.example.data.firebase.CallidusAiModel
import com.example.data.firebase.CallidusAiRepository
import com.example.data.firebase.CallidusFirestoreRepository
import com.example.data.firebase.DonationRecord
import com.example.data.firebase.IntelChatMessage
import com.example.data.firebase.OperationType
import com.example.data.firebase.SavedBookmark
import com.example.data.firebase.handleFirestoreError
import com.example.data.firebase.signOutUser
import com.example.data.local.CallidusPreferences
import com.example.data.local.LocalTelemetrySqlRepository
import com.example.data.local.TelemetrySnapshotEntity
import com.example.data.orbital.OrbitalPropagator
import com.example.data.repository.CallidusRepository
import com.example.data.seismic.RealTimeSeismicListener
import com.example.data.seismic.SeismicStreamEvent
import com.example.domain.model.AmenityCategory
import com.example.domain.model.ArTargetFilter
import com.example.domain.model.CallidusUiState
import com.example.domain.model.CctvCameraTarget
import com.example.domain.model.FeedStatus
import com.example.domain.model.GlobeImageryStyle
import com.example.domain.model.IntelLayer
import com.example.domain.model.LandmarkNode
import com.example.domain.model.MapAmenity
import com.example.domain.model.MaritimeVesselTarget
import com.example.domain.model.NavigationTravelMode
import com.example.domain.model.OpticMode
import com.example.domain.model.SatelliteElement
import com.example.domain.model.SeismicConnectionStatus
import com.example.domain.model.SelectedTarget
import com.example.domain.model.StrategicTheaters
import com.example.domain.model.TelemetryDashboardTab
import com.example.domain.model.TheaterPreset
import com.example.domain.model.ViewportMode
import com.example.domain.model.WorldometersStat
import com.example.engine.CallidusAudioEngine
import com.example.engine.Globe3DEngine
import com.example.ui.components.TelemetryCalculator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sin

class CallidusViewModel(
    private val currentUserId: String?,
    private val userDisplayName: String,
    private val userEmail: String?,
    private val firestoreRepo: CallidusFirestoreRepository,
    private val aiRepo: CallidusAiRepository,
    private val sqlRepo: LocalTelemetrySqlRepository,
    private val preferences: CallidusPreferences,
    private val repository: CallidusRepository = CallidusRepository(),
    private val seismicListener: RealTimeSeismicListener = RealTimeSeismicListener()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        CallidusUiState(
            isAuthenticated = currentUserId != null,
            operatorName = userDisplayName
        )
    )
    val uiState: StateFlow<CallidusUiState> = _uiState.asStateFlow()

    private val audioEngine = CallidusAudioEngine(viewModelScope)
    private var cachedSatelliteElements: List<SatelliteElement> = emptyList()
    private var flightJob: Job? = null
    private var chatJob: Job? = null
    private var velLon: Double = 0.0
    private var velLat: Double = 0.0
    private var lastAutoRotateEnabled = true

    init {
        restorePreferencesAndBoot()
        observeCloudAndLocalData()
    }

    private fun observeCloudAndLocalData() {
        // Room SQLite snapshot counter
        viewModelScope.launch {
            sqlRepo.snapshotCount
                .catch { /* keep resilient */ }
                .collect { count ->
                    _uiState.update { it.copy(sqlSnapshotCount = count) }
                }
        }

        val uid = currentUserId ?: return

        // Firestore Saved Bookmarks
        viewModelScope.launch {
            firestoreRepo.observeBookmarks(uid)
                .catch { e ->
                    if (e is CancellationException) throw e
                    handleFirestoreError(e, OperationType.LIST, "users/$uid/bookmarks")
                }
                .collect { list ->
                    _uiState.update { state ->
                        val cloudIds = list.map { it.targetKey }.toSet()
                        state.copy(
                            bookmarks = list,
                            pinnedTargetIds = state.pinnedTargetIds + cloudIds
                        )
                    }
                }
        }

        // Firestore Donations (Paybill 522522)
        viewModelScope.launch {
            firestoreRepo.observeDonations(uid)
                .catch { e ->
                    if (e is CancellationException) throw e
                    handleFirestoreError(e, OperationType.LIST, "users/$uid/donations")
                }
                .collect { list ->
                    _uiState.update { it.copy(donations = list) }
                }
        }

        // Firestore Gemini Intel Chat Messages
        viewModelScope.launch {
            firestoreRepo.observeIntelMessages(uid)
                .catch { e ->
                    if (e is CancellationException) throw e
                    handleFirestoreError(e, OperationType.LIST, "users/$uid/intel_messages")
                }
                .collect { msgs ->
                    _uiState.update { it.copy(chatMessages = msgs) }
                }
        }
    }

    private fun restorePreferencesAndBoot() {
        viewModelScope.launch {
            val savedOptic = preferences.opticModeFlow.first()
            val savedImagery = preferences.imageryStyleFlow.first()
            val savedOpacity = preferences.imageryOpacityFlow.first()
            val savedGraticule = preferences.showGraticuleFlow.first()
            val savedAtmosphere = preferences.showAtmosphereFlow.first()
            val savedLayers = preferences.enabledLayersFlow.first()
            val savedPins = preferences.pinnedTargetsFlow.first()
            _uiState.update {
                it.copy(
                    opticMode = savedOptic,
                    imageryStyle = savedImagery,
                    imageryOpacity = savedOpacity,
                    showGraticuleGrid = savedGraticule,
                    showAtmosphereGlow = savedAtmosphere,
                    enabledLayers = savedLayers,
                    pinnedTargetIds = savedPins
                )
            }
            if (currentUserId != null) {
                firestoreRepo.ensureUserProfile(
                    displayName = userDisplayName,
                    email = userEmail,
                    defaultOptic = savedOptic.name
                )
            }
            runBootAcquisitionSequence()
        }

        // Launch live telemetry loops
        viewModelScope.launch {
            refreshAllGlobalFeeds()
            while (isActive) {
                delay(90_000L)
                refreshAllGlobalFeeds()
            }
        }

        viewModelScope.launch {
            refreshRegionalTheaterTelemetry()
            while (isActive) {
                delay(20_000L)
                refreshRegionalTheaterTelemetry()
            }
        }

        // Launch dual-channel Real-Time Seismic Event Listener (EMSC WebSocket + USGS 1h rapid feed)
        startRealTimeSeismicListener()

        // Live real-time Keplerian satellite propagation loop (every 2s)
        viewModelScope.launch {
            while (isActive) {
                if (cachedSatelliteElements.isNotEmpty()) {
                    val now = System.currentTimeMillis()
                    val propagated = cachedSatelliteElements.map { OrbitalPropagator.propagate(it, now) }
                    _uiState.update { state ->
                        val updatedSelected = when (val sel = state.selectedTarget) {
                            is SelectedTarget.Satellite -> {
                                val refreshed = propagated.firstOrNull { it.element.noradId == sel.data.element.noradId }
                                if (refreshed != null) SelectedTarget.Satellite(refreshed) else sel
                            }
                            else -> sel
                        }
                        state.copy(
                            satellites = propagated,
                            selectedTarget = updatedSelected
                        )
                    }
                }
                delay(2_000L)
            }
        }

        // Gentle 3D orbital idle rotation + momentum inertia loop (~60fps when active)
        viewModelScope.launch {
            var lastTime = System.nanoTime()
            while (isActive) {
                delay(16L)
                val now = System.nanoTime()
                val dt = ((now - lastTime) / 1_000_000_000.0).coerceIn(0.001, 0.05)
                lastTime = now

                val state = _uiState.value
                if (state.viewportMode != ViewportMode.GLOBE_3D || state.camera.chaseMode) continue

                val hasInertia = abs(velLon) > 0.02 || abs(velLat) > 0.02
                if (hasInertia && flightJob?.isActive != true) {
                    val nextLon = Globe3DEngine.normalizeLon(state.camera.centerLon + velLon * dt * 60.0)
                    val nextLat = (state.camera.centerLat + velLat * dt * 60.0).coerceIn(-82.0, 82.0)
                    val bankRoll = (velLon * 2.2).toFloat().coerceIn(-16f, 16f)
                    velLon *= 0.93
                    velLat *= 0.93
                    _uiState.update { s ->
                        s.copy(
                            camera = s.camera.copy(
                                centerLat = nextLat,
                                centerLon = nextLon,
                                rollDeg = bankRoll
                            )
                        )
                    }
                } else if (state.autoRotateGlobe && flightJob?.isActive != true &&
                    state.selectedTarget is SelectedTarget.None &&
                    !state.isLayerDrawerOpen && !state.isChatOpen &&
                    !state.isDonationsOpen && !state.isCctvWallOpen &&
                    !state.isWorldometersOpen && !state.isSeismicSheetOpen &&
                    !state.isNavigationSheetOpen
                ) {
                    val driftSpeed = (4.8 / state.camera.zoom.coerceAtLeast(1f))
                    val nextLon = Globe3DEngine.normalizeLon(state.camera.centerLon + driftSpeed * dt)
                    val currentRoll = state.camera.rollDeg
                    val decayedRoll = if (abs(currentRoll) > 0.1f) currentRoll * 0.9f else 0f
                    _uiState.update { s ->
                        s.copy(
                            camera = s.camera.copy(
                                centerLon = nextLon,
                                rollDeg = decayedRoll
                            )
                        )
                    }
                } else if (abs(state.camera.rollDeg) > 0.1f && flightJob?.isActive != true) {
                    _uiState.update { s ->
                        s.copy(camera = s.camera.copy(rollDeg = s.camera.rollDeg * 0.88f))
                    }
                }
            }
        }
    }

    private fun startRealTimeSeismicListener() {
        // Channel 1: EMSC SeismicPortal WebSocket live tremor push stream
        viewModelScope.launch {
            seismicListener.observeLiveTremors()
                .catch { /* Keep USGS channel running if WebSocket unavailable */ }
                .collect { event ->
                    when (event) {
                        is SeismicStreamEvent.StatusChanged -> {
                            _uiState.update { state ->
                                state.copy(
                                    seismicStream = state.seismicStream.copy(status = event.status)
                                )
                            }
                        }
                        is SeismicStreamEvent.TremorDetected -> {
                            val tremor = event.tremor
                            val now = System.currentTimeMillis()
                            _uiState.update { state ->
                                val merged = RealTimeSeismicListener.mergeSeismicEvents(
                                    existing = state.earthquakes,
                                    incoming = listOf(tremor)
                                )
                                val updatedSel = when (val sel = state.selectedTarget) {
                                    is SelectedTarget.Quake -> if (sel.data.id == tremor.id) {
                                        SelectedTarget.Quake(tremor)
                                    } else sel
                                    else -> sel
                                }
                                val alertBanner = if (!event.isUpdate) {
                                    String.format(
                                        Locale.US,
                                        "LIVE TREMOR: M%.1f %s (%s)",
                                        tremor.magnitude,
                                        tremor.place,
                                        tremor.sourceNetwork
                                    )
                                } else state.actionBannerMessage
                                state.copy(
                                    earthquakes = merged,
                                    selectedTarget = updatedSel,
                                    seismicStream = state.seismicStream.copy(
                                        status = SeismicConnectionStatus.LIVE_STREAM,
                                        eventsReceivedCount = state.seismicStream.eventsReceivedCount + 1,
                                        latestLiveTremor = tremor,
                                        lastPacketAtMs = now
                                    ),
                                    actionBannerMessage = alertBanner
                                )
                            }
                            if (!event.isUpdate) {
                                audioEngine.playSeismicAlert()
                            }
                            setFeedStatus(
                                IntelLayer.SEISMIC_USGS,
                                FeedStatus.Live(_uiState.value.earthquakes.size, now)
                            )
                        }
                    }
                }
        }

        // Channel 2: Rapid 20-second USGS 1-hour real-time GeoJSON tremor listener
        viewModelScope.launch {
            while (isActive) {
                repository.fetchUsgsRecentHourEarthquakes().onSuccess { recentQuakes ->
                    if (recentQuakes.isNotEmpty()) {
                        val now = System.currentTimeMillis()
                        _uiState.update { state ->
                            val existingIds = state.earthquakes.mapTo(HashSet()) { it.id }
                            val newlyArrived = recentQuakes.firstOrNull { it.id !in existingIds && it.isRealtimeAlert }
                            val merged = RealTimeSeismicListener.mergeSeismicEvents(
                                existing = state.earthquakes,
                                incoming = recentQuakes
                            )
                            val nextStatus = if (state.seismicStream.status == SeismicConnectionStatus.LIVE_STREAM) {
                                SeismicConnectionStatus.LIVE_STREAM
                            } else {
                                SeismicConnectionStatus.POLLING
                            }
                            state.copy(
                                earthquakes = merged,
                                seismicStream = state.seismicStream.copy(
                                    status = nextStatus,
                                    latestLiveTremor = newlyArrived ?: state.seismicStream.latestLiveTremor ?: merged.firstOrNull { it.isRealtimeAlert },
                                    lastPacketAtMs = if (state.seismicStream.lastPacketAtMs == 0L) now else state.seismicStream.lastPacketAtMs
                                )
                            )
                        }
                        setFeedStatus(
                            IntelLayer.SEISMIC_USGS,
                            FeedStatus.Live(_uiState.value.earthquakes.size, now)
                        )
                    }
                }
                delay(20_000L)
            }
        }
    }

    private fun runBootAcquisitionSequence() {
        viewModelScope.launch {
            val startLon = -120.0
            val endLon = -25.0
            val steps = 42
            for (i in 0..steps) {
                val t = i.toFloat() / steps
                val ease = 1f - (1f - t) * (1f - t) * (1f - t)
                _uiState.update { s ->
                    s.copy(
                        bootProgress = ease,
                        camera = s.camera.copy(
                            centerLon = Globe3DEngine.normalizeLon(startLon + (endLon - startLon) * ease),
                            centerLat = 45.0 - 18.0 * ease,
                            zoom = 0.72f + 0.43f * ease,
                            pitchDeg = 8f + 18f * ease
                        )
                    )
                }
                delay(24L)
            }
            _uiState.update { it.copy(bootProgress = 1f) }
        }
    }

    fun refreshAllTelemetryNow() {
        audioEngine.playRadarPing()
        viewModelScope.launch {
            refreshAllGlobalFeeds()
            refreshRegionalTheaterTelemetry()
        }
    }

    private suspend fun refreshAllGlobalFeeds() = supervisorScope {
        setFeedStatus(IntelLayer.ORBITAL_SATS, FeedStatus.Syncing)
        setFeedStatus(IntelLayer.CCTV_CAMERAS, FeedStatus.Syncing)
        setFeedStatus(IntelLayer.MARITIME_AIS, FeedStatus.Syncing)
        setFeedStatus(IntelLayer.SEISMIC_USGS, FeedStatus.Syncing)
        setFeedStatus(IntelLayer.NATURAL_EONET, FeedStatus.Syncing)
        setFeedStatus(IntelLayer.SOLAR_WEATHER, FeedStatus.Syncing)

        launch {
            repository.fetchOrbitalElements().fold(
                onSuccess = { elements ->
                    cachedSatelliteElements = elements
                    val now = System.currentTimeMillis()
                    val propagated = elements.map { OrbitalPropagator.propagate(it, now) }
                    _uiState.update { it.copy(satellites = propagated) }
                    setFeedStatus(IntelLayer.ORBITAL_SATS, FeedStatus.Live(propagated.size, now))
                },
                onFailure = { err ->
                    setFeedStatus(IntelLayer.ORBITAL_SATS, FeedStatus.Error(err.message ?: "CelesTrak offline"))
                }
            )
        }

        launch {
            repository.fetchLiveCctvCameras().fold(
                onSuccess = { cams ->
                    _uiState.update { state ->
                        val prevById = state.cctvCameras.associateBy { it.id }
                        val merged = cams.map { cam ->
                            val old = prevById[cam.id]
                            if (old != null) {
                                cam.copy(
                                    azimuthDeg = old.azimuthDeg,
                                    fovDeg = old.fovDeg,
                                    rangeKm = old.rangeKm
                                )
                            } else cam
                        }
                        val updatedSelected = when (val sel = state.selectedTarget) {
                            is SelectedTarget.Cctv -> {
                                val refreshed = merged.firstOrNull { it.id == sel.data.id }
                                if (refreshed != null) SelectedTarget.Cctv(refreshed) else sel
                            }
                            else -> sel
                        }
                        state.copy(cctvCameras = merged, selectedTarget = updatedSelected)
                    }
                    setFeedStatus(IntelLayer.CCTV_CAMERAS, FeedStatus.Live(cams.size, System.currentTimeMillis()))
                },
                onFailure = { err ->
                    setFeedStatus(IntelLayer.CCTV_CAMERAS, FeedStatus.Error(err.message ?: "CCTV offline"))
                }
            )
        }

        launch {
            repository.fetchMaritimeVessels().fold(
                onSuccess = { vessels ->
                    _uiState.update { state ->
                        val updatedSelected = when (val sel = state.selectedTarget) {
                            is SelectedTarget.Vessel -> {
                                val refreshed = vessels.firstOrNull { it.mmsi == sel.data.mmsi }
                                if (refreshed != null) SelectedTarget.Vessel(refreshed) else sel
                            }
                            else -> sel
                        }
                        state.copy(maritimeVessels = vessels, selectedTarget = updatedSelected)
                    }
                    setFeedStatus(IntelLayer.MARITIME_AIS, FeedStatus.Live(vessels.size, System.currentTimeMillis()))
                },
                onFailure = { err ->
                    setFeedStatus(IntelLayer.MARITIME_AIS, FeedStatus.Error(err.message ?: "AIS offline"))
                }
            )
        }

        launch {
            repository.fetchUsgsEarthquakes().fold(
                onSuccess = { quakes ->
                    _uiState.update { state ->
                        val merged = RealTimeSeismicListener.mergeSeismicEvents(
                            existing = state.earthquakes,
                            incoming = quakes
                        )
                        state.copy(earthquakes = merged)
                    }
                    setFeedStatus(IntelLayer.SEISMIC_USGS, FeedStatus.Live(_uiState.value.earthquakes.size, System.currentTimeMillis()))
                },
                onFailure = { err ->
                    if (_uiState.value.earthquakes.isEmpty()) {
                        setFeedStatus(IntelLayer.SEISMIC_USGS, FeedStatus.Error(err.message ?: "USGS error"))
                    }
                }
            )
        }

        launch {
            repository.fetchNasaNaturalEvents().fold(
                onSuccess = { events ->
                    _uiState.update { it.copy(naturalEvents = events) }
                    setFeedStatus(IntelLayer.NATURAL_EONET, FeedStatus.Live(events.size, System.currentTimeMillis()))
                },
                onFailure = { err ->
                    setFeedStatus(IntelLayer.NATURAL_EONET, FeedStatus.Error(err.message ?: "EONET error"))
                }
            )
        }

        launch {
            repository.fetchSpaceWeather().fold(
                onSuccess = { sw ->
                    _uiState.update { it.copy(spaceWeather = sw) }
                    setFeedStatus(IntelLayer.SOLAR_WEATHER, FeedStatus.Live(1, System.currentTimeMillis()))
                },
                onFailure = { err ->
                    setFeedStatus(IntelLayer.SOLAR_WEATHER, FeedStatus.Error(err.message ?: "NOAA error"))
                }
            )
        }
    }

    private suspend fun refreshRegionalTheaterTelemetry() = supervisorScope {
        val snapshotState = _uiState.value
        val cam = snapshotState.camera
        setFeedStatus(IntelLayer.AIRCRAFT_ADSB, FeedStatus.Syncing)

        val flightJobLocal = launch {
            repository.fetchLiveAircraft(cam.centerLat, cam.centerLon).fold(
                onSuccess = { flights ->
                    _uiState.update { state ->
                        val updatedSelected = when (val sel = state.selectedTarget) {
                            is SelectedTarget.Flight -> {
                                val fresh = flights.firstOrNull { it.icao24 == sel.data.icao24 }
                                if (fresh != null) SelectedTarget.Flight(fresh) else sel
                            }
                            else -> sel
                        }
                        state.copy(
                            aircraft = flights,
                            selectedTarget = updatedSelected
                        )
                    }
                    setFeedStatus(IntelLayer.AIRCRAFT_ADSB, FeedStatus.Live(flights.size, System.currentTimeMillis()))
                },
                onFailure = { err ->
                    setFeedStatus(IntelLayer.AIRCRAFT_ADSB, FeedStatus.Error(err.message ?: "ADS-B error"))
                }
            )
        }

        val wxJobLocal = launch {
            repository.fetchLocalWeather(cam.centerLat, cam.centerLon).onSuccess { weather ->
                _uiState.update { it.copy(localWeather = weather) }
            }
        }

        val trafficJobLocal = launch {
            if (cam.zoom >= 2.5f && IntelLayer.TRAFFIC_FLOW in snapshotState.enabledLayers) {
                setFeedStatus(IntelLayer.TRAFFIC_FLOW, FeedStatus.Syncing)
                repository.fetchRoadTrafficFlow(cam.centerLat, cam.centerLon).fold(
                    onSuccess = { segments ->
                        _uiState.update { it.copy(trafficSegments = segments) }
                        setFeedStatus(IntelLayer.TRAFFIC_FLOW, FeedStatus.Live(segments.size, System.currentTimeMillis()))
                    },
                    onFailure = { err ->
                        setFeedStatus(IntelLayer.TRAFFIC_FLOW, FeedStatus.Error(err.message ?: "Overpass busy"))
                    }
                )
            }
        }

        flightJobLocal.join()
        wxJobLocal.join()
        trafficJobLocal.join()

        // Cache regional telemetry snapshot in local Room SQLite database
        val latest = _uiState.value
        runCatching {
            sqlRepo.recordTelemetrySnapshot(
                TelemetrySnapshotEntity(
                    theaterId = latest.activeTheaterId,
                    centerLat = latest.camera.centerLat,
                    centerLon = latest.camera.centerLon,
                    aircraftCount = latest.aircraft.size,
                    satelliteCount = latest.satellites.size,
                    earthquakeCount = latest.earthquakes.size,
                    naturalEventCount = latest.naturalEvents.size,
                    kpIndex = latest.spaceWeather.kpIndex
                )
            )
        }
    }

    private fun setFeedStatus(layer: IntelLayer, status: FeedStatus) {
        _uiState.update { s ->
            s.copy(feedStatuses = s.feedStatuses + (layer to status))
        }
    }

    fun setViewportMode(mode: ViewportMode) {
        audioEngine.playOpticClick()
        _uiState.update {
            it.copy(
                viewportMode = mode,
                camera = if (mode != ViewportMode.GLOBE_3D) it.camera.copy(chaseMode = false) else it.camera
            )
        }
    }

    fun toggleChatSheet(open: Boolean? = null) {
        _uiState.update {
            val next = open ?: !it.isChatOpen
            it.copy(
                isChatOpen = next,
                isDonationsOpen = false,
                isLayerDrawerOpen = false,
                isTheatersSheetOpen = false,
                isCctvWallOpen = false,
                isWorldometersOpen = false,
                isSeismicSheetOpen = false,
                isNavigationSheetOpen = false,
                aiError = null
            )
        }
    }

    fun toggleDonationsSheet(open: Boolean? = null) {
        _uiState.update {
            val next = open ?: !it.isDonationsOpen
            it.copy(
                isDonationsOpen = next,
                isChatOpen = false,
                isLayerDrawerOpen = false,
                isTheatersSheetOpen = false,
                isCctvWallOpen = false,
                isWorldometersOpen = false,
                isSeismicSheetOpen = false,
                isNavigationSheetOpen = false
            )
        }
    }

    fun toggleCctvWall(open: Boolean? = null) {
        _uiState.update {
            val next = open ?: !it.isCctvWallOpen
            it.copy(
                isCctvWallOpen = next,
                isChatOpen = false,
                isDonationsOpen = false,
                isLayerDrawerOpen = false,
                isTheatersSheetOpen = false,
                isWorldometersOpen = false,
                isSeismicSheetOpen = false,
                isNavigationSheetOpen = false
            )
        }
    }

    fun toggleWorldometersSheet(open: Boolean? = null) {
        _uiState.update {
            val next = open ?: !it.isWorldometersOpen
            it.copy(
                isWorldometersOpen = next,
                isChatOpen = false,
                isDonationsOpen = false,
                isLayerDrawerOpen = false,
                isTheatersSheetOpen = false,
                isCctvWallOpen = false,
                isSeismicSheetOpen = false,
                isNavigationSheetOpen = false
            )
        }
    }

    fun toggleSeismicSheet(open: Boolean? = null) {
        _uiState.update {
            val next = open ?: !it.isSeismicSheetOpen
            it.copy(
                isSeismicSheetOpen = next,
                isChatOpen = false,
                isDonationsOpen = false,
                isLayerDrawerOpen = false,
                isTheatersSheetOpen = false,
                isCctvWallOpen = false,
                isWorldometersOpen = false,
                isNavigationSheetOpen = false
            )
        }
    }

    fun updateWorldometersData(stats: List<WorldometersStat>, status: String) {
        _uiState.update { state ->
            state.copy(
                worldometersStats = if (stats.isNotEmpty()) stats else state.worldometersStats,
                worldometersStatus = status
            )
        }
    }

    suspend fun fetchCctvSnapshot(imageUrl: String): ByteArray? = repository.fetchLiveSnapshotBytes(imageUrl)

    fun calibrateCctvViewshed(cameraId: String, azimuthDeg: Float, fovDeg: Float, rangeKm: Float) {
        val normAz = ((azimuthDeg % 360f) + 360f) % 360f
        val normFov = fovDeg.coerceIn(15f, 140f)
        val normRange = rangeKm.coerceIn(0.3f, 6.0f)
        _uiState.update { state ->
            val updatedList = state.cctvCameras.map { cam ->
                if (cam.id == cameraId) {
                    cam.copy(azimuthDeg = normAz, fovDeg = normFov, rangeKm = normRange)
                } else cam
            }
            val updatedSelected = when (val sel = state.selectedTarget) {
                is SelectedTarget.Cctv -> if (sel.data.id == cameraId) {
                    SelectedTarget.Cctv(sel.data.copy(azimuthDeg = normAz, fovDeg = normFov, rangeKm = normRange))
                } else sel
                else -> sel
            }
            state.copy(cctvCameras = updatedList, selectedTarget = updatedSelected)
        }
    }

    fun flyToCctvCamera(cam: CctvCameraTarget) {
        _uiState.update {
            it.copy(
                viewportMode = ViewportMode.GLOBE_3D,
                selectedTarget = SelectedTarget.Cctv(cam),
                isCctvWallOpen = false,
                isWorldometersOpen = false,
                isSeismicSheetOpen = false,
                isLayerDrawerOpen = false,
                isTheatersSheetOpen = false,
                isChatOpen = false,
                isDonationsOpen = false
            )
        }
        flyTo3D(
            targetLat = cam.lat,
            targetLon = cam.lon,
            targetZoom = 5.6f,
            targetPitch = 52f,
            refreshRegionalAfter = true
        )
    }

    fun setVoiceListening(listening: Boolean) {
        _uiState.update { it.copy(isVoiceListening = listening) }
    }

    fun handleVoiceCommand(transcript: String) {
        val cleaned = transcript.trim()
        if (cleaned.isEmpty()) {
            _uiState.update { it.copy(isVoiceListening = false) }
            return
        }
        val lower = cleaned.lowercase(Locale.US)
        _uiState.update { it.copy(isVoiceListening = false, lastVoiceTranscript = cleaned) }

        val matchedOptic = when {
            lower.contains("flir") || lower.contains("thermal") || lower.contains("infrared") -> OpticMode.FLIR_THERMAL
            lower.contains("nvg") || lower.contains("night vision") || lower.contains("phosphor") -> OpticMode.NVG_PHOSPHOR
            lower.contains("crt") || lower.contains("scanline") -> OpticMode.CRT_MONITOR
            lower.contains("noir") || lower.contains("monochrome") || lower.contains("black and white") -> OpticMode.NOIR_SURVEILLANCE
            lower.contains("arctic") || lower.contains("snow") -> OpticMode.ARCTIC_SNOW
            lower.contains("holo") || lower.contains("wireframe") -> OpticMode.WIREFRAME_HOLO
            lower.contains("solar") || lower.contains("terminator") -> OpticMode.SOLAR_TERMINATOR
            lower.contains("tactical") || lower.contains("eo") || lower.contains("standard") -> OpticMode.EO_TACTICAL
            else -> null
        }
        if (matchedOptic != null) {
            setOpticMode(matchedOptic)
        }

        val matchedTheaterId = when {
            lower.contains("london") || lower.contains("heathrow") -> "london_channel"
            lower.contains("austin") || lower.contains("texas") -> "austin_cctv"
            lower.contains("baltic") || lower.contains("ship") || lower.contains("maritime") || lower.contains("vessel") -> "baltic_ais"
            lower.contains("san francisco") || lower.contains("bay area") -> "sf_bay"
            lower.contains("tokyo") || lower.contains("japan") -> "tokyo_ring"
            lower.contains("hormuz") || lower.contains("dubai") -> "hormuz_gulf"
            lower.contains("kennedy") || lower.contains("canaveral") || lower.contains("florida") -> "kennedy_orbit"
            lower.contains("suez") || lower.contains("mediterranean") || lower.contains("cairo") -> "mediterranean"
            lower.contains("global") || lower.contains("world") -> "global_orbit"
            else -> null
        }
        if (matchedTheaterId != null) {
            StrategicTheaters.firstOrNull { it.id == matchedTheaterId }?.let { selectTheater(it) }
        }

        val matchedImagery = when {
            lower.contains("terrain") || lower.contains("topo") || lower.contains("elevation") -> GlobeImageryStyle.TERRAIN
            lower.contains("street") || lower.contains("standard layer") || lower.contains("standard imagery") || lower.contains("standard map") -> GlobeImageryStyle.STANDARD
            lower.contains("satellite imagery") || lower.contains("satellite layer") || lower.contains("ortho") -> GlobeImageryStyle.SATELLITE
            else -> null
        }
        if (matchedImagery != null) {
            setImageryStyle(matchedImagery)
        }

        when {
            lower.contains("layer manager") || lower.contains("open layers") -> {
                toggleLayerDrawer(true)
                _uiState.update { it.copy(actionBannerMessage = "VOICE: \"$cleaned\" → Opened Layer Manager") }
            }
            matchedImagery != null -> {
                _uiState.update { it.copy(actionBannerMessage = "VOICE: \"$cleaned\" → ${matchedImagery.shortTitle}") }
            }
            lower.contains("seismic") || lower.contains("earthquake") || lower.contains("tremor") || lower.contains("quake") -> {
                toggleSeismicSheet(true)
                _uiState.update { it.copy(actionBannerMessage = "VOICE: \"$cleaned\" → Opened Live Seismic Monitor") }
            }
            lower.contains("worldometer") || lower.contains("population") || lower.contains("births") || lower.contains("global stats") -> {
                toggleWorldometersSheet(true)
                _uiState.update { it.copy(actionBannerMessage = "VOICE: \"$cleaned\" → Opened Live Worldometers") }
            }
            lower.contains("satellite tracking") || lower.contains("satellite telemetry") || lower.contains("orbital telemetry") -> {
                openTelemetryDashboard(TelemetryDashboardTab.SATELLITE)
                _uiState.update { it.copy(actionBannerMessage = "VOICE: \"$cleaned\" → Orbital Satellite Telemetry") }
            }
            lower.contains("aircraft telemetry") || lower.contains("flight telemetry") || lower.contains("telemetry dashboard") -> {
                openTelemetryDashboard(TelemetryDashboardTab.AIRCRAFT)
                _uiState.update { it.copy(actionBannerMessage = "VOICE: \"$cleaned\" → Live Aircraft Telemetry") }
            }
            lower.contains("hide telemetry") -> {
                toggleTelemetryDashboardVisible(false)
                _uiState.update { it.copy(actionBannerMessage = "VOICE: Hid Telemetry HUD") }
            }
            lower.contains("show telemetry") -> {
                toggleTelemetryDashboardVisible(true)
                _uiState.update { it.copy(actionBannerMessage = "VOICE: Showing Telemetry HUD") }
            }
            lower.contains("cctv") || lower.contains("camera wall") || lower.contains("surveillance") -> {
                toggleCctvWall(true)
                _uiState.update { it.copy(actionBannerMessage = "VOICE: \"$cleaned\" → Opened Live CCTV Wall") }
            }
            lower.contains("donate") || lower.contains("paybill") || lower.contains("mpesa") -> {
                toggleDonationsSheet(true)
                _uiState.update { it.copy(actionBannerMessage = "VOICE: \"$cleaned\" → Opened M-Pesa 522522") }
            }
            lower.contains("3d building") || lower.contains("buildings") || lower.contains("real scene") -> {
                setViewportMode(ViewportMode.SAT_3D_BUILDINGS)
                _uiState.update { it.copy(actionBannerMessage = "VOICE: \"$cleaned\" → Switched to 3D Buildings") }
            }
            lower.contains("ar ") || lower.contains("augmented") || lower.contains("camera hud") -> {
                setViewportMode(ViewportMode.AR_SKY_HUD)
                _uiState.update { it.copy(actionBannerMessage = "VOICE: \"$cleaned\" → Switched to AR Camera") }
            }
            lower.contains("navigate") || lower.contains("compass") || lower.contains("route") -> {
                toggleNavigationSheet(true)
                _uiState.update { it.copy(actionBannerMessage = "VOICE: \"$cleaned\" → Opened Navigation & Compass") }
            }
            lower.contains("clean hud") || lower.contains("hide hud") || lower.contains("declutter") -> {
                toggleCleanHudMode()
            }
            lower.contains("mute") || lower.contains("unmute") || lower.contains("sound") -> {
                toggleSound()
            }
            lower.contains("three") || lower.contains("hangar") || lower.contains("model") -> {
                setViewportMode(ViewportMode.THREEJS_HANGAR)
                _uiState.update { it.copy(actionBannerMessage = "VOICE: \"$cleaned\" → Switched to Three.js 3D") }
            }
            lower.contains("google map") || lower.contains("satellite map") -> {
                setViewportMode(ViewportMode.GOOGLE_MAPS)
                _uiState.update { it.copy(actionBannerMessage = "VOICE: \"$cleaned\" → Switched to Google Maps") }
            }
            lower.contains("globe") || lower.contains("earth") -> {
                setViewportMode(ViewportMode.GLOBE_3D)
                _uiState.update { it.copy(actionBannerMessage = "VOICE: \"$cleaned\" → Switched to 3D Globe") }
            }
            lower.contains("iss") || lower.contains("space station") -> {
                val iss = _uiState.value.satellites.firstOrNull { it.element.name.contains("ISS", true) }
                    ?: _uiState.value.satellites.firstOrNull()
                if (iss != null) {
                    selectTarget(SelectedTarget.Satellite(iss))
                    _uiState.update { it.copy(actionBannerMessage = "VOICE: Locked orbital track ${iss.element.name}") }
                }
            }
            lower.contains("exit chase") || lower.contains("leave chase") -> {
                exitChaseCam()
                _uiState.update { it.copy(actionBannerMessage = "VOICE: Exited 3D Chase Cam") }
            }
            lower.contains("chase") || lower.contains("cockpit") || lower.contains("ride along") -> {
                engageChaseCam()
                _uiState.update { it.copy(actionBannerMessage = "VOICE: Engaging 3D Chase Cam") }
            }
            matchedOptic != null || matchedTheaterId != null -> {
                val summary = listOfNotNull(matchedOptic?.displayName, matchedTheaterId).joinToString(" • ")
                _uiState.update { it.copy(actionBannerMessage = "VOICE: \"$cleaned\" → $summary") }
            }
            else -> {
                toggleChatSheet(true)
                sendGeminiChat(cleaned)
                _uiState.update { it.copy(actionBannerMessage = "VOICE → Gemini Analyst: \"$cleaned\"") }
            }
        }
    }

    fun setAiModel(model: CallidusAiModel) {
        _uiState.update { it.copy(selectedAiModel = model) }
    }

    fun sendGeminiChat(prompt: String) {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty() || _uiState.value.isAiSending) return

        val stateSnapshot = _uiState.value
        val modelChoice = stateSnapshot.selectedAiModel
        val targetLat = stateSnapshot.camera.centerLat
        val targetLon = stateSnapshot.camera.centerLon
        val contextStr = buildLiveTelemetryContext(stateSnapshot)

        chatJob?.cancel()
        chatJob = viewModelScope.launch {
            _uiState.update { it.copy(isAiSending = true, streamingChatReply = "", aiError = null) }

            if (currentUserId != null) {
                firestoreRepo.saveIntelMessage(
                    role = "user",
                    content = trimmed,
                    modelId = modelChoice.modelId,
                    targetLat = targetLat,
                    targetLon = targetLon
                )
            } else {
                val localUserMsg = IntelChatMessage(
                    id = "local_u_${System.nanoTime()}",
                    userId = "guest",
                    role = "user",
                    content = trimmed,
                    modelId = modelChoice.modelId,
                    targetLat = targetLat,
                    targetLon = targetLon
                )
                _uiState.update { it.copy(chatMessages = it.chatMessages + localUserMsg) }
            }

            aiRepo.streamChatTurn(
                modelChoice = modelChoice,
                historyMessages = stateSnapshot.chatMessages,
                liveTelemetryContext = contextStr,
                userPrompt = trimmed
            ).collect { chunk ->
                when (chunk) {
                    is AiStreamChunk.Delta -> {
                        _uiState.update { it.copy(streamingChatReply = chunk.accumulatedText) }
                    }
                    is AiStreamChunk.Completed -> {
                        _uiState.update {
                            it.copy(
                                isAiSending = false,
                                streamingChatReply = null,
                                aiError = null
                            )
                        }
                        if (currentUserId != null) {
                            firestoreRepo.saveIntelMessage(
                                role = "model",
                                content = chunk.finalText,
                                modelId = modelChoice.modelId,
                                targetLat = targetLat,
                                targetLon = targetLon
                            )
                        } else {
                            val localModelMsg = IntelChatMessage(
                                id = "local_m_${System.nanoTime()}",
                                userId = "guest",
                                role = "model",
                                content = chunk.finalText,
                                modelId = modelChoice.modelId,
                                targetLat = targetLat,
                                targetLon = targetLon
                            )
                            _uiState.update { it.copy(chatMessages = it.chatMessages + localModelMsg) }
                        }
                    }
                    is AiStreamChunk.Failed -> {
                        _uiState.update {
                            it.copy(
                                isAiSending = false,
                                streamingChatReply = null,
                                aiError = chunk.message
                            )
                        }
                    }
                }
            }
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            val uid = currentUserId
            if (uid != null) {
                firestoreRepo.clearIntelHistory(uid)
            }
            _uiState.update { it.copy(chatMessages = emptyList(), streamingChatReply = null, aiError = null) }
        }
    }

    fun openCoordinatesInGoogleMaps(lat: Double, lon: Double) {
        _uiState.update {
            it.copy(
                isChatOpen = false,
                isDonationsOpen = false,
                isLayerDrawerOpen = false,
                isTheatersSheetOpen = false,
                isCctvWallOpen = false,
                isWorldometersOpen = false,
                isSeismicSheetOpen = false,
                viewportMode = ViewportMode.GOOGLE_MAPS,
                camera = it.camera.copy(
                    centerLat = lat.coerceIn(-85.0, 85.0),
                    centerLon = Globe3DEngine.normalizeLon(lon),
                    chaseMode = false
                )
            )
        }
    }

    fun recordDonation(amountKes: Long, phone: String, reference: String) {
        viewModelScope.launch {
            if (currentUserId != null) {
                firestoreRepo.recordDonationPledge(amountKes, phone, reference).fold(
                    onSuccess = {
                        _uiState.update {
                            it.copy(actionBannerMessage = "Recorded KES $amountKes support for Paybill 522522 (Acct 1355643775)")
                        }
                    },
                    onFailure = { err ->
                        _uiState.update {
                            it.copy(actionBannerMessage = err.localizedMessage ?: "Failed to record donation")
                        }
                    }
                )
            } else {
                val safeRef = reference.trim().take(64)
                val localRec = DonationRecord(
                    id = "local_don_${System.nanoTime()}",
                    userId = "guest",
                    amountKes = amountKes,
                    donorPhone = phone.trim().take(24),
                    referenceCode = safeRef,
                    status = if (safeRef.length >= 6) "CONFIRMED" else "RECORDED"
                )
                _uiState.update {
                    it.copy(
                        donations = listOf(localRec) + it.donations,
                        actionBannerMessage = "Recorded KES $amountKes for Paybill 522522 (Acct 1355643775)"
                    )
                }
            }
        }
    }

    fun clearActionBanner() {
        _uiState.update { it.copy(actionBannerMessage = null) }
    }

    fun flyToBookmark(bookmark: SavedBookmark) {
        _uiState.update {
            it.copy(
                viewportMode = ViewportMode.GLOBE_3D,
                isLayerDrawerOpen = false,
                isTheatersSheetOpen = false,
                isCctvWallOpen = false,
                isWorldometersOpen = false,
                isSeismicSheetOpen = false,
                isChatOpen = false,
                isDonationsOpen = false
            )
        }
        flyTo3D(
            targetLat = bookmark.lat,
            targetLon = bookmark.lon,
            targetZoom = 4.2f,
            targetPitch = 44f,
            refreshRegionalAfter = true
        )
    }

    fun signOut(credentialManager: CredentialManager, onSignOutComplete: () -> Unit = {}) {
        flightJob?.cancel()
        chatJob?.cancel()
        signOutUser(credentialManager, viewModelScope, onSignOutComplete)
    }

    private fun buildLiveTelemetryContext(state: CallidusUiState): String = buildString {
        appendLine(
            String.format(
                Locale.US,
                "Viewport Center: lat=%.4f, lon=%.4f, zoom=%.2fx, pitch=%.0f deg, theater=%s",
                state.camera.centerLat,
                state.camera.centerLon,
                state.camera.zoom,
                state.camera.pitchDeg,
                state.activeTheaterId
            )
        )
        when (val sel = state.selectedTarget) {
            is SelectedTarget.Flight -> appendLine("Locked Flight: ${sel.data.callsign} (ICAO ${sel.data.icao24}) at ${sel.data.altitudeFeet}ft, ${sel.data.groundSpeedKnots}kts, hdg ${sel.data.headingDeg.toInt()} deg (${sel.data.lat}, ${sel.data.lon})")
            is SelectedTarget.Satellite -> appendLine("Locked Satellite: ${sel.data.element.name} NORAD ${sel.data.element.noradId} at ${sel.data.altitudeKm.toInt()}km (${sel.data.lat}, ${sel.data.lon})")
            is SelectedTarget.Cctv -> appendLine("Locked CCTV: ${sel.data.name} in ${sel.data.city} azimuth=${sel.data.azimuthDeg.toInt()} deg FOV=${sel.data.fovDeg.toInt()} deg (${sel.data.lat}, ${sel.data.lon})")
            is SelectedTarget.Vessel -> appendLine("Locked Vessel: ${sel.data.name} MMSI ${sel.data.mmsi} speed=${sel.data.speedKnots}kts hdg=${sel.data.headingDeg.toInt()} deg (${sel.data.lat}, ${sel.data.lon})")
            is SelectedTarget.Quake -> appendLine("Locked Quake: M${sel.data.magnitude} ${sel.data.place} depth ${sel.data.depthKm}km (${sel.data.lat}, ${sel.data.lon})")
            is SelectedTarget.Event -> appendLine("Locked Event: ${sel.data.title} (${sel.data.category}) at (${sel.data.lat}, ${sel.data.lon})")
            SelectedTarget.None -> Unit
        }
        appendLine("Space Weather: Kp=${state.spaceWeather.kpIndex} (${state.spaceWeather.stormScale})")
        state.localWeather?.let { wx ->
            appendLine("Regional Weather: ${wx.temperatureC}C, wind ${wx.windSpeedKmh}km/h, cloud ${wx.cloudCoverPct}%, pressure ${wx.pressureHpa}hPa")
        }
        appendLine("Live Counts: aircraft=${state.aircraft.size}, satellites=${state.satellites.size}, cctv=${state.cctvCameras.size}, ships=${state.maritimeVessels.size}, roadVectors=${state.trafficSegments.size}, earthquakes=${state.earthquakes.size}, naturalEvents=${state.naturalEvents.size}")
        if (state.aircraft.isNotEmpty()) {
            appendLine("Sample Aircraft: " + state.aircraft.take(5).joinToString("; ") { "${it.callsign} ${it.altitudeFeet}ft" })
        }
        if (state.earthquakes.isNotEmpty()) {
            appendLine("Seismic Listener (${state.seismicStream.status.label}): " + state.earthquakes.take(5).joinToString("; ") { "M${it.magnitude} ${it.place} [${it.sourceNetwork}]" })
        }
        if (state.worldometersStats.isNotEmpty()) {
            appendLine("Live Worldometers.info Counters: " + state.worldometersStats.take(10).joinToString("; ") { "${it.label}=${it.value}" })
        }
    }

    fun onGlobeDrag(deltaX: Float, deltaY: Float, viewportMinDim: Float) {
        flightJob?.cancel()
        val cam = _uiState.value.camera
        if (cam.chaseMode) {
            val nextYaw = (cam.chaseLookYawDeg + deltaX * 0.22f).coerceIn(-85f, 85f)
            val nextPitch = (cam.chaseLookPitchDeg - deltaY * 0.16f).coerceIn(-35f, 35f)
            _uiState.update {
                it.copy(camera = it.camera.copy(chaseLookYawDeg = nextYaw, chaseLookPitchDeg = nextPitch))
            }
            return
        }

        val sensitivity = 120.0 / (viewportMinDim.coerceAtLeast(300f) * cam.zoom.coerceAtLeast(0.8f))
        val dLon = -deltaX * sensitivity
        val dLat = deltaY * sensitivity

        velLon = dLon * 0.65
        velLat = dLat * 0.65

        val nextLon = Globe3DEngine.normalizeLon(cam.centerLon + dLon)
        val nextLat = (cam.centerLat + dLat).coerceIn(-82.0, 82.0)
        val bankRoll = (dLon * 2.4).toFloat().coerceIn(-14f, 14f)

        _uiState.update {
            it.copy(
                camera = it.camera.copy(
                    centerLat = nextLat,
                    centerLon = nextLon,
                    rollDeg = bankRoll
                )
            )
        }
    }

    fun onGlobePinchAndTilt(zoomFactor: Float, twoFingerPanY: Float) {
        val cam = _uiState.value.camera
        if (cam.chaseMode) return
        val nextZoom = (cam.zoom * zoomFactor).coerceIn(0.85f, 24.0f)
        val nextPitch = (cam.pitchDeg - twoFingerPanY * 0.18f).coerceIn(0f, 68f)
        _uiState.update {
            it.copy(camera = it.camera.copy(zoom = nextZoom, pitchDeg = nextPitch))
        }
    }

    fun setCameraPitch(pitchDeg: Float) {
        _uiState.update {
            it.copy(camera = it.camera.copy(pitchDeg = pitchDeg.coerceIn(0f, 68f)))
        }
    }

    fun setCameraZoom(zoom: Float) {
        _uiState.update {
            it.copy(camera = it.camera.copy(zoom = zoom.coerceIn(0.85f, 24.0f)))
        }
    }

    fun toggleAutoRotate() {
        _uiState.update { it.copy(autoRotateGlobe = !it.autoRotateGlobe) }
    }

    fun setOpticMode(mode: OpticMode) {
        audioEngine.playOpticClick()
        _uiState.update { it.copy(opticMode = mode) }
        viewModelScope.launch {
            preferences.saveOpticMode(mode)
            if (currentUserId != null) {
                firestoreRepo.updateOpticPreference(mode.name)
            }
        }
    }

    fun setImageryStyle(style: GlobeImageryStyle) {
        _uiState.update {
            it.copy(
                imageryStyle = style,
                actionBannerMessage = "3D BASEMAP: ${style.shortTitle.uppercase(Locale.US)}"
            )
        }
        viewModelScope.launch { preferences.saveImageryStyle(style) }
    }

    fun setImageryOpacity(opacity: Float) {
        val clamped = opacity.coerceIn(0.15f, 1.0f)
        _uiState.update { it.copy(imageryOpacity = clamped) }
        viewModelScope.launch { preferences.saveImageryOpacity(clamped) }
    }

    fun toggleGraticuleGrid() {
        val next = !_uiState.value.showGraticuleGrid
        _uiState.update { it.copy(showGraticuleGrid = next) }
        viewModelScope.launch { preferences.saveShowGraticule(next) }
    }

    fun toggleAtmosphereGlow() {
        val next = !_uiState.value.showAtmosphereGlow
        _uiState.update { it.copy(showAtmosphereGlow = next) }
        viewModelScope.launch { preferences.saveShowAtmosphere(next) }
    }

    fun toggleLayer(layer: IntelLayer) {
        val updated = _uiState.value.enabledLayers.let { current ->
            if (layer in current) current - layer else current + layer
        }
        _uiState.update { it.copy(enabledLayers = updated) }
        viewModelScope.launch { preferences.saveEnabledLayers(updated) }
    }

    fun toggleLayerDrawer(open: Boolean? = null) {
        _uiState.update {
            it.copy(
                isLayerDrawerOpen = open ?: !it.isLayerDrawerOpen,
                isTheatersSheetOpen = false,
                isCctvWallOpen = false,
                isWorldometersOpen = false,
                isSeismicSheetOpen = false,
                isChatOpen = false,
                isDonationsOpen = false,
                isNavigationSheetOpen = false
            )
        }
    }

    fun toggleTheatersSheet(open: Boolean? = null) {
        _uiState.update {
            it.copy(
                isTheatersSheetOpen = open ?: !it.isTheatersSheetOpen,
                isLayerDrawerOpen = false,
                isCctvWallOpen = false,
                isWorldometersOpen = false,
                isSeismicSheetOpen = false,
                isChatOpen = false,
                isDonationsOpen = false,
                isNavigationSheetOpen = false
            )
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun openTelemetryDashboard(tab: TelemetryDashboardTab) {
        _uiState.update {
            it.copy(
                viewportMode = ViewportMode.GLOBE_3D,
                telemetryDashboardTab = tab,
                isTelemetryDashboardVisible = true,
                isTelemetryDashboardExpanded = true,
                isLayerDrawerOpen = false,
                isTheatersSheetOpen = false,
                isCctvWallOpen = false,
                isWorldometersOpen = false,
                isSeismicSheetOpen = false,
                isChatOpen = false,
                isDonationsOpen = false,
                isNavigationSheetOpen = false
            )
        }
    }

    fun setTelemetryDashboardTab(tab: TelemetryDashboardTab) {
        _uiState.update { it.copy(telemetryDashboardTab = tab) }
    }

    fun toggleTelemetryDashboardExpanded() {
        _uiState.update { it.copy(isTelemetryDashboardExpanded = !it.isTelemetryDashboardExpanded) }
    }

    fun toggleTelemetryDashboardVisible(visible: Boolean? = null) {
        _uiState.update { it.copy(isTelemetryDashboardVisible = visible ?: !it.isTelemetryDashboardVisible) }
    }

    fun cycleTrackedAircraft(step: Int) {
        val state = _uiState.value
        val flights = state.aircraft
        if (flights.isEmpty()) return
        val current = TelemetryCalculator.resolveTrackedAircraft(
            aircraft = flights,
            selectedTarget = state.selectedTarget,
            trackedIcao = state.trackedAircraftIcao,
            centerLat = state.camera.centerLat,
            centerLon = state.camera.centerLon
        )
        val curIdx = current?.let { c -> flights.indexOfFirst { it.icao24 == c.icao24 } } ?: -1
        val nextIdx = if (curIdx < 0) 0 else ((curIdx + step) % flights.size + flights.size) % flights.size
        val nextFlight = flights[nextIdx]
        _uiState.update {
            it.copy(
                trackedAircraftIcao = nextFlight.icao24,
                telemetryDashboardTab = TelemetryDashboardTab.AIRCRAFT,
                selectedTarget = if (it.selectedTarget is SelectedTarget.Flight) {
                    SelectedTarget.Flight(nextFlight)
                } else {
                    it.selectedTarget
                }
            )
        }
    }

    fun cycleTrackedSatellite(step: Int) {
        val state = _uiState.value
        val sats = state.satellites
        if (sats.isEmpty()) return
        val current = TelemetryCalculator.resolveTrackedSatellite(
            satellites = sats,
            selectedTarget = state.selectedTarget,
            trackedNorad = state.trackedSatelliteNorad,
            centerLat = state.camera.centerLat,
            centerLon = state.camera.centerLon
        )
        val curIdx = current?.let { c -> sats.indexOfFirst { it.element.noradId == c.element.noradId } } ?: -1
        val nextIdx = if (curIdx < 0) 0 else ((curIdx + step) % sats.size + sats.size) % sats.size
        val nextSat = sats[nextIdx]
        _uiState.update {
            it.copy(
                trackedSatelliteNorad = nextSat.element.noradId,
                telemetryDashboardTab = TelemetryDashboardTab.SATELLITE,
                selectedTarget = if (it.selectedTarget is SelectedTarget.Satellite) {
                    SelectedTarget.Satellite(nextSat)
                } else {
                    it.selectedTarget
                }
            )
        }
    }

    fun togglePinTarget(targetId: String) {
        val currentSel = _uiState.value.selectedTarget
        val wasPinned = targetId in _uiState.value.pinnedTargetIds
        _uiState.update { state ->
            val updated = if (wasPinned) state.pinnedTargetIds - targetId else state.pinnedTargetIds + targetId
            state.copy(pinnedTargetIds = updated)
        }
        viewModelScope.launch {
            preferences.togglePinnedTarget(targetId)
            if (currentUserId != null) {
                if (wasPinned) {
                    val safeId = targetId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120)
                    firestoreRepo.removeBookmark(safeId)
                } else if (currentSel !is SelectedTarget.None && currentSel.targetId == targetId) {
                    when (currentSel) {
                        is SelectedTarget.Flight -> firestoreRepo.saveBookmark(
                            targetType = "FLIGHT",
                            targetKey = targetId,
                            title = currentSel.data.callsign,
                            subtitle = "ICAO ${currentSel.data.icao24} • ${currentSel.data.altitudeFeet}ft",
                            lat = currentSel.data.lat,
                            lon = currentSel.data.lon
                        )
                        is SelectedTarget.Satellite -> firestoreRepo.saveBookmark(
                            targetType = "SATELLITE",
                            targetKey = targetId,
                            title = currentSel.data.element.name,
                            subtitle = "NORAD ${currentSel.data.element.noradId}",
                            lat = currentSel.data.lat,
                            lon = currentSel.data.lon
                        )
                        is SelectedTarget.Cctv -> firestoreRepo.saveBookmark(
                            targetType = "CCTV",
                            targetKey = targetId,
                            title = currentSel.data.name,
                            subtitle = "${currentSel.data.city} • Az ${currentSel.data.azimuthDeg.toInt()}°",
                            lat = currentSel.data.lat,
                            lon = currentSel.data.lon
                        )
                        is SelectedTarget.Vessel -> firestoreRepo.saveBookmark(
                            targetType = "VESSEL",
                            targetKey = targetId,
                            title = currentSel.data.name,
                            subtitle = "MMSI ${currentSel.data.mmsi} • ${String.format(Locale.US, "%.1f", currentSel.data.speedKnots)} kts",
                            lat = currentSel.data.lat,
                            lon = currentSel.data.lon
                        )
                        is SelectedTarget.Quake -> firestoreRepo.saveBookmark(
                            targetType = "QUAKE",
                            targetKey = targetId,
                            title = "M${String.format(Locale.US, "%.1f", currentSel.data.magnitude)} ${currentSel.data.place}",
                            subtitle = "Depth ${currentSel.data.depthKm.toInt()}km",
                            lat = currentSel.data.lat,
                            lon = currentSel.data.lon
                        )
                        is SelectedTarget.Event -> firestoreRepo.saveBookmark(
                            targetType = "EVENT",
                            targetKey = targetId,
                            title = currentSel.data.title,
                            subtitle = currentSel.data.category,
                            lat = currentSel.data.lat,
                            lon = currentSel.data.lon
                        )
                        SelectedTarget.None -> Unit
                    }
                }
            }
        }
    }

    fun selectTheater(theater: TheaterPreset) {
        _uiState.update {
            it.copy(
                activeTheaterId = theater.id,
                isTheatersSheetOpen = false,
                isCctvWallOpen = false,
                isWorldometersOpen = false,
                isSeismicSheetOpen = false,
                selectedTarget = SelectedTarget.None
            )
        }
        flyTo3D(
            targetLat = theater.lat,
            targetLon = theater.lon,
            targetZoom = theater.zoom,
            targetPitch = theater.pitchDeg,
            refreshRegionalAfter = true
        )
    }

    fun selectTarget(target: SelectedTarget) {
        if (target !is SelectedTarget.None) {
            audioEngine.playTargetLock()
        }
        _uiState.update {
            it.copy(
                viewportMode = ViewportMode.GLOBE_3D,
                selectedTarget = target,
                telemetryDashboardTab = when (target) {
                    is SelectedTarget.Flight -> TelemetryDashboardTab.AIRCRAFT
                    is SelectedTarget.Satellite -> TelemetryDashboardTab.SATELLITE
                    else -> it.telemetryDashboardTab
                },
                trackedAircraftIcao = if (target is SelectedTarget.Flight) target.data.icao24 else it.trackedAircraftIcao,
                trackedSatelliteNorad = if (target is SelectedTarget.Satellite) target.data.element.noradId else it.trackedSatelliteNorad,
                isLayerDrawerOpen = false,
                isTheatersSheetOpen = false,
                isCctvWallOpen = false,
                isWorldometersOpen = false,
                isSeismicSheetOpen = false,
                isNavigationSheetOpen = false
            )
        }
        when (target) {
            SelectedTarget.None -> Unit
            is SelectedTarget.Flight -> flyTo3D(target.data.lat, target.data.lon, 5.2f, 52f)
            is SelectedTarget.Satellite -> flyTo3D(target.data.lat, target.data.lon, 2.1f, 38f)
            is SelectedTarget.Cctv -> flyTo3D(target.data.lat, target.data.lon, 5.6f, 52f, refreshRegionalAfter = true)
            is SelectedTarget.Vessel -> flyTo3D(target.data.lat, target.data.lon, 5.0f, 48f)
            is SelectedTarget.Quake -> flyTo3D(target.data.lat, target.data.lon, 4.4f, 46f)
            is SelectedTarget.Event -> flyTo3D(target.data.lat, target.data.lon, 4.0f, 44f)
        }
    }

    fun clearSelection() {
        _uiState.update {
            it.copy(
                selectedTarget = SelectedTarget.None,
                camera = it.camera.copy(chaseMode = false, chaseLookYawDeg = 0f, chaseLookPitchDeg = -6f)
            )
        }
    }

    fun engageChaseCam() {
        val sel = _uiState.value.selectedTarget
        if (sel is SelectedTarget.Flight || sel is SelectedTarget.Satellite ||
            sel is SelectedTarget.Cctv || sel is SelectedTarget.Vessel
        ) {
            lastAutoRotateEnabled = _uiState.value.autoRotateGlobe
            _uiState.update {
                it.copy(
                    autoRotateGlobe = false,
                    camera = it.camera.copy(
                        chaseMode = true,
                        chaseLookYawDeg = 0f,
                        chaseLookPitchDeg = -6f
                    )
                )
            }
        }
    }

    fun exitChaseCam() {
        _uiState.update {
            it.copy(
                autoRotateGlobe = lastAutoRotateEnabled,
                camera = it.camera.copy(
                    chaseMode = false,
                    chaseLookYawDeg = 0f,
                    chaseLookPitchDeg = -6f,
                    rollDeg = 0f
                )
            )
        }
    }

    /**
     * Cinematic 3D parabolic camera flight:
     * Lifts out into mid-orbit arc, banks roll smoothly into the turn,
     * slerps across shortest longitude, and settles into oblique 3D perspective.
     */
    fun flyTo3D(
        targetLat: Double,
        targetLon: Double,
        targetZoom: Float,
        targetPitch: Float,
        refreshRegionalAfter: Boolean = false
    ) {
        flightJob?.cancel()
        velLon = 0.0
        velLat = 0.0

        flightJob = viewModelScope.launch {
            val start = _uiState.value.camera.copy(chaseMode = false)
            val dLat = targetLat - start.centerLat
            val dLon = Globe3DEngine.shortestLonDelta(start.centerLon, targetLon)
            val distDeg = kotlin.math.sqrt(dLat * dLat + dLon * dLon)
            val midZoomDip = (minOf(start.zoom, targetZoom) * (1f - (distDeg / 240.0).toFloat().coerceIn(0f, 0.45f)))
                .coerceAtLeast(0.95f)
            val turnBankSign = if (dLon >= 0) 1f else -1f
            val maxBank = (distDeg / 8.0).toFloat().coerceIn(4f, 18f) * turnBankSign

            val steps = 44
            for (i in 1..steps) {
                val t = i.toFloat() / steps
                val ease = t * t * t * (t * (t * 6f - 15f) + 10f)
                val arcBell = sin(t * Math.PI).toFloat()

                val curLat = start.centerLat + dLat * ease
                val curLon = Globe3DEngine.normalizeLon(start.centerLon + dLon * ease)
                val linearZoom = start.zoom + (targetZoom - start.zoom) * ease
                val curZoom = linearZoom * (1f - 0.25f * arcBell) + midZoomDip * (0.25f * arcBell)
                val curPitch = start.pitchDeg + (targetPitch - start.pitchDeg) * ease
                val curRoll = maxBank * arcBell

                _uiState.update { s ->
                    s.copy(
                        camera = s.camera.copy(
                            centerLat = curLat,
                            centerLon = curLon,
                            zoom = curZoom.coerceIn(0.85f, 24f),
                            pitchDeg = curPitch.coerceIn(0f, 68f),
                            rollDeg = curRoll,
                            chaseMode = false
                        )
                    )
                }
                delay(18L)
            }

            if (refreshRegionalAfter) {
                refreshRegionalTheaterTelemetry()
            }
        }
    }

    fun toggleSound() {
        val next = !_uiState.value.isSoundEnabled
        audioEngine.isEnabled = next
        _uiState.update {
            it.copy(
                isSoundEnabled = next,
                actionBannerMessage = if (next) "OPS-ROOM AUDIO SYNTH: ONLINE" else "OPS-ROOM AUDIO: MUTED"
            )
        }
        if (next) audioEngine.playRadarPing()
    }

    fun toggleCleanHudMode() {
        _uiState.update { it.copy(isCleanHudMode = !it.isCleanHudMode) }
    }

    fun setArTargetFilter(filter: ArTargetFilter) {
        _uiState.update { it.copy(arTargetFilter = filter) }
    }

    fun updateObserverLocation(lat: Double, lon: Double) {
        _uiState.update { it.copy(observerLat = lat, observerLon = lon) }
    }

    fun toggleNavigationSheet(open: Boolean? = null) {
        _uiState.update {
            val next = open ?: !it.isNavigationSheetOpen
            it.copy(
                isNavigationSheetOpen = next,
                isLayerDrawerOpen = false,
                isTheatersSheetOpen = false,
                isCctvWallOpen = false,
                isWorldometersOpen = false,
                isSeismicSheetOpen = false,
                isChatOpen = false,
                isDonationsOpen = false
            )
        }
    }

    fun setNavTravelMode(mode: NavigationTravelMode) {
        _uiState.update { it.copy(navTravelMode = mode) }
        val existing = _uiState.value.activeRoute ?: return
        navigateToDestination(
            destLat = existing.destinationLat,
            destLon = existing.destinationLon,
            destLabel = existing.destinationLabel,
            originLat = existing.originLat,
            originLon = existing.originLon
        )
    }

    fun searchNavigationPlaces(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            _uiState.update { it.copy(geocodeResults = emptyList()) }
            return
        }
        viewModelScope.launch {
            repository.searchPlaces(trimmed).fold(
                onSuccess = { results ->
                    _uiState.update {
                        it.copy(
                            geocodeResults = results,
                            actionBannerMessage = if (results.isEmpty()) "No matching places found (Offline compass still ready)" else null
                        )
                    }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            geocodeResults = emptyList(),
                            actionBannerMessage = "Geocoder offline — use Offline Compass or GPS pin drop"
                        )
                    }
                }
            )
        }
    }

    fun navigateToDestination(
        destLat: Double,
        destLon: Double,
        destLabel: String,
        originLat: Double? = null,
        originLon: Double? = null
    ) {
        val snap = _uiState.value
        val startLat = originLat ?: snap.observerLat ?: snap.camera.centerLat
        val startLon = originLon ?: snap.observerLon ?: snap.camera.centerLon
        val originLabel = if (originLat != null || snap.observerLat != null) "Current GPS" else "Camera Crosshair"
        val mode = snap.navTravelMode

        _uiState.update { it.copy(isRouteLoading = true, routeError = null) }
        viewModelScope.launch {
            val route = repository.computeNavigationRoute(
                originLat = startLat,
                originLon = startLon,
                originLabel = originLabel,
                destLat = destLat,
                destLon = destLon,
                destLabel = destLabel,
                mode = mode
            )
            audioEngine.playTargetLock()
            _uiState.update {
                it.copy(
                    activeRoute = route,
                    activeNavStepIndex = 0,
                    isNavigatingLive = true,
                    isRouteLoading = false,
                    routeError = null,
                    actionBannerMessage = "ROUTE LOCKED → $destLabel (${route.sourceLabel})"
                )
            }
            if (_uiState.value.viewportMode == ViewportMode.GLOBE_3D) {
                flyTo3D(destLat, destLon, 4.4f, 48f, refreshRegionalAfter = true)
            }
        }
    }

    fun clearNavigationRoute() {
        _uiState.update {
            it.copy(
                activeRoute = null,
                activeNavStepIndex = 0,
                isNavigatingLive = false,
                routeError = null
            )
        }
    }

    fun flyToLandmark(landmark: LandmarkNode) {
        audioEngine.playTargetLock()
        _uiState.update {
            it.copy(
                camera = it.camera.copy(
                    centerLat = landmark.lat,
                    centerLon = landmark.lon,
                    zoom = landmark.zoom3D,
                    pitchDeg = landmark.pitch3D,
                    chaseMode = false
                ),
                actionBannerMessage = "LOCKED 3D SCENE: ${landmark.name.uppercase(Locale.US)}"
            )
        }
        scanAmenitiesAt(landmark.lat, landmark.lon)
    }

    fun setAmenityCategory(category: AmenityCategory) {
        val cam = _uiState.value.camera
        _uiState.update { it.copy(selectedAmenityCategory = category) }
        scanAmenitiesAt(cam.centerLat, cam.centerLon)
    }

    fun scanAmenitiesAt(lat: Double, lon: Double) {
        val cat = _uiState.value.selectedAmenityCategory
        _uiState.update { it.copy(isLoadingAmenities = true) }
        viewModelScope.launch {
            repository.fetchMapAmenities(lat, lon, cat).fold(
                onSuccess = { list ->
                    _uiState.update {
                        it.copy(
                            mapAmenities = list,
                            isLoadingAmenities = false,
                            actionBannerMessage = if (list.isNotEmpty()) {
                                "AMENITIES: ${list.size} ${cat.shortCode} spots indexed"
                            } else {
                                "No ${cat.shortCode} amenities within 3.5 km"
                            }
                        )
                    }
                },
                onFailure = {
                    _uiState.update { it.copy(isLoadingAmenities = false) }
                }
            )
        }
    }

    fun selectAmenity(amenity: MapAmenity?) {
        _uiState.update { it.copy(selectedAmenity = amenity) }
        if (amenity != null) audioEngine.playOpticClick()
    }

    fun centerGlobeOnGps(lat: Double, lon: Double) {
        updateObserverLocation(lat, lon)
        _uiState.update {
            it.copy(
                camera = it.camera.copy(
                    centerLat = lat,
                    centerLon = lon,
                    zoom = it.camera.zoom.coerceAtLeast(6.0f)
                )
            )
        }
    }
}

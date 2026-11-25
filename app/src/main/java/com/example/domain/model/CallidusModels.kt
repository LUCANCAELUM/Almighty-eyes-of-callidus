package com.example.domain.model

import com.example.data.firebase.CallidusAiModel
import com.example.data.firebase.DonationRecord
import com.example.data.firebase.IntelChatMessage
import com.example.data.firebase.SavedBookmark
import com.example.ui.theme.ArcticSnowPalette
import com.example.ui.theme.CrtMonitorPalette
import com.example.ui.theme.EoTacticalPalette
import com.example.ui.theme.FlirThermalPalette
import com.example.ui.theme.NoirSurveillancePalette
import com.example.ui.theme.NvgPhosphorPalette
import com.example.ui.theme.OpticPalette
import com.example.ui.theme.SolarTerminatorPalette
import com.example.ui.theme.WireframeHoloPalette

enum class ViewportMode(val label: String) {
    GLOBE_3D("3D GLOBE"),
    SAT_3D_BUILDINGS("3D BUILDINGS"),
    AR_SKY_HUD("AR CAMERA"),
    GOOGLE_MAPS("GOOGLE SAT"),
    THREEJS_HANGAR("THREE.JS 3D")
}

enum class TelemetryDashboardTab(val label: String) {
    AIRCRAFT("AIRCRAFT"),
    SATELLITE("SATELLITE")
}

enum class ArTargetFilter(val label: String) {
    ALL("ALL TARGETS"),
    AIRCRAFT("AIRCRAFT"),
    SATELLITES("SATELLITES")
}

enum class NavigationTravelMode(val osrmProfile: String, val label: String) {
    DRIVE("driving", "DRIVE"),
    WALK("foot", "WALK"),
    BIKE("bike", "BIKE"),
    AIR_DIRECT("air", "AIR ROUTE")
}

enum class GlobeImageryStyle(
    val id: String,
    val label: String,
    val shortTitle: String,
    val subtitle: String,
    val tileUrlTemplate: String
) {
    STANDARD(
        id = "standard",
        label = "STANDARD",
        shortTitle = "Standard Vector",
        subtitle = "ArcGIS World Street & Boundaries",
        tileUrlTemplate = "https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/{z}/{y}/{x}"
    ),
    SATELLITE(
        id = "satellite",
        label = "SATELLITE",
        shortTitle = "Satellite Imagery",
        subtitle = "Esri High-Res Orbital Ortho",
        tileUrlTemplate = "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"
    ),
    TERRAIN(
        id = "terrain",
        label = "TERRAIN",
        shortTitle = "Terrain Relief",
        subtitle = "Esri World Physical & Topo",
        tileUrlTemplate = "https://server.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer/tile/{z}/{y}/{x}"
    )
}

enum class OpticMode(
    val code: String,
    val displayName: String,
    val shortDesc: String,
    val palette: OpticPalette
) {
    EO_TACTICAL("EO-01", "CALLIDUS EO", "Electro-Optical Obsidian", EoTacticalPalette),
    FLIR_THERMAL("IR-02", "FLIR THERMAL", "Infrared Heat Spectrum", FlirThermalPalette),
    NVG_PHOSPHOR("NV-03", "NVG PHOSPHOR", "Gen-4 Night Phosphor", NvgPhosphorPalette),
    CRT_MONITOR("CR-04", "CRT OPS-ROOM", "Phosphor Scanline CRT", CrtMonitorPalette),
    NOIR_SURVEILLANCE("NR-05", "NOIR RECON", "Monochrome Silver-Halide", NoirSurveillancePalette),
    ARCTIC_SNOW("SN-06", "ARCTIC SNOW", "Alpine Elevation Contrast", ArcticSnowPalette),
    WIREFRAME_HOLO("WF-07", "HOLO MESH", "Orbital Vector Lattice", WireframeHoloPalette),
    SOLAR_TERMINATOR("SL-08", "SOLAR CHRONO", "Live Dawn/Dusk Flux", SolarTerminatorPalette)
}

enum class IntelLayer(
    val id: String,
    val title: String,
    val sourceLabel: String
) {
    AIRCRAFT_ADSB("adsb", "Live ADS-B Flights", "airplanes.live / OpenSky"),
    ORBITAL_SATS("orbits", "Orbital Satellites", "CelesTrak GP + ISS"),
    CCTV_CAMERAS("cctv", "Global Live CCTV & Video", "TfL + NYC + Caltrans + SG + Nordic + ATX"),
    MARITIME_AIS("maritime", "Live Maritime AIS Vessels", "Digitraffic Marine AIS"),
    TRAFFIC_FLOW("traffic", "Street Traffic Vectors", "OSM Overpass Live"),
    SEISMIC_USGS("seismic", "Seismic Tremors (24h)", "USGS GeoJSON"),
    NATURAL_EONET("eonet", "Fires, Storms & Volcanoes", "NASA EONET v3"),
    SUBSEA_CABLES("cables", "Subsea Fiber & Tectonics", "Global Backbone"),
    SOLAR_WEATHER("solar", "Magnetosphere & Wind", "NOAA SWPC + Open-Meteo")
}

sealed interface FeedStatus {
    data object Idle : FeedStatus
    data object Syncing : FeedStatus
    data class Live(val count: Int, val updatedAtMs: Long) : FeedStatus
    data class Error(val reason: String) : FeedStatus
}

data class GeoPoint3D(
    val lat: Double,
    val lon: Double,
    val altitudeKm: Double = 0.0
)

data class AircraftTarget(
    val icao24: String,
    val callsign: String,
    val registration: String,
    val aircraftType: String,
    val lat: Double,
    val lon: Double,
    val altitudeFeet: Int,
    val groundSpeedKnots: Int,
    val headingDeg: Float,
    val verticalRateFpm: Int,
    val squawk: String,
    val category: String
) {
    val altitudeKm: Double get() = altitudeFeet * 0.0003048
}

enum class SatelliteCategory(val label: String) {
    SPACE_STATION("SPACE STATION"),
    EARTH_OBS("EARTH OBS / OPTICS"),
    COMMS("CONSTELLATION"),
    NAVIGATION("GNSS / NAV")
}

data class SatelliteElement(
    val noradId: Int,
    val name: String,
    val category: SatelliteCategory,
    val inclinationDeg: Double,
    val raanDeg: Double,
    val eccentricity: Double,
    val argPerigeeDeg: Double,
    val meanAnomalyDeg: Double,
    val meanMotionRevPerDay: Double,
    val epochMs: Long
)

data class SatelliteTarget(
    val element: SatelliteElement,
    val lat: Double,
    val lon: Double,
    val altitudeKm: Double,
    val velocityKmS: Double,
    val groundTrack: List<GeoPoint3D>
)

data class CctvCameraTarget(
    val id: String,
    val name: String,
    val city: String,
    val region: String = "GLOBAL",
    val lat: Double,
    val lon: Double,
    val imageUrl: String,
    val videoUrl: String? = null,
    val streamType: String = "LIVE CAM",
    val azimuthDeg: Float = 45f,
    val fovDeg: Float = 56f,
    val rangeKm: Float = 1.2f,
    val status: String = "ONLINE"
)

data class MaritimeVesselTarget(
    val mmsi: Long,
    val name: String,
    val shipType: String,
    val lat: Double,
    val lon: Double,
    val speedKnots: Double,
    val headingDeg: Float
)

data class TrafficRoadSegment(
    val id: Long,
    val highwayClass: String,
    val points: List<GeoPoint3D>
)

data class EarthquakeTarget(
    val id: String,
    val title: String,
    val place: String,
    val magnitude: Double,
    val depthKm: Double,
    val lat: Double,
    val lon: Double,
    val timeMs: Long,
    val tsunamiAlert: Boolean,
    val sourceNetwork: String = "USGS",
    val isRealtimeAlert: Boolean = false
)

enum class SeismicConnectionStatus(val label: String) {
    CONNECTING("CONNECTING"),
    LIVE_STREAM("LIVE STREAM"),
    POLLING("USGS SYNC"),
    RECONNECTING("RECONNECTING")
}

data class SeismicStreamTelemetry(
    val status: SeismicConnectionStatus = SeismicConnectionStatus.CONNECTING,
    val eventsReceivedCount: Int = 0,
    val latestLiveTremor: EarthquakeTarget? = null,
    val lastPacketAtMs: Long = 0L
)

data class NaturalEventTarget(
    val id: String,
    val title: String,
    val category: String,
    val lat: Double,
    val lon: Double,
    val dateIso: String,
    val sourceLabel: String
)

data class SpaceWeatherState(
    val kpIndex: Double = 0.0,
    val stormScale: String = "G0 QUIET",
    val observedTime: String = ""
)

data class LocalWeatherTelemetry(
    val temperatureC: Double,
    val windSpeedKmh: Double,
    val windDirectionDeg: Double,
    val cloudCoverPct: Int,
    val pressureHpa: Double
)

enum class WorldometersCategory(val title: String) {
    POPULATION("WORLD POPULATION"),
    GOVERNMENT_ECON("ECONOMICS & PRODUCTION"),
    SOCIETY_MEDIA("SOCIETY & MEDIA"),
    ENVIRONMENT("ENVIRONMENT & CO2"),
    FOOD_WATER("FOOD & WATER"),
    ENERGY("GLOBAL ENERGY"),
    HEALTH("GLOBAL HEALTH")
}

data class WorldometersStat(
    val key: String,
    val label: String,
    val value: String,
    val category: WorldometersCategory
)

data class TheaterPreset(
    val id: String,
    val name: String,
    val regionCode: String,
    val lat: Double,
    val lon: Double,
    val zoom: Float,
    val pitchDeg: Float,
    val briefing: String
)

data class GeocodePlaceResult(
    val name: String,
    val displayName: String,
    val lat: Double,
    val lon: Double,
    val category: String = "PLACE"
)

data class NavigationStep(
    val instruction: String,
    val roadName: String,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val lat: Double,
    val lon: Double,
    val maneuverType: String = "turn"
)

data class NavigationRoute(
    val originLabel: String,
    val destinationLabel: String,
    val originLat: Double,
    val originLon: Double,
    val destinationLat: Double,
    val destinationLon: Double,
    val travelMode: NavigationTravelMode,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val sourceLabel: String,
    val coordinates: List<GeoPoint3D>,
    val steps: List<NavigationStep>
)

data class LandmarkNode(
    val id: String,
    val name: String,
    val city: String,
    val code: String,
    val lat: Double,
    val lon: Double,
    val zoom3D: Float = 6.2f,
    val pitch3D: Float = 56f
)

enum class AmenityCategory(
    val label: String,
    val shortCode: String,
    val overpassFilter: String,
    val colorHex: Long
) {
    ALL(
        label = "All Amenities",
        shortCode = "ALL",
        overpassFilter = "[\"amenity\"~\"hospital|clinic|pharmacy|restaurant|cafe|fuel|charging_station|police|fire_station|bank|parking\"]",
        colorHex = 0xFF00E5FF
    ),
    HOSPITAL(
        label = "Medical & Hospitals",
        shortCode = "MEDICAL",
        overpassFilter = "[\"amenity\"~\"hospital|clinic|pharmacy|doctors\"]",
        colorHex = 0xFFFF5252
    ),
    FOOD(
        label = "Dining & Cafes",
        shortCode = "FOOD",
        overpassFilter = "[\"amenity\"~\"restaurant|cafe|fast_food\"]",
        colorHex = 0xFFFFB300
    ),
    FUEL(
        label = "Fuel & EV Charge",
        shortCode = "FUEL/EV",
        overpassFilter = "[\"amenity\"~\"fuel|charging_station\"]",
        colorHex = 0xFF00FF9C
    ),
    TRANSIT(
        label = "Transit & Air",
        shortCode = "TRANSIT",
        overpassFilter = "[\"public_transport\"=\"station\"]",
        colorHex = 0xFF40C4FF
    ),
    LODGING(
        label = "Hotels & Stays",
        shortCode = "HOTELS",
        overpassFilter = "[\"tourism\"~\"hotel|hostel|motel\"]",
        colorHex = 0xFFE040FB
    ),
    SAFETY(
        label = "Police & Emergency",
        shortCode = "SAFETY",
        overpassFilter = "[\"amenity\"~\"police|fire_station|townhall\"]",
        colorHex = 0xFF64B5F6
    ),
    FINANCE_PARK(
        label = "Banks, ATM & Park",
        shortCode = "BANK/PARK",
        overpassFilter = "[\"amenity\"~\"bank|atm|parking\"]",
        colorHex = 0xFFAEE571
    )
}

data class MapAmenity(
    val id: String,
    val name: String,
    val category: AmenityCategory,
    val subType: String,
    val lat: Double,
    val lon: Double,
    val address: String = "",
    val openingHours: String = ""
)

sealed interface SelectedTarget {
    data object None : SelectedTarget
    data class Flight(val data: AircraftTarget) : SelectedTarget
    data class Satellite(val data: SatelliteTarget) : SelectedTarget
    data class Cctv(val data: CctvCameraTarget) : SelectedTarget
    data class Vessel(val data: MaritimeVesselTarget) : SelectedTarget
    data class Quake(val data: EarthquakeTarget) : SelectedTarget
    data class Event(val data: NaturalEventTarget) : SelectedTarget

    val targetId: String?
        get() = when (this) {
            None -> null
            is Flight -> "flight_${data.icao24}"
            is Satellite -> "sat_${data.element.noradId}"
            is Cctv -> "cctv_${data.id}"
            is Vessel -> "ship_${data.mmsi}"
            is Quake -> "quake_${data.id}"
            is Event -> "event_${data.id}"
        }
}

data class CameraState3D(
    val centerLat: Double = 28.0,
    val centerLon: Double = -40.0,
    val zoom: Float = 1.15f,
    val pitchDeg: Float = 26f,
    val rollDeg: Float = 0f,
    val chaseMode: Boolean = false,
    val chaseLookYawDeg: Float = 0f,
    val chaseLookPitchDeg: Float = -6f
)

data class CallidusUiState(
    val camera: CameraState3D = CameraState3D(),
    val viewportMode: ViewportMode = ViewportMode.GLOBE_3D,
    val opticMode: OpticMode = OpticMode.EO_TACTICAL,
    val imageryStyle: GlobeImageryStyle = GlobeImageryStyle.SATELLITE,
    val imageryOpacity: Float = 1.0f,
    val showGraticuleGrid: Boolean = true,
    val showAtmosphereGlow: Boolean = true,
    val enabledLayers: Set<IntelLayer> = IntelLayer.entries.toSet(),
    val aircraft: List<AircraftTarget> = emptyList(),
    val satellites: List<SatelliteTarget> = emptyList(),
    val cctvCameras: List<CctvCameraTarget> = emptyList(),
    val maritimeVessels: List<MaritimeVesselTarget> = emptyList(),
    val trafficSegments: List<TrafficRoadSegment> = emptyList(),
    val earthquakes: List<EarthquakeTarget> = emptyList(),
    val naturalEvents: List<NaturalEventTarget> = emptyList(),
    val spaceWeather: SpaceWeatherState = SpaceWeatherState(),
    val localWeather: LocalWeatherTelemetry? = null,
    val feedStatuses: Map<IntelLayer, FeedStatus> = emptyMap(),
    val selectedTarget: SelectedTarget = SelectedTarget.None,
    val pinnedTargetIds: Set<String> = emptySet(),
    val activeTheaterId: String = "global_orbit",
    val isLayerDrawerOpen: Boolean = false,
    val isTheatersSheetOpen: Boolean = false,
    val isCctvWallOpen: Boolean = false,
    val isWorldometersOpen: Boolean = false,
    val isSeismicSheetOpen: Boolean = false,
    val isChatOpen: Boolean = false,
    val isDonationsOpen: Boolean = false,
    val isNavigationSheetOpen: Boolean = false,
    val telemetryDashboardTab: TelemetryDashboardTab = TelemetryDashboardTab.AIRCRAFT,
    val isTelemetryDashboardVisible: Boolean = true,
    val isTelemetryDashboardExpanded: Boolean = true,
    val trackedAircraftIcao: String? = null,
    val trackedSatelliteNorad: Int? = null,
    val arTargetFilter: ArTargetFilter = ArTargetFilter.ALL,
    val observerLat: Double? = null,
    val observerLon: Double? = null,
    val activeRoute: NavigationRoute? = null,
    val activeNavStepIndex: Int = 0,
    val isNavigatingLive: Boolean = false,
    val isRouteLoading: Boolean = false,
    val routeError: String? = null,
    val geocodeResults: List<GeocodePlaceResult> = emptyList(),
    val navTravelMode: NavigationTravelMode = NavigationTravelMode.DRIVE,
    val isSoundEnabled: Boolean = false,
    val isCleanHudMode: Boolean = false,
    val mapAmenities: List<MapAmenity> = emptyList(),
    val selectedAmenityCategory: AmenityCategory = AmenityCategory.ALL,
    val selectedAmenity: MapAmenity? = null,
    val isLoadingAmenities: Boolean = false,
    val worldometersStats: List<WorldometersStat> = emptyList(),
    val worldometersStatus: String = "CONNECTING",
    val seismicStream: SeismicStreamTelemetry = SeismicStreamTelemetry(),
    val isVoiceListening: Boolean = false,
    val lastVoiceTranscript: String? = null,
    val selectedAiModel: CallidusAiModel = CallidusAiModel.BALANCED,
    val chatMessages: List<IntelChatMessage> = emptyList(),
    val streamingChatReply: String? = null,
    val isAiSending: Boolean = false,
    val aiError: String? = null,
    val bookmarks: List<SavedBookmark> = emptyList(),
    val donations: List<DonationRecord> = emptyList(),
    val sqlSnapshotCount: Int = 0,
    val isAuthenticated: Boolean = false,
    val operatorName: String = "Guest Operator",
    val actionBannerMessage: String? = null,
    val autoRotateGlobe: Boolean = true,
    val searchQuery: String = "",
    val bootProgress: Float = 0f
)

val GlobalLandmarks = listOf(
    LandmarkNode("tower_bridge", "Tower Bridge & Shard", "London", "LON-01", 51.5055, -0.0754),
    LandmarkNode("manhattan_mid", "Empire State & Midtown", "New York", "NYC-02", 40.7484, -73.9857),
    LandmarkNode("golden_gate", "Golden Gate Span", "San Francisco", "SFO-03", 37.8199, -122.4783),
    LandmarkNode("tokyo_skytree", "Tokyo Bay & Skytree", "Tokyo", "TYO-04", 35.7101, 139.8107),
    LandmarkNode("eiffel_tower", "Eiffel Tower & Seine", "Paris", "PAR-05", 48.8584, 2.2945),
    LandmarkNode("burj_khalifa", "Burj Khalifa Downtown", "Dubai", "DXB-06", 25.1972, 55.2744),
    LandmarkNode("kicc_nairobi", "KICC & Nairobi CBD", "Nairobi", "NBO-07", -1.2884, 36.8233),
    LandmarkNode("austin_capitol", "Texas Capitol Corridor", "Austin", "AUS-08", 30.2747, -97.7404),
    LandmarkNode("pentagon_dc", "National Mall & Arlington", "Washington DC", "WDC-09", 38.8719, -77.0563),
    LandmarkNode("ksc_pad39", "LC-39A Orbital Pad", "Cape Canaveral", "KSC-10", 28.6084, -80.6043)
)

val StrategicTheaters = listOf(
    TheaterPreset(
        id = "global_orbit",
        name = "Global Orbital View",
        regionCode = "GEO-ALL",
        lat = 22.0,
        lon = 12.0,
        zoom = 1.05f,
        pitchDeg = 18f,
        briefing = "Full spherical planetary telemetry & multi-constellation sweep."
    ),
    TheaterPreset(
        id = "london_channel",
        name = "London TfL CCTV & Air",
        regionCode = "EUR-LON",
        lat = 51.5074,
        lon = -0.1278,
        zoom = 5.4f,
        pitchDeg = 48f,
        briefing = "Live TfL JamCam CCTV viewsheds, OSM road traffic vectors & ADS-B corridor."
    ),
    TheaterPreset(
        id = "austin_cctv",
        name = "Austin TX CCTV & Traffic",
        regionCode = "NAM-AUS",
        lat = 30.2672,
        lon = -97.7431,
        zoom = 5.4f,
        pitchDeg = 48f,
        briefing = "City of Austin live traffic camera grid, 3D viewshed cones & I-35 artery flow."
    ),
    TheaterPreset(
        id = "baltic_ais",
        name = "Baltic Maritime AIS",
        regionCode = "SEA-FIN",
        lat = 59.95,
        lon = 24.45,
        zoom = 4.6f,
        pitchDeg = 44f,
        briefing = "Real-time Digitraffic Marine AIS vessel positions, speed vectors & shipping lanes."
    ),
    TheaterPreset(
        id = "sf_bay",
        name = "San Francisco & Pacific",
        regionCode = "NAM-SFO",
        lat = 37.62,
        lon = -122.38,
        zoom = 4.6f,
        pitchDeg = 48f,
        briefing = "San Andreas seismic corridor, Bay road vectors & trans-Pacific air traffic."
    ),
    TheaterPreset(
        id = "tokyo_ring",
        name = "Tokyo Bay & Ring of Fire",
        regionCode = "APC-HND",
        lat = 35.55,
        lon = 139.78,
        zoom = 4.5f,
        pitchDeg = 50f,
        briefing = "Subduction seismic monitoring, urban expressway arteries & East Asian aviation."
    ),
    TheaterPreset(
        id = "hormuz_gulf",
        name = "Strait of Hormuz",
        regionCode = "MEA-DXB",
        lat = 25.25,
        lon = 55.36,
        zoom = 4.2f,
        pitchDeg = 44f,
        briefing = "Strategic maritime chokepoint & long-haul aviation crossroads."
    ),
    TheaterPreset(
        id = "kennedy_orbit",
        name = "Cape Canaveral Launch",
        regionCode = "ORB-KSC",
        lat = 28.57,
        lon = -80.65,
        zoom = 3.8f,
        pitchDeg = 42f,
        briefing = "Eastern Range orbital inclination corridor & Atlantic tracking."
    ),
    TheaterPreset(
        id = "mediterranean",
        name = "Mediterranean / Suez",
        regionCode = "MED-CAI",
        lat = 33.5,
        lon = 24.0,
        zoom = 3.4f,
        pitchDeg = 40f,
        briefing = "Subsea fiber backbone & active Mediterranean fault zone."
    )
)

package com.example

import com.example.domain.model.AircraftTarget
import com.example.domain.model.CallidusUiState
import com.example.domain.model.GeoPoint3D
import com.example.domain.model.SatelliteCategory
import com.example.domain.model.SatelliteElement
import com.example.domain.model.SatelliteTarget
import com.example.domain.model.SelectedTarget
import com.example.domain.model.TelemetryDashboardTab
import com.example.ui.components.TelemetryCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TelemetryDashboardTest {

    private val londonFlight = AircraftTarget(
        icao24 = "4ca123",
        callsign = "BAW117",
        registration = "G-STBC",
        aircraftType = "B77W",
        lat = 51.47,
        lon = -0.45,
        altitudeFeet = 34250,
        groundSpeedKnots = 468,
        headingDeg = 274f,
        verticalRateFpm = 450,
        squawk = "2214",
        category = "Heavy"
    )

    private val nycFlight = AircraftTarget(
        icao24 = "a1b2c3",
        callsign = "UAL901",
        registration = "N27901",
        aircraftType = "B789",
        lat = 40.64,
        lon = -73.78,
        altitudeFeet = 38000,
        groundSpeedKnots = 492,
        headingDeg = 65f,
        verticalRateFpm = 0,
        squawk = "4102",
        category = "Heavy"
    )

    private val issSatellite = SatelliteTarget(
        element = SatelliteElement(
            noradId = 25544,
            name = "ISS (ZARYA)",
            category = SatelliteCategory.SPACE_STATION,
            inclinationDeg = 51.64,
            raanDeg = 120.0,
            eccentricity = 0.0005,
            argPerigeeDeg = 45.0,
            meanAnomalyDeg = 10.0,
            meanMotionRevPerDay = 15.5,
            epochMs = 1_700_000_000_000L
        ),
        lat = 34.0,
        lon = -118.0,
        altitudeKm = 418.4,
        velocityKmS = 7.66,
        groundTrack = listOf(
            GeoPoint3D(32.0, -120.0, 418.4),
            GeoPoint3D(34.0, -118.0, 418.4),
            GeoPoint3D(36.0, -115.8, 418.4)
        )
    )

    private val gpsSatellite = SatelliteTarget(
        element = SatelliteElement(
            noradId = 48859,
            name = "GPS III SV05",
            category = SatelliteCategory.NAVIGATION,
            inclinationDeg = 55.0,
            raanDeg = 200.0,
            eccentricity = 0.002,
            argPerigeeDeg = 20.0,
            meanAnomalyDeg = 80.0,
            meanMotionRevPerDay = 2.0,
            epochMs = 1_700_000_000_000L
        ),
        lat = 10.0,
        lon = 15.0,
        altitudeKm = 20200.0,
        velocityKmS = 3.87,
        groundTrack = listOf(
            GeoPoint3D(8.0, 14.0, 20200.0),
            GeoPoint3D(10.0, 15.0, 20200.0),
            GeoPoint3D(12.0, 16.0, 20200.0)
        )
    )

    @Test
    fun resolveTrackedAircraft_prioritizesSelectedThenTrackedIcaoThenNearest() {
        val list = listOf(londonFlight, nycFlight)

        val locked = TelemetryCalculator.resolveTrackedAircraft(
            aircraft = list,
            selectedTarget = SelectedTarget.Flight(nycFlight),
            trackedIcao = londonFlight.icao24,
            centerLat = 51.5,
            centerLon = -0.1
        )
        assertEquals("UAL901", locked?.callsign)

        val tracked = TelemetryCalculator.resolveTrackedAircraft(
            aircraft = list,
            selectedTarget = SelectedTarget.None,
            trackedIcao = nycFlight.icao24,
            centerLat = 51.5,
            centerLon = -0.1
        )
        assertEquals("UAL901", tracked?.callsign)

        val nearest = TelemetryCalculator.resolveTrackedAircraft(
            aircraft = list,
            selectedTarget = SelectedTarget.None,
            trackedIcao = null,
            centerLat = 51.5,
            centerLon = -0.1
        )
        assertEquals("BAW117", nearest?.callsign)
    }

    @Test
    fun resolveTrackedSatellite_prioritizesSelectedThenTrackedNoradThenIss() {
        val list = listOf(gpsSatellite, issSatellite)

        val locked = TelemetryCalculator.resolveTrackedSatellite(
            satellites = list,
            selectedTarget = SelectedTarget.Satellite(gpsSatellite),
            trackedNorad = issSatellite.element.noradId,
            centerLat = 34.0,
            centerLon = -118.0
        )
        assertEquals(48859, locked?.element?.noradId)

        val autoIss = TelemetryCalculator.resolveTrackedSatellite(
            satellites = list,
            selectedTarget = SelectedTarget.None,
            trackedNorad = null,
            centerLat = 10.0,
            centerLon = 15.0
        )
        assertNotNull(autoIss)
        assertEquals("ISS (ZARYA)", autoIss?.element?.name)
    }

    @Test
    fun compassAndAvionicsMetrics_formatCorrectly() {
        assertEquals("N", TelemetryCalculator.compassCardinal(355f))
        assertEquals("E", TelemetryCalculator.compassCardinal(90f))
        assertEquals("W", TelemetryCalculator.compassCardinal(274f))
        assertEquals("FL343", TelemetryCalculator.flightLevelLabel(34250))
        assertEquals("LEO", TelemetryCalculator.orbitRegimeLabel(418.4))
        assertEquals("MEO", TelemetryCalculator.orbitRegimeLabel(20200.0))
        assertEquals("GEO", TelemetryCalculator.orbitRegimeLabel(35786.0))
        assertEquals(92.9, TelemetryCalculator.orbitalPeriodMinutes(15.5), 0.15)
    }

    @Test
    fun satelliteTrackHeadingDeg_computesNorthEastAzimuthForAscendingTrack() {
        val heading = TelemetryCalculator.satelliteTrackHeadingDeg(issSatellite)
        assertTrue("Expected NE orbital azimuth in 25..65 deg, got $heading", heading in 25f..65f)
    }

    @Test
    fun uiStateDefaults_enableFloatingTelemetryDashboard() {
        val defaults = CallidusUiState()
        assertTrue(defaults.isTelemetryDashboardVisible)
        assertTrue(defaults.isTelemetryDashboardExpanded)
        assertEquals(TelemetryDashboardTab.AIRCRAFT, defaults.telemetryDashboardTab)
    }
}

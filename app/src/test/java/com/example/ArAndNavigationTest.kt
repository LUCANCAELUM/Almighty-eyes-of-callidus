package com.example

import com.example.data.repository.CallidusRepository
import com.example.domain.model.AmenityCategory
import com.example.domain.model.GlobalLandmarks
import com.example.domain.model.NavigationTravelMode
import com.example.domain.model.ViewportMode
import com.example.engine.ArSkyProjectionEngine
import com.example.engine.OfflineSteeringCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArAndNavigationTest {

    @Test
    fun arSkyProjection_targetDueNorthAndElevated_projectsCenterFov() {
        val polar = ArSkyProjectionEngine.computeSkyPolar(
            obsLat = 0.0,
            obsLon = 0.0,
            obsAltKm = 0.0,
            tgtLat = 0.5,
            tgtLon = 0.0,
            tgtAltKm = 12.0
        )
        assertTrue("Expected near-North azimuth, got ${polar.azimuthDeg}", polar.azimuthDeg < 5f || polar.azimuthDeg > 355f)
        assertTrue("Expected positive elevation angle, got ${polar.elevationDeg}", polar.elevationDeg > 5f)
        assertTrue("Expected reasonable slant range, got ${polar.slantRangeKm}", polar.slantRangeKm in 45.0..75.0)

        val proj = ArSkyProjectionEngine.projectToCameraViewport(
            obsLat = 0.0,
            obsLon = 0.0,
            obsAltKm = 0.0,
            tgtLat = 0.5,
            tgtLon = 0.0,
            tgtAltKm = 12.0,
            camAzimuthDeg = 0f,
            camElevationDeg = polar.elevationDeg,
            viewportWidth = 1080f,
            viewportHeight = 1920f
        )
        assertTrue(proj.isInFov)
        assertEquals(540f, proj.screenX, 25f)
        assertEquals(960f, proj.screenY, 25f)
    }

    @Test
    fun arSkyProjection_shortestAngleWraparound() {
        assertEquals(20f, ArSkyProjectionEngine.shortestAngleDiffDeg(10f, 350f), 0.01f)
        assertEquals(-20f, ArSkyProjectionEngine.shortestAngleDiffDeg(350f, 10f), 0.01f)
        assertFalse(
            ArSkyProjectionEngine.projectToCameraViewport(
                obsLat = 51.5,
                obsLon = -0.1,
                obsAltKm = 0.0,
                tgtLat = 51.5,
                tgtLon = 2.0,
                tgtAltKm = 10.0,
                camAzimuthDeg = 270f,
                camElevationDeg = 5f,
                viewportWidth = 1080f,
                viewportHeight = 1920f
            ).isInFov
        )
    }

    @Test
    fun offlineSteering_computesTurnAndArrivalProperly() {
        val eastVector = OfflineSteeringCalculator.computeSteering(
            currentLat = 0.0,
            currentLon = 0.0,
            compassHeadingDeg = 0f,
            destLat = 0.0,
            destLon = 0.5
        )
        assertEquals(90f, eastVector.targetBearingDeg, 1.0f)
        assertEquals("E", eastVector.cardinalDirection)
        assertEquals(90f, eastVector.relativeTurnDeg, 1.0f)
        assertTrue(eastVector.steerInstruction.contains("TURN RIGHT"))

        val arrived = OfflineSteeringCalculator.computeSteering(
            currentLat = 51.5055,
            currentLon = -0.0754,
            compassHeadingDeg = 120f,
            destLat = 51.5055,
            destLon = -0.0754
        )
        assertEquals("ARRIVED AT WAYPOINT", arrived.steerInstruction)
    }

    @Test
    fun repositoryOfflineCompassRoute_generatesGreatCircleArc() {
        val route = CallidusRepository.buildDirectCompassRoute(
            originLat = -1.2884,
            originLon = 36.8233,
            originLabel = "Nairobi",
            destLat = 51.5055,
            destLon = -0.0754,
            destLabel = "London",
            mode = NavigationTravelMode.AIR_DIRECT
        )
        assertTrue(route.distanceMeters > 6_000_000.0)
        assertTrue(route.durationSeconds > 20_000.0)
        assertEquals(29, route.coordinates.size)
        assertEquals(2, route.steps.size)
        assertTrue(route.steps.first().instruction.contains("Head"))
    }

    @Test
    fun viewportsLandmarksAndAmenities_areConfigured() {
        assertEquals(5, ViewportMode.entries.size)
        assertTrue(GlobalLandmarks.size >= 8)
        assertTrue(AmenityCategory.entries.any { it == AmenityCategory.HOSPITAL })
    }
}

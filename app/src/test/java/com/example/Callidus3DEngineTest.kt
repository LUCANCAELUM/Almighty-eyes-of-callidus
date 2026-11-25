package com.example

import com.example.data.orbital.OrbitalPropagator
import com.example.domain.model.CameraState3D
import com.example.domain.model.SatelliteCategory
import com.example.domain.model.SatelliteElement
import com.example.engine.Globe3DEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Callidus3DEngineTest {

    @Test
    fun `project center point lands on screen center when pitch is zero`() {
        val cam = CameraState3D(centerLat = 37.6, centerLon = -122.4, zoom = 1.0f, pitchDeg = 0f, rollDeg = 0f)
        val engine = Globe3DEngine(1000f, 2000f, cam)
        val p = engine.project(37.6, -122.4, 1.0f)

        assertTrue(p.isVisible)
        assertEquals(500f, p.screenX, 0.5f)
        assertEquals(1000f, p.screenY, 0.5f)
    }

    @Test
    fun `antipodal point is occluded behind 3D sphere`() {
        val cam = CameraState3D(centerLat = 0.0, centerLon = 0.0, zoom = 1.0f, pitchDeg = 0f, rollDeg = 0f)
        val engine = Globe3DEngine(1000f, 1000f, cam)
        val back = engine.project(0.0, 180.0, 1.0f)

        assertFalse(back.isVisible)
    }

    @Test
    fun `shortestLonDelta wraps across international date line`() {
        val delta = Globe3DEngine.shortestLonDelta(175.0, -175.0)
        assertEquals(10.0, delta, 0.001)

        val deltaReverse = Globe3DEngine.shortestLonDelta(-175.0, 175.0)
        assertEquals(-10.0, deltaReverse, 0.001)
    }

    @Test
    fun `keplerian orbital propagator yields valid ISS altitude and velocity`() {
        val issElement = SatelliteElement(
            noradId = 25544,
            name = "ISS (ZARYA)",
            category = SatelliteCategory.SPACE_STATION,
            inclinationDeg = 51.64,
            raanDeg = 120.0,
            eccentricity = 0.0005,
            argPerigeeDeg = 45.0,
            meanAnomalyDeg = 10.0,
            meanMotionRevPerDay = 15.5,
            epochMs = System.currentTimeMillis() - 600_000L
        )
        val propagated = OrbitalPropagator.propagate(issElement, System.currentTimeMillis())

        assertTrue(propagated.lat in -52.5..52.5)
        assertTrue(propagated.lon in -180.0..180.0)
        assertTrue("ISS alt should be ~400-440km, was ${propagated.altitudeKm}", propagated.altitudeKm in 350.0..480.0)
        assertTrue("ISS vel should be ~7.66 km/s, was ${propagated.velocityKmS}", propagated.velocityKmS in 7.4..7.9)
        assertTrue(propagated.groundTrack.size >= 30)
    }
}

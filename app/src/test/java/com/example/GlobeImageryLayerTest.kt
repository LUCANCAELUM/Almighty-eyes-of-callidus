package com.example

import com.example.domain.model.CallidusUiState
import com.example.domain.model.GlobeImageryStyle
import com.example.engine.SatelliteTileKey
import com.example.engine.SatelliteTileManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlobeImageryLayerTest {

    @Test
    fun buildTileUrl_producesCorrectArcGisEndpointsForAllStyles() {
        val key = SatelliteTileKey(z = 4, x = 9, y = 6)

        val standardUrl = SatelliteTileManager.buildTileUrl(GlobeImageryStyle.STANDARD, key)
        val satelliteUrl = SatelliteTileManager.buildTileUrl(GlobeImageryStyle.SATELLITE, key)
        val terrainUrl = SatelliteTileManager.buildTileUrl(GlobeImageryStyle.TERRAIN, key)

        assertEquals(
            "https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/4/6/9",
            standardUrl
        )
        assertEquals(
            "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/4/6/9",
            satelliteUrl
        )
        assertEquals(
            "https://server.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer/tile/4/6/9",
            terrainUrl
        )
    }

    @Test
    fun styleCacheKey_separatesCachesAcrossImageryStyles() {
        val key = SatelliteTileKey(z = 2, x = 1, y = 2)

        val stdKey = SatelliteTileManager.styleCacheKey(GlobeImageryStyle.STANDARD, key)
        val satKey = SatelliteTileManager.styleCacheKey(GlobeImageryStyle.SATELLITE, key)
        val terKey = SatelliteTileManager.styleCacheKey(GlobeImageryStyle.TERRAIN, key)

        assertEquals("standard/2/1/2", stdKey)
        assertEquals("satellite/2/1/2", satKey)
        assertEquals("terrain/2/1/2", terKey)
        assertNotEquals(stdKey, satKey)
        assertNotEquals(satKey, terKey)
        assertNotEquals(stdKey, terKey)
    }

    @Test
    fun uiStateDefaults_enableSatelliteFullOpacityAnd3DRenderingOverlays() {
        val state = CallidusUiState()

        assertEquals(GlobeImageryStyle.SATELLITE, state.imageryStyle)
        assertEquals(1.0f, state.imageryOpacity, 0.001f)
        assertTrue(state.showGraticuleGrid)
        assertTrue(state.showAtmosphereGlow)
        assertEquals(3, GlobeImageryStyle.entries.size)
    }
}

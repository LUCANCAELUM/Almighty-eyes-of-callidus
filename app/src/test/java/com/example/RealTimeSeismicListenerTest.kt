package com.example

import com.example.data.seismic.RealTimeSeismicListener
import com.example.domain.model.EarthquakeTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RealTimeSeismicListenerTest {

    @Test
    fun parseEmscWebSocketPayload_parsesCreateTremorPacket() {
        val sampleJson = """
            {
              "action": "create",
              "data": {
                "type": "Feature",
                "id": "20261118_0000123",
                "geometry": {
                  "type": "Point",
                  "coordinates": [140.82, 38.15, -12.4]
                },
                "properties": {
                  "unid": "20261118_0000123",
                  "time": "2026-11-18T22:00:00.0Z",
                  "lat": 38.15,
                  "lon": 140.82,
                  "depth": 12.4,
                  "mag": 5.3,
                  "auth": "JMA",
                  "flynn_region": "NEAR EAST COAST OF HONSHU, JAPAN"
                }
              }
            }
        """.trimIndent()

        val event = RealTimeSeismicListener.parseEmscWebSocketPayload(sampleJson)
        assertNotNull(event)
        assertFalse(event!!.isUpdate)
        assertEquals("emsc_20261118_0000123", event.tremor.id)
        assertEquals(5.3, event.tremor.magnitude, 0.01)
        assertEquals(12.4, event.tremor.depthKm, 0.01)
        assertEquals(38.15, event.tremor.lat, 0.001)
        assertEquals(140.82, event.tremor.lon, 0.001)
        assertEquals("EMSC/JMA", event.tremor.sourceNetwork)
        assertTrue(event.tremor.isRealtimeAlert)
    }

    @Test
    fun mergeSeismicEvents_deduplicatesCrossAgencyDetections() {
        val now = 1_795_040_000_000L
        val usgs = EarthquakeTarget(
            id = "us7000abc",
            title = "M4.8 Near Coast of Chile",
            place = "Near Coast of Chile",
            magnitude = 4.8,
            depthKm = 18.0,
            lat = -33.4,
            lon = -71.8,
            timeMs = now,
            tsunamiAlert = false,
            sourceNetwork = "USGS 24H",
            isRealtimeAlert = false
        )
        val emscDuplicate = EarthquakeTarget(
            id = "emsc_2026_chile",
            title = "M4.9 CENTRAL CHILE",
            place = "CENTRAL CHILE",
            magnitude = 4.9,
            depthKm = 15.0,
            lat = -33.45,
            lon = -71.75,
            timeMs = now + 15_000L,
            tsunamiAlert = false,
            sourceNetwork = "EMSC/GUC",
            isRealtimeAlert = true
        )
        val distinctQuake = EarthquakeTarget(
            id = "emsc_2026_greece",
            title = "M3.7 GREECE",
            place = "GREECE",
            magnitude = 3.7,
            depthKm = 10.0,
            lat = 38.2,
            lon = 22.1,
            timeMs = now + 30_000L,
            tsunamiAlert = false,
            sourceNetwork = "EMSC/ATH",
            isRealtimeAlert = true
        )

        val merged = RealTimeSeismicListener.mergeSeismicEvents(
            existing = listOf(usgs),
            incoming = listOf(emscDuplicate, distinctQuake)
        )

        assertEquals(2, merged.size)
        assertTrue(merged.any { it.id == "emsc_2026_greece" })
        val mergedChile = merged.first { it.lat < 0.0 }
        assertTrue(mergedChile.isRealtimeAlert)
        assertTrue(mergedChile.sourceNetwork.contains("EMSC"))
    }
}

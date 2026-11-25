package com.example.data.seismic

import com.example.data.remote.CallidusRetrofitProvider
import com.example.domain.model.EarthquakeTarget
import com.example.domain.model.SeismicConnectionStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

sealed interface SeismicStreamEvent {
    data class StatusChanged(val status: SeismicConnectionStatus) : SeismicStreamEvent
    data class TremorDetected(val tremor: EarthquakeTarget, val isUpdate: Boolean) : SeismicStreamEvent
}

class RealTimeSeismicListener(
    private val client: OkHttpClient = CallidusRetrofitProvider.okHttpClient
) {
    fun observeLiveTremors(): Flow<SeismicStreamEvent> = callbackFlow {
        var backoffMs = 3_000L
        var activeSocket: WebSocket? = null
        var closed = false

        val connectionJob = launch {
            while (isActive && !closed) {
                trySend(SeismicStreamEvent.StatusChanged(SeismicConnectionStatus.CONNECTING))
                val completedDeferred = kotlinx.coroutines.CompletableDeferred<Unit>()
                val request = Request.Builder()
                    .url(EMSC_WEBSOCKET_URL)
                    .build()

                val listener = object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        backoffMs = 3_000L
                        trySend(SeismicStreamEvent.StatusChanged(SeismicConnectionStatus.LIVE_STREAM))
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        parseEmscWebSocketPayload(text)?.let { event ->
                            trySend(event)
                        }
                    }

                    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                        webSocket.close(1000, null)
                    }

                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                        if (!closed) {
                            trySend(SeismicStreamEvent.StatusChanged(SeismicConnectionStatus.RECONNECTING))
                        }
                        if (!completedDeferred.isCompleted) completedDeferred.complete(Unit)
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        if (!closed) {
                            trySend(SeismicStreamEvent.StatusChanged(SeismicConnectionStatus.RECONNECTING))
                        }
                        if (!completedDeferred.isCompleted) completedDeferred.complete(Unit)
                    }
                }

                activeSocket = client.newWebSocket(request, listener)
                completedDeferred.await()

                if (!closed && isActive) {
                    delay(backoffMs)
                    backoffMs = (backoffMs * 2L).coerceAtMost(24_000L)
                }
            }
        }

        awaitClose {
            closed = true
            connectionJob.cancel()
            activeSocket?.close(1000, "Callidus listener disposed")
            activeSocket = null
        }
    }

    companion object {
        const val EMSC_WEBSOCKET_URL = "wss://www.seismicportal.eu/standing_order/websocket"

        fun parseEmscWebSocketPayload(
            rawJson: String,
            fallbackNowMs: Long = System.currentTimeMillis()
        ): SeismicStreamEvent.TremorDetected? {
            return runCatching {
                val root = JSONObject(rawJson)
                val action = root.optString("action", "create").lowercase(Locale.US)
                val data = root.optJSONObject("data") ?: return null
                val props = data.optJSONObject("properties") ?: return null
                val geom = data.optJSONObject("geometry")
                val coords = geom?.optJSONArray("coordinates")

                val lon = props.optDouble("lon", Double.NaN).takeIf { !it.isNaN() }
                    ?: coords?.optDouble(0, Double.NaN)?.takeIf { !it.isNaN() }
                    ?: return null
                val lat = props.optDouble("lat", Double.NaN).takeIf { !it.isNaN() }
                    ?: coords?.optDouble(1, Double.NaN)?.takeIf { !it.isNaN() }
                    ?: return null

                val rawDepth = props.optDouble("depth", Double.NaN).takeIf { !it.isNaN() }
                    ?: coords?.optDouble(2, 10.0)
                    ?: 10.0
                val depthKm = abs(rawDepth)
                val mag = props.optDouble("mag", Double.NaN).takeIf { !it.isNaN() } ?: return null

                val unid = props.optString("unid").ifBlank { data.optString("id") }
                    .ifBlank { "emsc_${lat}_${lon}" }
                val region = props.optString("flynn_region").ifBlank { "Global Seismic Zone" }
                val auth = props.optString("auth").ifBlank { "EMSC" }
                val timeIso = props.optString("time")
                val timeMs = parseIsoUtcMillis(timeIso) ?: fallbackNowMs

                val tremor = EarthquakeTarget(
                    id = "emsc_$unid",
                    title = String.format(Locale.US, "M%.1f %s", mag, region),
                    place = region,
                    magnitude = mag,
                    depthKm = depthKm,
                    lat = lat,
                    lon = lon,
                    timeMs = timeMs,
                    tsunamiAlert = mag >= 6.5 && depthKm <= 70.0,
                    sourceNetwork = "EMSC/$auth",
                    isRealtimeAlert = true
                )
                SeismicStreamEvent.TremorDetected(
                    tremor = tremor,
                    isUpdate = action == "update"
                )
            }.getOrNull()
        }

        fun mergeSeismicEvents(
            existing: List<EarthquakeTarget>,
            incoming: List<EarthquakeTarget>,
            maxItems: Int = 120
        ): List<EarthquakeTarget> {
            val combined = LinkedHashMap<String, EarthquakeTarget>()
            for (item in existing) {
                combined[item.id] = item
            }
            for (candidate in incoming) {
                val sameId = combined[candidate.id]
                if (sameId != null) {
                    combined[candidate.id] = candidate.copy(
                        isRealtimeAlert = candidate.isRealtimeAlert || sameId.isRealtimeAlert
                    )
                    continue
                }
                // Deduplicate cross-agency detections (EMSC vs USGS) of the same rupture
                val nearDuplicateKey = combined.entries.firstOrNull { (_, other) ->
                    abs(other.lat - candidate.lat) < 0.4 &&
                        abs(other.lon - candidate.lon) < 0.4 &&
                        abs(other.timeMs - candidate.timeMs) < 90_000L &&
                        abs(other.magnitude - candidate.magnitude) < 0.6
                }?.key
                if (nearDuplicateKey != null) {
                    val prev = combined[nearDuplicateKey]!!
                    combined[nearDuplicateKey] = if (candidate.isRealtimeAlert && !prev.isRealtimeAlert) {
                        candidate.copy(
                            tsunamiAlert = prev.tsunamiAlert || candidate.tsunamiAlert,
                            sourceNetwork = "${candidate.sourceNetwork}+${prev.sourceNetwork}"
                        )
                    } else {
                        prev.copy(
                            isRealtimeAlert = prev.isRealtimeAlert || candidate.isRealtimeAlert,
                            tsunamiAlert = prev.tsunamiAlert || candidate.tsunamiAlert
                        )
                    }
                } else {
                    combined[candidate.id] = candidate
                }
            }
            return combined.values
                .sortedWith(
                    compareByDescending<EarthquakeTarget> { it.isRealtimeAlert }
                        .thenByDescending { it.timeMs }
                        .thenByDescending { it.magnitude }
                )
                .take(maxItems)
        }

        private fun parseIsoUtcMillis(iso: String): Long? {
            if (iso.isBlank()) return null
            val cleaned = iso.substringBefore(".").substringBefore("Z").trim()
            return runCatching {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                sdf.parse(cleaned)?.time
            }.getOrNull()
        }
    }
}

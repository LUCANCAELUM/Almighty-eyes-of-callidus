package com.example.data.repository

import android.util.JsonReader
import android.util.JsonToken
import com.example.data.orbital.OrbitalPropagator
import com.example.data.remote.CallidusNetworkApi
import com.example.data.remote.CallidusRetrofitProvider
import com.example.domain.model.AircraftTarget
import com.example.domain.model.AmenityCategory
import com.example.domain.model.CctvCameraTarget
import com.example.domain.model.EarthquakeTarget
import com.example.domain.model.GeoPoint3D
import com.example.domain.model.GeocodePlaceResult
import com.example.domain.model.LocalWeatherTelemetry
import com.example.domain.model.MapAmenity
import com.example.domain.model.MaritimeVesselTarget
import com.example.domain.model.NaturalEventTarget
import com.example.domain.model.NavigationRoute
import com.example.domain.model.NavigationStep
import com.example.domain.model.NavigationTravelMode
import com.example.domain.model.SatelliteCategory
import com.example.domain.model.SatelliteElement
import com.example.domain.model.SpaceWeatherState
import com.example.domain.model.TrafficRoadSegment
import com.example.engine.GeoCoord
import com.example.engine.Globe3DEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.InputStreamReader
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

class CallidusRepository(
    private val api: CallidusNetworkApi = CallidusRetrofitProvider.api
) {

    suspend fun fetchLiveAircraft(centerLat: Double, centerLon: Double): Result<List<AircraftTarget>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val primary = runCatching {
                    api.getAirplanesNearPoint(centerLat, centerLon, 250)
                }.getOrNull()

                val parsedPrimary = primary?.aircraft.orEmpty().mapNotNull { item ->
                    val lat = item.lat ?: return@mapNotNull null
                    val lon = item.lon ?: return@mapNotNull null
                    val hex = item.hex ?: return@mapNotNull null
                    val alt = when (val rawAlt = item.altBaro) {
                        is Number -> rawAlt.toInt()
                        is String -> rawAlt.toIntOrNull() ?: 0
                        else -> 0
                    }
                    AircraftTarget(
                        icao24 = hex.uppercase(),
                        callsign = item.flight?.trim()?.takeIf { it.isNotEmpty() } ?: hex.uppercase(),
                        registration = item.registration?.trim().orEmpty(),
                        aircraftType = item.type?.trim().orEmpty(),
                        lat = lat,
                        lon = lon,
                        altitudeFeet = alt,
                        groundSpeedKnots = item.groundSpeed?.roundToInt() ?: 0,
                        headingDeg = item.track?.toFloat() ?: 0f,
                        verticalRateFpm = item.baroRate ?: 0,
                        squawk = item.squawk ?: "----",
                        category = item.category ?: "A3"
                    )
                }.take(90)

                if (parsedPrimary.isNotEmpty()) {
                    return@runCatching parsedPrimary
                }

                val laMin = (centerLat - 6.0).coerceIn(-89.0, 89.0)
                val laMax = (centerLat + 6.0).coerceIn(-89.0, 89.0)
                val loMin = (centerLon - 8.0).coerceIn(-180.0, 180.0)
                val loMax = (centerLon + 8.0).coerceIn(-180.0, 180.0)
                val openSky = api.getOpenSkyBox(laMin, loMin, laMax, loMax)
                openSky.states.orEmpty().mapNotNull { row ->
                    if (row.size < 11) return@mapNotNull null
                    val icao = (row[0] as? String) ?: return@mapNotNull null
                    val callsign = (row[1] as? String)?.trim()?.takeIf { it.isNotEmpty() } ?: icao.uppercase()
                    val lon = (row[5] as? Number)?.toDouble() ?: return@mapNotNull null
                    val lat = (row[6] as? Number)?.toDouble() ?: return@mapNotNull null
                    val altMeters = (row[7] as? Number)?.toDouble() ?: (row[13] as? Number)?.toDouble() ?: 0.0
                    val velMs = (row[9] as? Number)?.toDouble() ?: 0.0
                    val track = (row[10] as? Number)?.toFloat() ?: 0f
                    val vRate = (row[11] as? Number)?.toDouble() ?: 0.0

                    AircraftTarget(
                        icao24 = icao.uppercase(),
                        callsign = callsign,
                        registration = (row[2] as? String).orEmpty(),
                        aircraftType = "ADS-B",
                        lat = lat,
                        lon = lon,
                        altitudeFeet = (altMeters * 3.28084).roundToInt(),
                        groundSpeedKnots = (velMs * 1.94384).roundToInt(),
                        headingDeg = track,
                        verticalRateFpm = (vRate * 196.85).roundToInt(),
                        squawk = (row[14] as? String) ?: "----",
                        category = "OSKY"
                    )
                }.take(90)
            }
        }

    suspend fun fetchLiveCctvCameras(): Result<List<CctvCameraTarget>> =
        withContext(Dispatchers.IO) {
            runCatching {
                coroutineScope {
                    val tflDeferred = async { runCatching { api.getTflJamCams() }.getOrDefault(emptyList()) }
                    val austinDeferred = async { runCatching { api.getAustinCameras() }.getOrDefault(emptyList()) }
                    val nycDeferred = async { runCatching { api.getNycCameras() }.getOrDefault(emptyList()) }
                    val sfDeferred = async { runCatching { api.getCaltransD4Cameras() }.getOrNull() }
                    val laDeferred = async { runCatching { api.getCaltransD7Cameras() }.getOrNull() }
                    val sgDeferred = async { runCatching { api.getSingaporeCameras() }.getOrNull() }
                    val fiDeferred = async { runCatching { api.getFinlandWeatherCams() }.getOrNull() }

                    val tflCams = tflDeferred.await().mapNotNull { place ->
                        val lat = place.lat ?: return@mapNotNull null
                        val lon = place.lon ?: return@mapNotNull null
                        val id = place.id ?: return@mapNotNull null
                        val props = place.additionalProperties.orEmpty()
                        val available = props.firstOrNull { it.key == "available" }?.value
                        if (available.equals("false", ignoreCase = true)) return@mapNotNull null
                        val imgUrl = props.firstOrNull { it.key == "imageUrl" }?.value
                            ?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                        val vidUrl = props.firstOrNull { it.key == "videoUrl" }?.value
                            ?.takeIf { it.isNotBlank() }
                        val seedAzimuth = (abs(id.hashCode()) % 360).toFloat()

                        CctvCameraTarget(
                            id = "tfl_$id",
                            name = place.commonName?.trim()?.ifEmpty { id } ?: id,
                            city = "London (TfL JamCam)",
                            region = "EUROPE",
                            lat = lat,
                            lon = lon,
                            imageUrl = imgUrl,
                            videoUrl = vidUrl,
                            streamType = if (vidUrl != null) "LIVE MP4 VIDEO" else "LIVE CAM",
                            azimuthDeg = seedAzimuth,
                            fovDeg = 58f,
                            rangeKm = 1.25f,
                            status = if (vidUrl != null) "VIDEO STREAM" else "LIVE CAM"
                        )
                    }.take(65)

                    fun parseCaltransList(
                        resp: com.example.data.remote.CaltransResponse?,
                        prefix: String,
                        cityLabel: String
                    ): List<CctvCameraTarget> {
                        return resp?.data.orEmpty().mapNotNull { wrapper ->
                            val dto = wrapper.cctv ?: return@mapNotNull null
                            if (dto.inService.equals("false", ignoreCase = true)) return@mapNotNull null
                            val loc = dto.location ?: return@mapNotNull null
                            val lat = loc.latitude?.toDoubleOrNull() ?: return@mapNotNull null
                            val lon = loc.longitude?.toDoubleOrNull() ?: return@mapNotNull null
                            val imgUrl = dto.imageData?.staticData?.currentImageUrl?.takeIf { it.isNotBlank() }
                                ?: return@mapNotNull null
                            val hlsUrl = dto.imageData.streamingVideoUrl?.takeIf { it.isNotBlank() }
                            val camIdx = dto.index ?: "${lat}_${lon}"
                            val dirAz = when (loc.direction?.uppercase(Locale.US)) {
                                "NORTH", "N" -> 0f
                                "EAST", "E" -> 90f
                                "SOUTH", "S" -> 180f
                                "WEST", "W" -> 270f
                                else -> (abs(camIdx.hashCode()) % 360).toFloat()
                            }
                            CctvCameraTarget(
                                id = "${prefix}_$camIdx",
                                name = loc.locationName?.trim()?.ifEmpty { loc.nearbyPlace ?: "Caltrans $camIdx" }
                                    ?: (loc.nearbyPlace ?: "Caltrans $camIdx"),
                                city = cityLabel,
                                region = "AMERICAS",
                                lat = lat,
                                lon = lon,
                                imageUrl = imgUrl,
                                videoUrl = hlsUrl,
                                streamType = if (hlsUrl != null) "LIVE HLS VIDEO" else "LIVE CAM",
                                azimuthDeg = dirAz,
                                fovDeg = 60f,
                                rangeKm = 1.5f,
                                status = if (hlsUrl != null) "LIVE HLS STREAM" else "ONLINE"
                            )
                        }.take(45)
                    }

                    val sfCams = parseCaltransList(sfDeferred.await(), "cal_d4", "San Francisco Bay (Caltrans)")
                    val laCams = parseCaltransList(laDeferred.await(), "cal_d7", "Los Angeles (Caltrans)")

                    val nycCams = nycDeferred.await().mapNotNull { cam ->
                        val id = cam.id ?: return@mapNotNull null
                        val lat = cam.latitude ?: return@mapNotNull null
                        val lon = cam.longitude ?: return@mapNotNull null
                        if (cam.isOnline.equals("false", ignoreCase = true)) return@mapNotNull null
                        val imgUrl = cam.imageUrl?.takeIf { it.isNotBlank() }
                            ?: "https://webcams.nyctmc.org/api/cameras/$id/image"
                        CctvCameraTarget(
                            id = "nyc_$id",
                            name = cam.name?.trim()?.ifEmpty { "NYC Cam $id" } ?: "NYC Cam $id",
                            city = "New York (${cam.area ?: "NYC DOT"})",
                            region = "AMERICAS",
                            lat = lat,
                            lon = lon,
                            imageUrl = imgUrl,
                            videoUrl = null,
                            streamType = "LIVE MJPEG/CAM",
                            azimuthDeg = (abs(id.hashCode()) % 360).toFloat(),
                            fovDeg = 55f,
                            rangeKm = 1.15f,
                            status = "ONLINE"
                        )
                    }.take(55)

                    val austinCams = austinDeferred.await().mapNotNull { cam ->
                        val id = cam.cameraId ?: return@mapNotNull null
                        val coords = cam.location?.coordinates ?: return@mapNotNull null
                        if (coords.size < 2) return@mapNotNull null
                        val lon = coords[0]
                        val lat = coords[1]
                        if (cam.cameraStatus.equals("OFFLINE", ignoreCase = true)) return@mapNotNull null
                        val imgUrl = cam.screenshotAddress?.takeIf { it.isNotBlank() }
                            ?: "https://cctv.austinmobility.io/image/$id.jpg"
                        val seedAzimuth = (abs(id.hashCode()) % 360).toFloat()

                        CctvCameraTarget(
                            id = "atx_$id",
                            name = cam.locationName?.trim()?.ifEmpty { "Austin Cam $id" } ?: "Austin Cam $id",
                            city = "Austin TX (DOT)",
                            region = "AMERICAS",
                            lat = lat,
                            lon = lon,
                            imageUrl = imgUrl,
                            videoUrl = null,
                            streamType = "LIVE CAM",
                            azimuthDeg = seedAzimuth,
                            fovDeg = 62f,
                            rangeKm = 1.35f,
                            status = cam.cameraStatus?.uppercase() ?: "ONLINE"
                        )
                    }.take(40)

                    val sgCams = sgDeferred.await()?.items?.firstOrNull()?.cameras.orEmpty().mapNotNull { cam ->
                        val id = cam.cameraId ?: return@mapNotNull null
                        val imgUrl = cam.image?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                        val lat = cam.location?.latitude ?: return@mapNotNull null
                        val lon = cam.location.longitude ?: return@mapNotNull null
                        CctvCameraTarget(
                            id = "sg_$id",
                            name = "Singapore Expressways Cam $id",
                            city = "Singapore (LTA)",
                            region = "ASIA-PACIFIC",
                            lat = lat,
                            lon = lon,
                            imageUrl = imgUrl,
                            videoUrl = null,
                            streamType = "LIVE CAM",
                            azimuthDeg = (abs(id.hashCode()) % 360).toFloat(),
                            fovDeg = 60f,
                            rangeKm = 1.4f,
                            status = "ONLINE"
                        )
                    }.take(40)

                    val fiCams = fiDeferred.await()?.features.orEmpty().mapNotNull { feat ->
                        val id = feat.id ?: return@mapNotNull null
                        val coords = feat.geometry?.coordinates ?: return@mapNotNull null
                        if (coords.size < 2) return@mapNotNull null
                        val lon = coords[0]
                        val lat = coords[1]
                        val presetId = feat.properties?.presets?.firstOrNull { it.inCollection != false }?.id
                            ?: feat.properties?.presets?.firstOrNull()?.id
                            ?: return@mapNotNull null
                        CctvCameraTarget(
                            id = "fi_$id",
                            name = feat.properties?.name?.trim()?.ifEmpty { "Finland Road $id" } ?: "Finland Road $id",
                            city = "Finland (Digitraffic)",
                            region = "EUROPE",
                            lat = lat,
                            lon = lon,
                            imageUrl = "https://weathercam.digitraffic.fi/$presetId.jpg",
                            videoUrl = null,
                            streamType = "LIVE CAM",
                            azimuthDeg = (abs(id.hashCode()) % 360).toFloat(),
                            fovDeg = 62f,
                            rangeKm = 1.5f,
                            status = "ONLINE"
                        )
                    }.take(40)

                    // Interleave regions cleanly so every continent & video feed surfaces prominently
                    val videoFirst = (sfCams + laCams + tflCams).filter { !it.videoUrl.isNullOrBlank() }
                    val others = (nycCams + sgCams + fiCams + austinCams + tflCams + sfCams + laCams)
                    val combined = (videoFirst + others).distinctBy { it.id }
                    if (combined.isEmpty()) error("No public CCTV feeds reachable")
                    combined
                }
            }
        }

    suspend fun fetchMaritimeVessels(): Result<List<MaritimeVesselTarget>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val body = api.streamMaritimeAisLocations()
                val vessels = mutableListOf<MaritimeVesselTarget>()
                body.byteStream().use { stream ->
                    JsonReader(InputStreamReader(stream)).use { reader ->
                        reader.beginObject()
                        while (reader.hasNext()) {
                            when (reader.nextName()) {
                                "features" -> {
                                    reader.beginArray()
                                    while (reader.hasNext() && vessels.size < 110) {
                                        parseAisFeature(reader)?.let { v ->
                                            if (v.speedKnots >= 0.4) {
                                                vessels.add(v)
                                            }
                                        }
                                    }
                                    // Stop reading remainder of massive stream cleanly
                                    break
                                }
                                else -> reader.skipValue()
                            }
                        }
                    }
                }
                if (vessels.isEmpty()) error("AIS feed returned 0 moving vessels")
                vessels
            }
        }

    private fun parseAisFeature(reader: JsonReader): MaritimeVesselTarget? {
        var mmsi = 0L
        var lon: Double? = null
        var lat: Double? = null
        var sog = 0.0
        var cog = 0f
        var heading = 511

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "mmsi" -> if (reader.peek() == JsonToken.NUMBER) mmsi = reader.nextLong() else reader.skipValue()
                "geometry" -> {
                    reader.beginObject()
                    while (reader.hasNext()) {
                        if (reader.nextName() == "coordinates") {
                            reader.beginArray()
                            if (reader.hasNext()) lon = reader.nextDouble()
                            if (reader.hasNext()) lat = reader.nextDouble()
                            while (reader.hasNext()) reader.skipValue()
                            reader.endArray()
                        } else {
                            reader.skipValue()
                        }
                    }
                    reader.endObject()
                }
                "properties" -> {
                    reader.beginObject()
                    while (reader.hasNext()) {
                        when (reader.nextName()) {
                            "mmsi" -> if (mmsi == 0L && reader.peek() == JsonToken.NUMBER) mmsi = reader.nextLong() else reader.skipValue()
                            "sog" -> if (reader.peek() == JsonToken.NUMBER) sog = reader.nextDouble() else reader.skipValue()
                            "cog" -> if (reader.peek() == JsonToken.NUMBER) cog = reader.nextDouble().toFloat() else reader.skipValue()
                            "heading" -> if (reader.peek() == JsonToken.NUMBER) heading = reader.nextInt() else reader.skipValue()
                            else -> reader.skipValue()
                        }
                    }
                    reader.endObject()
                }
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        val validLat = lat ?: return null
        val validLon = lon ?: return null
        if (mmsi == 0L) return null
        val effectiveHeading = if (heading in 0..359) heading.toFloat() else cog
        return MaritimeVesselTarget(
            mmsi = mmsi,
            name = "VESSEL-$mmsi",
            shipType = if (sog > 14.0) "FAST CARGO / FERRY" else "AIS COMMERCIAL",
            lat = validLat,
            lon = validLon,
            speedKnots = sog,
            headingDeg = effectiveHeading
        )
    }

    suspend fun fetchRoadTrafficFlow(lat: Double, lon: Double): Result<List<TrafficRoadSegment>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val query = String.format(
                    Locale.US,
                    "[out:json][timeout:10];way[\"highway\"~\"motorway|trunk|primary\"](around:11000,%.4f,%.4f);out geom 36;",
                    lat,
                    lon
                )
                val res = api.getOverpassTrafficWays(query)
                res.elements.orEmpty().mapNotNull { el ->
                    val geom = el.geometry ?: return@mapNotNull null
                    if (geom.size < 2) return@mapNotNull null
                    val pts = geom.mapNotNull { pt ->
                        val pLat = pt.lat ?: return@mapNotNull null
                        val pLon = pt.lon ?: return@mapNotNull null
                        GeoPoint3D(pLat, pLon)
                    }
                    if (pts.size < 2) return@mapNotNull null
                    TrafficRoadSegment(
                        id = el.id ?: pts.hashCode().toLong(),
                        highwayClass = el.tags?.get("highway") ?: "primary",
                        points = pts
                    )
                }
            }
        }

    suspend fun fetchLiveSnapshotBytes(imageUrl: String): ByteArray? =
        withContext(Dispatchers.IO) {
            runCatching {
                val sep = if (imageUrl.contains("?")) "&" else "?"
                val bustedUrl = "$imageUrl${sep}t=${System.currentTimeMillis()}"
                val req = Request.Builder().url(bustedUrl).build()
                CallidusRetrofitProvider.okHttpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use null
                    resp.body?.bytes()
                }
            }.getOrNull()
        }

    suspend fun fetchOrbitalElements(): Result<List<SatelliteElement>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val stations = runCatching {
                    api.getCelesTrakGroup("https://celestrak.org/NORAD/elements/gp.php?GROUP=stations&FORMAT=json")
                }.getOrDefault(emptyList())

                val visual = runCatching {
                    api.getCelesTrakGroup("https://celestrak.org/NORAD/elements/gp.php?GROUP=visual&FORMAT=json")
                }.getOrDefault(emptyList())

                val gps = runCatching {
                    api.getCelesTrakGroup("https://celestrak.org/NORAD/elements/gp.php?GROUP=gps-ops&FORMAT=json")
                }.getOrDefault(emptyList())

                val combined = (stations.take(10) + visual.take(18) + gps.take(8))
                    .distinctBy { it.noradCatId }

                if (combined.isEmpty()) {
                    error("CelesTrak GP feed returned 0 orbital elements")
                }

                combined.mapNotNull { gp ->
                    val id = gp.noradCatId ?: return@mapNotNull null
                    val name = gp.objectName?.trim() ?: "SAT-$id"
                    val meanMotion = gp.meanMotion ?: return@mapNotNull null
                    val inc = gp.inclination ?: return@mapNotNull null
                    val raan = gp.raOfAscNode ?: return@mapNotNull null
                    val ecc = gp.eccentricity ?: 0.001
                    val argP = gp.argOfPericenter ?: 0.0
                    val meanAnomaly = gp.meanAnomaly ?: 0.0

                    val cat = when {
                        name.contains("ISS", true) || name.contains("CSS", true) ||
                            name.contains("TIANHE", true) || name.contains("ZARYA", true) -> SatelliteCategory.SPACE_STATION
                        name.contains("GPS", true) || name.contains("NAVSTAR", true) -> SatelliteCategory.NAVIGATION
                        name.contains("STARLINK", true) || name.contains("IRIDIUM", true) -> SatelliteCategory.COMMS
                        else -> SatelliteCategory.EARTH_OBS
                    }

                    SatelliteElement(
                        noradId = id,
                        name = name,
                        category = cat,
                        inclinationDeg = inc,
                        raanDeg = raan,
                        eccentricity = ecc,
                        argPerigeeDeg = argP,
                        meanAnomalyDeg = meanAnomaly,
                        meanMotionRevPerDay = meanMotion,
                        epochMs = OrbitalPropagator.parseEpochMillis(gp.epoch)
                    )
                }
            }
        }

    suspend fun fetchUsgsEarthquakes(): Result<List<EarthquakeTarget>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val nowMs = System.currentTimeMillis()
                parseUsgsResponse(api.getUsgsEarthquakes(), "USGS 24H", nowMs)
                    .sortedByDescending { it.magnitude }
                    .take(85)
            }
        }

    suspend fun fetchUsgsRecentHourEarthquakes(): Result<List<EarthquakeTarget>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val nowMs = System.currentTimeMillis()
                parseUsgsResponse(api.getUsgsEarthquakesLastHour(), "USGS 1H", nowMs)
                    .sortedByDescending { it.timeMs }
                    .take(50)
            }
        }

    private fun parseUsgsResponse(
        res: com.example.data.remote.UsgsEarthquakeResponse,
        network: String,
        nowMs: Long
    ): List<EarthquakeTarget> {
        return res.features.orEmpty().mapNotNull { feature ->
            val coords = feature.geometry?.coordinates ?: return@mapNotNull null
            if (coords.size < 3) return@mapNotNull null
            val props = feature.properties ?: return@mapNotNull null
            val mag = props.mag ?: return@mapNotNull null
            val eventTime = props.time ?: nowMs
            EarthquakeTarget(
                id = feature.id ?: "${coords[1]}_${coords[0]}",
                title = props.title ?: "M$mag Earthquake",
                place = props.place ?: "Unknown Epicenter",
                magnitude = mag,
                depthKm = coords[2],
                lat = coords[1],
                lon = coords[0],
                timeMs = eventTime,
                tsunamiAlert = (props.tsunami ?: 0) == 1,
                sourceNetwork = network,
                isRealtimeAlert = (nowMs - eventTime) < 20 * 60_000L
            )
        }
    }

    suspend fun fetchNasaNaturalEvents(): Result<List<NaturalEventTarget>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val res = api.getNasaEonetEvents()
                res.events.orEmpty().mapNotNull { ev ->
                    val geom = ev.geometry?.lastOrNull() ?: return@mapNotNull null
                    val coords = geom.coordinates ?: return@mapNotNull null
                    if (coords.size < 2) return@mapNotNull null
                    val lon = (coords[0] as? Number)?.toDouble() ?: return@mapNotNull null
                    val lat = (coords[1] as? Number)?.toDouble() ?: return@mapNotNull null
                    val catTitle = ev.categories?.firstOrNull()?.title ?: "Natural Event"
                    NaturalEventTarget(
                        id = ev.id ?: ev.title.orEmpty(),
                        title = ev.title ?: "Active Event",
                        category = catTitle,
                        lat = lat,
                        lon = lon,
                        dateIso = geom.date ?: "",
                        sourceLabel = "NASA EONET"
                    )
                }.take(60)
            }
        }

    suspend fun fetchSpaceWeather(): Result<SpaceWeatherState> =
        withContext(Dispatchers.IO) {
            runCatching {
                val rows = api.getNoaaKpIndex()
                val latest = rows.lastOrNull { it.size >= 2 && it[1].toDoubleOrNull() != null }
                    ?: error("NOAA Kp index unavailable")
                val kp = latest[1].toDoubleOrNull() ?: 0.0
                val scale = when {
                    kp >= 8.0 -> "G4 SEVERE"
                    kp >= 7.0 -> "G3 STRONG"
                    kp >= 6.0 -> "G2 MODERATE"
                    kp >= 5.0 -> "G1 MINOR"
                    kp >= 4.0 -> "ACTIVE"
                    else -> "G0 QUIET"
                }
                SpaceWeatherState(
                    kpIndex = kp,
                    stormScale = scale,
                    observedTime = latest[0]
                )
            }
        }

    suspend fun fetchLocalWeather(lat: Double, lon: Double): Result<LocalWeatherTelemetry> =
        withContext(Dispatchers.IO) {
            runCatching {
                val res = api.getLocalWeather(lat, lon)
                val c = res.current ?: error("Missing current weather")
                LocalWeatherTelemetry(
                    temperatureC = c.temperature2m ?: 0.0,
                    windSpeedKmh = c.windSpeed10m ?: 0.0,
                    windDirectionDeg = c.windDirection10m ?: 0.0,
                    cloudCoverPct = c.cloudCover ?: 0,
                    pressureHpa = c.surfacePressure ?: 1013.2
                )
            }
        }

    suspend fun fetchMapAmenities(
        lat: Double,
        lon: Double,
        category: AmenityCategory,
        radiusMeters: Int = 3500
    ): Result<List<MapAmenity>> = withContext(Dispatchers.IO) {
        runCatching {
            val filterExpr = category.overpassFilter
            val query = String.format(
                Locale.US,
                "[out:json][timeout:12];(node%s(around:%d,%.5f,%.5f);way%s(around:%d,%.5f,%.5f););out center 55;",
                filterExpr,
                radiusMeters,
                lat,
                lon,
                filterExpr,
                radiusMeters,
                lat,
                lon
            )
            val res = api.getOverpassTrafficWays(query)
            res.elements.orEmpty().mapNotNull { el ->
                val aLat = el.lat ?: el.center?.lat ?: return@mapNotNull null
                val aLon = el.lon ?: el.center?.lon ?: return@mapNotNull null
                val tags = el.tags.orEmpty()
                val rawSub = tags["amenity"] ?: tags["tourism"] ?: tags["public_transport"] ?: tags["railway"] ?: "place"
                val name = tags["name"]?.trim()?.takeIf { it.isNotEmpty() }
                    ?: rawSub.replace("_", " ").replaceFirstChar { it.uppercase() }
                val resolvedCat = when (rawSub.lowercase(Locale.US)) {
                    "hospital", "clinic", "pharmacy", "doctors" -> AmenityCategory.HOSPITAL
                    "restaurant", "cafe", "fast_food", "food_court" -> AmenityCategory.FOOD
                    "fuel", "charging_station" -> AmenityCategory.FUEL
                    "station", "bus_station", "subway_entrance", "ferry_terminal" -> AmenityCategory.TRANSIT
                    "hotel", "hostel", "motel", "guest_house" -> AmenityCategory.LODGING
                    "police", "fire_station", "townhall" -> AmenityCategory.SAFETY
                    "bank", "atm", "parking" -> AmenityCategory.FINANCE_PARK
                    else -> if (category == AmenityCategory.ALL) AmenityCategory.ALL else category
                }
                val addr = listOfNotNull(
                    tags["addr:housenumber"],
                    tags["addr:street"],
                    tags["addr:city"]
                ).joinToString(" ").trim()
                MapAmenity(
                    id = "am_${el.id ?: "${aLat}_${aLon}"}",
                    name = name,
                    category = resolvedCat,
                    subType = rawSub.replace("_", " ").uppercase(Locale.US),
                    lat = aLat,
                    lon = aLon,
                    address = addr,
                    openingHours = tags["opening_hours"].orEmpty()
                )
            }
                .sortedBy { Globe3DEngine.haversineKm(lat, lon, it.lat, it.lon) }
                .take(45)
        }
    }

    suspend fun searchPlaces(query: String): Result<List<GeocodePlaceResult>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val trimmed = query.trim()
                if (trimmed.isEmpty()) return@runCatching emptyList()
                api.searchPlacesNominatim(trimmed).mapNotNull { dto ->
                    val lat = dto.lat?.toDoubleOrNull() ?: return@mapNotNull null
                    val lon = dto.lon?.toDoubleOrNull() ?: return@mapNotNull null
                    val full = dto.displayName?.trim().orEmpty()
                    val shortName = dto.name?.trim()?.takeIf { it.isNotEmpty() }
                        ?: full.substringBefore(",").trim().ifEmpty { "Target" }
                    GeocodePlaceResult(
                        name = shortName,
                        displayName = full.ifEmpty { shortName },
                        lat = lat,
                        lon = lon,
                        category = dto.type?.uppercase(Locale.US) ?: "PLACE"
                    )
                }
            }
        }

    suspend fun computeNavigationRoute(
        originLat: Double,
        originLon: Double,
        originLabel: String,
        destLat: Double,
        destLon: Double,
        destLabel: String,
        mode: NavigationTravelMode
    ): NavigationRoute = withContext(Dispatchers.IO) {
        if (mode != NavigationTravelMode.AIR_DIRECT) {
            val osrmResult = runCatching {
                val url = String.format(
                    Locale.US,
                    "https://router.project-osrm.org/route/v1/%s/%.5f,%.5f;%.5f,%.5f?overview=full&geometries=geojson&steps=true",
                    mode.osrmProfile,
                    originLon,
                    originLat,
                    destLon,
                    destLat
                )
                val response = api.getOsrmRoute(url)
                val first = response.routes?.firstOrNull()
                    ?: error("No road route returned")
                val coords = first.geometry?.coordinates.orEmpty().mapNotNull { pair ->
                    if (pair.size >= 2) GeoPoint3D(lat = pair[1], lon = pair[0], altitudeKm = 0.25) else null
                }
                val steps = first.legs.orEmpty().flatMap { it.steps.orEmpty() }.mapNotNull { step ->
                    val loc = step.maneuver?.location
                    val sLat = loc?.getOrNull(1) ?: originLat
                    val sLon = loc?.getOrNull(0) ?: originLon
                    val type = step.maneuver?.type?.replace("_", " ") ?: "continue"
                    val mod = step.maneuver?.modifier?.trim().orEmpty()
                    val road = step.name?.trim().orEmpty()
                    val formatted = buildString {
                        append(type.replaceFirstChar { it.uppercase() })
                        if (mod.isNotEmpty()) append(" ").append(mod)
                        if (road.isNotEmpty()) append(" onto ").append(road)
                    }.trim()
                    NavigationStep(
                        instruction = formatted.ifEmpty { "Continue ahead" },
                        roadName = road.ifEmpty { "Route segment" },
                        distanceMeters = step.distance ?: 0.0,
                        durationSeconds = step.duration ?: 0.0,
                        lat = sLat,
                        lon = sLon,
                        maneuverType = step.maneuver?.type ?: "turn"
                    )
                }
                NavigationRoute(
                    originLabel = originLabel,
                    destinationLabel = destLabel,
                    originLat = originLat,
                    originLon = originLon,
                    destinationLat = destLat,
                    destinationLon = destLon,
                    travelMode = mode,
                    distanceMeters = first.distance ?: 0.0,
                    durationSeconds = first.duration ?: 0.0,
                    sourceLabel = "OSRM TURN-BY-TURN (${mode.label})",
                    coordinates = coords.ifEmpty {
                        listOf(GeoPoint3D(originLat, originLon, 0.25), GeoPoint3D(destLat, destLon, 0.25))
                    },
                    steps = steps
                )
            }.getOrNull()

            if (osrmResult != null && osrmResult.coordinates.size >= 2) {
                return@withContext osrmResult
            }
        }

        buildDirectCompassRoute(
            originLat = originLat,
            originLon = originLon,
            originLabel = originLabel,
            destLat = destLat,
            destLon = destLon,
            destLabel = destLabel,
            mode = mode
        )
    }

    companion object {
        fun buildDirectCompassRoute(
            originLat: Double,
            originLon: Double,
            originLabel: String,
            destLat: Double,
            destLon: Double,
            destLabel: String,
            mode: NavigationTravelMode
        ): NavigationRoute {
            val distanceKm = Globe3DEngine.haversineKm(originLat, originLon, destLat, destLon)
            val distanceMeters = distanceKm * 1000.0
            val speedKmh = when (mode) {
                NavigationTravelMode.AIR_DIRECT -> 820.0
                NavigationTravelMode.DRIVE -> 65.0
                NavigationTravelMode.BIKE -> 18.0
                NavigationTravelMode.WALK -> 5.0
            }
            val durationSeconds = (distanceKm / speedKmh) * 3600.0
            val arcAltKm = if (mode == NavigationTravelMode.AIR_DIRECT) (distanceKm * 0.04).coerceIn(1.5, 420.0) else 0.5
            val arcCoords = Globe3DEngine.greatCircleArc(
                a = GeoCoord(originLat, originLon),
                b = GeoCoord(destLat, destLon),
                steps = 28
            ).mapIndexed { idx, pt ->
                val frac = idx / 28.0
                val bell = kotlin.math.sin(frac * Math.PI)
                GeoPoint3D(pt.lat, pt.lon, arcAltKm * bell)
            }
            val bearingDeg = com.example.ui.components.TelemetryCalculator.initialBearingDeg(
                originLat,
                originLon,
                destLat,
                destLon
            )
            val cardinal = com.example.ui.components.TelemetryCalculator.compassCardinal(bearingDeg)
            val steps = listOf(
                NavigationStep(
                    instruction = String.format(
                        Locale.US,
                        "Head %s (%.0f°) toward %s",
                        cardinal,
                        bearingDeg,
                        destLabel
                    ),
                    roadName = if (mode == NavigationTravelMode.AIR_DIRECT) "Great-Circle Air Corridor" else "Offline Compass Bearing",
                    distanceMeters = distanceMeters,
                    durationSeconds = durationSeconds,
                    lat = originLat,
                    lon = originLon,
                    maneuverType = "depart"
                ),
                NavigationStep(
                    instruction = "Arrive at $destLabel",
                    roadName = "Destination Waypoint",
                    distanceMeters = 0.0,
                    durationSeconds = 0.0,
                    lat = destLat,
                    lon = destLon,
                    maneuverType = "arrive"
                )
            )
            return NavigationRoute(
                originLabel = originLabel,
                destinationLabel = destLabel,
                originLat = originLat,
                originLon = originLon,
                destinationLat = destLat,
                destinationLon = destLon,
                travelMode = mode,
                distanceMeters = distanceMeters,
                durationSeconds = durationSeconds,
                sourceLabel = if (mode == NavigationTravelMode.AIR_DIRECT) "GREAT-CIRCLE AIR CORRIDOR" else "OFFLINE COMPASS BEARING",
                coordinates = arcCoords,
                steps = steps
            )
        }
    }
}

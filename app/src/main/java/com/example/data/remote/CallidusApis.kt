package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

// -------------------- AIRPLANES.LIVE ADS-B --------------------
@JsonClass(generateAdapter = true)
data class AirplanesLiveResponse(
    @Json(name = "ac") val aircraft: List<AirplanesLiveItem>? = null,
    @Json(name = "now") val now: Long? = null
)

@JsonClass(generateAdapter = true)
data class AirplanesLiveItem(
    @Json(name = "hex") val hex: String? = null,
    @Json(name = "flight") val flight: String? = null,
    @Json(name = "r") val registration: String? = null,
    @Json(name = "t") val type: String? = null,
    @Json(name = "lat") val lat: Double? = null,
    @Json(name = "lon") val lon: Double? = null,
    @Json(name = "alt_baro") val altBaro: Any? = null,
    @Json(name = "gs") val groundSpeed: Double? = null,
    @Json(name = "track") val track: Double? = null,
    @Json(name = "baro_rate") val baroRate: Int? = null,
    @Json(name = "squawk") val squawk: String? = null,
    @Json(name = "category") val category: String? = null
)

// -------------------- OPENSKY NETWORK FALLBACK --------------------
@JsonClass(generateAdapter = true)
data class OpenSkyResponse(
    @Json(name = "time") val time: Long? = null,
    @Json(name = "states") val states: List<List<Any?>>? = null
)

// -------------------- CELESTRAK GP JSON --------------------
@JsonClass(generateAdapter = true)
data class CelesTrakGpItem(
    @Json(name = "OBJECT_NAME") val objectName: String? = null,
    @Json(name = "NORAD_CAT_ID") val noradCatId: Int? = null,
    @Json(name = "EPOCH") val epoch: String? = null,
    @Json(name = "MEAN_MOTION") val meanMotion: Double? = null,
    @Json(name = "ECCENTRICITY") val eccentricity: Double? = null,
    @Json(name = "INCLINATION") val inclination: Double? = null,
    @Json(name = "RA_OF_ASC_NODE") val raOfAscNode: Double? = null,
    @Json(name = "ARG_OF_PERICENTER") val argOfPericenter: Double? = null,
    @Json(name = "MEAN_ANOMALY") val meanAnomaly: Double? = null
)

// -------------------- WHERETHEISS.AT LIVE ISS --------------------
@JsonClass(generateAdapter = true)
data class IssLiveResponse(
    @Json(name = "id") val id: Int? = null,
    @Json(name = "latitude") val latitude: Double? = null,
    @Json(name = "longitude") val longitude: Double? = null,
    @Json(name = "altitude") val altitude: Double? = null,
    @Json(name = "velocity") val velocity: Double? = null
)

// -------------------- TFL JAMCAM LONDON CCTV --------------------
@JsonClass(generateAdapter = true)
data class TflJamCamPlace(
    @Json(name = "id") val id: String? = null,
    @Json(name = "commonName") val commonName: String? = null,
    @Json(name = "lat") val lat: Double? = null,
    @Json(name = "lon") val lon: Double? = null,
    @Json(name = "additionalProperties") val additionalProperties: List<TflAdditionalProperty>? = null
)

@JsonClass(generateAdapter = true)
data class TflAdditionalProperty(
    @Json(name = "key") val key: String? = null,
    @Json(name = "value") val value: String? = null
)

// -------------------- AUSTIN TX TRAFFIC CAMERAS --------------------
@JsonClass(generateAdapter = true)
data class AustinCameraItem(
    @Json(name = "camera_id") val cameraId: String? = null,
    @Json(name = "location_name") val locationName: String? = null,
    @Json(name = "screenshot_address") val screenshotAddress: String? = null,
    @Json(name = "camera_status") val cameraStatus: String? = null,
    @Json(name = "location") val location: AustinCameraLocation? = null
)

@JsonClass(generateAdapter = true)
data class AustinCameraLocation(
    @Json(name = "coordinates") val coordinates: List<Double>? = null // [lon, lat]
)

// -------------------- GLOBAL PUBLIC CCTV FEEDS (NYC / CALTRANS / SINGAPORE / FINLAND) --------------------
@JsonClass(generateAdapter = true)
data class NycCameraItem(
    @Json(name = "id") val id: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "latitude") val latitude: Double? = null,
    @Json(name = "longitude") val longitude: Double? = null,
    @Json(name = "area") val area: String? = null,
    @Json(name = "isOnline") val isOnline: String? = null,
    @Json(name = "imageUrl") val imageUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class CaltransResponse(
    @Json(name = "data") val data: List<CaltransDataWrapper>? = null
)

@JsonClass(generateAdapter = true)
data class CaltransDataWrapper(
    @Json(name = "cctv") val cctv: CaltransCctvDto? = null
)

@JsonClass(generateAdapter = true)
data class CaltransCctvDto(
    @Json(name = "index") val index: String? = null,
    @Json(name = "inService") val inService: String? = null,
    @Json(name = "location") val location: CaltransLocationDto? = null,
    @Json(name = "imageData") val imageData: CaltransImageDataDto? = null
)

@JsonClass(generateAdapter = true)
data class CaltransLocationDto(
    @Json(name = "locationName") val locationName: String? = null,
    @Json(name = "nearbyPlace") val nearbyPlace: String? = null,
    @Json(name = "latitude") val latitude: String? = null,
    @Json(name = "longitude") val longitude: String? = null,
    @Json(name = "direction") val direction: String? = null
)

@JsonClass(generateAdapter = true)
data class CaltransImageDataDto(
    @Json(name = "streamingVideoURL") val streamingVideoUrl: String? = null,
    @Json(name = "static") val staticData: CaltransStaticImageDto? = null
)

@JsonClass(generateAdapter = true)
data class CaltransStaticImageDto(
    @Json(name = "currentImageURL") val currentImageUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class SingaporeTrafficResponse(
    @Json(name = "items") val items: List<SingaporeTrafficItem>? = null
)

@JsonClass(generateAdapter = true)
data class SingaporeTrafficItem(
    @Json(name = "cameras") val cameras: List<SingaporeCameraDto>? = null
)

@JsonClass(generateAdapter = true)
data class SingaporeCameraDto(
    @Json(name = "camera_id") val cameraId: String? = null,
    @Json(name = "image") val image: String? = null,
    @Json(name = "location") val location: SingaporeCamLocDto? = null
)

@JsonClass(generateAdapter = true)
data class SingaporeCamLocDto(
    @Json(name = "latitude") val latitude: Double? = null,
    @Json(name = "longitude") val longitude: Double? = null
)

@JsonClass(generateAdapter = true)
data class FinlandWeathercamResponse(
    @Json(name = "features") val features: List<FinlandWeathercamFeature>? = null
)

@JsonClass(generateAdapter = true)
data class FinlandWeathercamFeature(
    @Json(name = "id") val id: String? = null,
    @Json(name = "geometry") val geometry: FinlandCamGeometry? = null,
    @Json(name = "properties") val properties: FinlandCamProperties? = null
)

@JsonClass(generateAdapter = true)
data class FinlandCamGeometry(
    @Json(name = "coordinates") val coordinates: List<Double>? = null
)

@JsonClass(generateAdapter = true)
data class FinlandCamProperties(
    @Json(name = "name") val name: String? = null,
    @Json(name = "presets") val presets: List<FinlandCamPreset>? = null
)

@JsonClass(generateAdapter = true)
data class FinlandCamPreset(
    @Json(name = "id") val id: String? = null,
    @Json(name = "inCollection") val inCollection: Boolean? = null
)

// -------------------- OSM OVERPASS STREET TRAFFIC & MAP AMENITIES --------------------
@JsonClass(generateAdapter = true)
data class OverpassResponse(
    @Json(name = "elements") val elements: List<OverpassElement>? = null
)

@JsonClass(generateAdapter = true)
data class OverpassElement(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "lat") val lat: Double? = null,
    @Json(name = "lon") val lon: Double? = null,
    @Json(name = "center") val center: OverpassGeomPt? = null,
    @Json(name = "tags") val tags: Map<String, String>? = null,
    @Json(name = "geometry") val geometry: List<OverpassGeomPt>? = null
)

@JsonClass(generateAdapter = true)
data class OverpassGeomPt(
    @Json(name = "lat") val lat: Double? = null,
    @Json(name = "lon") val lon: Double? = null
)

// -------------------- USGS EARTHQUAKES GEOJSON --------------------
@JsonClass(generateAdapter = true)
data class UsgsEarthquakeResponse(
    @Json(name = "features") val features: List<UsgsFeature>? = null
)

@JsonClass(generateAdapter = true)
data class UsgsFeature(
    @Json(name = "id") val id: String? = null,
    @Json(name = "properties") val properties: UsgsProperties? = null,
    @Json(name = "geometry") val geometry: UsgsGeometry? = null
)

@JsonClass(generateAdapter = true)
data class UsgsProperties(
    @Json(name = "mag") val mag: Double? = null,
    @Json(name = "place") val place: String? = null,
    @Json(name = "time") val time: Long? = null,
    @Json(name = "tsunami") val tsunami: Int? = null,
    @Json(name = "title") val title: String? = null
)

@JsonClass(generateAdapter = true)
data class UsgsGeometry(
    @Json(name = "coordinates") val coordinates: List<Double>? = null // [lon, lat, depthKm]
)

// -------------------- NASA EONET V3 --------------------
@JsonClass(generateAdapter = true)
data class EonetResponse(
    @Json(name = "events") val events: List<EonetEvent>? = null
)

@JsonClass(generateAdapter = true)
data class EonetEvent(
    @Json(name = "id") val id: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "categories") val categories: List<EonetCategory>? = null,
    @Json(name = "geometry") val geometry: List<EonetGeometry>? = null
)

@JsonClass(generateAdapter = true)
data class EonetCategory(
    @Json(name = "id") val id: String? = null,
    @Json(name = "title") val title: String? = null
)

@JsonClass(generateAdapter = true)
data class EonetGeometry(
    @Json(name = "date") val date: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "coordinates") val coordinates: List<Any>? = null
)

// -------------------- OPEN-METEO LOCAL TELEMETRY --------------------
@JsonClass(generateAdapter = true)
data class OpenMeteoResponse(
    @Json(name = "current") val current: OpenMeteoCurrent? = null
)

@JsonClass(generateAdapter = true)
data class OpenMeteoCurrent(
    @Json(name = "temperature_2m") val temperature2m: Double? = null,
    @Json(name = "wind_speed_10m") val windSpeed10m: Double? = null,
    @Json(name = "wind_direction_10m") val windDirection10m: Double? = null,
    @Json(name = "cloud_cover") val cloudCover: Int? = null,
    @Json(name = "surface_pressure") val surfacePressure: Double? = null
)

// -------------------- NOMINATIM GEOCODING --------------------
@JsonClass(generateAdapter = true)
data class NominatimPlaceDto(
    @Json(name = "display_name") val displayName: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "lat") val lat: String? = null,
    @Json(name = "lon") val lon: String? = null,
    @Json(name = "type") val type: String? = null
)

// -------------------- OSRM TURN-BY-TURN ROUTING --------------------
@JsonClass(generateAdapter = true)
data class OsrmRouteResponse(
    @Json(name = "code") val code: String? = null,
    @Json(name = "routes") val routes: List<OsrmRouteDto>? = null
)

@JsonClass(generateAdapter = true)
data class OsrmRouteDto(
    @Json(name = "distance") val distance: Double? = null,
    @Json(name = "duration") val duration: Double? = null,
    @Json(name = "geometry") val geometry: OsrmGeometryDto? = null,
    @Json(name = "legs") val legs: List<OsrmLegDto>? = null
)

@JsonClass(generateAdapter = true)
data class OsrmGeometryDto(
    @Json(name = "coordinates") val coordinates: List<List<Double>>? = null
)

@JsonClass(generateAdapter = true)
data class OsrmLegDto(
    @Json(name = "steps") val steps: List<OsrmStepDto>? = null
)

@JsonClass(generateAdapter = true)
data class OsrmStepDto(
    @Json(name = "distance") val distance: Double? = null,
    @Json(name = "duration") val duration: Double? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "maneuver") val maneuver: OsrmManeuverDto? = null
)

@JsonClass(generateAdapter = true)
data class OsrmManeuverDto(
    @Json(name = "type") val type: String? = null,
    @Json(name = "modifier") val modifier: String? = null,
    @Json(name = "location") val location: List<Double>? = null
)

interface CallidusNetworkApi {

    @GET("https://api.airplanes.live/v2/point/{lat}/{lon}/{radius}")
    suspend fun getAirplanesNearPoint(
        @Path("lat") lat: Double,
        @Path("lon") lon: Double,
        @Path("radius") radiusNm: Int = 250
    ): AirplanesLiveResponse

    @GET("https://opensky-network.org/api/states/all")
    suspend fun getOpenSkyBox(
        @Query("lamin") laMin: Double,
        @Query("lomin") loMin: Double,
        @Query("lamax") laMax: Double,
        @Query("lomax") loMax: Double
    ): OpenSkyResponse

    @GET
    suspend fun getCelesTrakGroup(@Url url: String): List<CelesTrakGpItem>

    @GET("https://api.wheretheiss.at/v1/satellites/25544")
    suspend fun getIssLive(): IssLiveResponse

    @GET("https://api.tfl.gov.uk/Place/Type/JamCam")
    suspend fun getTflJamCams(): List<TflJamCamPlace>

    @GET("https://data.austintexas.gov/resource/b4k4-adkb.json?%24limit=100")
    suspend fun getAustinCameras(): List<AustinCameraItem>

    @GET("https://webcams.nyctmc.org/api/cameras")
    suspend fun getNycCameras(): List<NycCameraItem>

    @GET("https://cwwp2.dot.ca.gov/data/d4/cctv/cctvStatusD04.json")
    suspend fun getCaltransD4Cameras(): CaltransResponse

    @GET("https://cwwp2.dot.ca.gov/data/d7/cctv/cctvStatusD07.json")
    suspend fun getCaltransD7Cameras(): CaltransResponse

    @GET("https://api.data.gov.sg/v1/transport/traffic-images")
    suspend fun getSingaporeCameras(): SingaporeTrafficResponse

    @GET("https://tie.digitraffic.fi/api/weathercam/v1/stations")
    suspend fun getFinlandWeatherCams(): FinlandWeathercamResponse

    @Streaming
    @GET("https://meri.digitraffic.fi/api/ais/v1/locations")
    suspend fun streamMaritimeAisLocations(
        @Header("Digitraffic-User") userHeader: String = "CallidusOrbitalIntel/1.0"
    ): ResponseBody

    @GET("https://overpass-api.de/api/interpreter")
    suspend fun getOverpassTrafficWays(
        @Query("data") query: String
    ): OverpassResponse

    @GET("https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/all_day.geojson")
    suspend fun getUsgsEarthquakes(): UsgsEarthquakeResponse

    @GET("https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/all_hour.geojson")
    suspend fun getUsgsEarthquakesLastHour(): UsgsEarthquakeResponse

    @GET("https://eonet.gsfc.nasa.gov/api/v3/events?status=open&limit=65")
    suspend fun getNasaEonetEvents(): EonetResponse

    @GET("https://services.swpc.noaa.gov/products/noaa-planetary-k-index.json")
    suspend fun getNoaaKpIndex(): List<List<String>>

    @GET("https://api.open-meteo.com/v1/forecast")
    suspend fun getLocalWeather(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("current") current: String = "temperature_2m,wind_speed_10m,wind_direction_10m,cloud_cover,surface_pressure"
    ): OpenMeteoResponse

    @GET("https://nominatim.openstreetmap.org/search")
    suspend fun searchPlacesNominatim(
        @Query("q") query: String,
        @Query("format") format: String = "jsonv2",
        @Query("limit") limit: Int = 6
    ): List<NominatimPlaceDto>

    @GET
    suspend fun getOsrmRoute(@Url url: String): OsrmRouteResponse
}

object CallidusRetrofitProvider {
    val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(18, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("User-Agent", "CallidusOrbitalIntel/1.0 (Android)")
                    .header("Accept", "application/json, image/*")
                    .build()
                chain.proceed(req)
            }
            .addInterceptor(logging)
            .build()
    }

    val api: CallidusNetworkApi by lazy {
        val moshi = com.squareup.moshi.Moshi.Builder()
            .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
            .build()

        Retrofit.Builder()
            .baseUrl("https://api.airplanes.live/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(CallidusNetworkApi::class.java)
    }
}

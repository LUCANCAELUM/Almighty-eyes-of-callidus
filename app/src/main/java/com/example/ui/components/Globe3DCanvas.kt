package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.testTag
import java.util.Locale
import com.example.domain.model.AircraftTarget
import com.example.domain.model.CallidusUiState
import com.example.domain.model.CctvCameraTarget
import com.example.domain.model.EarthquakeTarget
import com.example.domain.model.GlobeImageryStyle
import com.example.domain.model.GlobalLandmarks
import com.example.domain.model.IntelLayer
import com.example.domain.model.MaritimeVesselTarget
import com.example.domain.model.NaturalEventTarget
import com.example.domain.model.NavigationRoute
import com.example.domain.model.OpticMode
import com.example.domain.model.SatelliteCategory
import com.example.domain.model.SatelliteTarget
import com.example.domain.model.SelectedTarget
import com.example.domain.model.TrafficRoadSegment
import com.example.engine.GeoCoord
import com.example.engine.Globe3DEngine
import com.example.engine.LiveGpsCompassState
import com.example.engine.ProjectedPoint
import com.example.engine.SatelliteTileKey
import com.example.engine.SatelliteTileManager
import com.example.engine.Vec3
import com.example.engine.WorldGeospatialData
import com.example.ui.theme.OpticPalette
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

private data class Star3D(
    val dir: Vec3,
    val size: Float,
    val brightness: Float,
    val depthLayer: Float
)

private data class ScreenHitTarget(
    val x: Float,
    val y: Float,
    val target: SelectedTarget
)

@Composable
fun Globe3DCanvas(
    state: CallidusUiState,
    onDrag: (dx: Float, dy: Float, minDim: Float) -> Unit,
    onPinchTilt: (zoomFactor: Float, twoFingerPanY: Float) -> Unit,
    onTargetSelected: (SelectedTarget) -> Unit,
    modifier: Modifier = Modifier,
    gpsCompassState: LiveGpsCompassState? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "globe_anim")
    val pulsePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )
    val packetPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "packets"
    )
    val radarPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(7200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar"
    )

    // Sweeping 3D optic transition iris wipe
    val opticWipe = remember { Animatable(1f) }
    var previousOptic by remember { mutableStateOf(state.opticMode) }
    LaunchedEffect(state.opticMode) {
        if (state.opticMode != previousOptic) {
            previousOptic = state.opticMode
            opticWipe.snapTo(0f)
            opticWipe.animateTo(1f, animationSpec = tween(550))
        }
    }

    val stars = remember {
        val rng = java.util.Random(42019L)
        List(240) { i ->
            val theta = rng.nextDouble() * 2.0 * PI
            val phi = kotlin.math.acos(2.0 * rng.nextDouble() - 1.0)
            Star3D(
                dir = Vec3(
                    (sin(phi) * cos(theta)).toFloat(),
                    cos(phi).toFloat(),
                    (sin(phi) * sin(theta)).toFloat()
                ),
                size = 0.8f + rng.nextFloat() * 1.9f,
                brightness = 0.25f + rng.nextFloat() * 0.75f,
                depthLayer = if (i % 3 == 0) 1.0f else if (i % 3 == 1) 0.7f else 0.45f
            )
        }
    }

    val tileScope = rememberCoroutineScope()
    val tileManager = remember(tileScope) { SatelliteTileManager(tileScope) }
    val tileRevision by tileManager.tileRevision.collectAsState()

    LaunchedEffect(state.imageryStyle) {
        tileManager.prewarmGlobalBaseTiles(state.imageryStyle)
    }

    val hitTargets = remember { mutableListOf<ScreenHitTarget>() }
    val seismicLabelPaint = remember {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
            textSize = 24f
        }
    }
    val palette = state.opticMode.palette

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .testTag("callidus_3d_globe")
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var totalDrag = 0f
                    var wasMultiTouch = false
                    val downPos = down.position

                    while (true) {
                        val event = awaitPointerEvent()
                        val pressedCount = event.changes.count { it.pressed }
                        if (pressedCount == 0) {
                            if (!wasMultiTouch && totalDrag < 18f) {
                                val hit = hitTargets.minByOrNull {
                                    hypot((it.x - downPos.x).toDouble(), (it.y - downPos.y).toDouble())
                                }
                                if (hit != null && hypot((hit.x - downPos.x).toDouble(), (hit.y - downPos.y).toDouble()) < 72.0) {
                                    onTargetSelected(hit.target)
                                }
                            }
                            break
                        }

                        if (pressedCount >= 2) {
                            wasMultiTouch = true
                            val zoomChange = event.calculateZoom()
                            val pan = event.calculatePan()
                            if (abs(zoomChange - 1f) > 0.002f || abs(pan.y) > 0.5f) {
                                onPinchTilt(zoomChange, pan.y)
                            }
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        } else {
                            val change = event.changes.firstOrNull { it.pressed } ?: break
                            val delta = change.position - change.previousPosition
                            val dist = hypot(delta.x.toDouble(), delta.y.toDouble()).toFloat()
                            if (dist > 0.2f) {
                                totalDrag += dist
                                val minDim = minOf(size.width, size.height).toFloat()
                                onDrag(delta.x, delta.y, minDim)
                                change.consume()
                            }
                        }
                    }
                }
            }
    ) {
        hitTargets.clear()

        val engine = Globe3DEngine(
            viewportWidth = size.width,
            viewportHeight = size.height,
            camera = state.camera
        )

        // 1. Deep Space Gradient Backdrop
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(palette.spaceBgTop, palette.spaceBgBottom, palette.spaceBgTop)
            )
        )

        // 2. 3D Rotating Starfield Parallax
        drawStarfield3D(engine, stars, palette.hudAccent)

        // 3. Outer Atmospheric Rayleigh Glow Halo
        if (state.showAtmosphereGlow) {
            val atmRadius = engine.baseRadius * 1.21f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.atmosphereGlow.copy(alpha = 0.26f),
                        palette.atmosphereGlow.copy(alpha = 0.09f),
                        Color.Transparent
                    ),
                    center = Offset(engine.centerX, engine.centerY),
                    radius = atmRadius
                ),
                radius = atmRadius,
                center = Offset(engine.centerX, engine.centerY)
            )
        }

        // 4. Base 3D Earth Sphere with Dynamic Solar Terminator Lighting
        drawEarthSphereBody(engine, state.opticMode, palette)

        // 4b. Live 3D Globe Imagery Mesh (Standard Vector / Satellite Ortho / Terrain Relief)
        @Suppress("UNUSED_EXPRESSION")
        tileRevision
        val hasImageryBase = drawSatelliteGlobeTexture3D(
            engine = engine,
            tileManager = tileManager,
            imageryStyle = state.imageryStyle,
            imageryOpacity = state.imageryOpacity,
            centerLat = state.camera.centerLat,
            centerLon = state.camera.centerLon,
            zoom = state.camera.zoom,
            opticMode = state.opticMode
        )

        // 5. 3D Graticule Meridians & Parallels
        if (state.showGraticuleGrid) {
            drawGraticule3D(engine, palette.graticuleStroke, state.opticMode == OpticMode.WIREFRAME_HOLO)
        }

        // 6. 3D Tectonic Plate Boundaries & Subsea Fiber-Optic Corridors
        if (IntelLayer.SUBSEA_CABLES in state.enabledLayers) {
            drawTectonicLines3D(engine, palette.quakeColor.copy(alpha = 0.25f))
            drawSubseaCables3D(engine, palette.cableColor, palette.hudAccent, packetPhase)
        }

        // 7. 3D Continental Vector Outlines (transparent fill over basemap imagery; solid fill if loading or blended)
        drawContinents3D(engine, state.opticMode, palette, fillLand = !hasImageryBase || state.imageryOpacity < 0.55f)

        // 8. Night-Side City Lights (within Solar Terminator shadow)
        drawCityLights3D(engine, state.opticMode, palette)

        // 9. Magnetosphere & Aurora Ovals driven by live NOAA Kp index
        if (IntelLayer.SOLAR_WEATHER in state.enabledLayers) {
            drawAuroraOvals3D(engine, state.spaceWeather.kpIndex, pulsePhase, palette.hudAccent)
        }

        // 10. Volumetric Orbital Radar Sweep Arc across Sphere
        drawOrbitalRadarSweep(engine, radarPhase, palette.hudAccent)

        // 11. Limb Fresnel Rim Highlight
        if (state.showAtmosphereGlow) {
            drawCircle(
                color = palette.atmosphereGlow.copy(alpha = 0.42f),
                radius = engine.baseRadius * 1.005f,
                center = Offset(engine.centerX, engine.centerY),
                style = Stroke(width = 2.4f)
            )
        }

        // 12. 3D Street Traffic Vector Arteries & Animated Flow Particles (OSM Overpass)
        if (IntelLayer.TRAFFIC_FLOW in state.enabledLayers && state.trafficSegments.isNotEmpty()) {
            drawTrafficFlow3D(engine, state.trafficSegments, palette.trafficColor, packetPhase)
        }

        // 13. 3D Public CCTV Cameras & Calibratable Viewshed Cones
        if (IntelLayer.CCTV_CAMERAS in state.enabledLayers) {
            val selectedCctvId = (state.selectedTarget as? SelectedTarget.Cctv)?.data?.id
            state.cctvCameras.forEach { cam ->
                drawCctvViewshed3D(
                    engine = engine,
                    cam = cam,
                    isSelected = cam.id == selectedCctvId,
                    zoom = state.camera.zoom,
                    cctvColor = palette.cctvColor,
                    pulsePhase = pulsePhase,
                    hitTargets = hitTargets
                )
            }
        }

        // 14. 3D Live Maritime AIS Vessels
        if (IntelLayer.MARITIME_AIS in state.enabledLayers) {
            state.maritimeVessels.forEach { vessel ->
                drawMaritimeVessel3D(engine, vessel, palette.shipColor, state.camera.zoom, hitTargets)
            }
        }

        // 15. 3D Real-Time Pulsating Seismic Nodes & Spherical Shockwaves
        if (IntelLayer.SEISMIC_USGS in state.enabledLayers) {
            val nowMs = System.currentTimeMillis()
            val selectedQuakeId = (state.selectedTarget as? SelectedTarget.Quake)?.data?.id
            state.earthquakes.forEach { quake ->
                drawEarthquake3D(
                    engine = engine,
                    quake = quake,
                    pulsePhase = pulsePhase,
                    nowMs = nowMs,
                    zoom = state.camera.zoom,
                    isSelected = quake.id == selectedQuakeId,
                    quakeColor = palette.quakeColor,
                    labelPaint = seismicLabelPaint,
                    hitTargets = hitTargets
                )
            }
        }

        // 16. 3D NASA EONET Natural Events (Wildfires, Storms, Volcanoes)
        if (IntelLayer.NATURAL_EONET in state.enabledLayers) {
            state.naturalEvents.forEach { ev ->
                drawNaturalEvent3D(engine, ev, pulsePhase, palette.eventColor, hitTargets)
            }
        }

        // 17. 3D ADS-B Live Aircraft Vectors
        if (IntelLayer.AIRCRAFT_ADSB in state.enabledLayers) {
            state.aircraft.forEach { flight ->
                drawAircraft3D(engine, flight, state.opticMode, palette.aircraftColor, state.camera.zoom, hitTargets)
            }
        }

        // 18. 3D Orbital Satellites, Elliptical Tracks & Ground Sensor Swaths
        if (IntelLayer.ORBITAL_SATS in state.enabledLayers) {
            state.satellites.forEach { sat ->
                drawSatellite3D(
                    engine = engine,
                    sat = sat,
                    isSelected = (state.selectedTarget as? SelectedTarget.Satellite)?.data?.element?.noradId == sat.element.noradId,
                    satColor = palette.satelliteColor,
                    goldColor = Color(0xFFFFB300),
                    pulsePhase = pulsePhase,
                    hitTargets = hitTargets
                )
            }
        }

        // 18b. God's Eye View Global Landmarks Diamond Beacons
        drawGlobalLandmarks3D(
            engine = engine,
            zoom = state.camera.zoom,
            accent = palette.hudAccent,
            labelPaint = seismicLabelPaint
        )

        // 18c. Active Turn-by-Turn / Compass Navigation Route Corridor on 3D Globe
        state.activeRoute?.let { route ->
            drawNavigationRoute3D(
                engine = engine,
                route = route,
                packetPhase = packetPhase,
                pulsePhase = pulsePhase,
                labelPaint = seismicLabelPaint
            )
        }

        // 18d. Live Hardware GPS Observer Position & Offline Breadcrumb Trail
        gpsCompassState?.let { gps ->
            drawGpsObserverTrail3D(
                engine = engine,
                gps = gps,
                pulsePhase = pulsePhase
            )
        }

        // 19. Active Target 3D Lock-On Reticle
        drawSelectedTargetReticle3D(engine, state.selectedTarget, pulsePhase, palette.hudAccent)

        // 20. Upgraded 8-Mode Optical Post-FX (CRT, NVG, FLIR, Noir, Snow, Holo, Iris + Corner Reticles)
        drawOpticPostFx(state.opticMode, palette.scanlineAlpha, palette.hudAccent, opticWipe.value, state.bootProgress, radarPhase)
    }
}

private fun DrawScope.drawStarfield3D(
    engine: Globe3DEngine,
    stars: List<Star3D>,
    accent: Color
) {
    val cx = size.width * 0.5f
    val cy = size.height * 0.5f
    val spread = maxOf(size.width, size.height) * 0.9f
    for (star in stars) {
        val cam = engine.worldToCamera(star.dir)
        if (cam.z > 0.05f) {
            val sx = cx + cam.x * spread * star.depthLayer
            val sy = cy - cam.y * spread * star.depthLayer
            if (sx in 0f..size.width && sy in 0f..size.height) {
                val color = if (star.size > 2.2f) accent.copy(alpha = star.brightness * 0.65f)
                else Color.White.copy(alpha = star.brightness * 0.72f)
                drawCircle(color = color, radius = star.size, center = Offset(sx, sy))
            }
        }
    }
}

private fun DrawScope.drawEarthSphereBody(
    engine: Globe3DEngine,
    opticMode: OpticMode,
    palette: OpticPalette
) {
    val center = Offset(engine.centerX, engine.centerY)
    val r = engine.baseRadius

    val sunCam = engine.worldToCamera(engine.sunWorldVec)
    val lightOffsetX = sunCam.x * r * 0.55f
    val lightOffsetY = -sunCam.y * r * 0.55f

    val dayColor = if (opticMode == OpticMode.SOLAR_TERMINATOR) {
        palette.globeOceanDay.copy(alpha = 0.98f)
    } else {
        palette.globeOceanDay
    }

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(dayColor, palette.globeOceanNight),
            center = Offset(center.x + lightOffsetX, center.y + lightOffsetY),
            radius = r * 1.45f
        ),
        radius = r,
        center = center
    )
}

private fun DrawScope.drawSatelliteGlobeTexture3D(
    engine: Globe3DEngine,
    tileManager: SatelliteTileManager,
    imageryStyle: GlobeImageryStyle,
    imageryOpacity: Float,
    centerLat: Double,
    centerLon: Double,
    zoom: Float,
    opticMode: OpticMode
): Boolean {
    val baseTiles = tileManager.getOrFallbackBaseTiles(imageryStyle)
    val detailKeys = SatelliteTileManager.detailTilesAroundCamera(centerLat, centerLon, zoom)
    val loadedDetail = detailKeys.mapNotNull { k -> tileManager.getTileBitmap(k, imageryStyle)?.let { k to it } }
    if (baseTiles.isEmpty() && loadedDetail.isEmpty()) return false

    val baseAlpha = if (opticMode == OpticMode.WIREFRAME_HOLO) 150 else 255
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        alpha = (baseAlpha * imageryOpacity.coerceIn(0.15f, 1.0f)).toInt().coerceIn(25, 255)
        colorFilter = satelliteAndroidColorFilter(opticMode)
    }

    drawIntoCanvas { canvas ->
        val native = canvas.nativeCanvas
        val clipPath = AndroidPath().apply {
            addCircle(engine.centerX, engine.centerY, engine.baseRadius, AndroidPath.Direction.CW)
        }
        native.save()
        native.clipPath(clipPath)

        for ((key, bmp) in baseTiles) {
            renderSatelliteTileMesh(native, engine, key, bmp, gridN = 6, opticMode = opticMode, paint = paint)
        }
        for ((key, bmp) in loadedDetail) {
            renderSatelliteTileMesh(native, engine, key, bmp, gridN = 4, opticMode = opticMode, paint = paint)
        }

        native.restore()
    }
    return baseTiles.isNotEmpty()
}

private fun renderSatelliteTileMesh(
    nativeCanvas: android.graphics.Canvas,
    engine: Globe3DEngine,
    key: SatelliteTileKey,
    bitmap: Bitmap,
    gridN: Int,
    opticMode: OpticMode,
    paint: Paint
) {
    val bounds = SatelliteTileManager.tileBounds(key)
    val midLat = (bounds.latNorth + bounds.latSouth) * 0.5
    val midLon = (bounds.lonWest + bounds.lonEast) * 0.5
    val midCam = engine.worldToCamera(engine.geoToWorldUnit(midLat, midLon))
    if (midCam.z < -0.35f) return

    val count = (gridN + 1) * (gridN + 1)
    val verts = FloatArray(count * 2)
    val colors = IntArray(count)
    var anyFront = false

    val focalRatio = 0.96f // matches Globe3DEngine focalLength/cameraDistance
    val isNightSensor = opticMode == OpticMode.FLIR_THERMAL ||
        opticMode == OpticMode.NVG_PHOSPHOR ||
        opticMode == OpticMode.CRT_MONITOR ||
        opticMode == OpticMode.WIREFRAME_HOLO

    var vIdx = 0
    for (row in 0..gridN) {
        val v = row.toFloat() / gridN
        val lat = bounds.latNorth + (bounds.latSouth - bounds.latNorth) * v
        for (col in 0..gridN) {
            val u = col.toFloat() / gridN
            val lon = bounds.lonWest + (bounds.lonEast - bounds.lonWest) * u

            val unitWorld = engine.geoToWorldUnit(lat, lon)
            val camUnit = engine.worldToCamera(unitWorld)

            if (camUnit.z > -0.05f) anyFront = true

            val pt = if (camUnit.z >= 0f) {
                engine.projectWorldVec(unitWorld, 1.0005f)
            } else {
                val lenXY = hypot(camUnit.x.toDouble(), camUnit.y.toDouble()).toFloat().coerceAtLeast(1e-4f)
                val rimX = (camUnit.x / lenXY) * 1.04f
                val rimY = (camUnit.y / lenXY) * 1.04f
                ProjectedPoint(
                    screenX = engine.centerX + rimX * engine.baseRadius * focalRatio,
                    screenY = engine.centerY - rimY * engine.baseRadius * focalRatio,
                    depthZ = 0f,
                    isVisible = false,
                    perspectiveScale = focalRatio,
                    camVec = camUnit
                )
            }

            verts[vIdx * 2] = pt.screenX
            verts[vIdx * 2 + 1] = pt.screenY

            val solar = engine.solarIlluminationAt(lat, lon)
            val limb = (0.78f + 0.22f * camUnit.z.coerceIn(0f, 1f))
            val light = if (isNightSensor) {
                (0.72f + 0.28f * solar) * limb
            } else {
                (0.32f + 0.68f * solar) * limb
            }.coerceIn(0.22f, 1f)
            val ch = (light * 255f).toInt().coerceIn(0, 255)
            colors[vIdx] = android.graphics.Color.argb(255, ch, ch, ch)
            vIdx++
        }
    }

    if (!anyFront) return

    nativeCanvas.drawBitmapMesh(
        bitmap,
        gridN,
        gridN,
        verts,
        0,
        colors,
        0,
        paint
    )
}

private fun satelliteAndroidColorFilter(opticMode: OpticMode): ColorMatrixColorFilter? {
    val cm = android.graphics.ColorMatrix()
    when (opticMode) {
        OpticMode.EO_TACTICAL, OpticMode.SOLAR_TERMINATOR -> return null
        OpticMode.FLIR_THERMAL -> cm.set(
            floatArrayOf(
                1.35f, 0.3f, 0.1f, 0f, 18f,
                0.22f, 0.68f, 0.12f, 0f, -10f,
                0.05f, 0.1f, 0.55f, 0f, -18f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        OpticMode.NVG_PHOSPHOR -> cm.set(
            floatArrayOf(
                0.1f, 0.22f, 0.05f, 0f, -12f,
                0.35f, 1.35f, 0.25f, 0f, 16f,
                0.1f, 0.35f, 0.15f, 0f, -6f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        OpticMode.CRT_MONITOR -> cm.set(
            floatArrayOf(
                0.24f, 0.35f, 0.2f, 0f, -8f,
                0.2f, 1.0f, 0.45f, 0f, 4f,
                0.35f, 0.5f, 1.35f, 0f, 14f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        OpticMode.NOIR_SURVEILLANCE -> {
            val c = 1.35f
            val r = 0.2126f * c
            val g = 0.7152f * c
            val b = 0.0722f * c
            val t = (-0.5f * c + 0.5f) * 255f
            cm.set(
                floatArrayOf(
                    r, g, b, 0f, t,
                    r, g, b, 0f, t,
                    r, g, b, 0f, t,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        }
        OpticMode.ARCTIC_SNOW -> cm.set(
            floatArrayOf(
                0.95f, 0.12f, 0.22f, 0f, 20f,
                0.1f, 1.05f, 0.22f, 0f, 22f,
                0.18f, 0.22f, 1.25f, 0f, 28f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        OpticMode.WIREFRAME_HOLO -> cm.set(
            floatArrayOf(
                0.15f, 0.2f, 0.45f, 0f, -15f,
                0.2f, 0.9f, 0.9f, 0f, 8f,
                0.4f, 0.7f, 1.45f, 0f, 22f,
                0f, 0f, 0f, 1f, 0f
            )
        )
    }
    return ColorMatrixColorFilter(cm)
}

private fun DrawScope.drawOrbitalRadarSweep(
    engine: Globe3DEngine,
    radarPhase: Float,
    accent: Color
) {
    val cx = engine.centerX
    val cy = engine.centerY
    val r = engine.baseRadius
    val startDeg = radarPhase * 360f
    drawArc(
        brush = Brush.sweepGradient(
            colors = listOf(
                Color.Transparent,
                accent.copy(alpha = 0.02f),
                accent.copy(alpha = 0.14f)
            ),
            center = Offset(cx, cy)
        ),
        startAngle = startDeg,
        sweepAngle = 52f,
        useCenter = true,
        topLeft = Offset(cx - r, cy - r),
        size = Size(r * 2f, r * 2f)
    )
}

private fun DrawScope.drawGraticule3D(
    engine: Globe3DEngine,
    strokeColor: Color,
    isHolo: Boolean
) {
    val strokeW = if (isHolo) 1.35f else 0.85f
    for (lat in listOf(-60, -30, 0, 30, 60)) {
        val path = Path()
        var started = false
        val isEquator = lat == 0
        for (lon in -180..180 step 8) {
            val pt = engine.project(lat.toDouble(), lon.toDouble(), 1.001f)
            if (pt.isVisible) {
                if (!started) {
                    path.moveTo(pt.screenX, pt.screenY)
                    started = true
                } else {
                    path.lineTo(pt.screenX, pt.screenY)
                }
            } else {
                started = false
            }
        }
        drawPath(
            path = path,
            color = if (isEquator) strokeColor.copy(alpha = (strokeColor.alpha * 2.1f).coerceAtMost(0.8f)) else strokeColor,
            style = Stroke(width = if (isEquator) strokeW * 1.5f else strokeW)
        )
    }

    for (lon in -150..180 step 30) {
        val path = Path()
        var started = false
        for (lat in -84..84 step 6) {
            val pt = engine.project(lat.toDouble(), lon.toDouble(), 1.001f)
            if (pt.isVisible) {
                if (!started) {
                    path.moveTo(pt.screenX, pt.screenY)
                    started = true
                } else {
                    path.lineTo(pt.screenX, pt.screenY)
                }
            } else {
                started = false
            }
        }
        drawPath(path = path, color = strokeColor, style = Stroke(width = strokeW))
    }
}

private fun DrawScope.drawContinents3D(
    engine: Globe3DEngine,
    opticMode: OpticMode,
    palette: OpticPalette,
    fillLand: Boolean
) {
    val strokeW = when (opticMode) {
        OpticMode.WIREFRAME_HOLO -> 2.1f
        OpticMode.CRT_MONITOR -> 1.8f
        else -> 1.35f
    }

    for (polygon in WorldGeospatialData.continents) {
        val path = Path()
        var hasVisible = false
        var started = false
        var avgSolar = 0f
        var count = 0

        for (i in polygon.indices) {
            val pt = polygon[i]
            val p = engine.project(pt.lat, pt.lon, 1.002f)
            if (p.isVisible) {
                hasVisible = true
                avgSolar += engine.solarIlluminationAt(pt.lat, pt.lon)
                count++
                if (!started) {
                    path.moveTo(p.screenX, p.screenY)
                    started = true
                } else {
                    path.lineTo(p.screenX, p.screenY)
                }
            } else {
                started = false
            }
        }

        if (hasVisible && count > 0) {
            val solarFactor = (avgSolar / count).coerceIn(0f, 1f)
            if (fillLand) {
                val fillColor = androidx.compose.ui.graphics.lerp(
                    palette.landFillNight,
                    palette.landFillDay,
                    solarFactor
                )
                drawPath(path = path, color = fillColor)
            }
            val borderAlpha = if (fillLand) (0.55f + 0.35f * solarFactor) else (0.28f + 0.28f * solarFactor)
            drawPath(
                path = path,
                color = palette.coastlineStroke.copy(alpha = borderAlpha),
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )
        }
    }
}

private fun DrawScope.drawCityLights3D(
    engine: Globe3DEngine,
    opticMode: OpticMode,
    palette: OpticPalette
) {
    val lightColor = when (opticMode) {
        OpticMode.NVG_PHOSPHOR -> Color(0xFF69F0AE)
        OpticMode.FLIR_THERMAL -> Color(0xFFFF6E40)
        OpticMode.NOIR_SURVEILLANCE -> Color(0xFFE0E4EC)
        else -> Color(0xFFFFD54F)
    }
    for (city in WorldGeospatialData.cityLightNodes) {
        val p = engine.project(city.lat, city.lon, 1.003f)
        if (!p.isVisible) continue
        val solar = engine.solarIlluminationAt(city.lat, city.lon)
        val nightFactor = if (opticMode == OpticMode.FLIR_THERMAL) 0.9f else (1f - solar).coerceIn(0.18f, 1f)
        drawCircle(
            color = lightColor.copy(alpha = 0.36f * nightFactor),
            radius = 6.0f * p.perspectiveScale,
            center = Offset(p.screenX, p.screenY)
        )
        drawCircle(
            color = lightColor.copy(alpha = 0.85f * nightFactor),
            radius = 1.9f * p.perspectiveScale,
            center = Offset(p.screenX, p.screenY)
        )
    }
}

private fun DrawScope.drawTectonicLines3D(engine: Globe3DEngine, color: Color) {
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
    for (line in WorldGeospatialData.tectonicLines) {
        val path = Path()
        var started = false
        for (coord in line) {
            val p = engine.project(coord.lat, coord.lon, 1.002f)
            if (p.isVisible) {
                if (!started) {
                    path.moveTo(p.screenX, p.screenY)
                    started = true
                } else {
                    path.lineTo(p.screenX, p.screenY)
                }
            } else {
                started = false
            }
        }
        drawPath(path = path, color = color, style = Stroke(width = 1.4f, pathEffect = dashEffect))
    }
}

private fun DrawScope.drawSubseaCables3D(
    engine: Globe3DEngine,
    cableColor: Color,
    pulseColor: Color,
    packetPhase: Float
) {
    for ((idx, corridor) in WorldGeospatialData.subseaCables.withIndex()) {
        val fullArc = mutableListOf<GeoCoord>()
        for (i in 0 until corridor.waypoints.lastIndex) {
            fullArc.addAll(Globe3DEngine.greatCircleArc(corridor.waypoints[i], corridor.waypoints[i + 1], 10))
        }
        val path = Path()
        var started = false
        val projectedArc = mutableListOf<ProjectedPoint>()
        for (pt in fullArc) {
            val p = engine.project(pt.lat, pt.lon, 1.004f)
            projectedArc.add(p)
            if (p.isVisible) {
                if (!started) {
                    path.moveTo(p.screenX, p.screenY)
                    started = true
                } else {
                    path.lineTo(p.screenX, p.screenY)
                }
            } else {
                started = false
            }
        }
        drawPath(path = path, color = cableColor, style = Stroke(width = 1.6f))

        if (projectedArc.isNotEmpty()) {
            val pIdx = (((packetPhase + idx * 0.21f) % 1f) * projectedArc.lastIndex).toInt()
            val pkt = projectedArc[pIdx]
            if (pkt.isVisible) {
                drawCircle(
                    color = pulseColor.copy(alpha = 0.9f),
                    radius = 3.2f * pkt.perspectiveScale,
                    center = Offset(pkt.screenX, pkt.screenY)
                )
            }
        }
    }
}

private fun DrawScope.drawTrafficFlow3D(
    engine: Globe3DEngine,
    segments: List<TrafficRoadSegment>,
    trafficColor: Color,
    packetPhase: Float
) {
    for ((idx, seg) in segments.withIndex()) {
        val path = Path()
        var started = false
        val projected = mutableListOf<ProjectedPoint>()
        for (pt in seg.points) {
            val p = engine.project(pt.lat, pt.lon, 1.0035f)
            projected.add(p)
            if (p.isVisible) {
                if (!started) {
                    path.moveTo(p.screenX, p.screenY)
                    started = true
                } else {
                    path.lineTo(p.screenX, p.screenY)
                }
            } else {
                started = false
            }
        }
        val isMotorway = seg.highwayClass == "motorway" || seg.highwayClass == "trunk"
        drawPath(
            path = path,
            color = trafficColor.copy(alpha = if (isMotorway) 0.65f else 0.38f),
            style = Stroke(width = if (isMotorway) 2.4f else 1.5f, cap = StrokeCap.Round)
        )

        // Glowing vehicle particles moving along real OSM road corridors
        if (projected.size >= 2) {
            for (car in 0..1) {
                val frac = (packetPhase * (if (isMotorway) 2.0f else 1.3f) + idx * 0.17f + car * 0.5f) % 1f
                val fIdx = frac * (projected.size - 1)
                val i0 = fIdx.toInt().coerceIn(0, projected.lastIndex)
                val i1 = (i0 + 1).coerceIn(0, projected.lastIndex)
                val sub = fIdx - i0
                val p0 = projected[i0]
                val p1 = projected[i1]
                if (p0.isVisible && p1.isVisible) {
                    val vx = p0.screenX + (p1.screenX - p0.screenX) * sub
                    val vy = p0.screenY + (p1.screenY - p0.screenY) * sub
                    drawCircle(
                        color = trafficColor.copy(alpha = 0.35f),
                        radius = 4.2f,
                        center = Offset(vx, vy)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 1.6f,
                        center = Offset(vx, vy)
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawCctvViewshed3D(
    engine: Globe3DEngine,
    cam: CctvCameraTarget,
    isSelected: Boolean,
    zoom: Float,
    cctvColor: Color,
    pulsePhase: Float,
    hitTargets: MutableList<ScreenHitTarget>
) {
    val origin = engine.project(cam.lat, cam.lon, 1.0035f)
    if (!origin.isVisible) return

    hitTargets.add(ScreenHitTarget(origin.screenX, origin.screenY, SelectedTarget.Cctv(cam)))

    // Project 3D viewshed frustum cone onto the sphere's tangent plane
    val (east, north, up) = engine.tangentFrameWorld(cam.lat, cam.lon)
    // Scale angular radius smoothly so viewshed cones are clearly visible at city zoom & recognizable globally
    val angularDist = (cam.rangeKm / 6371.0f) * (if (zoom > 3.2f) 4.2f else 14f)
    val startAz = cam.azimuthDeg - cam.fovDeg * 0.5f
    val endAz = cam.azimuthDeg + cam.fovDeg * 0.5f

    val conePath = Path().apply {
        moveTo(origin.screenX, origin.screenY)
        val steps = 6
        for (i in 0..steps) {
            val azRad = Math.toRadians((startAz + (endAz - startAz) * (i.toFloat() / steps)).toDouble())
            val dirWorld = (north * cos(azRad).toFloat() + east * sin(azRad).toFloat()).normalized()
            val edgeWorld = (up + dirWorld * angularDist).normalized()
            val p = engine.projectWorldVec(edgeWorld, 1.0035f)
            lineTo(p.screenX, p.screenY)
        }
        close()
    }

    val coneAlpha = if (isSelected) 0.42f + 0.14f * sin(pulsePhase * 2f * PI.toFloat()) else 0.22f
    drawPath(path = conePath, color = cctvColor.copy(alpha = coneAlpha))
    drawPath(
        path = conePath,
        color = cctvColor.copy(alpha = if (isSelected) 0.95f else 0.58f),
        style = Stroke(width = if (isSelected) 2.2f else 1.2f)
    )

    // Camera node dot
    val dotR = if (isSelected) 5.5f else (3.4f * origin.perspectiveScale.coerceIn(0.8f, 1.5f))
    drawCircle(color = cctvColor, radius = dotR, center = Offset(origin.screenX, origin.screenY))
    drawCircle(color = Color.White, radius = dotR * 0.42f, center = Offset(origin.screenX, origin.screenY))
}

private fun DrawScope.drawMaritimeVessel3D(
    engine: Globe3DEngine,
    vessel: MaritimeVesselTarget,
    shipColor: Color,
    zoom: Float,
    hitTargets: MutableList<ScreenHitTarget>
) {
    val p = engine.project(vessel.lat, vessel.lon, 1.003f)
    if (!p.isVisible) return

    hitTargets.add(ScreenHitTarget(p.screenX, p.screenY, SelectedTarget.Vessel(vessel)))

    val (east, north, up) = engine.tangentFrameWorld(vessel.lat, vessel.lon)
    val hdgRad = Math.toRadians(vessel.headingDeg.toDouble())
    val dirWorld = (north * cos(hdgRad).toFloat() + east * sin(hdgRad).toFloat()).normalized()
    val rightWorld = dirWorld.cross(up).normalized()

    val len = (0.011f / zoom.coerceIn(1f, 6f).let { sqrt(it) })
    val bowPt = engine.projectWorldVec((up + dirWorld * len).normalized(), 1.003f)
    val portPt = engine.projectWorldVec((up - dirWorld * (len * 0.5f) - rightWorld * (len * 0.45f)).normalized(), 1.003f)
    val stbdPt = engine.projectWorldVec((up - dirWorld * (len * 0.5f) + rightWorld * (len * 0.45f)).normalized(), 1.003f)
    val wakePt = engine.projectWorldVec((up - dirWorld * (len * 1.9f)).normalized(), 1.003f)

    // Foam wake
    drawLine(
        color = shipColor.copy(alpha = 0.42f),
        start = Offset(p.screenX, p.screenY),
        end = Offset(wakePt.screenX, wakePt.screenY),
        strokeWidth = 1.6f
    )

    val hull = Path().apply {
        moveTo(bowPt.screenX, bowPt.screenY)
        lineTo(stbdPt.screenX, stbdPt.screenY)
        lineTo(portPt.screenX, portPt.screenY)
        close()
    }
    drawPath(path = hull, color = shipColor)
}

private fun DrawScope.drawAuroraOvals3D(
    engine: Globe3DEngine,
    kpIndex: Double,
    pulsePhase: Float,
    accent: Color
) {
    val intensity = (0.28f + (kpIndex / 9.0).toFloat() * 0.65f).coerceIn(0.25f, 0.95f)
    val auroraGreen = Color(0xFF00FF9C).copy(alpha = intensity * 0.48f)
    val auroraViolet = Color(0xFFB388FF).copy(alpha = intensity * 0.32f)

    for (poleSign in listOf(1.0, -1.0)) {
        val baseLat = poleSign * (68.0 - kpIndex * 1.4)
        val path = Path()
        var started = false
        for (lon in -180..180 step 10) {
            val wave = sin(Math.toRadians(lon * 3.0) + pulsePhase * 2.0 * PI) * 2.2
            val p = engine.project(baseLat + wave, lon.toDouble(), 1.028f)
            if (p.isVisible) {
                if (!started) {
                    path.moveTo(p.screenX, p.screenY)
                    started = true
                } else {
                    path.lineTo(p.screenX, p.screenY)
                }
            } else {
                started = false
            }
        }
        drawPath(path = path, color = auroraGreen, style = Stroke(width = 6.5f))
        drawPath(path = path, color = auroraViolet, style = Stroke(width = 2.5f))
        if (kpIndex >= 4.0) {
            drawPath(path = path, color = accent.copy(alpha = 0.25f), style = Stroke(width = 1.2f))
        }
    }
}

private fun DrawScope.drawEarthquake3D(
    engine: Globe3DEngine,
    quake: EarthquakeTarget,
    pulsePhase: Float,
    nowMs: Long,
    zoom: Float,
    isSelected: Boolean,
    quakeColor: Color,
    labelPaint: Paint,
    hitTargets: MutableList<ScreenHitTarget>
) {
    val epicenter = engine.project(quake.lat, quake.lon, 1.005f)
    if (!epicenter.isVisible) return

    hitTargets.add(ScreenHitTarget(epicenter.screenX, epicenter.screenY, SelectedTarget.Quake(quake)))

    val ageMs = (nowMs - quake.timeMs).coerceAtLeast(0L)
    val isLiveAlert = quake.isRealtimeAlert || ageMs < 20 * 60_000L
    val isMajor = quake.magnitude >= 4.5 || quake.tsunamiAlert
    val speedMult = if (isLiveAlert) 2f else 1f
    val waveExpand = ((pulsePhase * speedMult + abs(quake.lat.toFloat()) * 0.09f) % 1f)
    val breathWave = 0.5f + 0.5f * sin(waveExpand * 2f * PI.toFloat())

    val nodeColor = when {
        quake.tsunamiAlert -> Color(0xFFFFD54F)
        isLiveAlert -> Color(0xFFFF5252)
        quake.magnitude >= 5.0 -> Color(0xFFFF7043)
        else -> quakeColor
    }
    val pScale = epicenter.perspectiveScale.coerceIn(0.7f, 1.8f)

    // 1. Surface radial thermal bloom
    val glowRadius = (13f + quake.magnitude.toFloat() * 4.4f) * pScale * (0.85f + 0.35f * breathWave)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                nodeColor.copy(alpha = if (isLiveAlert) 0.56f else 0.34f),
                nodeColor.copy(alpha = 0.12f),
                Color.Transparent
            ),
            center = Offset(epicenter.screenX, epicenter.screenY),
            radius = glowRadius
        ),
        radius = glowRadius,
        center = Offset(epicenter.screenX, epicenter.screenY)
    )

    // 2. Triple concentric spherical shockwaves projected onto the 3D globe tangent plane
    val (east, north, up) = engine.tangentFrameWorld(quake.lat, quake.lon)
    val magNorm = (quake.magnitude.toFloat().coerceIn(0.8f, 9.0f) / 8.5f)
    val baseRingRad = 0.015f + magNorm * 0.052f + (if (isLiveAlert) 0.012f else 0f)

    for (ring in 0..2) {
        val phase = (waveExpand + ring * 0.333f) % 1f
        val r = baseRingRad * (0.28f + phase * 1.45f)
        val alpha = (1f - phase) * (if (isLiveAlert || isSelected) 0.92f else 0.72f)
        val ringColor = if (ring == 0 && (isLiveAlert || isMajor)) Color(0xFFFFD54F) else nodeColor
        val path = Path()
        for (deg in 0..360 step 20) {
            val rad = Math.toRadians(deg.toDouble())
            val ptWorld = (up + east * (cos(rad) * r).toFloat() + north * (sin(rad) * r).toFloat()).normalized()
            val p = engine.projectWorldVec(ptWorld, 1.005f)
            if (deg == 0) path.moveTo(p.screenX, p.screenY) else path.lineTo(p.screenX, p.screenY)
        }
        drawPath(
            path = path,
            color = ringColor.copy(alpha = alpha),
            style = Stroke(width = if (isLiveAlert) 2.4f else 1.8f)
        )
    }

    // 3. Vertical 3D Epicenter Energy Beacon Column + Luminous Floating Tip Node
    val beaconAlt = 1.005f + 0.012f + magNorm * 0.046f + 0.008f * breathWave
    val beaconTip = engine.project(quake.lat, quake.lon, beaconAlt)
    if (beaconTip.isVisible) {
        drawLine(
            color = nodeColor.copy(alpha = 0.86f),
            start = Offset(epicenter.screenX, epicenter.screenY),
            end = Offset(beaconTip.screenX, beaconTip.screenY),
            strokeWidth = (if (isLiveAlert) 2.4f else 1.6f) * pScale
        )
        val tipR = (2.6f + quake.magnitude.toFloat() * 0.55f) * pScale * (0.85f + 0.35f * breathWave)
        drawCircle(
            color = Color.White.copy(alpha = 0.9f),
            radius = tipR * 0.55f,
            center = Offset(beaconTip.screenX, beaconTip.screenY)
        )
        drawCircle(
            color = nodeColor,
            radius = tipR,
            center = Offset(beaconTip.screenX, beaconTip.screenY),
            style = Stroke(width = 1.8f)
        )
    }

    // 4. Anchored Surface Epicenter Core
    val dotR = (3.4f + quake.magnitude.toFloat() * 0.85f) * pScale * (0.9f + 0.22f * breathWave)
    drawCircle(color = nodeColor, radius = dotR, center = Offset(epicenter.screenX, epicenter.screenY))
    drawCircle(color = Color.White.copy(alpha = 0.85f), radius = dotR * 0.4f, center = Offset(epicenter.screenX, epicenter.screenY))

    // 5. Crisp Magnitude Readout for major, real-time alert, selected, or zoomed nodes
    if (zoom >= 1.85f || isMajor || isLiveAlert || isSelected) {
        val anchor = if (beaconTip.isVisible) beaconTip else epicenter
        val prefix = if (isLiveAlert) "LIVE " else ""
        val text = String.format(Locale.US, "%sM%.1f", prefix, quake.magnitude)
        labelPaint.color = if (isLiveAlert) Color(0xFFFFD54F).toArgb() else Color.White.toArgb()
        labelPaint.textSize = (20f * pScale).coerceIn(18f, 30f)
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawText(
                text,
                anchor.screenX,
                anchor.screenY - 12f * pScale,
                labelPaint
            )
        }
    }
}

private fun DrawScope.drawNaturalEvent3D(
    engine: Globe3DEngine,
    ev: NaturalEventTarget,
    pulsePhase: Float,
    eventColor: Color,
    hitTargets: MutableList<ScreenHitTarget>
) {
    val base = engine.project(ev.lat, ev.lon, 1.004f)
    if (!base.isVisible) return
    hitTargets.add(ScreenHitTarget(base.screenX, base.screenY, SelectedTarget.Event(ev)))

    val isStorm = ev.category.contains("Storm", true)
    val color = if (isStorm) Color(0xFF40C4FF) else eventColor
    val plumeTop = engine.project(ev.lat, ev.lon, 1.032f + 0.01f * sin(pulsePhase * 2f * PI.toFloat()))
    drawLine(
        color = color.copy(alpha = 0.75f),
        start = Offset(base.screenX, base.screenY),
        end = Offset(plumeTop.screenX, plumeTop.screenY),
        strokeWidth = 2.0f
    )
    drawCircle(
        color = color.copy(alpha = 0.35f),
        radius = 8.5f * plumeTop.perspectiveScale.coerceIn(0.7f, 1.5f),
        center = Offset(plumeTop.screenX, plumeTop.screenY)
    )
    drawCircle(
        color = color,
        radius = 4.0f * plumeTop.perspectiveScale.coerceIn(0.7f, 1.5f),
        center = Offset(plumeTop.screenX, plumeTop.screenY)
    )
}

private fun DrawScope.drawAircraft3D(
    engine: Globe3DEngine,
    flight: AircraftTarget,
    opticMode: OpticMode,
    planeColor: Color,
    zoom: Float,
    hitTargets: MutableList<ScreenHitTarget>
) {
    val altScale = 1.012f + (flight.altitudeKm.toFloat() / 420f).coerceIn(0.004f, 0.035f)
    val groundPt = engine.project(flight.lat, flight.lon, 1.002f)
    val airPt = engine.project(flight.lat, flight.lon, altScale)
    if (!airPt.isVisible) return

    hitTargets.add(ScreenHitTarget(airPt.screenX, airPt.screenY, SelectedTarget.Flight(flight)))

    if (opticMode == OpticMode.FLIR_THERMAL) {
        drawCircle(
            color = Color(0xFFFF3D00).copy(alpha = 0.28f),
            radius = 11f * airPt.perspectiveScale.coerceIn(0.7f, 1.4f),
            center = Offset(airPt.screenX, airPt.screenY)
        )
    }

    if (groundPt.isVisible) {
        drawLine(
            color = planeColor.copy(alpha = 0.36f),
            start = Offset(groundPt.screenX, groundPt.screenY),
            end = Offset(airPt.screenX, airPt.screenY),
            strokeWidth = 1.2f
        )
        drawCircle(
            color = planeColor.copy(alpha = 0.45f),
            radius = 2.0f,
            center = Offset(groundPt.screenX, groundPt.screenY)
        )
    }

    val (east, north, up) = engine.tangentFrameWorld(flight.lat, flight.lon)
    val hdgRad = Math.toRadians(flight.headingDeg.toDouble())
    val dirWorld = (north * cos(hdgRad).toFloat() + east * sin(hdgRad).toFloat()).normalized()
    val rightWorld = dirWorld.cross(up).normalized()

    val noseLen = (0.016f / zoom.coerceIn(1f, 6f).let { sqrt(it) })
    val wingLen = noseLen * 0.72f

    val nosePt = engine.projectWorldVec((up + dirWorld * noseLen).normalized(), altScale)
    val leftWingPt = engine.projectWorldVec((up - dirWorld * (noseLen * 0.45f) - rightWorld * wingLen).normalized(), altScale)
    val rightWingPt = engine.projectWorldVec((up - dirWorld * (noseLen * 0.45f) + rightWorld * wingLen).normalized(), altScale)
    val tailPt = engine.projectWorldVec((up - dirWorld * (noseLen * 1.7f)).normalized(), altScale)

    drawLine(
        color = planeColor.copy(alpha = 0.45f),
        start = Offset(airPt.screenX, airPt.screenY),
        end = Offset(tailPt.screenX, tailPt.screenY),
        strokeWidth = 1.8f
    )

    val deltaPath = Path().apply {
        moveTo(nosePt.screenX, nosePt.screenY)
        lineTo(rightWingPt.screenX, rightWingPt.screenY)
        lineTo(airPt.screenX, airPt.screenY)
        lineTo(leftWingPt.screenX, leftWingPt.screenY)
        close()
    }
    drawPath(path = deltaPath, color = planeColor)
}

private fun DrawScope.drawSatellite3D(
    engine: Globe3DEngine,
    sat: SatelliteTarget,
    isSelected: Boolean,
    satColor: Color,
    goldColor: Color,
    pulsePhase: Float,
    hitTargets: MutableList<ScreenHitTarget>
) {
    val satRadiusScale = 1.07f + (sat.altitudeKm.toFloat() / 6371f).coerceIn(0.04f, 0.42f) * 0.75f
    val color = when {
        sat.element.category == SatelliteCategory.SPACE_STATION -> goldColor
        isSelected -> Color.White
        else -> satColor
    }

    val showTrack = isSelected || sat.element.category == SatelliteCategory.SPACE_STATION
    if (showTrack && sat.groundTrack.size > 2) {
        val frontPath = Path()
        var frontStarted = false
        for (gp in sat.groundTrack) {
            val rScale = 1.07f + (gp.altitudeKm.toFloat() / 6371f).coerceIn(0.04f, 0.42f) * 0.75f
            val p = engine.project(gp.lat, gp.lon, rScale)
            if (p.isVisible) {
                if (!frontStarted) {
                    frontPath.moveTo(p.screenX, p.screenY)
                    frontStarted = true
                } else {
                    frontPath.lineTo(p.screenX, p.screenY)
                }
            } else {
                frontStarted = false
            }
        }
        drawPath(
            path = frontPath,
            color = color.copy(alpha = if (isSelected) 0.75f else 0.42f),
            style = Stroke(width = if (isSelected) 2.2f else 1.3f)
        )
    }

    val satPt = engine.project(sat.lat, sat.lon, satRadiusScale)
    if (!satPt.isVisible) return

    hitTargets.add(ScreenHitTarget(satPt.screenX, satPt.screenY, SelectedTarget.Satellite(sat)))

    val nadirPt = engine.project(sat.lat, sat.lon, 1.002f)
    if (nadirPt.isVisible && (isSelected || sat.element.category == SatelliteCategory.SPACE_STATION)) {
        // Sensor swath cone from satellite to ground footprint
        val fpR = (16f + 8f * pulsePhase) * nadirPt.perspectiveScale.coerceIn(0.6f, 1.5f)
        val swathPath = Path().apply {
            moveTo(satPt.screenX, satPt.screenY)
            lineTo(nadirPt.screenX - fpR, nadirPt.screenY)
            lineTo(nadirPt.screenX + fpR, nadirPt.screenY)
            close()
        }
        drawPath(path = swathPath, color = color.copy(alpha = 0.14f))
        drawLine(
            color = color.copy(alpha = 0.55f),
            start = Offset(satPt.screenX, satPt.screenY),
            end = Offset(nadirPt.screenX, nadirPt.screenY),
            strokeWidth = 1.4f
        )
        drawCircle(
            color = color.copy(alpha = 0.35f * (1f - pulsePhase * 0.5f)),
            radius = fpR,
            center = Offset(nadirPt.screenX, nadirPt.screenY),
            style = Stroke(width = 1.4f)
        )
    }

    val pScale = satPt.perspectiveScale.coerceIn(0.75f, 1.6f)
    val c = Offset(satPt.screenX, satPt.screenY)
    if (sat.element.category == SatelliteCategory.SPACE_STATION) {
        drawCircle(color = color.copy(alpha = 0.25f), radius = 14f * pScale, center = c)
    }
    drawLine(
        color = color,
        start = Offset(c.x - 9f * pScale, c.y),
        end = Offset(c.x + 9f * pScale, c.y),
        strokeWidth = 2.6f * pScale,
        cap = StrokeCap.Round
    )
    drawCircle(color = Color.White, radius = 3.2f * pScale, center = c)
    drawCircle(color = color, radius = 4.8f * pScale, center = c, style = Stroke(width = 1.6f))
}

private fun DrawScope.drawSelectedTargetReticle3D(
    engine: Globe3DEngine,
    selected: SelectedTarget,
    pulsePhase: Float,
    accent: Color
) {
    val projected = when (selected) {
        SelectedTarget.None -> return
        is SelectedTarget.Flight -> {
            val altScale = 1.012f + (selected.data.altitudeKm.toFloat() / 420f).coerceIn(0.004f, 0.035f)
            engine.project(selected.data.lat, selected.data.lon, altScale)
        }
        is SelectedTarget.Satellite -> {
            val satScale = 1.07f + (selected.data.altitudeKm.toFloat() / 6371f).coerceIn(0.04f, 0.42f) * 0.75f
            engine.project(selected.data.lat, selected.data.lon, satScale)
        }
        is SelectedTarget.Cctv -> engine.project(selected.data.lat, selected.data.lon, 1.0035f)
        is SelectedTarget.Vessel -> engine.project(selected.data.lat, selected.data.lon, 1.003f)
        is SelectedTarget.Quake -> engine.project(selected.data.lat, selected.data.lon, 1.005f)
        is SelectedTarget.Event -> engine.project(selected.data.lat, selected.data.lon, 1.032f)
    }
    if (!projected.isVisible) return

    val center = Offset(projected.screenX, projected.screenY)
    val radius = 24f + sin(pulsePhase * 2f * PI.toFloat()) * 4f
    rotate(degrees = pulsePhase * 360f, pivot = center) {
        for (angle in listOf(0f, 90f, 180f, 270f)) {
            drawArc(
                color = accent,
                startAngle = angle + 12f,
                sweepAngle = 52f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = 2.4f, cap = StrokeCap.Round)
            )
        }
    }
}

private fun DrawScope.drawOpticPostFx(
    opticMode: OpticMode,
    scanlineAlpha: Float,
    accent: Color,
    wipeProgress: Float,
    bootProgress: Float,
    radarPhase: Float
) {
    // CRT / NVG / FLIR / Noir Scanlines
    if (scanlineAlpha > 0.02f) {
        val step = if (opticMode == OpticMode.CRT_MONITOR) 7f else 10f
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = Color.Black.copy(alpha = scanlineAlpha),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = if (opticMode == OpticMode.CRT_MONITOR) 2.5f else 2f
            )
            y += step
        }
    }

    // Mode-specific optical vignettes & sweeps
    when (opticMode) {
        OpticMode.NVG_PHOSPHOR -> {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.72f)),
                    center = Offset(size.width * 0.5f, size.height * 0.5f),
                    radius = minOf(size.width, size.height) * 0.72f
                ),
                radius = maxOf(size.width, size.height),
                center = Offset(size.width * 0.5f, size.height * 0.5f)
            )
        }
        OpticMode.CRT_MONITOR, OpticMode.NOIR_SURVEILLANCE -> {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
                    center = Offset(size.width * 0.5f, size.height * 0.5f),
                    radius = maxOf(size.width, size.height) * 0.78f
                )
            )
        }
        OpticMode.WIREFRAME_HOLO -> {
            val scanY = size.height * radarPhase
            drawLine(
                brush = Brush.horizontalGradient(listOf(Color.Transparent, accent.copy(alpha = 0.45f), Color.Transparent)),
                start = Offset(0f, scanY),
                end = Offset(size.width, scanY),
                strokeWidth = 3f
            )
        }
        else -> Unit
    }

    // Sweeping 3D radial optic iris wave when switching optic modes
    if (wipeProgress < 0.99f) {
        val maxR = hypot(size.width.toDouble(), size.height.toDouble()).toFloat() * 0.7f
        val curR = maxR * wipeProgress
        drawCircle(
            color = accent.copy(alpha = (1f - wipeProgress) * 0.75f),
            radius = curR,
            center = Offset(size.width * 0.5f, size.height * 0.5f),
            style = Stroke(width = 5f * (1f - wipeProgress) + 1.5f)
        )
    }

    // Boot orbital lock-on reticle expansion
    if (bootProgress < 0.99f) {
        val c = Offset(size.width * 0.5f, size.height * 0.5f)
        val r = (minOf(size.width, size.height) * 0.42f) * (1.25f - 0.25f * bootProgress)
        drawCircle(
            color = accent.copy(alpha = (1f - bootProgress) * 0.8f),
            radius = r,
            center = c,
            style = Stroke(width = 2.5f)
        )
    }

    // God's Eye View perimeter targeting L-brackets
    val pad = 16f
    val arm = 24f
    val bColor = accent.copy(alpha = 0.52f)
    val bStroke = 2.0f
    // Top-Left
    drawLine(bColor, Offset(pad, pad), Offset(pad + arm, pad), bStroke)
    drawLine(bColor, Offset(pad, pad), Offset(pad, pad + arm), bStroke)
    // Top-Right
    drawLine(bColor, Offset(size.width - pad, pad), Offset(size.width - pad - arm, pad), bStroke)
    drawLine(bColor, Offset(size.width - pad, pad), Offset(size.width - pad, pad + arm), bStroke)
    // Bottom-Left
    drawLine(bColor, Offset(pad, size.height - pad), Offset(pad + arm, size.height - pad), bStroke)
    drawLine(bColor, Offset(pad, size.height - pad), Offset(pad, size.height - pad - arm), bStroke)
    // Bottom-Right
    drawLine(bColor, Offset(size.width - pad, size.height - pad), Offset(size.width - pad - arm, size.height - pad), bStroke)
    drawLine(bColor, Offset(size.width - pad, size.height - pad), Offset(size.width - pad, size.height - pad - arm), bStroke)
}

private fun DrawScope.drawGlobalLandmarks3D(
    engine: Globe3DEngine,
    zoom: Float,
    accent: Color,
    labelPaint: Paint
) {
    val diamondColor = Color(0xFFFFD54F)
    for (lm in GlobalLandmarks) {
        val pt = engine.project(lm.lat, lm.lon, 1.0045f)
        if (!pt.isVisible) continue
        val r = if (zoom >= 2.2f) 6.5f else 4.2f
        val cx = pt.screenX
        val cy = pt.screenY
        val diamond = Path().apply {
            moveTo(cx, cy - r)
            lineTo(cx + r, cy)
            lineTo(cx, cy + r)
            lineTo(cx - r, cy)
            close()
        }
        drawPath(path = diamond, color = diamondColor.copy(alpha = 0.25f))
        drawPath(path = diamond, color = diamondColor, style = Stroke(width = 1.6f))
        if (zoom >= 2.2f) {
            labelPaint.color = diamondColor.toArgb()
            labelPaint.textSize = 20f
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText(lm.code, cx, cy - r - 6f, labelPaint)
            }
        } else {
            drawCircle(color = accent.copy(alpha = 0.8f), radius = 1.8f, center = Offset(cx, cy))
        }
    }
}

private fun DrawScope.drawNavigationRoute3D(
    engine: Globe3DEngine,
    route: NavigationRoute,
    packetPhase: Float,
    pulsePhase: Float,
    labelPaint: Paint
) {
    val routeColor = Color(0xFF00FF9C)
    val pts = route.coordinates
    if (pts.size >= 2) {
        val path = Path()
        var started = false
        val projected = mutableListOf<ProjectedPoint>()
        for (p3 in pts) {
            val rScale = 1.005f + (p3.altitudeKm.toFloat() / 6371f).coerceIn(0f, 0.08f)
            val p = engine.project(p3.lat, p3.lon, rScale)
            projected.add(p)
            if (p.isVisible) {
                if (!started) {
                    path.moveTo(p.screenX, p.screenY)
                    started = true
                } else {
                    path.lineTo(p.screenX, p.screenY)
                }
            } else {
                started = false
            }
        }
        drawPath(
            path = path,
            color = routeColor.copy(alpha = 0.28f),
            style = Stroke(width = 8f, cap = StrokeCap.Round)
        )
        drawPath(
            path = path,
            color = routeColor,
            style = Stroke(width = 3.2f, cap = StrokeCap.Round)
        )
        val idx = (packetPhase * (projected.size - 1)).toInt().coerceIn(0, projected.lastIndex)
        val pkt = projected[idx]
        if (pkt.isVisible) {
            drawCircle(color = Color.White, radius = 4.5f, center = Offset(pkt.screenX, pkt.screenY))
            drawCircle(color = routeColor, radius = 8.5f, center = Offset(pkt.screenX, pkt.screenY), style = Stroke(width = 1.8f))
        }
    }

    val originPt = engine.project(route.originLat, route.originLon, 1.006f)
    if (originPt.isVisible) {
        drawCircle(color = Color(0xFF00E5FF), radius = 5.5f, center = Offset(originPt.screenX, originPt.screenY))
    }

    val destPt = engine.project(route.destinationLat, route.destinationLon, 1.006f)
    if (destPt.isVisible) {
        val c = Offset(destPt.screenX, destPt.screenY)
        drawCircle(color = routeColor.copy(alpha = 0.28f + 0.22f * pulsePhase), radius = 14f + 6f * pulsePhase, center = c)
        drawCircle(color = routeColor, radius = 6f, center = c)
        labelPaint.color = routeColor.toArgb()
        labelPaint.textSize = 22f
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawText("DEST", c.x, c.y - 16f, labelPaint)
        }
    }
}

private fun DrawScope.drawGpsObserverTrail3D(
    engine: Globe3DEngine,
    gps: LiveGpsCompassState,
    pulsePhase: Float
) {
    val gpsColor = Color(0xFF00E5FF)
    if (gps.breadcrumbTrail.size >= 2) {
        val trailPath = Path()
        var started = false
        for (pt in gps.breadcrumbTrail) {
            val p = engine.project(pt.lat, pt.lon, 1.004f)
            if (p.isVisible) {
                if (!started) {
                    trailPath.moveTo(p.screenX, p.screenY)
                    started = true
                } else {
                    trailPath.lineTo(p.screenX, p.screenY)
                }
            } else {
                started = false
            }
        }
        drawPath(path = trailPath, color = gpsColor.copy(alpha = 0.72f), style = Stroke(width = 2.4f, cap = StrokeCap.Round))
    }
    if (gps.hasGpsFix) {
        val me = engine.project(gps.lat, gps.lon, 1.005f)
        if (me.isVisible) {
            val c = Offset(me.screenX, me.screenY)
            drawCircle(color = gpsColor.copy(alpha = 0.35f * (1f - pulsePhase * 0.5f)), radius = 12f + 10f * pulsePhase, center = c)
            drawCircle(color = Color.White, radius = 4.2f, center = c)
            drawCircle(color = gpsColor, radius = 6.5f, center = c, style = Stroke(width = 2.0f))
        }
    }
}

package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Core Callidus Obsidian Tactical Palette
val ObsidianVoid = Color(0xFF04070D)
val ObsidianSurface = Color(0xFF0A111D)
val ObsidianPanel = Color(0xD90D1626)
val ObsidianPanelElevated = Color(0xEB132036)
val ObsidianBorder = Color(0x4D00E5FF)

val CallidusCyan = Color(0xFF00E5FF)
val CallidusCyanDim = Color(0xFF00839E)
val CallidusGold = Color(0xFFFFB300)
val CallidusCrimson = Color(0xFFFF3D71)
val CallidusEmerald = Color(0xFF00FF9C)
val CallidusViolet = Color(0xFFB388FF)

val TextPrimary = Color(0xFFEAF6FF)
val TextSecondary = Color(0xFF94ADC7)
val TextMuted = Color(0xFF5A738E)

// Optical Mode Render Palettes
data class OpticPalette(
    val spaceBgTop: Color,
    val spaceBgBottom: Color,
    val globeOceanDay: Color,
    val globeOceanNight: Color,
    val landFillDay: Color,
    val landFillNight: Color,
    val coastlineStroke: Color,
    val graticuleStroke: Color,
    val atmosphereGlow: Color,
    val aircraftColor: Color,
    val satelliteColor: Color,
    val quakeColor: Color,
    val eventColor: Color,
    val cableColor: Color,
    val cctvColor: Color,
    val shipColor: Color,
    val trafficColor: Color,
    val hudAccent: Color,
    val scanlineAlpha: Float
)

val EoTacticalPalette = OpticPalette(
    spaceBgTop = Color(0xFF020409),
    spaceBgBottom = Color(0xFF071121),
    globeOceanDay = Color(0xFF0A1E36),
    globeOceanNight = Color(0xFF040B16),
    landFillDay = Color(0xFF142E47),
    landFillNight = Color(0xFF091524),
    coastlineStroke = Color(0xFF28C7E8),
    graticuleStroke = Color(0x2600E5FF),
    atmosphereGlow = Color(0xFF00D2FF),
    aircraftColor = Color(0xFFFFC107),
    satelliteColor = Color(0xFF00E5FF),
    quakeColor = Color(0xFFFF3D71),
    eventColor = Color(0xFFFF6D00),
    cableColor = Color(0x667C4DFF),
    cctvColor = Color(0xFF00FF9C),
    shipColor = Color(0xFF40C4FF),
    trafficColor = Color(0xFFFFEA00),
    hudAccent = CallidusCyan,
    scanlineAlpha = 0.04f
)

val FlirThermalPalette = OpticPalette(
    spaceBgTop = Color(0xFF0D0214),
    spaceBgBottom = Color(0xFF1A061C),
    globeOceanDay = Color(0xFF1E0B36),
    globeOceanNight = Color(0xFF0E041A),
    landFillDay = Color(0xFF47154E),
    landFillNight = Color(0xFF24092E),
    coastlineStroke = Color(0xFFFF6E40),
    graticuleStroke = Color(0x2EFF9100),
    atmosphereGlow = Color(0xFFFF3D00),
    aircraftColor = Color(0xFFFFFF8D),
    satelliteColor = Color(0xFFFFAB40),
    quakeColor = Color(0xFFFFFFFF),
    eventColor = Color(0xFFFF1744),
    cableColor = Color(0x88FF4081),
    cctvColor = Color(0xFFFFD180),
    shipColor = Color(0xFFFF80AB),
    trafficColor = Color(0xFFFFFF00),
    hudAccent = Color(0xFFFF9100),
    scanlineAlpha = 0.08f
)

val NvgPhosphorPalette = OpticPalette(
    spaceBgTop = Color(0xFF010804),
    spaceBgBottom = Color(0xFF03140A),
    globeOceanDay = Color(0xFF052412),
    globeOceanNight = Color(0xFF020E07),
    landFillDay = Color(0xFF0B3D20),
    landFillNight = Color(0xFF041C0E),
    coastlineStroke = Color(0xFF00FF9C),
    graticuleStroke = Color(0x2B00FF9C),
    atmosphereGlow = Color(0xFF00E676),
    aircraftColor = Color(0xFFB9F6CA),
    satelliteColor = Color(0xFF00FF9C),
    quakeColor = Color(0xFFFFEA00),
    eventColor = Color(0xFF69F0AE),
    cableColor = Color(0x6600C853),
    cctvColor = Color(0xFF00E676),
    shipColor = Color(0xFFA5D6A7),
    trafficColor = Color(0xFF76FF03),
    hudAccent = CallidusEmerald,
    scanlineAlpha = 0.14f
)

val CrtMonitorPalette = OpticPalette(
    spaceBgTop = Color(0xFF030709),
    spaceBgBottom = Color(0xFF071318),
    globeOceanDay = Color(0xFF08242C),
    globeOceanNight = Color(0xFF030E12),
    landFillDay = Color(0xFF103F48),
    landFillNight = Color(0xFF071E24),
    coastlineStroke = Color(0xFF26FFE6),
    graticuleStroke = Color(0x3626FFE6),
    atmosphereGlow = Color(0xFF00E5FF),
    aircraftColor = Color(0xFFFFF176),
    satelliteColor = Color(0xFF80DEEA),
    quakeColor = Color(0xFFFF5252),
    eventColor = Color(0xFFFF8A65),
    cableColor = Color(0x7726FFE6),
    cctvColor = Color(0xFF64FFDA),
    shipColor = Color(0xFF4DD0E1),
    trafficColor = Color(0xFFFFEE58),
    hudAccent = Color(0xFF26FFE6),
    scanlineAlpha = 0.22f
)

val NoirSurveillancePalette = OpticPalette(
    spaceBgTop = Color(0xFF050506),
    spaceBgBottom = Color(0xFF101114),
    globeOceanDay = Color(0xFF15171A),
    globeOceanNight = Color(0xFF090A0C),
    landFillDay = Color(0xFF2A2D33),
    landFillNight = Color(0xFF16181C),
    coastlineStroke = Color(0xFFE0E4EC),
    graticuleStroke = Color(0x28E0E4EC),
    atmosphereGlow = Color(0xFFB0B8C4),
    aircraftColor = Color(0xFFFFFFFF),
    satelliteColor = Color(0xFFD5DBE3),
    quakeColor = Color(0xFFFF5252),
    eventColor = Color(0xFFFFB74D),
    cableColor = Color(0x669AA2AE),
    cctvColor = Color(0xFFF5F7FA),
    shipColor = Color(0xFFB0BEC5),
    trafficColor = Color(0xFFFFFFFF),
    hudAccent = Color(0xFFE6ECF5),
    scanlineAlpha = 0.09f
)

val ArcticSnowPalette = OpticPalette(
    spaceBgTop = Color(0xFF08121E),
    spaceBgBottom = Color(0xFF13253B),
    globeOceanDay = Color(0xFF193754),
    globeOceanNight = Color(0xFF0B1C2E),
    landFillDay = Color(0xFFBFD7EA),
    landFillNight = Color(0xFF53728F),
    coastlineStroke = Color(0xFFFFFFFF),
    graticuleStroke = Color(0x33B3E5FC),
    atmosphereGlow = Color(0xFFE1F5FE),
    aircraftColor = Color(0xFFFF6D00),
    satelliteColor = Color(0xFF00E5FF),
    quakeColor = Color(0xFFFF1744),
    eventColor = Color(0xFFFF9100),
    cableColor = Color(0x7780D8FF),
    cctvColor = Color(0xFF00E676),
    shipColor = Color(0xFF00B0FF),
    trafficColor = Color(0xFFFFAB00),
    hudAccent = Color(0xFF81D4FA),
    scanlineAlpha = 0.02f
)

val WireframeHoloPalette = OpticPalette(
    spaceBgTop = Color(0xFF01060D),
    spaceBgBottom = Color(0xFF041426),
    globeOceanDay = Color(0xFF030D1A),
    globeOceanNight = Color(0xFF020810),
    landFillDay = Color(0x1800E5FF),
    landFillNight = Color(0x0D00E5FF),
    coastlineStroke = Color(0xFF40F2FF),
    graticuleStroke = Color(0x4700E5FF),
    atmosphereGlow = Color(0xFF18FFFF),
    aircraftColor = Color(0xFF00FF9C),
    satelliteColor = Color(0xFFFFFFFF),
    quakeColor = Color(0xFFFF4081),
    eventColor = Color(0xFFFFD740),
    cableColor = Color(0x9900B0FF),
    cctvColor = Color(0xFF00FF9C),
    shipColor = Color(0xFF84FFFF),
    trafficColor = Color(0xFFFFFF00),
    hudAccent = Color(0xFF40F2FF),
    scanlineAlpha = 0.06f
)

val SolarTerminatorPalette = OpticPalette(
    spaceBgTop = Color(0xFF03050C),
    spaceBgBottom = Color(0xFF0E1526),
    globeOceanDay = Color(0xFF0F3557),
    globeOceanNight = Color(0xFF030712),
    landFillDay = Color(0xFF1F5470),
    landFillNight = Color(0xFF080F1E),
    coastlineStroke = Color(0xFFFFCA28),
    graticuleStroke = Color(0x22FFD54F),
    atmosphereGlow = Color(0xFFFFA726),
    aircraftColor = Color(0xFFFFE082),
    satelliteColor = Color(0xFF4FC3F7),
    quakeColor = Color(0xFFFF5252),
    eventColor = Color(0xFFFF7043),
    cableColor = Color(0x77FFD54F),
    cctvColor = Color(0xFF00E676),
    shipColor = Color(0xFF40C4FF),
    trafficColor = Color(0xFFFFD54F),
    hudAccent = CallidusGold,
    scanlineAlpha = 0.03f
)

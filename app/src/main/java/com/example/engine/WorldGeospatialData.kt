package com.example.engine

data class GeoCoord(val lat: Double, val lon: Double)

data class SubseaCableCorridor(
    val name: String,
    val capacityTbps: Int,
    val waypoints: List<GeoCoord>
)

/**
 * Geographically accurate continental coastlines, island chains, subsea fiber corridors,
 * tectonic plate boundaries, and major urban night-light clusters.
 */
object WorldGeospatialData {

    val continents: List<List<GeoCoord>> = listOf(
        // North & Central America
        listOf(
            GeoCoord(71.0, -156.5), GeoCoord(70.2, -145.0), GeoCoord(69.0, -135.0),
            GeoCoord(70.5, -125.0), GeoCoord(68.5, -110.0), GeoCoord(69.5, -95.0),
            GeoCoord(64.0, -90.0), GeoCoord(62.5, -78.0), GeoCoord(58.5, -78.0),
            GeoCoord(54.0, -79.5), GeoCoord(52.0, -78.0), GeoCoord(58.0, -68.0),
            GeoCoord(60.5, -64.5), GeoCoord(52.0, -55.5), GeoCoord(47.5, -53.0),
            GeoCoord(45.5, -61.0), GeoCoord(43.5, -65.5), GeoCoord(44.5, -67.0),
            GeoCoord(42.0, -70.0), GeoCoord(40.7, -74.0), GeoCoord(37.0, -76.0),
            GeoCoord(35.2, -75.5), GeoCoord(32.0, -80.8), GeoCoord(25.8, -80.2),
            GeoCoord(25.0, -81.5), GeoCoord(28.0, -82.8), GeoCoord(30.0, -85.5),
            GeoCoord(29.2, -90.0), GeoCoord(27.8, -97.0), GeoCoord(22.2, -97.8),
            GeoCoord(19.2, -96.1), GeoCoord(21.5, -87.0), GeoCoord(18.5, -88.3),
            GeoCoord(15.8, -88.5), GeoCoord(15.0, -83.2), GeoCoord(10.5, -83.5),
            GeoCoord(8.9, -79.5), GeoCoord(7.5, -80.5), GeoCoord(9.5, -84.8),
            GeoCoord(13.5, -90.0), GeoCoord(16.0, -95.0), GeoCoord(19.0, -104.3),
            GeoCoord(23.2, -106.4), GeoCoord(31.5, -114.0), GeoCoord(23.0, -110.0),
            GeoCoord(28.0, -114.2), GeoCoord(32.5, -117.1), GeoCoord(34.4, -120.5),
            GeoCoord(37.8, -122.5), GeoCoord(42.0, -124.3), GeoCoord(48.4, -124.7),
            GeoCoord(51.0, -128.0), GeoCoord(55.0, -133.0), GeoCoord(59.5, -139.5),
            GeoCoord(60.5, -148.0), GeoCoord(55.5, -161.0), GeoCoord(59.0, -162.0),
            GeoCoord(63.5, -162.0), GeoCoord(65.6, -168.0), GeoCoord(68.5, -166.5)
        ),
        // Greenland
        listOf(
            GeoCoord(82.5, -35.0), GeoCoord(81.5, -16.0), GeoCoord(76.0, -19.0),
            GeoCoord(70.5, -22.0), GeoCoord(65.5, -37.0), GeoCoord(60.0, -44.0),
            GeoCoord(64.2, -51.7), GeoCoord(70.0, -53.0), GeoCoord(76.0, -68.0),
            GeoCoord(81.0, -62.0), GeoCoord(83.2, -45.0)
        ),
        // South America
        listOf(
            GeoCoord(11.0, -75.0), GeoCoord(12.2, -71.5), GeoCoord(10.5, -67.0),
            GeoCoord(10.0, -62.0), GeoCoord(6.8, -58.0), GeoCoord(2.0, -50.0),
            GeoCoord(-1.5, -48.5), GeoCoord(-5.8, -35.2), GeoCoord(-13.0, -38.5),
            GeoCoord(-22.9, -43.2), GeoCoord(-25.5, -48.5), GeoCoord(-34.0, -53.5),
            GeoCoord(-38.0, -57.5), GeoCoord(-42.5, -64.0), GeoCoord(-48.0, -66.0),
            GeoCoord(-54.8, -68.3), GeoCoord(-53.5, -73.0), GeoCoord(-46.5, -75.0),
            GeoCoord(-37.0, -73.5), GeoCoord(-33.0, -71.6), GeoCoord(-23.6, -70.4),
            GeoCoord(-18.4, -70.3), GeoCoord(-12.0, -77.1), GeoCoord(-5.0, -81.2),
            GeoCoord(-2.2, -80.9), GeoCoord(4.0, -77.5), GeoCoord(8.5, -77.5)
        ),
        // Europe + Iberia + Italy + Balkans + Scandinavia
        listOf(
            GeoCoord(36.0, -5.6), GeoCoord(37.0, -8.9), GeoCoord(41.2, -8.7),
            GeoCoord(43.5, -9.2), GeoCoord(43.5, -1.8), GeoCoord(47.3, -2.5),
            GeoCoord(48.5, -4.7), GeoCoord(49.5, 0.1), GeoCoord(51.2, 2.9),
            GeoCoord(53.5, 6.5), GeoCoord(55.5, 8.2), GeoCoord(57.7, 10.6),
            GeoCoord(55.5, 12.6), GeoCoord(58.5, 11.2), GeoCoord(59.0, 5.5),
            GeoCoord(63.4, 10.4), GeoCoord(69.5, 18.0), GeoCoord(71.1, 25.8),
            GeoCoord(69.5, 31.0), GeoCoord(66.5, 40.0), GeoCoord(68.5, 44.0),
            GeoCoord(69.8, 60.0), GeoCoord(67.0, 68.0), GeoCoord(55.0, 61.0),
            GeoCoord(47.0, 39.0), GeoCoord(45.2, 36.5), GeoCoord(44.5, 33.5),
            GeoCoord(46.5, 30.8), GeoCoord(43.2, 28.0), GeoCoord(41.2, 29.1),
            GeoCoord(40.2, 26.2), GeoCoord(38.0, 24.0), GeoCoord(36.5, 22.5),
            GeoCoord(40.5, 19.5), GeoCoord(45.5, 13.8), GeoCoord(41.8, 16.0),
            GeoCoord(38.0, 15.6), GeoCoord(40.8, 14.2), GeoCoord(44.4, 8.9),
            GeoCoord(43.3, 5.4), GeoCoord(41.4, 2.2), GeoCoord(36.7, -4.4)
        ),
        // Great Britain & Ireland
        listOf(
            GeoCoord(50.1, -5.5), GeoCoord(51.5, -3.5), GeoCoord(53.4, -3.1),
            GeoCoord(55.9, -4.8), GeoCoord(58.6, -3.1), GeoCoord(57.1, -2.1),
            GeoCoord(55.0, -1.4), GeoCoord(52.8, 1.7), GeoCoord(51.1, 1.3),
            GeoCoord(50.7, -1.8)
        ),
        listOf(
            GeoCoord(51.5, -9.8), GeoCoord(53.3, -9.6), GeoCoord(55.3, -7.2),
            GeoCoord(54.6, -5.6), GeoCoord(53.3, -6.2), GeoCoord(52.1, -6.5)
        ),
        // Africa
        listOf(
            GeoCoord(35.8, -5.8), GeoCoord(36.8, 3.0), GeoCoord(37.2, 10.2),
            GeoCoord(32.9, 13.2), GeoCoord(31.2, 29.9), GeoCoord(31.5, 32.3),
            GeoCoord(27.0, 34.0), GeoCoord(20.0, 37.2), GeoCoord(15.5, 39.5),
            GeoCoord(12.0, 43.3), GeoCoord(11.9, 51.3), GeoCoord(2.0, 45.3),
            GeoCoord(-4.0, 39.7), GeoCoord(-11.0, 40.5), GeoCoord(-19.8, 34.8),
            GeoCoord(-26.0, 32.6), GeoCoord(-33.9, 25.6), GeoCoord(-34.8, 20.0),
            GeoCoord(-33.9, 18.4), GeoCoord(-22.9, 14.5), GeoCoord(-12.5, 13.4),
            GeoCoord(-5.8, 12.2), GeoCoord(0.4, 9.4), GeoCoord(4.0, 9.7),
            GeoCoord(6.4, 3.4), GeoCoord(5.3, -4.0), GeoCoord(4.4, -7.7),
            GeoCoord(8.5, -13.2), GeoCoord(14.7, -17.5), GeoCoord(21.0, -17.0),
            GeoCoord(27.0, -13.5), GeoCoord(33.6, -7.6)
        ),
        // Madagascar
        listOf(
            GeoCoord(-12.0, 49.3), GeoCoord(-15.5, 50.4), GeoCoord(-25.0, 47.0),
            GeoCoord(-24.8, 44.0), GeoCoord(-17.0, 44.2), GeoCoord(-13.5, 47.9)
        ),
        // Asia + Middle East + India + SE Asia + Siberia
        listOf(
            GeoCoord(31.5, 34.5), GeoCoord(36.0, 36.0), GeoCoord(37.0, 27.2),
            GeoCoord(41.2, 29.2), GeoCoord(41.6, 41.6), GeoCoord(44.5, 38.0),
            GeoCoord(47.0, 39.5), GeoCoord(55.0, 61.0), GeoCoord(67.0, 68.0),
            GeoCoord(73.0, 72.0), GeoCoord(77.5, 104.0), GeoCoord(73.0, 126.0),
            GeoCoord(70.5, 155.0), GeoCoord(66.2, -170.0), GeoCoord(64.5, 178.0),
            GeoCoord(59.5, 163.0), GeoCoord(51.0, 156.7), GeoCoord(54.0, 140.0),
            GeoCoord(43.1, 131.9), GeoCoord(39.0, 127.8), GeoCoord(35.1, 129.0),
            GeoCoord(37.5, 126.6), GeoCoord(39.0, 121.6), GeoCoord(36.0, 120.4),
            GeoCoord(31.2, 121.5), GeoCoord(24.5, 118.1), GeoCoord(22.3, 114.2),
            GeoCoord(20.8, 106.7), GeoCoord(16.0, 108.2), GeoCoord(10.5, 107.0),
            GeoCoord(13.5, 100.5), GeoCoord(7.0, 100.5), GeoCoord(1.3, 103.8),
            GeoCoord(5.5, 100.2), GeoCoord(16.5, 96.5), GeoCoord(22.3, 91.8),
            GeoCoord(21.5, 87.5), GeoCoord(13.1, 80.3), GeoCoord(8.1, 77.5),
            GeoCoord(15.4, 73.8), GeoCoord(19.1, 72.8), GeoCoord(22.8, 69.0),
            GeoCoord(24.9, 67.0), GeoCoord(25.2, 62.3), GeoCoord(26.5, 56.3),
            GeoCoord(29.4, 48.0), GeoCoord(25.3, 55.3), GeoCoord(22.5, 59.8),
            GeoCoord(17.0, 54.1), GeoCoord(12.8, 45.0), GeoCoord(13.0, 43.3),
            GeoCoord(21.5, 39.2), GeoCoord(28.5, 34.5)
        ),
        // Japan
        listOf(
            GeoCoord(31.4, 130.6), GeoCoord(33.5, 130.2), GeoCoord(35.5, 135.5),
            GeoCoord(38.0, 139.0), GeoCoord(41.4, 140.5), GeoCoord(45.4, 141.9),
            GeoCoord(43.2, 145.5), GeoCoord(40.5, 141.6), GeoCoord(35.6, 140.8),
            GeoCoord(34.6, 135.2), GeoCoord(33.0, 132.8)
        ),
        // Sumatra / Java / Borneo
        listOf(
            GeoCoord(5.5, 95.3), GeoCoord(1.5, 102.0), GeoCoord(-5.9, 105.9),
            GeoCoord(-4.0, 102.0), GeoCoord(1.0, 97.5)
        ),
        listOf(
            GeoCoord(-6.1, 105.9), GeoCoord(-7.2, 112.8), GeoCoord(-8.4, 114.5),
            GeoCoord(-7.8, 110.0)
        ),
        listOf(
            GeoCoord(6.8, 117.0), GeoCoord(1.5, 118.8), GeoCoord(-3.4, 116.0),
            GeoCoord(-2.5, 110.2), GeoCoord(1.5, 109.2), GeoCoord(4.5, 114.0)
        ),
        // Philippines
        listOf(
            GeoCoord(18.5, 121.0), GeoCoord(14.6, 122.0), GeoCoord(12.5, 124.0),
            GeoCoord(7.0, 126.2), GeoCoord(6.8, 122.0), GeoCoord(14.5, 120.2)
        ),
        // Australia
        listOf(
            GeoCoord(-12.4, 130.8), GeoCoord(-12.2, 136.8), GeoCoord(-15.5, 136.5),
            GeoCoord(-11.0, 142.5), GeoCoord(-19.2, 146.8), GeoCoord(-27.5, 153.5),
            GeoCoord(-33.9, 151.2), GeoCoord(-37.8, 149.8), GeoCoord(-38.0, 144.9),
            GeoCoord(-35.0, 138.5), GeoCoord(-32.0, 132.0), GeoCoord(-33.9, 122.0),
            GeoCoord(-35.0, 117.9), GeoCoord(-32.0, 115.8), GeoCoord(-21.9, 114.1),
            GeoCoord(-18.0, 122.2), GeoCoord(-14.5, 125.5)
        ),
        // New Zealand
        listOf(
            GeoCoord(-34.5, 172.7), GeoCoord(-37.7, 178.5), GeoCoord(-41.3, 174.8),
            GeoCoord(-46.6, 168.4), GeoCoord(-43.5, 169.5), GeoCoord(-39.0, 174.0),
            GeoCoord(-36.8, 174.7)
        ),
        // Antarctica Ring
        listOf(
            GeoCoord(-70.0, -180.0), GeoCoord(-72.0, -140.0), GeoCoord(-73.5, -100.0),
            GeoCoord(-64.5, -60.0), GeoCoord(-72.0, -30.0), GeoCoord(-70.0, 10.0),
            GeoCoord(-68.5, 60.0), GeoCoord(-67.0, 110.0), GeoCoord(-71.0, 150.0),
            GeoCoord(-74.0, 175.0)
        )
    )

    val subseaCables: List<SubseaCableCorridor> = listOf(
        SubseaCableCorridor(
            "MAREA / TAT-14 Transatlantic",
            200,
            listOf(
                GeoCoord(36.8, -76.0), GeoCoord(40.5, -62.0), GeoCoord(46.0, -40.0),
                GeoCoord(49.5, -18.0), GeoCoord(50.1, -5.5), GeoCoord(51.4, -0.1)
            )
        ),
        SubseaCableCorridor(
            "FASTER Transpacific",
            160,
            listOf(
                GeoCoord(45.5, -123.9), GeoCoord(47.0, -145.0), GeoCoord(45.5, -170.0),
                GeoCoord(41.5, 165.0), GeoCoord(35.5, 140.8)
            )
        ),
        SubseaCableCorridor(
            "SEA-ME-WE-5 Backbone",
            120,
            listOf(
                GeoCoord(43.3, 5.4), GeoCoord(37.5, 14.5), GeoCoord(31.2, 32.3),
                GeoCoord(20.0, 38.5), GeoCoord(12.6, 43.3), GeoCoord(14.0, 56.0),
                GeoCoord(10.0, 72.0), GeoCoord(6.9, 79.8), GeoCoord(3.0, 98.0), GeoCoord(1.3, 103.8)
            )
        ),
        SubseaCableCorridor(
            "SACS South Atlantic",
            80,
            listOf(
                GeoCoord(-3.7, -38.5), GeoCoord(-6.5, -20.0), GeoCoord(-8.8, 0.0), GeoCoord(-8.8, 13.2)
            )
        ),
        SubseaCableCorridor(
            "PLCN Pacific Link",
            144,
            listOf(
                GeoCoord(33.9, -118.4), GeoCoord(26.0, -145.0), GeoCoord(21.3, -157.8),
                GeoCoord(22.0, 160.0), GeoCoord(24.0, 135.0), GeoCoord(22.3, 114.2)
            )
        )
    )

    val tectonicLines: List<List<GeoCoord>> = listOf(
        // Pacific Ring of Fire (West)
        listOf(
            GeoCoord(-45.0, 168.0), GeoCoord(-22.0, 175.0), GeoCoord(-6.0, 152.0),
            GeoCoord(6.0, 127.0), GeoCoord(22.0, 122.0), GeoCoord(35.0, 141.0),
            GeoCoord(46.0, 152.0), GeoCoord(54.0, 162.0), GeoCoord(53.0, -172.0),
            GeoCoord(58.0, -150.0)
        ),
        // San Andreas / East Pacific
        listOf(
            GeoCoord(40.5, -124.5), GeoCoord(37.8, -122.5), GeoCoord(34.0, -117.5),
            GeoCoord(24.0, -109.0), GeoCoord(14.0, -93.0), GeoCoord(0.0, -82.0),
            GeoCoord(-18.0, -72.0), GeoCoord(-38.0, -74.0)
        ),
        // Mid-Atlantic Ridge
        listOf(
            GeoCoord(65.0, -18.0), GeoCoord(52.0, -32.0), GeoCoord(36.0, -34.0),
            GeoCoord(15.0, -45.0), GeoCoord(0.0, -25.0), GeoCoord(-20.0, -13.0),
            GeoCoord(-45.0, -15.0)
        )
    )

    // Major metropolitan night-light cluster points
    val cityLightNodes: List<GeoCoord> = listOf(
        GeoCoord(40.71, -74.01), GeoCoord(34.05, -118.24), GeoCoord(37.77, -122.42),
        GeoCoord(41.88, -87.63), GeoCoord(29.76, -95.37), GeoCoord(43.65, -79.38),
        GeoCoord(19.43, -99.13), GeoCoord(-23.55, -46.63), GeoCoord(-34.60, -58.38),
        GeoCoord(4.71, -74.07), GeoCoord(51.51, -0.13), GeoCoord(48.86, 2.35),
        GeoCoord(52.52, 13.40), GeoCoord(40.42, -3.70), GeoCoord(41.90, 12.50),
        GeoCoord(55.75, 37.62), GeoCoord(41.01, 28.98), GeoCoord(30.04, 31.24),
        GeoCoord(25.20, 55.27), GeoCoord(24.71, 46.67), GeoCoord(6.52, 3.38),
        GeoCoord(-26.20, 28.05), GeoCoord(19.08, 72.88), GeoCoord(28.61, 77.21),
        GeoCoord(12.97, 77.59), GeoCoord(23.81, 90.41), GeoCoord(13.76, 100.50),
        GeoCoord(1.35, 103.82), GeoCoord(-6.21, 106.85), GeoCoord(14.60, 120.98),
        GeoCoord(22.32, 114.17), GeoCoord(31.23, 121.47), GeoCoord(39.90, 116.40),
        GeoCoord(37.57, 126.98), GeoCoord(35.68, 139.77), GeoCoord(34.69, 135.50),
        GeoCoord(-33.87, 151.21), GeoCoord(-37.81, 144.96), GeoCoord(-36.85, 174.76)
    )
}

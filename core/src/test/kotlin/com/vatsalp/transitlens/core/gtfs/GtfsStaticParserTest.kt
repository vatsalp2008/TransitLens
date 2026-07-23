package com.vatsalp.transitlens.core.gtfs

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GtfsStaticParserTest {

    @Test
    fun parsesStopsWithQuotedCommasAndWheelchairFlag() {
        val csv = """
            stop_id,stop_name,stop_lat,stop_lon,wheelchair_boarding
            1001,"Pine St & 3rd Ave, Bay 2",47.6101,-122.3421,1
            1002,Westlake,47.6115,-122.3376,2
        """.trimIndent()

        val stops = GtfsStaticParser.parseStops(csv)
        assertEquals(2, stops.size)
        assertEquals("Pine St & 3rd Ave, Bay 2", stops[0].stopName)
        assertEquals(47.6101, stops[0].lat)
        assertTrue(stops[0].toTransitNode().hasWheelchairAccess)
        assertFalse(stops[1].toTransitNode().hasWheelchairAccess)
    }

    @Test
    fun stripsUtf8ByteOrderMark() {
        val csv = "${0xFEFF.toChar()}route_id,route_short_name,route_long_name,route_type\n100,49,Broadway,3"
        val routes = GtfsStaticParser.parseRoutes(csv)
        assertEquals(1, routes.size)
        assertEquals("100", routes[0].routeId)
        assertEquals("49", routes[0].shortName)
    }

    @Test
    fun handlesEscapedQuotes() {
        val csv = "stop_id,stop_name,stop_lat,stop_lon\n5,\"The \"\"Hub\"\"\",47.6,-122.3"
        val stops = GtfsStaticParser.parseStops(csv)
        assertEquals(1, stops.size)
        assertEquals("The \"Hub\"", stops[0].stopName)
    }

    @Test
    fun skipsRowsMissingCoordinates() {
        val csv = "stop_id,stop_name,stop_lat,stop_lon\nX,No Coords,,\nY,Good,47.6,-122.3"
        val stops = GtfsStaticParser.parseStops(csv)
        assertEquals(1, stops.size)
        assertEquals("Y", stops[0].stopId)
    }

    @Test
    fun handlesCarriageReturnLineEndings() {
        val csv = "route_id,route_short_name,route_long_name,route_type\r\n7,7,Rainier,3\r\n"
        val routes = GtfsStaticParser.parseRoutes(csv)
        assertEquals(1, routes.size)
        assertEquals("Rainier", routes[0].longName)
    }
}

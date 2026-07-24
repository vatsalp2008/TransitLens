package com.vatsalp.transitlens.gtfsrt

import com.google.transit.realtime.GtfsRealtime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GtfsRealtimeParserTest {

    private fun feed(stopId: String, routeId: String, arrivalEpoch: Long): ByteArray =
        GtfsRealtime.FeedMessage.newBuilder()
            .setHeader(GtfsRealtime.FeedHeader.newBuilder().setGtfsRealtimeVersion("2.0"))
            .addEntity(
                GtfsRealtime.FeedEntity.newBuilder()
                    .setId("e1")
                    .setTripUpdate(
                        GtfsRealtime.TripUpdate.newBuilder()
                            .setTrip(GtfsRealtime.TripDescriptor.newBuilder().setRouteId(routeId).setTripId("T1"))
                            .addStopTimeUpdate(
                                GtfsRealtime.TripUpdate.StopTimeUpdate.newBuilder()
                                    .setStopId(stopId)
                                    .setArrival(
                                        GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setTime(arrivalEpoch),
                                    ),
                            ),
                    ),
            )
            .build()
            .toByteArray()

    @Test
    fun parsesArrivalForRequestedStopAndMapsRouteName() {
        val now = 1_000_000L
        val preds = GtfsRealtimeParser.parseTripUpdates(
            feed(stopId = "1001", routeId = "100", arrivalEpoch = now + 120),
            stopId = "1001",
            routeShortNames = mapOf("100" to "49"),
            nowSeconds = now,
        )
        assertEquals(1, preds.size)
        assertEquals("49", preds[0].routeShortName)
        assertEquals(120, preds[0].arrivalSeconds)
    }

    @Test
    fun ignoresUpdatesForOtherStops() {
        val now = 1_000_000L
        val preds = GtfsRealtimeParser.parseTripUpdates(
            feed(stopId = "9999", routeId = "100", arrivalEpoch = now + 60),
            stopId = "1001",
            nowSeconds = now,
        )
        assertTrue(preds.isEmpty())
    }
}

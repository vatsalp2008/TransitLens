package com.vatsalp.transitlens.gtfsrt

import com.google.transit.realtime.GtfsRealtime
import com.vatsalp.transitlens.core.model.ArrivalPrediction
import com.vatsalp.transitlens.core.model.PredictionConfidence

/**
 * Parses a GTFS-RT `FeedMessage` protobuf into arrival predictions for one stop.
 * Kept free of Android/network deps so it is JVM unit-tested with protobuf fixtures.
 */
object GtfsRealtimeParser {

    fun parseTripUpdates(
        bytes: ByteArray,
        stopId: String,
        routeShortNames: Map<String, String> = emptyMap(),
        nowSeconds: Long = System.currentTimeMillis() / 1000,
    ): List<ArrivalPrediction> {
        val feed = GtfsRealtime.FeedMessage.parseFrom(bytes)
        val result = ArrayList<ArrivalPrediction>()
        for (entity in feed.entityList) {
            if (!entity.hasTripUpdate()) continue
            val tripUpdate = entity.tripUpdate
            val routeId = tripUpdate.trip.routeId
            for (update in tripUpdate.stopTimeUpdateList) {
                if (update.stopId != stopId || !update.hasArrival()) continue
                result.add(
                    ArrivalPrediction(
                        routeId = routeId,
                        routeShortName = routeShortNames[routeId] ?: routeId,
                        headsign = tripUpdate.trip.tripId,
                        arrivalSeconds = (update.arrival.time - nowSeconds).toInt(),
                        isWheelchairAccessible = false,
                        confidence = PredictionConfidence.PREDICTED,
                    ),
                )
            }
        }
        return result.sortedBy { it.arrivalSeconds }
    }
}

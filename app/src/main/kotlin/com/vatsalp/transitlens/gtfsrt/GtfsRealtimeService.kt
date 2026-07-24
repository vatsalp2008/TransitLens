package com.vatsalp.transitlens.gtfsrt

import com.vatsalp.transitlens.core.model.ArrivalPrediction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fetches a GTFS-RT trip-updates protobuf feed and parses arrivals for a stop.
 * Point [feedUrl] at a OneBusAway / King County Metro GTFS-RT endpoint; live
 * data requires a free OneBusAway API key embedded in the URL. Returns an empty
 * list on any network/parse failure so callers fall back to cached schedules.
 */
@Singleton
class GtfsRealtimeService @Inject constructor() {

    private val client = OkHttpClient()

    suspend fun getArrivals(
        feedUrl: String,
        stopId: String,
        routeShortNames: Map<String, String> = emptyMap(),
    ): List<ArrivalPrediction> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url(feedUrl).build()
            client.newCall(request).execute().use { response ->
                val body = response.body
                if (!response.isSuccessful || body == null) {
                    emptyList()
                } else {
                    GtfsRealtimeParser.parseTripUpdates(body.bytes(), stopId, routeShortNames)
                }
            }
        }.getOrDefault(emptyList())
    }
}

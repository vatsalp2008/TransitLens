package com.vatsalp.transitlens.data.gtfs

import com.vatsalp.transitlens.core.gtfs.GtfsStaticParser
import com.vatsalp.transitlens.core.model.NodeType
import com.vatsalp.transitlens.core.model.TransitGraph
import com.vatsalp.transitlens.core.model.TransitNode
import com.vatsalp.transitlens.core.routing.GraphBuilder
import com.vatsalp.transitlens.data.db.RouteEntity
import com.vatsalp.transitlens.data.db.StopEntity
import com.vatsalp.transitlens.data.db.TransitDatabase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Imports GTFS static CSVs (parsed by :core [GtfsStaticParser]) into the Room
 * cache, and builds a routable pedestrian graph from the cached stops.
 */
@Singleton
class GtfsStaticImporter @Inject constructor(
    private val db: TransitDatabase,
) {
    suspend fun importFromText(stopsCsv: String, routesCsv: String) {
        val stops = GtfsStaticParser.parseStops(stopsCsv).map {
            StopEntity(it.stopId, it.stopName, it.lat, it.lon, it.wheelchairBoarding)
        }
        val routes = GtfsStaticParser.parseRoutes(routesCsv).map {
            RouteEntity(it.routeId, it.shortName, it.longName, it.type)
        }
        db.stopDao().insertAll(stops)
        db.routeDao().insertAll(routes)
    }

    suspend fun isPopulated(): Boolean = db.stopDao().count() > 0

    suspend fun buildWalkingGraph(maxEdgeMeters: Int = 400): TransitGraph {
        val nodes = db.stopDao().all().map {
            TransitNode(
                id = it.stopId,
                type = NodeType.STOP,
                lat = it.lat,
                lon = it.lon,
                hasWheelchairAccess = it.wheelchairBoarding == 1,
                name = it.name,
            )
        }
        return GraphBuilder.buildWalkingGraph(nodes, maxEdgeMeters)
    }
}

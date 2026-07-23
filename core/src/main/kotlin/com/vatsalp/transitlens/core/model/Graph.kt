package com.vatsalp.transitlens.core.model

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class NodeType { STOP, WAYPOINT, TRANSFER }

enum class TransitMode { WALK, BUS, TRAIN, TRANSFER }

data class LatLng(val lat: Double, val lon: Double)

data class TransitNode(
    val id: String,
    val type: NodeType,
    val lat: Double,
    val lon: Double,
    val hasElevator: Boolean = false,
    val hasWheelchairAccess: Boolean = true,
    val hillGrade: Float = 0f,
    val hasAudioAnnouncement: Boolean = false,
    val name: String? = null,
) {
    val latLng: LatLng get() = LatLng(lat, lon)
}

data class TransitEdge(
    val from: String,
    val to: String,
    val mode: TransitMode,
    val durationSeconds: Int,
    val distanceMeters: Int,
    val requiresStairs: Boolean = false,
    val hillGrade: Float = 0f,
    val wheelchairAccessible: Boolean = true,
    val routeId: String? = null,
    val routeShortName: String? = null,
)

/** Directed graph of transit + pedestrian edges the router searches over. */
class TransitGraph(
    nodes: Collection<TransitNode>,
    val edges: List<TransitEdge>,
) {
    val nodes: Map<String, TransitNode> = nodes.associateBy { it.id }
    private val adjacency: Map<String, List<TransitEdge>> = edges.groupBy { it.from }

    fun edgesFrom(nodeId: String): List<TransitEdge> = adjacency[nodeId].orEmpty()

    fun node(id: String): TransitNode? = nodes[id]

    fun nearestNode(target: LatLng): TransitNode? =
        nodes.values.minByOrNull { haversineMeters(target, it.latLng) }
}

/** Great-circle distance in meters between two coordinates. */
fun haversineMeters(a: LatLng, b: LatLng): Double {
    val earthRadius = 6_371_000.0
    val dLat = Math.toRadians(b.lat - a.lat)
    val dLon = Math.toRadians(b.lon - a.lon)
    val lat1 = Math.toRadians(a.lat)
    val lat2 = Math.toRadians(b.lat)
    val h = sin(dLat / 2) * sin(dLat / 2) +
        cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
    return 2 * earthRadius * atan2(sqrt(h), sqrt(1 - h))
}

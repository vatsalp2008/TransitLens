package com.vatsalp.transitlens.core.gtfs

import com.vatsalp.transitlens.core.model.NodeType
import com.vatsalp.transitlens.core.model.TransitNode

data class GtfsStop(
    val stopId: String,
    val stopName: String,
    val lat: Double,
    val lon: Double,
    val wheelchairBoarding: Int = 0, // GTFS: 0 = unknown, 1 = accessible, 2 = not accessible
) {
    fun toTransitNode(): TransitNode = TransitNode(
        id = stopId,
        type = NodeType.STOP,
        lat = lat,
        lon = lon,
        hasWheelchairAccess = wheelchairBoarding == 1,
        name = stopName,
    )
}

data class GtfsRoute(
    val routeId: String,
    val shortName: String,
    val longName: String,
    val type: Int,
)

/**
 * Parser for GTFS static feed CSVs. Kept in :core (pure Kotlin) so the same
 * parsing is unit-tested here and reused by the Android Room cache layer.
 */
object GtfsStaticParser {

    private const val BOM = 0xFEFF

    fun parseStops(csv: String): List<GtfsStop> =
        parseCsv(csv).mapNotNull { row ->
            val id = row["stop_id"]?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val lat = row["stop_lat"]?.toDoubleOrNull() ?: return@mapNotNull null
            val lon = row["stop_lon"]?.toDoubleOrNull() ?: return@mapNotNull null
            GtfsStop(
                stopId = id,
                stopName = row["stop_name"].orEmpty(),
                lat = lat,
                lon = lon,
                wheelchairBoarding = row["wheelchair_boarding"]?.toIntOrNull() ?: 0,
            )
        }

    fun parseRoutes(csv: String): List<GtfsRoute> =
        parseCsv(csv).mapNotNull { row ->
            val id = row["route_id"]?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            GtfsRoute(
                routeId = id,
                shortName = row["route_short_name"].orEmpty(),
                longName = row["route_long_name"].orEmpty(),
                type = row["route_type"]?.toIntOrNull() ?: -1,
            )
        }

    /** Parse CSV text into a list of column-name -> value maps. */
    fun parseCsv(text: String): List<Map<String, String>> {
        val withoutBom = if (text.isNotEmpty() && text[0].code == BOM) text.substring(1) else text
        val rows = splitRows(withoutBom)
        if (rows.isEmpty()) return emptyList()
        val header = rows.first().map { it.trim() }
        return rows.drop(1)
            .filter { row -> row.any { it.isNotBlank() } }
            .map { fields ->
                header.indices.associate { i -> header[i] to fields.getOrNull(i)?.trim().orEmpty() }
            }
    }

    /** Minimal RFC 4180-style tokenizer: handles quoted fields, escaped quotes, and CRLF. */
    private fun splitRows(text: String): List<List<String>> {
        val rows = ArrayList<List<String>>()
        val field = StringBuilder()
        var current = ArrayList<String>()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            when {
                inQuotes -> when {
                    ch == '"' && i + 1 < text.length && text[i + 1] == '"' -> {
                        field.append('"'); i++
                    }
                    ch == '"' -> inQuotes = false
                    else -> field.append(ch)
                }
                ch == '"' -> inQuotes = true
                ch == ',' -> { current.add(field.toString()); field.clear() }
                ch == '\n' -> { current.add(field.toString()); field.clear(); rows.add(current); current = ArrayList() }
                ch == '\r' -> Unit // handled as part of CRLF
                else -> field.append(ch)
            }
            i++
        }
        if (field.isNotEmpty() || current.isNotEmpty()) {
            current.add(field.toString())
            rows.add(current)
        }
        return rows
    }
}

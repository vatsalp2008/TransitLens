package com.vatsalp.transitlens.core.text

import com.vatsalp.transitlens.core.model.TransitTextContext

/**
 * Extracts structured transit entities (route numbers, stop id, arrival time,
 * accessibility symbols) from raw OCR text produced by ML Kit.
 *
 * Arrival-time and stop-id spans are stripped before route extraction so that,
 * e.g., the "3" in "3:45" or the digits of a stop id are not mistaken for a
 * route number.
 */
object TransitTextExtractor {

    private const val WHEELCHAIR_SYMBOL = 0x267F // ♿

    private val TIME = Regex("""\b\d{1,2}:\d{2}\s*(?:[AaPp][Mm])?""")
    private val STOP_ID = Regex("""\bStop\s*#?\s*(\d{4,5})\b""", RegexOption.IGNORE_CASE)
    private val ROUTE = Regex("""\b\d{1,3}[A-Z]?\b""")
    private val ACCESSIBILITY_KEYWORDS = Regex("(?i)wheelchair|elevator|accessible")

    fun extract(text: String, knownRouteNames: Set<String> = emptySet()): TransitTextContext {
        val arrival = TIME.find(text)?.value?.trim()?.replace(Regex("""\s+"""), " ")
        val stopId = STOP_ID.find(text)?.groupValues?.getOrNull(1)

        // Blank out time and stop-id spans before scanning for route numbers.
        val cleaned = STOP_ID.replace(TIME.replace(text, " "), " ")
        val routes = LinkedHashSet<String>()
        ROUTE.findAll(cleaned).forEach { routes.add(it.value) }
        knownRouteNames.forEach { name ->
            val re = Regex("""\b${Regex.escape(name)}\b""", RegexOption.IGNORE_CASE)
            if (re.containsMatchIn(text)) routes.add(name)
        }

        val hasAccessibility = ACCESSIBILITY_KEYWORDS.containsMatchIn(text) ||
            text.any { it.code == WHEELCHAIR_SYMBOL }

        return TransitTextContext(
            routeNumbers = routes.toList(),
            stopId = stopId,
            arrivalDisplay = arrival,
            hasAccessibilitySymbol = hasAccessibility,
        )
    }
}

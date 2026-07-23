package com.vatsalp.transitlens.core.text

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TransitTextExtractorTest {

    @Test
    fun extractsRouteStopAndArrivalWithoutFalsePositives() {
        val text = "Route 49 to University District, Stop 12345, arrives 3:45 PM"
        val result = TransitTextExtractor.extract(text)

        assertContains(result.routeNumbers, "49")
        assertEquals("12345", result.stopId)
        assertEquals("3:45 PM", result.arrivalDisplay)
        // The "3" and "45" from the time and the stop-id digits must not become routes.
        assertFalse(result.routeNumbers.contains("3"))
        assertFalse(result.routeNumbers.contains("45"))
        assertFalse(result.routeNumbers.contains("12345"))
    }

    @Test
    fun detectsAccessibilityKeyword() {
        assertTrue(TransitTextExtractor.extract("Elevator out of service").hasAccessibilitySymbol)
    }

    @Test
    fun detectsWheelchairGlyph() {
        val text = "Bay 2 ${0x267F.toChar()}"
        assertTrue(TransitTextExtractor.extract(text).hasAccessibilitySymbol)
    }

    @Test
    fun recognizesNamedRoutes() {
        val result = TransitTextExtractor.extract("Link light rail to Angle Lake", knownRouteNames = setOf("Link"))
        assertContains(result.routeNumbers, "Link")
    }

    @Test
    fun matchesRouteWithLetterSuffix() {
        assertContains(TransitTextExtractor.extract("Board the 550X").routeNumbers, "550X")
    }

    @Test
    fun emptyOnPlainText() {
        val result = TransitTextExtractor.extract("Welcome aboard")
        assertTrue(result.routeNumbers.isEmpty())
        assertEquals(null, result.stopId)
        assertEquals(null, result.arrivalDisplay)
        assertFalse(result.hasAccessibilitySymbol)
    }
}

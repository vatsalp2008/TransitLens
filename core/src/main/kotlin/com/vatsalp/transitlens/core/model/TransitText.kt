package com.vatsalp.transitlens.core.model

/** Structured transit entities extracted from OCR text (see spec Model 3). */
data class TransitTextContext(
    val routeNumbers: List<String> = emptyList(),
    val stopId: String? = null,
    val arrivalDisplay: String? = null,
    val hasAccessibilitySymbol: Boolean = false,
) {
    companion object {
        val EMPTY = TransitTextContext()
    }
}

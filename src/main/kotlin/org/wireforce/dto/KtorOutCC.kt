package org.wireforce.dto


/**
 * Represents the output structure of a Ktor task for Closed Captions (CC),
 * containing a list of data entries providing information about each caption.
 *
 * @property data The list of data entries, each representing a caption with duration, start time, and text.
 */
data class KtorOutCC(
    val `data`: List<Data>
) {
    /**
     * Represents an individual caption entry within the KtorOutCC output structure.
     *
     * @property duration The duration of the caption in seconds.
     * @property start The start time of the caption in seconds.
     * @property text The text content of the caption.
     */
    data class Data(
        val duration: Double,
        val start: Double,
        val text: String
    )
}

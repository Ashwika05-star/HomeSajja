package com.homesajja.app.data.model

/** Fixed launch-city list for signup and discovery scoping. */
object Cities {
    val ALL = listOf("Mumbai", "Pune", "Bengaluru", "Delhi", "Hyderabad")

    /** City centres (latitude, longitude): where the shop-location map opens before a pin is set. */
    private val CENTRES = mapOf(
        "Mumbai" to (19.0760 to 72.8777),
        "Pune" to (18.5204 to 73.8567),
        "Bengaluru" to (12.9716 to 77.5946),
        "Delhi" to (28.6139 to 77.2090),
        "Hyderabad" to (17.3850 to 78.4867),
    )

    fun centreOf(city: String): Pair<Double, Double> = CENTRES[city] ?: CENTRES.getValue("Mumbai")
}

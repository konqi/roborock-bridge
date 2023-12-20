package de.konqi.roborockbridge.protocol.mqtt.response.map

/**
 * The value received via mqtt has to be divided by 50 to match the pixel map
 */
const val SCALE_DIVISOR = 50

/**
 * Multiply the values with 100 before dividing by 50 and divide by 100 at the end.
 * This should ALWAYS provide a "clean" value with up to two decimal places.
 */
private fun safeCorrect(
    value: Int, offsetCorrection: Int
): Float = (value * 100 / SCALE_DIVISOR - offsetCorrection * 100) / 100f

private fun safeCorrect(value: UInt, offsetCorrection: UInt) =
    safeCorrect(value.toInt(), offsetCorrection.toInt())

private fun safeCorrect(value: UShort, offsetCorrection: UShort) =
    safeCorrect(value.toInt(), offsetCorrection.toInt())

open class Coordinate<T>(
    open val x: T,
    open val y: T
)

fun Coordinate<UInt>.correct(top: UInt, left: UInt): Coordinate<Float> {
    return Coordinate(x = safeCorrect(x, left), y = safeCorrect(y, top))
}

fun Coordinate<UShort>.correct(top: UShort, left: UShort): Coordinate<Float> {
    return Coordinate(x = safeCorrect(x, left), y = safeCorrect(y, top))
}
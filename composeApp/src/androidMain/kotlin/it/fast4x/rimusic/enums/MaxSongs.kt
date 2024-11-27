package it.fast4x.rimusic.enums

enum class MaxSongs {
    `50`,
    `100`,
    `200`,
    `300`,
    `500`,
    `1000`,
    `2000`,
    `3000`,
    Unlimited;

    fun toInt(): Int =
        when( this ) {
            /** A million is still within [Int.MIN_VALUE] and [Int.MAX_VALUE] */
            Unlimited -> 1_000_000
            else -> this.name.toInt()
        }
}
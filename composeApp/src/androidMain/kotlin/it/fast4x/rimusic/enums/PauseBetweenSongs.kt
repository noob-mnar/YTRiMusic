package it.fast4x.rimusic.enums

// Default values are in seconds
enum class PauseBetweenSongs {
    `0`,
    `5`,
    `10`,
    `15`,
    `20`,
    `30`,
    `40`,
    `50`,
    `60`;

    val asMillis: Long = this.name
                             .toLong()
                             .times( 1000 )
}

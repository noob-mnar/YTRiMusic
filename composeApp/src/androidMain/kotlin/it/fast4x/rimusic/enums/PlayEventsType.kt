package it.fast4x.rimusic.enums

enum class PlayEventsType(
    val searchLimit: Int
) {
    MostPlayed( 1 ),
    LastPlayed( 3 ),
    CasualPlayed( 100 )
}
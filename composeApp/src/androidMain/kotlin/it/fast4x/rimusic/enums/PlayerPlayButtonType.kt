package it.fast4x.rimusic.enums

enum class PlayerPlayButtonType(
    val width: Int,
    val height: Int
) {
    Disabled( 60, 60 ),
    Default( 60, 60 ),
    Rectangular( 110, 70 ),
    CircularRibbed( 100, 100 ),
    Square( 80, 80 );
}
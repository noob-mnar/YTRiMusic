package it.fast4x.rimusic.enums

enum class PlayerPlayButtonType {
    Disabled,
    Default,
    Rectangular,
    CircularRibbed,
    Square;

    val height: Int
        get() = when (this) {
            Default -> 60
            Rectangular -> 70
            CircularRibbed -> 100
            Square -> 80
            Disabled -> 60
        }

    val width: Int
        get() = when (this) {
            Default -> 60
            Rectangular -> 110
            CircularRibbed -> 100
            Square -> 80
            Disabled -> 60

        }
}
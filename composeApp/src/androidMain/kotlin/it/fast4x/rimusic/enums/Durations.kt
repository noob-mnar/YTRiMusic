package it.fast4x.rimusic.enums

enum class DurationInSeconds {
    Disabled,
    `3`,
    `4`,
    `5`,
    `6`,
    `7`,
    `8`,
    `9`,
    `10`,
    `11`,
    `12`;

    val seconds: Int
        get() = when (this) {
            Disabled -> 0
            `3` -> 3
            `4` -> 4
            `5` -> 5
            `6` -> 6
            `7` -> 7
            `8` -> 8
            `9` -> 9
            `10` -> 10
            `11` -> 11
            `12` -> 12
        } * 1000


    val fadeOutRange: ClosedFloatingPointRange<Float>
        get() = when (this) {
        Disabled -> 0f..0f
        `11`, `12` -> 96.00f..96.20f
        `9`, `10` -> 97.00f..97.20f
        `7`, `8` -> 98.00f..98.20f
        `5`, `6` -> 98.21f..99.20f
        `3`, `4` -> 99.21f..99.99f
    }

    val fadeInRange: ClosedFloatingPointRange<Float>
        get() = 00.00f..00.00f

}

enum class DurationInMinutes {
    Disabled,
    `3`,
    `5`,
    `10`,
    `15`,
    `20`,
    `25`,
    `30`,
    `60`;

    val asMillis: Long = toInt() * 3_600_000L

    fun toInt() =
        if( this == Disabled )
            0
        else
            this.name.toInt()
}

enum class DurationInMilliseconds( val asMillis: Int ) {
    Disabled( 0 ),
    `100ms`( 100 ),
    `200ms`( 200 ),
    `300ms`( 300 ),
    `400ms`( 400 ),
    `500ms`( 500 ),
    `600ms`( 600 ),
    `700ms`( 700 ),
    `800ms`( 800 ),
    `900ms`( 900 ),
    `1000ms`( 1000 );
}

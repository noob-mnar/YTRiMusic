package it.fast4x.rimusic.enums

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import it.fast4x.rimusic.R
import me.knighthat.enums.TextView

enum class DurationInMinutes: TextView {
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

    override val text: String
        @Composable
        get() = when( this ) {
            Disabled -> stringResource( R.string.vt_disabled )
            else -> this.name
        }
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

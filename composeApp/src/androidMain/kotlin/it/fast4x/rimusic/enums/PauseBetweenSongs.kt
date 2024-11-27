package it.fast4x.rimusic.enums

import androidx.compose.runtime.Composable
import me.knighthat.enums.TextView

// Default values are in seconds
enum class PauseBetweenSongs: TextView {
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

    override val text: String
        @Composable
        get() = "${this.name}s"
}

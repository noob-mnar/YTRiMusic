package it.fast4x.rimusic.enums

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import it.fast4x.rimusic.R
import me.knighthat.colorPalette
import me.knighthat.enums.Drawable

enum class DownloadedStateMedia(
    @field:DrawableRes override val iconId: Int
): Drawable {

    CACHED( R.drawable.download ),

    CACHED_AND_DOWNLOADED( R.drawable.downloaded ),

    DOWNLOADED( R.drawable.downloaded ),

    NOT_CACHED_OR_DOWNLOADED( R.drawable.download );

    val color: Color
        @Composable
        get() = when( this ) {
            NOT_CACHED_OR_DOWNLOADED -> colorPalette().textDisabled
            else -> colorPalette().text
        }
}
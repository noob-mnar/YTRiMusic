package it.fast4x.rimusic.enums

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

enum class ThumbnailRoundness( val radius: Int ) {
    None( 0 ),
    Light( 8 ),
    Medium( 12 ),
    Heavy( 16 );

    fun shape(): Shape = when (this) {
        None -> RectangleShape
        else -> RoundedCornerShape(this.radius.dp)
    }
}

package it.fast4x.rimusic.enums

import androidx.annotation.StringRes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import it.fast4x.rimusic.R
import me.knighthat.enums.TextView

enum class ThumbnailRoundness(
    val radius: Int,
    @field:StringRes override val textId: Int
): TextView {

    None( 0, R.string.none ),

    Light( 8, R.string.light ),

    Medium( 12, R.string.medium ),

    Heavy( 16, R.string.heavy );

    fun shape(): Shape = when (this) {
        None -> RectangleShape
        else -> RoundedCornerShape(this.radius.dp)
    }
}

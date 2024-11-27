package me.knighthat.enums

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource

interface Drawable {

    val iconId: Int
        @DrawableRes
        get() = throw UnsupportedOperationException( "Please use [${this::class.simpleName}#icon] directly!" )

    val icon: Painter
        @Composable
        get() = painterResource( this.iconId )
}
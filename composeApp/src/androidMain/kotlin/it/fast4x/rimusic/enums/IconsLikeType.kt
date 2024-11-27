package it.fast4x.rimusic.enums

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import it.fast4x.rimusic.R
import me.knighthat.enums.TextView

enum class IconLikeType(
    @field:DrawableRes val filledId: Int,
    @field:DrawableRes val outlineId: Int,
    @field:StringRes override val textId: Int
): TextView {

    Apple( R.drawable.heart_apple, R.drawable.heart_apple_outline, R.string.icon_like_apple ),

    Breaked( R.drawable.heart_breaked_no, R.drawable.heart_breaked_yes, R.string.icon_like_breaked ),

    Brilliant( R.drawable.heart_brilliant, R.drawable.heart_brilliant_outline, R.string.icon_like_brilliant ),

    Essential( R.drawable.heart, R.drawable.heart_outline, R.string.pcontrols_essential ),

    Gift( R.drawable.heart_gift, R.drawable.heart_gift_outline, R.string.icon_like_gift ),

    Shape( R.drawable.heart_shape, R.drawable.heart_shape_outline, R.string.icon_like_shape ),

    Striped( R.drawable.heart_striped, R.drawable.heart_striped_outline, R.string.icon_like_striped );
}
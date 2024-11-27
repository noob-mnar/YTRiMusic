package it.fast4x.rimusic.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import it.fast4x.rimusic.R
import it.fast4x.rimusic.enums.IconLikeType

@Composable
fun getLikedIcon(): Int {
    val iconLikeType by rememberPreference(iconLikeTypeKey, IconLikeType.Essential)

    return iconLikeType.filledId
}

@Composable
fun getUnlikedIcon(): Int {
    val iconLikeType by rememberPreference(iconLikeTypeKey, IconLikeType.Essential)

    return iconLikeType.outlineId
}

@Composable
fun getDislikedIcon(): Int {
    return R.drawable.heart_dislike
}
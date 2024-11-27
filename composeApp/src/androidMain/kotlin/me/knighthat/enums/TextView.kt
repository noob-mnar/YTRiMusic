package me.knighthat.enums

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

interface TextView {

    @get:StringRes
    val textId: Int

    val text: String
        @Composable
        get() = stringResource( this.textId )
}
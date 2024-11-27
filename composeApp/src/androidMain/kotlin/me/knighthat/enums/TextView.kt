package me.knighthat.enums

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

interface TextView {

    val textId: Int
        @StringRes
        get() = throw UnsupportedOperationException( "Please use [${this::class.simpleName}#text] directly!" )

    val text: String
        @Composable
        get() = stringResource( this.textId )
}
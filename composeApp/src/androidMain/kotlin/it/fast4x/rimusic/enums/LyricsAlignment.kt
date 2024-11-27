package it.fast4x.rimusic.enums

import androidx.compose.ui.text.style.TextAlign

enum class LyricsAlignment( val textAlign: TextAlign ) {
    Left( TextAlign.Start ),
    Center( TextAlign.Center ),
    Right( TextAlign.End );
}
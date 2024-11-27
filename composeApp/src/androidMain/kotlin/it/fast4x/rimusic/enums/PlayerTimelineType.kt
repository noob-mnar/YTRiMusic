package it.fast4x.rimusic.enums

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import it.fast4x.rimusic.R
import me.knighthat.enums.TextView

enum class PlayerTimelineType(
    @field:StringRes override val textId: Int
): TextView {

    Default( R.string._default ),
    Wavy( R.string.wavy_timeline ),
    PinBar( R.string.bodied_bar ),
    BodiedBar( R.string.pin_bar ),
    FakeAudioBar( R.string.fake_audio_bar ),
    ThinBar( R.string.thin_bar ),
    // TODO: Add "Colored bar" to strings.xml
    ColoredBar( -1 );

    override val text: String
        @Composable
        get() = when( this ) {
            ColoredBar -> "Colored bar"
            else -> super.text
        }
}
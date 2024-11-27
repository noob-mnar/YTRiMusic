package it.fast4x.rimusic.enums

import androidx.annotation.DrawableRes
import androidx.media3.common.Player
import it.fast4x.rimusic.R
import org.intellij.lang.annotations.MagicConstant

enum class QueueLoopType(
    @field:MagicConstant(valuesFromClass = Player.RepeatMode::class) val type: Int,
    @field:DrawableRes val iconId: Int
) {
    Default( Player.REPEAT_MODE_OFF, R.drawable.repeat ),
    RepeatOne( Player.REPEAT_MODE_ONE, R.drawable.repeatone ),
    RepeatAll( Player.REPEAT_MODE_ALL, R.drawable.infinite );

    companion object {
        @JvmStatic
        fun fromType(
            @MagicConstant(valuesFromClass = Player.RepeatMode::class) type: Int
        ) = when( type ) {
            Player.REPEAT_MODE_ONE -> RepeatOne
            Player.REPEAT_MODE_ALL -> RepeatAll
            else -> Default
        }
    }

    /**
     * Going through all values of [QueueLoopType] from top to bottom.
     *
     * Once pointer reaches the end (last value), [next] will return
     * back the the first value
     */
    fun next(): QueueLoopType = when( this ) {
        Default -> RepeatOne
        RepeatOne -> RepeatAll
        // Avoid using else, when a new value is added
        // this will make sure that the dev have to
        // make a case for that value.
        RepeatAll -> Default
    }
}
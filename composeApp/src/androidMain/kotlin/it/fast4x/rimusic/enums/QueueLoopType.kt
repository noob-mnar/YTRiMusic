package it.fast4x.rimusic.enums

import androidx.media3.common.Player
import org.intellij.lang.annotations.MagicConstant

enum class QueueLoopType(
    @field:MagicConstant(valuesFromClass = Player.RepeatMode::class) val type: Int
) {
    Default( Player.REPEAT_MODE_OFF ),
    RepeatOne( Player.REPEAT_MODE_ONE ),
    RepeatAll( Player.REPEAT_MODE_ALL );

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
}
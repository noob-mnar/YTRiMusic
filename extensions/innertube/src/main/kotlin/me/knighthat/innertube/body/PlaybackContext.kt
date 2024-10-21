package me.knighthat.innertube.body

import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class PlaybackContext(
    val contextPlaybackContext: ContentPlaybackContext
) {

    companion object {

        val DEFAULT = PlaybackContext(
            ContentPlaybackContext( ContentPlaybackContext.getTimeStamp() )
        )
    }

    @Serializable
    data class ContentPlaybackContext( val signatureTimestamp: Long ) {

        companion object {

            fun getTimeStamp(): Long =
                LocalDate.now().toEpochDay() - LocalDate.ofEpochDay(0).toEpochDay()
        }
    }
}
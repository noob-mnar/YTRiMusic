package it.fast4x.innertube.models.bodies

import kotlinx.serialization.Serializable
import me.knighthat.innertube.body.Context
import me.knighthat.innertube.body.PlaybackContext

@Serializable
data class PlayerBody(
    val videoId: String,
    val playlistId: String? = null,
    val context: Context = Context.DEFAULT_ANDROID,
    val playbackContext: PlaybackContext = PlaybackContext.DEFAULT
) {

    /**
     * Required parameter as mentioned in
     * [#3](https://github.com/zerodytrash/YouTube-Internal-Clients/issues/3)
     */
    val racyCheckOk: Boolean
        get() = true

    /**
     * Required parameter as mentioned in
     * [#3](https://github.com/zerodytrash/YouTube-Internal-Clients/issues/3)
     */
    val contentCheckOk: Boolean
        get() = true
}

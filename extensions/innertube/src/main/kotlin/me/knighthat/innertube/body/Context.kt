package me.knighthat.innertube.body

import kotlinx.serialization.Serializable
import me.knighthat.innertube.client.AndroidMusic
import me.knighthat.innertube.client.IOSMusic

@Serializable
data class Context(
    val client: Client,
    val thirdParty: ThirdParty = ThirdParty()
) {

    companion object {

        val DEFAULT_ANDROID = Context(
            client = Client(
                clientName = AndroidMusic.CLIENT_NAME,
                clientVersion = AndroidMusic.CLIENT_VERSION
            )
        )

        val DEFAULT_IOS = Context (
            client = Client(
                clientName = IOSMusic.CLIENT_NAME,
                clientVersion = IOSMusic.CLIENT_VERSION
            )
        )
    }

    @Serializable
    data class Client(
        val hl: String = "en",
        val gl: String = "US",
        val clientName: String,
        val clientVersion: String,
        val clientScreen: String = "WATCH",
        val androidSdkVersion: Int = 24
    )

    @Serializable
    data class ThirdParty(
        val embedUrl: String = "https://music.youtube.com"
    )
}

package it.fast4x.innertube.requests

import io.ktor.client.call.body
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.models.PlayerResponse
import it.fast4x.innertube.models.bodies.PlayerBody
import it.fast4x.innertube.utils.runCatchingNonCancellable
import it.fast4x.piped.models.Session
import me.knighthat.innertube.body.Context
import me.knighthat.innertube.client.AndroidMusic
import me.knighthat.innertube.client.IOSMusic

suspend fun Innertube.player(
    body: PlayerBody,
    pipedSession: Session
) = runCatchingNonCancellable {

    var response = client.post(player) {
        setBody(body)
        mask("playabilityStatus.status,playerConfig.audioConfig,streamingData.adaptiveFormats,videoDetails.videoId")
    }.body<PlayerResponse>()

    println("PlayerService Innertube.player response $response")

    // Retry this process with API key
    if( response.playabilityStatus?.status == "LOGIN_REQUIRED" ) {
        println( "Innertube.player fetch failed! Resending with API key" )

        response = client.post( player ) {
            parameter( "key", AndroidMusic.API_KEY )
            setBody( body )
            mask( "playabilityStatus.status,playerConfig.audioConfig,streamingData.adaptiveFormats,videoDetails.videoId" )
        }.body<PlayerResponse>()
    }

    // If problem persists, switch platform
    if( response.playabilityStatus?.status == "LOGIN_REQUIRED" ) {
        println( "Innertube.player fetch failed #2! Switching to IOS" )

        response = client.post( player ) {
            parameter( "key", IOSMusic.API_KEY )
            setBody(
                body.copy(
                    context = Context.DEFAULT_IOS
                )
            )
            mask( "playabilityStatus.status,playerConfig.audioConfig,streamingData.adaptiveFormats,videoDetails.videoId" )
        }.body<PlayerResponse>()
    }

    response
}


package it.fast4x.rimusic.utils

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import it.fast4x.rimusic.R
import it.fast4x.rimusic.enums.MaxSongs
import it.fast4x.rimusic.service.modern.PlayerServiceModern
import it.fast4x.rimusic.ui.components.themed.SmartMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.knighthat.appContext

@UnstableApi
fun playShuffledSongs( mediaItems: List<MediaItem>, context: Context, binder: PlayerServiceModern.Binder? ) {

    if ( binder == null ) return

    // Send message saying that there's no song to play
    if( mediaItems.isEmpty() ) {
        SmartMessage(
            message = appContext().resources.getString( R.string.no_song_to_shuffle ),
            context = context
        )
        return
    }

    val maxSongsInQueue = context.preferences
                                 .getEnum( maxSongsInQueueKey, MaxSongs.`500` )
                                 .toInt()

    mediaItems.let { songs ->

        // Return whole list if its size is less than queue size
        val songsInQueue = songs.shuffled().take( maxSongsInQueue )
        CoroutineScope( Dispatchers.Main ).launch {
            binder.stopRadio()
            binder.player.forcePlayFromBeginning( songsInQueue )
        }
    }
}
package it.fast4x.rimusic.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.media3.common.util.UnstableApi
import it.fast4x.compose.persist.persist
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.models.bodies.BrowseBody
import it.fast4x.innertube.requests.albumPage
import it.fast4x.innertube.requests.artistPage
import it.fast4x.rimusic.Database
import it.fast4x.rimusic.models.Album
import it.fast4x.rimusic.models.Artist
import it.fast4x.rimusic.models.SongAlbumMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Composable
fun UpdateYoutubeArtist(browseId: String) {

    var artistPage by persist<Innertube.ArtistPage?>("artist/$browseId/artistPage")
    var artist by persist<Artist?>("artist/$browseId/artist")
    val tabIndex by rememberPreference(artistScreenTabIndexKey, defaultValue = 0)

    LaunchedEffect(Unit) {
        Database.artist
                .flowFindById( browseId )
                .combine(
                    snapshotFlow { tabIndex }.map { it != 4 }
                ) { artist, mustFetch -> artist to mustFetch }
                .distinctUntilChanged()
                // Collect on IO thread to keep it from interfering with UI thread
                .collect( CoroutineScope(Dispatchers.IO) ) { (currentArtist, mustFetch) ->
                    artist = currentArtist

                    if( artistPage != null || !(currentArtist?.timestamp == null || mustFetch) )
                        return@collect

                    Innertube.artistPage( BrowseBody(browseId = browseId) )
                        ?.onSuccess {
                            artistPage = it

                            Database.artist.safeUpsert(
                                Artist(
                                    browseId,
                                    it.name,
                                    it.thumbnail?.url,
                                    System.currentTimeMillis(),
                                    artist?.bookmarkedAt
                                )
                            )
                        }
                }
    }
}

@UnstableApi
@Composable
fun UpdateYoutubeAlbum (browseId: String) {
    var album by persist<Album?>("album/$browseId/album")
    var albumPage by persist<Innertube.PlaylistOrAlbumPage?>("album/$browseId/albumPage")
    val tabIndex by rememberSaveable {mutableStateOf(0)}
    LaunchedEffect(browseId) {
        Database.album
            .flowFindById( browseId )
            .combine(
                snapshotFlow { tabIndex }
            ) { album, tabIndex -> album to tabIndex }
            // Collect on IO thread to keep it from interfering with UI thread
            .collect( CoroutineScope(Dispatchers.IO) ) { (currentAlbum, _) ->
                album = currentAlbum

                if( albumPage != null || currentAlbum?.timestamp != null )
                    return@collect

                Innertube.albumPage( BrowseBody(browseId = browseId) )
                         ?.onSuccess { currentAlbumPage ->
                             albumPage = currentAlbumPage

                             currentAlbumPage.run {
                                 Database.album.safeUpsert(
                                     Album(
                                         browseId,
                                         title,
                                         thumbnail?.url,
                                         year,
                                         authors?.joinToString("") { it.name ?: "" },
                                         url,
                                         album?.bookmarkedAt
                                     )
                                 )
                                 songsPage?.items
                                          ?.map( Innertube.SongItem::asMediaItem )
                                          ?.onEach( Database::insert )
                                          ?.mapIndexed { position, mediaItem ->
                                              SongAlbumMap(
                                                  mediaItem.mediaId,
                                                  browseId,
                                                  position
                                              )
                                          }
                                          ?.also( Database.songAlbumMap::safeUpsert )
                             }

                             Database.album.deleteById( browseId )
                         }
            }
    }
}
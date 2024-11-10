package me.knighthat.database.table

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import it.fast4x.rimusic.enums.PlaylistSongSortBy
import it.fast4x.rimusic.enums.SortOrder
import it.fast4x.rimusic.models.PlaylistPreview
import it.fast4x.rimusic.models.Song
import it.fast4x.rimusic.models.SongEntity
import it.fast4x.rimusic.models.SongPlaylistMap
import it.fast4x.rimusic.utils.thumbnail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

@Dao
@RewriteQueriesToDropUnusedColumns
interface SongPlaylistMapTable: Table<SongPlaylistMap, Unit> {

    @Query("SELECT * FROM SongPlaylistMap WHERE songId = :id")
    fun findById( id: String ): SongPlaylistMap?

    fun isMapped( id: String ): Boolean = findById( id ) != null

    @Query("SELECT playlistId FROM SongPlaylistMap WHERE songId = :songId")
    fun flowMapsOf( songId: String ): Flow<List<Long>>

    @Query("""
        SELECT DISTINCT S.* 
        FROM Song S 
        INNER JOIN SongPlaylistMap SPM ON S.id = SPM.songId
        INNER JOIN Playlist P ON P.id = SPM.playlistId 
        WHERE P.name LIKE :prefix || '%'
    """)
    fun flowSongsOf( prefix: String ): Flow<List<Song>>

    @Query(
        """
        SELECT S.* 
        FROM Song S 
        INNER JOIN songplaylistmap SPM ON S.id=SPM.songId 
        WHERE SPM.playlistId = :id 
        ORDER BY SPM.position 
        LIMIT :limit
    """)
    fun flowFindSongsById( id: Long, limit: Long ): Flow<List<Song>>

    @Query("""
        SELECT id, name, browseId, (
            SELECT COUNT(*) 
            FROM SongPlaylistMap 
            WHERE playlistId = id
        ) as songCount 
        FROM Playlist 
        WHERE id = :id
    """)
    fun flowPreview( id: Long ): Flow<PlaylistPreview?>

    @Query("""
        UPDATE SongPlaylistMap 
        SET position = 
          CASE 
            WHEN position < :fromPosition THEN position + 1
            WHEN position > :fromPosition THEN position - 1
            ELSE :toPosition
          END 
        WHERE playlistId = :playlistId AND position 
        BETWEEN MIN(:fromPosition,:toPosition) and MAX(:fromPosition,:toPosition)
    """)
    fun move( playlistId: Long, fromPosition: Int, toPosition: Int )

    @Query("""
        UPDATE SongPlaylistMap 
        SET position = :toPosition 
        WHERE playlistId = :playlistId AND songId = :songId
    """)
    fun updatePosition(playlistId: Long, songId: String, toPosition: Int)

    @Query("DELETE FROM SongPlaylistMap WHERE playlistId = :id")
    fun deleteById( id: Long )

    @Query("DELETE FROM SongPlaylistMap WHERE songId = :songId")
    fun removeSong( songId: String )

    fun thumbnailsOf( id: Long, sizePx: Int ): Flow<List<String?>> =
        flowFindSongsById( id, 4L ).flowOn( Dispatchers.IO )
                                          .distinctUntilChanged()
                                          .map { list ->
                                              list.mapNotNull( Song::thumbnailUrl )
                                                  .map { url ->
                                                      url.thumbnail( sizePx / 2 )
                                                  }
                                          }

    /*
                START SONGS FROM PLAYLIST
     */

    @Query("""
        SELECT S.*, Album.title as albumTitle 
        FROM Song S 
        INNER JOIN songplaylistmap SP ON S.id=SP.songId 
        LEFT JOIN SongAlbumMap ON SongAlbumMap.songId = S.id 
        LEFT JOIN Album ON Album.id = SongAlbumMap.albumId 
        WHERE SP.playlistId=:id 
        ORDER BY S.artistsText COLLATE NOCASE 
    """)
    fun sortByArtistFrom( id: Long ): Flow<List<SongEntity>>

    @Query("""
        SELECT S.*, Album.title as albumTitle 
        FROM Song S 
        INNER JOIN songplaylistmap SP ON S.id=SP.songId 
        LEFT JOIN SongAlbumMap ON SongAlbumMap.songId = S.id 
        LEFT JOIN Album ON Album.id = SongAlbumMap.albumId 
        WHERE SP.playlistId=:id 
        ORDER BY S.title COLLATE NOCASE 
    """)
    fun sortByTitleFrom( id: Long ): Flow<List<SongEntity>>

    @Query("""
        SELECT S.*, Album.title as albumTitle 
        FROM Song S 
        INNER JOIN songplaylistmap SP ON S.id=SP.songId 
        LEFT JOIN SongAlbumMap ON SongAlbumMap.songId = S.id 
        LEFT JOIN Album ON Album.id = SongAlbumMap.albumId 
        WHERE SP.playlistId=:id 
        ORDER BY SP.position
    """)
    fun sortByPositionFrom( id: Long ): Flow<List<SongEntity>>

    @Query("""
        SELECT S.*, Album.title as albumTitle 
        FROM Song S 
        INNER JOIN songplaylistmap SP ON S.id=SP.songId 
        LEFT JOIN SongAlbumMap ON SongAlbumMap.songId = S.id 
        LEFT JOIN Album ON Album.id = SongAlbumMap.albumId 
        WHERE SP.playlistId=:id 
        ORDER BY S.totalPlayTimeMs
    """)
    fun sortByPlayTimeFrom( id: Long ): Flow<List<SongEntity>>

    @Query("""
        SELECT DISTINCT S.*, Album.title as albumTitle 
        FROM Song S 
        INNER JOIN songplaylistmap SP ON S.id=SP.songId 
        LEFT JOIN Event E ON E.songId=S.id 
        LEFT JOIN SongAlbumMap ON SongAlbumMap.songId = S.id 
        LEFT JOIN Album ON Album.id = SongAlbumMap.albumId 
        WHERE SP.playlistId=:id 
        ORDER BY E.timestamp
    """)
    fun sortByDatePlayedFrom( id: Long ): Flow<List<SongEntity>>

    @Query("""
        SELECT DISTINCT S.*, Album.title as albumTitle 
        FROM Song S 
        INNER JOIN songplaylistmap SP ON S.id=SP.songId 
        LEFT JOIN Event E ON E.songId=S.id 
        LEFT JOIN SongAlbumMap ON SongAlbumMap.songId = S.id 
        LEFT JOIN Album ON Album.id = SongAlbumMap.albumId 
        WHERE SP.playlistId=:id 
        ORDER BY E.timestamp 
    """)
    fun sortByAlbumYearFrom( id: Long ): Flow<List<SongEntity>>

    @Query("""
        SELECT S.*, Album.title as albumTitle 
        FROM Song S 
        INNER JOIN songplaylistmap SP ON S.id=SP.songId 
        LEFT JOIN SongAlbumMap ON SongAlbumMap.songId = S.id 
        LEFT JOIN Album ON Album.id = SongAlbumMap.albumId 
        WHERE SP.playlistId=:id 
        ORDER BY S.durationText
    """)
    fun sortByDurationFrom( id: Long ): Flow<List<SongEntity>>

    @Query("""
        SELECT DISTINCT S.*, A.title as albumTitle 
        FROM Song S 
        INNER JOIN songplaylistmap SP ON S.id=SP.songId 
        LEFT JOIN songalbummap SA ON SA.songId=SP.songId 
        LEFT JOIN Album A ON A.Id=SA.albumId 
        WHERE SP.playlistId=:id 
        ORDER BY S.artistsText COLLATE NOCASE , A.title COLLATE NOCASE 
    """)
    fun sortByArtistAndAlbumFrom( id: Long ): Flow<List<SongEntity>>

    @Query("""
        SELECT DISTINCT S.*, A.title as albumTitle 
        FROM Song S 
        INNER JOIN songplaylistmap SP ON S.id=SP.songId 
        LEFT JOIN songalbummap SA ON SA.songId=SP.songId 
        LEFT JOIN Album A ON A.Id=SA.albumId 
        WHERE SP.playlistId=:id ORDER BY A.title COLLATE NOCASE 
    """)
    fun sortByAlbumFrom( id: Long ): Flow<List<SongEntity>>

    @Query("""
        SELECT S.*, Album.title as albumTitle 
        FROM Song S 
        INNER JOIN songplaylistmap SP ON S.id=SP.songId 
        LEFT JOIN SongAlbumMap ON SongAlbumMap.songId = S.id 
        LEFT JOIN Album ON Album.id = SongAlbumMap.albumId 
        WHERE SP.playlistId=:id 
        ORDER BY S.ROWID
    """)
    fun sortByRowIdFrom( id: Long ): Flow<List<SongEntity>>

    @Query("""
        SELECT S.*, Album.title as albumTitle 
        FROM Song S 
        INNER JOIN songplaylistmap SP ON S.id=SP.songId 
        LEFT JOIN SongAlbumMap ON SongAlbumMap.songId = S.id 
        LEFT JOIN Album ON Album.id = SongAlbumMap.albumId 
        WHERE SP.playlistId=:id 
        ORDER BY S.LikedAt COLLATE NOCASE 
    """)
    fun sortByLikedAtFrom( id: Long ): Flow<List<SongEntity>>

    fun songsFrom(
        id: Long,
        sortBy: PlaylistSongSortBy = PlaylistSongSortBy.Title,
        sortOrder: SortOrder = SortOrder.Ascending
    ): Flow<List<SongEntity>> = when( sortBy ) {
        PlaylistSongSortBy.Album -> sortByAlbumFrom( id )
        PlaylistSongSortBy.AlbumYear -> sortByAlbumYearFrom( id )
        PlaylistSongSortBy.Artist -> sortByArtistFrom( id )
        PlaylistSongSortBy.ArtistAndAlbum -> sortByArtistAndAlbumFrom( id )
        PlaylistSongSortBy.DatePlayed -> sortByDatePlayedFrom( id )
        PlaylistSongSortBy.PlayTime -> sortByPlayTimeFrom( id )
        PlaylistSongSortBy.Position -> sortByPositionFrom( id )
        PlaylistSongSortBy.Title -> sortByTitleFrom( id )
        PlaylistSongSortBy.Duration -> sortByDurationFrom( id )
        PlaylistSongSortBy.DateLiked -> sortByLikedAtFrom( id )
        PlaylistSongSortBy.DateAdded -> sortByRowIdFrom( id )
    }.map {
        if( sortOrder == SortOrder.Descending )
            it.reversed()
        else
            it
    }

    /*
                END SONGS FROM PLAYLIST
     */
}
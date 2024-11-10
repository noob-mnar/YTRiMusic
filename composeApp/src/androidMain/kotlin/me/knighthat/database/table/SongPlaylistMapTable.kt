package me.knighthat.database.table

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import it.fast4x.rimusic.models.PlaylistPreview
import it.fast4x.rimusic.models.Song
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
    fun flowFindSongsById( id: Long, limit: ULong ): Flow<List<Song>>

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
        flowFindSongsById( id, 4UL ).flowOn( Dispatchers.IO )
                                          .distinctUntilChanged()
                                          .map { list ->
                                              list.mapNotNull( Song::thumbnailUrl )
                                                  .map { url ->
                                                      url.thumbnail( sizePx / 2 )
                                                  }
                                          }
}
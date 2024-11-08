package me.knighthat.database.table

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import it.fast4x.rimusic.MONTHLY_PREFIX
import it.fast4x.rimusic.models.Playlist
import it.fast4x.rimusic.models.PlaylistPreview
import it.fast4x.rimusic.models.PlaylistWithSongs
import kotlinx.coroutines.flow.Flow

@Dao
@RewriteQueriesToDropUnusedColumns
interface PlaylistTable: Table<Playlist, Long> {

    @Query("SELECT * FROM Playlist WHERE id = :id")
    fun findById( id: Long ): Playlist?

    @Query("SELECT * FROM Playlist WHERE name = :name")
    fun findByName( name: String ): Playlist?

    @Query("""
        SELECT Playlist.*, (
            SELECT COUNT(*) 
            FROM SongPlaylistMap 
            WHERE playlistId = Playlist.id
        ) as songCount 
        FROM Song 
        JOIN SongPlaylistMap ON Song.id = SongPlaylistMap.songId 
        JOIN Event ON Song.id = Event.songId 
        JOIN Playlist ON Playlist.id = SongPlaylistMap.playlistId 
        WHERE (:to - Event.timestamp) <= :from 
        GROUP BY Playlist.id 
        ORDER BY SUM(Event.playTime) DESC 
        LIMIT :limit
    """)
    fun flowMostPlayedBetween( from: Long, to: Long, limit: Long ): Flow<List<PlaylistPreview>>

    @Query("""
        UPDATE Playlist 
        SET name = :name
        WHERE id = :id
    """)
    fun updateName( id: Long, name: String )

    @Query("SELECT * FROM Playlist WHERE trim(name) COLLATE NOCASE = trim(:name) COLLATE NOCASE")
    fun flowPlaylistWithSongsByName(name: String ): Flow<PlaylistWithSongs?>

    @Query("SELECT * FROM Playlist WHERE id = :id")
    fun playlistWithSongsById( id: Long ): PlaylistWithSongs?

    @Query("""
        SELECT id, name, browseId, (
            SELECT COUNT(*) 
            FROM SongPlaylistMap 
            WHERE playlistId = id
        ) as songCount 
        FROM Playlist 
        WHERE name LIKE '$MONTHLY_PREFIX' || '%'
    """)
    fun flowMonthlyPreview(): Flow<List<PlaylistPreview>>
}
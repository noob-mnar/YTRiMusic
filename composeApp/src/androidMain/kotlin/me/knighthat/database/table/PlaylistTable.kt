package me.knighthat.database.table

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import it.fast4x.rimusic.MONTHLY_PREFIX
import it.fast4x.rimusic.enums.PlaylistSortBy
import it.fast4x.rimusic.enums.SortOrder
import it.fast4x.rimusic.models.Playlist
import it.fast4x.rimusic.models.PlaylistPreview
import it.fast4x.rimusic.models.PlaylistWithSongs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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
        ORDER BY SUM(Event.playTime) 
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
        ) AS songCount 
        FROM Playlist 
        WHERE name LIKE '$MONTHLY_PREFIX' || '%'
    """)
    fun flowMonthlyPreview(): Flow<List<PlaylistPreview>>

    @Query("""
        SELECT id, name, browseId, (
            SELECT COUNT(*) 
            FROM SongPlaylistMap 
            WHERE playlistId = id
        ) AS songCount 
        FROM Playlist 
        ORDER BY name COLLATE NOCASE
    """)
    fun sortByName(): Flow<List<PlaylistPreview>>

    @Query("""
        SELECT id, name, browseId, (
            SELECT COUNT(*) 
            FROM SongPlaylistMap 
            WHERE playlistId = id
        ) AS songCount 
        FROM Playlist 
        ORDER BY ROWID
    """)
    fun sortByRowId(): Flow<List<PlaylistPreview>>

    @Query("""
        SELECT id, name, browseId, (
            SELECT COUNT(*) 
            FROM SongPlaylistMap 
            WHERE playlistId = id
        ) AS songCount 
        FROM Playlist 
        ORDER BY songCount
    """)
    fun sortBySongsCount(): Flow<List<PlaylistPreview>>

    @Query("""
        SELECT id, name, browseId, (
            SELECT COUNT(*) FROM SongPlaylistMap WHERE playlistId = id
        ) as songCount, (
            SELECT SUM(Song.totalPlayTimeMs) 
            FROM Song 
            JOIN SongPlaylistMap ON Song.id = SongPlaylistMap.songId 
            WHERE SongPlaylistMap.playlistId = Playlist.id
        ) as TotPlayTime 
        FROM Playlist
        ORDER BY 4
    """)
    fun sortMostPlayed(): Flow<List<PlaylistPreview>>

    fun flowAllPreviews(
        sortBy: PlaylistSortBy = PlaylistSortBy.Name,
        sortOrder: SortOrder = SortOrder.Ascending
    ): Flow<List<PlaylistPreview>> = when( sortBy ) {
        PlaylistSortBy.MostPlayed -> sortMostPlayed()
        PlaylistSortBy.Name -> sortByName()
        PlaylistSortBy.DateAdded -> sortByRowId()
        PlaylistSortBy.SongCount -> sortBySongsCount()
    }.map {
        if( sortOrder == SortOrder.Descending )
            it.reversed()
        else
            it
    }
}
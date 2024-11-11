package me.knighthat.database.table

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import it.fast4x.rimusic.enums.AlbumSortBy
import it.fast4x.rimusic.enums.SortOrder
import it.fast4x.rimusic.models.Album
import it.fast4x.rimusic.models.Info
import it.fast4x.rimusic.models.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Dao
@RewriteQueriesToDropUnusedColumns
interface AlbumTable: Table<Album, Long> {

    @Query("SELECT * FROM Album WHERE id = :id")
    fun flowFindById( id: String ): Flow<Album?>

    @Query("UPDATE Album SET thumbnailUrl = :url WHERE id = :id")
    fun updateCover(id: String, url: String ): Int

    @Query("UPDATE Album SET authorsText = :artist WHERE id = :id")
    fun updateAuthors( id: String, artist: String ): Int

    @Query("UPDATE Album SET title = :title WHERE id = :id")
    fun updateTitle( id: String, title: String ): Int

    @Query("UPDATE Album SET bookmarkedAt = :timestamp WHERE id = :id")
    fun bookmark(
        id: String,
        timestamp: Long? = System.currentTimeMillis()
    ): Int

    @Query("SELECT bookmarkedAt FROM Album WHERE id = :id")
    fun bookmarkedAt( id: String ): Long?

    @Query("""
        SELECT DISTINCT S.* 
        FROM Song S 
        INNER JOIN SongAlbumMap SAM ON S.id = SAM.songId 
        INNER JOIN Album A ON A.id = SAM.albumId 
        WHERE A.bookmarkedAt IS NOT NULL
    """)
    fun flowSongsOfBookmarked(): Flow<List<Song>>

    @Query("""
        SELECT * 
        FROM Song 
        JOIN SongAlbumMap SAM ON Song.id = SAM.songId 
        WHERE SAM.albumId = :id 
        ORDER BY position
    """)
    fun findSongsOf( id: String ): List<Song>

    @Query("""
        SELECT * 
        FROM Song 
        JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId 
        WHERE SongAlbumMap.albumId = :id 
        AND position IS NOT NULL 
        ORDER BY position
    """)
    fun flowSongsOf( id: String ): Flow<List<Song>>

    @Query("DELETE FROM SongAlbumMap WHERE albumId = :id")
    fun deleteById( id: String )

    @Query("""
        SELECT albumId AS id, NULL AS name, 0 AS size 
        FROM SongAlbumMap 
        WHERE songId = :songId
    """)
    fun info( songId: String ): Info?

    @Query("""
        SELECT Album.* 
        FROM Song 
        JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId 
        JOIN Event ON Song.id = Event.songId 
        JOIN Album ON Album.id = SongAlbumMap.albumId 
        WHERE (:to - Event.timestamp) <= :from 
        GROUP BY Album.id 
        ORDER BY SUM(Event.playTime) 
        LIMIT :limit
    """)
    fun flowMostPlayedBetween( from: Long, to: Long, limit: Long ): Flow<List<Album>>

    @Query("""
        SELECT *, (
            SELECT SUM(CAST(REPLACE(durationText, ':', '') AS INTEGER)) 
            FROM Song 
            JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId 
            WHERE SongAlbumMap.albumId = Album.id 
            AND position IS NOT NULL
        ) as totalDuration 
        FROM Album 
        WHERE bookmarkedAt IS NOT NULL
        ORDER BY totalDuration
    """)
    fun sortByDuration(): Flow<List<Album>>

    @Query("""
        SELECT *, (
            SELECT COUNT(*) 
            FROM SongAlbumMap 
            WHERE albumId = Album.id
        ) as songCount 
        FROM Album 
        WHERE bookmarkedAt IS NOT NULL 
        ORDER BY songCount
    """)
    fun sortBySongsCount(): Flow<List<Album>>

    @Query("""
        SELECT * 
        FROM Album 
        WHERE bookmarkedAt IS NOT NULL 
        ORDER BY authorsText COLLATE NOCASE
    """)
    fun sortByArtist(): Flow<List<Album>>

    @Query("""
        SELECT * 
        FROM Album 
        WHERE bookmarkedAt IS NOT NULL 
        ORDER BY title COLLATE NOCASE
    """)
    fun sortByTitle(): Flow<List<Album>>

    @Query("""
        SELECT * 
        FROM Album 
        WHERE bookmarkedAt IS NOT NULL 
        ORDER BY bookmarkedAt
    """)
    fun sortByRowId(): Flow<List<Album>>

    @Query("""
        SELECT * 
        FROM Album 
        WHERE bookmarkedAt IS NOT NULL 
        ORDER BY year
    """)
    fun sortByYear(): Flow<List<Album>>

    fun flowAll(
        sortBy: AlbumSortBy = AlbumSortBy.Title,
        sortOrder: SortOrder = SortOrder.Ascending
    ): Flow<List<Album>> = when( sortBy ) {
        AlbumSortBy.Title -> sortByTitle()
        AlbumSortBy.Year -> sortByYear()
        AlbumSortBy.DateAdded -> sortByRowId()
        AlbumSortBy.Artist -> sortByArtist()
        AlbumSortBy.Songs -> sortBySongsCount()
        AlbumSortBy.Duration -> sortByDuration()
    }.map {
        if( sortOrder == SortOrder.Descending )
            it.reversed()
        else
            it
    }
}
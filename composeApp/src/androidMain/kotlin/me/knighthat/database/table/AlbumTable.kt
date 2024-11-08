package me.knighthat.database.table

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import it.fast4x.rimusic.models.Album
import it.fast4x.rimusic.models.Song
import kotlinx.coroutines.flow.Flow

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

}
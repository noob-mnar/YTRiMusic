package me.knighthat.database.table

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Transaction
import it.fast4x.rimusic.Database
import it.fast4x.rimusic.models.Song
import it.fast4x.rimusic.models.SongEntity
import it.fast4x.rimusic.service.LOCAL_KEY_PREFIX
import kotlinx.coroutines.flow.Flow

@Dao
@RewriteQueriesToDropUnusedColumns
interface SongTable: Table<Song, Long> {

    @Query("SELECT * FROM Song WHERE id = :id")
    fun findById( id: String ): Song?

    @Query("SELECT * FROM Song")
    fun all(): List<Song>

    @Query("SELECT * FROM Song")
    fun flowAll(): Flow<List<Song>>

    @Query("SELECT * FROM Song")
    fun flowAllAsSongEntity(): Flow<List<SongEntity>>

    @Query("""
        SELECT * 
        FROM Song 
        WHERE id 
        LIKE '$LOCAL_KEY_PREFIX%'
    """)
    fun onDevice(): List<Song>

    @Query("""
        SELECT * 
        FROM Song 
        WHERE likedAt IS NOT NULL 
        ORDER BY likedAt
    """)
    fun favorites(): List<Song>

    @Query("""
        UPDATE Song 
        SET totalPlayTimeMs = totalPlayTimeMs + :duration 
        WHERE id = :id
    """)
    fun addTotalPlayTime( id: String, duration: Long )

    @Query("UPDATE Song SET totalPlayTimeMs = 0 WHERE id = :id")
    fun resetTotalPlayTime( id: String )

    @Query("UPDATE Song SET title = :title WHERE id = :id")
    fun updateTitle( id: String, title: String )

    @Query("UPDATE Song SET artistsText = :artists WHERE id = :id")
    fun updateArtist( id: String, artists: String )

    fun contains( id: String ): Boolean = findById(id) != null

    @Query("UPDATE Song SET likedAt = :at WHERE id = :id")
    fun like(
        id: String,
        at: Long? = System.currentTimeMillis()
    ): Int

    @Query("SELECT likedAt FROM Song WHERE id = :id")
    fun likedAt( id: String ): Long?

    @Query("SELECT likedAt FROM Song WHERE id = :id")
    fun flowLikedAt( id: String ): Flow<Long?>

    @Transaction
    fun toggleLike( id: String ) = Database.transaction {
        if( likedAt( id ) == null )
            like( id )
        else
            like( id, null )
    }

    fun isLiked( id: String ): Boolean = likedAt( id ) != null
}
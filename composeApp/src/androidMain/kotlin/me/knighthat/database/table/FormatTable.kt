package me.knighthat.database.table

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Transaction
import it.fast4x.rimusic.Database
import it.fast4x.rimusic.models.Format
import it.fast4x.rimusic.models.SongWithContentLength
import kotlinx.coroutines.flow.Flow

@Dao
@RewriteQueriesToDropUnusedColumns
interface FormatTable: Table<Format, Unit> {

    @Query("SELECT * FROM Format WHERE songId = :id")
    fun flowFindById( id: String ): Flow<Format?>

    @Query("UPDATE Format SET contentLength = 0 WHERE songId = :songId")
    fun resetFormatContentLength(songId: String)

    @Transaction
    fun safeResetContentLength( id: String ) = Database.transaction {
        try {
            resetFormatContentLength( id )
        } catch ( _: Exception ) {}
    }

    @Query("SELECT contentLength FROM Format WHERE songId = :id")
    fun contentLength( id: String ): Long?

    @Query("DELETE FROM Format WHERE songId = :id")
    fun deleteById( id: String )

    @Query("SELECT loudnessDb FROM Format WHERE songId = :id")
    fun flowLoudness( id: String ): Flow<Float?>

    @Query("""
        SELECT Song.*, contentLength 
        FROM Song 
        JOIN Format ON id = songId 
        WHERE contentLength IS NOT NULL 
        AND totalPlayTimeMs > 0 
        ORDER BY Song.ROWID DESC
    """)
    fun songsWithContentLength(): List<SongWithContentLength>
}
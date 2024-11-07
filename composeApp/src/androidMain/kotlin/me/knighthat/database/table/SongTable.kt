package me.knighthat.database.table

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import it.fast4x.rimusic.models.Song
import it.fast4x.rimusic.service.LOCAL_KEY_PREFIX

@Dao
interface SongTable: Table<Song, Long> {

    @Query("SELECT * FROM Song WHERE id = :id")
    fun findById( id: String ): Song?

    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM Song")
    fun all(): List<Song>

    @RewriteQueriesToDropUnusedColumns
    @Query("""
        SELECT * 
        FROM Song 
        WHERE id 
        LIKE '$LOCAL_KEY_PREFIX%'
    """)
    fun onDevice(): List<Song>

    @RewriteQueriesToDropUnusedColumns
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
}
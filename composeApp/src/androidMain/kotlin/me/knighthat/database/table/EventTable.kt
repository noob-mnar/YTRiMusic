package me.knighthat.database.table

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import it.fast4x.rimusic.models.Event
import it.fast4x.rimusic.models.EventWithSong
import it.fast4x.rimusic.models.Song
import it.fast4x.rimusic.models.SongEntity
import it.fast4x.rimusic.service.LOCAL_KEY_PREFIX
import kotlinx.coroutines.flow.Flow
import me.knighthat.database.DatabaseTable

@Dao
@RewriteQueriesToDropUnusedColumns
@DatabaseTable("Event")
interface EventTable: Table<Event, Unit> {

    @Query("""
        SELECT DISTINCT (timestamp / 86400000) AS timestampDay, event.* 
        FROM event 
        ORDER BY rowId DESC
    """)
    fun flowEventsWithSongs(): Flow<List<EventWithSong>>

    @Query("SELECT COUNT (*) FROM Event")
    fun flowCountAll(): Flow<Long>

    @Query("DELETE FROM Event WHERE songId = :id")
    fun deleteBySongId( id: String )

    @Query("""
        SELECT Song.* 
        FROM Event 
        JOIN Song ON Song.id = songId 
        WHERE Song.id NOT LIKE '$LOCAL_KEY_PREFIX%' 
        GROUP BY songId 
        ORDER BY SUM(playTime) DESC 
        LIMIT :limit
    """)
    fun trending(limit: Int ): List<Song>

    @Query("""
        SELECT Song.* 
        FROM Event 
        JOIN Song ON Song.id = songId 
        WHERE (:now - Event.timestamp) <= :period 
        AND Song.id NOT LIKE '$LOCAL_KEY_PREFIX%' 
        GROUP BY songId 
        ORDER BY SUM(playTime) DESC 
        LIMIT :limit
    """)
    fun flowTrendingAsSongEntity(
        limit: Int,
        period: Long,
        now: Long = System.currentTimeMillis()
    ): Flow<List<SongEntity>>

    @Query("""
        SELECT Song.* 
        FROM Event 
        JOIN Song ON Song.id = songId 
        WHERE playTime > 0 
        AND Song.id NOT LIKE '$LOCAL_KEY_PREFIX%' 
        GROUP BY songId 
        ORDER BY timestamp DESC 
        LIMIT :limit
    """)
    fun lastPlayed( limit: Int ): Flow<List<Song>>
}
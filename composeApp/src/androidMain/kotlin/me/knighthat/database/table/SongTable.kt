package me.knighthat.database.table

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Transaction
import it.fast4x.rimusic.Database
import it.fast4x.rimusic.enums.SongSortBy
import it.fast4x.rimusic.enums.SortOrder
import it.fast4x.rimusic.models.Song
import it.fast4x.rimusic.models.SongEntity
import it.fast4x.rimusic.service.LOCAL_KEY_PREFIX
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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

    @Query("""
        SELECT Song.* 
        FROM Event 
        JOIN Song ON Song.id = songId 
        WHERE (:to - Event.timestamp) <= :from 
        GROUP BY songId 
        ORDER BY SUM(playTime) DESC 
        LIMIT :limit
    """)
    fun flowMostPlayedBetween( from: Long, to: Long, limit: Long ): Flow<List<Song>>

    @Query("""
        SELECT Song.* 
        FROM Event 
        JOIN Song ON Song.id = songId 
        WHERE 
            CAST(strftime('%m',timestamp / 1000,'unixepoch') AS INTEGER) = :month 
            AND 
            CAST(strftime('%Y',timestamp / 1000,'unixepoch') as INTEGER) = :year
        GROUP BY songId 
        ORDER BY timestamp DESC 
        LIMIT :limit
    """)
    fun mostPlayedByYearMonth( year: Long, month: Long, limit: Long ): List<Song>

    /*
            START FAVORITES
     */

    @Query("""
        SELECT * 
        FROM Song 
        WHERE likedAt IS NOT NULL 
        ORDER BY artistsText
    """)
    fun sortFavoritesByArtist(): Flow<List<SongEntity>>

    @Query("""
        SELECT * 
        FROM Song 
        WHERE likedAt IS NOT NULL 
        ORDER BY totalPlayTimeMs
    """)
    fun sortFavoritesByPlayTime(): Flow<List<SongEntity>>

    @Query("""
        SELECT * 
        FROM Song 
        WHERE likedAt IS NOT NULL 
        ORDER BY title COLLATE NOCASE
    """)
    fun sortFavoritesByTitle(): Flow<List<SongEntity>>

    @Query("""
        SELECT * 
        FROM Song 
        WHERE likedAt IS NOT NULL 
        ORDER BY ROWID
    """)
    fun sortFavoritesByRowId(): Flow<List<SongEntity>>

    @Query("""
        SELECT * 
        FROM Song 
        WHERE likedAt IS NOT NULL 
        ORDER BY likedAt
    """)
    fun sortFavoritesByLikedAt(): Flow<List<SongEntity>>

    @Query("""
        SELECT DISTINCT S.* 
        FROM Song S 
        LEFT JOIN Event E ON E.songId = S.id 
        WHERE likedAt IS NOT NULL
        ORDER BY E.timestamp
    """)
    fun sortFavoritesByDatePlayed(): Flow<List<SongEntity>>

    @Query("""
        SELECT * 
        FROM Song 
        WHERE likedAt IS NOT NULL 
        ORDER BY durationText
    """)
    fun sortFavoritesByDuration(): Flow<List<SongEntity>>

    fun flowAllFavorites(
        sortBy: SongSortBy = SongSortBy.DateLiked,
        sortOrder: SortOrder = SortOrder.Ascending
    ): Flow<List<SongEntity>> = when( sortBy ) {
        SongSortBy.PlayTime -> sortFavoritesByPlayTime()
        SongSortBy.Title,
        SongSortBy.AlbumName -> sortFavoritesByTitle()
        SongSortBy.DateAdded -> sortFavoritesByRowId()
        SongSortBy.DatePlayed -> sortFavoritesByDatePlayed()
        SongSortBy.DateLiked -> sortFavoritesByLikedAt()
        SongSortBy.Artist -> sortFavoritesByArtist()
        SongSortBy.Duration -> sortFavoritesByDuration()
    }.map {
        if( sortOrder == SortOrder.Descending )
            it.reversed()
        else
            it
    }

    /*
            END FAVORITES
    */


    /*
            START OFFLINE
    */

    @Query("""
        SELECT Song.*, contentLength 
        FROM Song 
        INNER JOIN Format ON id = songId 
        WHERE contentLength IS NOT NULL 
        AND totalPlayTimeMs > 0 
        ORDER BY totalPlayTimeMs
    """)
    fun sortOfflineByPlayTime(): Flow<List<SongEntity>>

    @Query("""
        SELECT Song.*, contentLength 
        FROM Song 
        INNER JOIN Format ON id = songId 
        WHERE contentLength IS NOT NULL 
        AND totalPlayTimeMs > 0 
        ORDER BY Song.title
    """)
    fun sortOfflineByTitle(): Flow<List<SongEntity>>

    @Query("""
        SELECT Song.*, contentLength 
        FROM Song 
        INNER JOIN Format ON id = songId 
        WHERE contentLength IS NOT NULL 
        AND totalPlayTimeMs > 0 
        ORDER BY Song.ROWID
    """)
    fun sortOfflineByRowId(): Flow<List<SongEntity>>

    @Query("""
        SELECT Song.*, contentLength 
        FROM Song 
        INNER JOIN Format ON id = songId 
        WHERE contentLength IS NOT NULL 
        AND totalPlayTimeMs > 0 
        ORDER BY Song.likedAt
    """)
    fun sortOfflineByLikedAt(): Flow<List<SongEntity>>

    @Query("""
        SELECT Song.*, contentLength 
        FROM Song 
        INNER JOIN Format ON id = songId 
        WHERE contentLength IS NOT NULL 
        AND totalPlayTimeMs > 0 
        ORDER BY Song.artistsText
    """)
    fun sortOfflineByArtist(): Flow<List<SongEntity>>

    @Query("""
        SELECT Song.*, contentLength 
        FROM Song 
        INNER JOIN Format ON id = songId 
        WHERE contentLength IS NOT NULL 
        AND totalPlayTimeMs > 0 
        ORDER BY Song.durationText
    """)
    fun sortOfflineByDuration(): Flow<List<SongEntity>>

    fun flowAllOffline(
        sortBy: SongSortBy = SongSortBy.Title,
        sortOrder: SortOrder = SortOrder.Ascending
    ): Flow<List<SongEntity>> = when( sortBy ) {
        SongSortBy.PlayTime,
        SongSortBy.DatePlayed -> sortOfflineByPlayTime()
        SongSortBy.Title,
        SongSortBy.AlbumName -> sortOfflineByTitle()
        SongSortBy.DateAdded -> sortOfflineByRowId()
        SongSortBy.DateLiked -> sortOfflineByLikedAt()
        SongSortBy.Artist -> sortOfflineByArtist()
        SongSortBy.Duration -> sortOfflineByDuration()
    }

    /*
            END OFFLINE
    */


    /*
            START ALL ONLINE SONGS
    */

    @Query("""
        SELECT Song.*, Album.title as albumTitle 
        FROM Song 
        LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId  
        LEFT JOIN Album ON Album.id = SongAlbumMap.albumId 
        WHERE (
            Song.totalPlayTimeMs > :showHiddenSongs 
            OR Song.likedAt NOT NULL
        ) 
        AND Song.id NOT LIKE '$LOCAL_KEY_PREFIX%' 
        ORDER BY Song.ROWID 
        LIMIT :limit
    """
    )
    fun sortByRowId( showHiddenSongs: Int, limit: Long ): Flow<List<SongEntity>>

    @Query("""
        SELECT Song.*, Album.title as albumTitle 
        FROM Song 
        LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId  
        LEFT JOIN Album ON Album.id = SongAlbumMap.albumId 
        WHERE (
            Song.totalPlayTimeMs > :showHiddenSongs 
            OR Song.likedAt NOT NULL
        ) 
        AND Song.id NOT LIKE '$LOCAL_KEY_PREFIX%' 
        ORDER BY Song.title COLLATE NOCASE 
        LIMIT :limit
    """
    )
    fun sortByTitle( showHiddenSongs: Int, limit: Long ): Flow<List<SongEntity>>

    @Query("""
        SELECT Song.*, Album.title as albumTitle 
        FROM Song 
        LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId  
        LEFT JOIN Album ON Album.id = SongAlbumMap.albumId 
        WHERE (
            Song.totalPlayTimeMs > :showHiddenSongs 
            OR Song.likedAt NOT NULL
        ) 
        AND Song.id NOT LIKE '$LOCAL_KEY_PREFIX%' 
        ORDER BY Song.totalPlayTimeMs 
        LIMIT :limit
    """
    )
    fun sortByPlayTime( showHiddenSongs: Int, limit: Long ): Flow<List<SongEntity>>

    @Query("""
        SELECT DISTINCT Song.*, Album.title as albumTitle 
        FROM Song 
        LEFT JOIN Event E ON E.songId = Song.id 
        LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId 
        LEFT JOIN Album ON Album.id = SongAlbumMap.albumId 
        WHERE (
            Song.totalPlayTimeMs > :showHiddenSongs 
            OR Song.likedAt NOT NULL
        ) 
        AND Song.id NOT LIKE '$LOCAL_KEY_PREFIX%' 
        ORDER BY E.timestamp 
        LIMIT :limit
    """
    )
    fun sortByPlayedDate( showHiddenSongs: Int, limit: Long ): Flow<List<SongEntity>>

    @Query("""
        SELECT Song.*, Album.title as albumTitle 
        FROM Song 
        LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId  
        LEFT JOIN Album ON Album.id = SongAlbumMap.albumId 
        WHERE (
            Song.totalPlayTimeMs > :showHiddenSongs 
            OR Song.likedAt NOT NULL
        ) 
        AND Song.id NOT LIKE '$LOCAL_KEY_PREFIX%' 
        ORDER BY Song.likedAt 
        LIMIT :limit
    """
    )
    fun sortByLikedAt( showHiddenSongs: Int, limit: Long ): Flow<List<SongEntity>>

    @Query("""
        SELECT Song.*, Album.title as albumTitle 
        FROM Song 
        LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId  
        LEFT JOIN Album ON Album.id = SongAlbumMap.albumId 
        WHERE (
            Song.totalPlayTimeMs > :showHiddenSongs 
            OR Song.likedAt NOT NULL
        ) 
        AND Song.id NOT LIKE '$LOCAL_KEY_PREFIX%' 
        ORDER BY Song.artistsText 
        LIMIT :limit
    """
    )
    fun sortByArtist( showHiddenSongs: Int, limit: Long ): Flow<List<SongEntity>>

    @Query("""
        SELECT Song.*, Album.title as albumTitle 
        FROM Song 
        LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId  
        LEFT JOIN Album ON Album.id = SongAlbumMap.albumId 
        WHERE (
            Song.totalPlayTimeMs > :showHiddenSongs 
            OR Song.likedAt NOT NULL
        ) 
        AND Song.id NOT LIKE '$LOCAL_KEY_PREFIX%' 
        ORDER BY Song.durationText 
        LIMIT :limit
    """
    )
    fun sortByDuration( showHiddenSongs: Int, limit: Long ): Flow<List<SongEntity>>

    @Query("""
        SELECT Song.*, Album.title as albumTitle 
        FROM Song 
        LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId  
        LEFT JOIN Album ON Album.id = SongAlbumMap.albumId 
        WHERE (
            Song.totalPlayTimeMs > :showHiddenSongs 
            OR Song.likedAt NOT NULL
        ) 
        AND Song.id NOT LIKE '$LOCAL_KEY_PREFIX%' 
        ORDER BY Album.title COLLATE NOCASE 
        LIMIT :limit
    """
    )
    fun sortByAlbum( showHiddenSongs: Int, limit: Long ): Flow<List<SongEntity>>

    fun flowAll(
        sortBy: SongSortBy = SongSortBy.Title,
        sortOrder: SortOrder = SortOrder.Ascending,
        showHiddenSongs: Int = 0,
        limit: Long = Long.MAX_VALUE
    ): Flow<List<SongEntity>> = when( sortBy ) {
        SongSortBy.PlayTime -> sortByPlayTime( showHiddenSongs, limit )
        SongSortBy.Title -> sortByTitle( showHiddenSongs, limit )
        SongSortBy.DateAdded -> sortByRowId( showHiddenSongs, limit )
        SongSortBy.DatePlayed -> sortByPlayedDate( showHiddenSongs, limit )
        SongSortBy.DateLiked -> sortByLikedAt( showHiddenSongs, limit )
        SongSortBy.Artist -> sortByArtist( showHiddenSongs, limit )
        SongSortBy.Duration -> sortByDuration( showHiddenSongs, limit )
        SongSortBy.AlbumName -> sortByAlbum( showHiddenSongs, limit )
    }.map {
        if( sortOrder == SortOrder.Descending )
            it.reversed()
        else
            it
    }

    /*
            END ALL ONLINE SONGS
    */


}
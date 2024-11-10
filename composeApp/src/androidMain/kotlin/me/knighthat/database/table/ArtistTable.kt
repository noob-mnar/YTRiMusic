package me.knighthat.database.table

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import it.fast4x.rimusic.enums.ArtistSortBy
import it.fast4x.rimusic.enums.SortOrder
import it.fast4x.rimusic.models.Artist
import it.fast4x.rimusic.models.Info
import it.fast4x.rimusic.models.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Dao
@RewriteQueriesToDropUnusedColumns
interface ArtistTable: Table<Artist, Long> {

    @Query("SELECT * FROM Artist WHERE id = :id")
    fun flowFindById( id: String ): Flow<Artist?>

    @Query("SELECT * FROM Artist WHERE id in (:ids)")
    fun flowFindAllByIds( ids: Collection<String> ): Flow<List<Artist>>

    @Query("""
        SELECT * 
        FROM Song 
        JOIN SongArtistMap ON Song.id = SongArtistMap.songId 
        WHERE SongArtistMap.artistId = :id 
        AND totalPlayTimeMs > 0 
        ORDER BY Song.ROWID DESC
    """)
    fun flowSongsOf( id: String ): Flow<List<Song>>

    @Query("SELECT * FROM Song WHERE artistsText = :name")
    fun findSongsByName( name: String ): List<Song>

    @Query("""
        SELECT id, name, 0 AS size 
        FROM Artist 
        LEFT JOIN SongArtistMap ON id = artistId 
        WHERE songId = :songId
    """)
    fun info( songId: String ): List<Info>

    @Query("""
        SELECT Artist.* 
        FROM Song 
        JOIN SongArtistMap ON Song.id = SongArtistMap.songId 
        JOIN Event ON Song.id = Event.songId 
        JOIN Artist ON Artist.id = SongArtistMap.artistId 
        WHERE (:to - Event.timestamp) <= :from 
        GROUP BY Artist.id 
        ORDER BY SUM(Event.playTime) DESC 
        LIMIT :limit
    """)
    fun flowMostPlayedBetween( from: Long, to: Long, limit: Long ): Flow<List<Artist>>

    @Query("""
        SELECT DISTINCT S.* 
        FROM Song S 
        INNER JOIN SongArtistMap SPM ON S.id = SPM.songId
        INNER JOIN Artist A ON A.id = SPM.artistId 
        WHERE A.bookmarkedAt IS NOT NULL
    """)
    fun flowSongsOfFollowing(): Flow<List<Song>>

    @Query("""
        SELECT * 
        FROM Artist 
        WHERE bookmarkedAt IS NOT NULL 
        ORDER BY name
    """)
    fun flowFollowing(): Flow<List<Artist>>

    @Query("""
        SELECT * 
        FROM Artist 
        WHERE bookmarkedAt IS NOT NULL 
        ORDER BY name
    """)
    fun sortByNameAsc(): Flow<List<Artist>>

    @Query("""
        SELECT * 
        FROM Artist 
        WHERE bookmarkedAt IS NOT NULL 
        ORDER BY bookmarkedAt
    """)
    fun sortByRowIdAsc(): Flow<List<Artist>>

    fun flowAll(
        sortBy: ArtistSortBy = ArtistSortBy.Name,
        sortOrder: SortOrder = SortOrder.Ascending
    ): Flow<List<Artist>> = when (sortBy) {
        ArtistSortBy.Name -> sortByNameAsc()
        ArtistSortBy.DateAdded -> sortByRowIdAsc()
    }.map {
        if (sortOrder == SortOrder.Descending)
            it.reversed()
        else
            it
    }
}
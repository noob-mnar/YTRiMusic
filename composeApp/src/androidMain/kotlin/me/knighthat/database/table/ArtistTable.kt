package me.knighthat.database.table

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import it.fast4x.rimusic.models.Artist
import it.fast4x.rimusic.models.Song
import kotlinx.coroutines.flow.Flow

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
}
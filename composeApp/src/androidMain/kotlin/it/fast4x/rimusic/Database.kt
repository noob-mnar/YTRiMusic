package it.fast4x.rimusic

import android.database.sqlite.SQLiteConstraintException
import androidx.annotation.WorkerThread
import androidx.media3.common.MediaItem
import androidx.room.AutoMigration
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.RoomWarnings
import androidx.room.Transaction
import androidx.room.TypeConverters
import androidx.room.Update
import androidx.room.Upsert
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import it.fast4x.rimusic.database.Converters
import it.fast4x.rimusic.enums.AlbumSortBy
import it.fast4x.rimusic.enums.ArtistSortBy
import it.fast4x.rimusic.enums.PlaylistSongSortBy
import it.fast4x.rimusic.enums.PlaylistSortBy
import it.fast4x.rimusic.enums.SongSortBy
import it.fast4x.rimusic.enums.SortOrder
import it.fast4x.rimusic.models.Album
import it.fast4x.rimusic.models.Artist
import it.fast4x.rimusic.models.Event
import it.fast4x.rimusic.models.Format
import it.fast4x.rimusic.models.Info
import it.fast4x.rimusic.models.Lyrics
import it.fast4x.rimusic.models.Playlist
import it.fast4x.rimusic.models.PlaylistPreview
import it.fast4x.rimusic.models.PlaylistWithSongs
import it.fast4x.rimusic.models.QueuedMediaItem
import it.fast4x.rimusic.models.SearchQuery
import it.fast4x.rimusic.models.Song
import it.fast4x.rimusic.models.SongAlbumMap
import it.fast4x.rimusic.models.SongArtistMap
import it.fast4x.rimusic.models.SongEntity
import it.fast4x.rimusic.models.SongPlaylistMap
import it.fast4x.rimusic.models.SongWithContentLength
import it.fast4x.rimusic.models.SortedSongPlaylistMap
import it.fast4x.rimusic.service.LOCAL_KEY_PREFIX
import kotlinx.coroutines.flow.Flow
import me.knighthat.appContext
import me.knighthat.database.migrator.From10To11Migration
import me.knighthat.database.migrator.From11To12Migration
import me.knighthat.database.migrator.From14To15Migration
import me.knighthat.database.migrator.From20To21Migration
import me.knighthat.database.migrator.From21To22Migration
import me.knighthat.database.migrator.From22To23Migration
import me.knighthat.database.migrator.From3To4Migration
import me.knighthat.database.migrator.From7To8Migration
import me.knighthat.database.migrator.From8To9Migration
import me.knighthat.database.table.AlbumTable
import me.knighthat.database.table.ArtistTable
import me.knighthat.database.table.EventTable
import me.knighthat.database.table.FormatTable
import me.knighthat.database.table.SearchQueryTable
import me.knighthat.database.table.SongTable


@Dao
interface Database {
    companion object : Database by DatabaseInitializer.Instance.database
    
    val path: String?
        get() = DatabaseInitializer.Instance.openHelper.writableDatabase.path

    val song: SongTable
        get() = DatabaseInitializer.Instance.song
    val artist: ArtistTable
        get() = DatabaseInitializer.Instance.artist
    val album: AlbumTable
        get() = DatabaseInitializer.Instance.album
    val format: FormatTable
        get() = DatabaseInitializer.Instance.format
    val event: EventTable
        get() = DatabaseInitializer.Instance.event
    val searchQuery: SearchQueryTable
        get() = DatabaseInitializer.Instance.searchQuery


    @Transaction
    @Query("SELECT * FROM Song WHERE totalPlayTimeMs > 0 ORDER BY totalPlayTimeMs DESC LIMIT :count")
    @RewriteQueriesToDropUnusedColumns
    fun topSongs(count: Int = 10): Flow<List<Song>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT * FROM Song")
    fun listAllSongsAsFlow(): Flow<List<SongEntity>>

    @Transaction
    @Query("SELECT count(playlistId) FROM SongPlaylistMap WHERE songId = :id")
    fun songUsedInPlaylists(id: String): Int

    @Transaction
    @Query("SELECT * FROM Song")
    fun listAllSongs(): List<Song>

    @Query("SELECT COUNT(1) FROM Song WHERE likedAt IS NOT NULL")
    fun likedSongsCount(): Flow<Int>

    @Query("SELECT COUNT(1) FROM Song INNER JOIN Format ON id = songId WHERE contentLength IS NOT NULL AND totalPlayTimeMs > 0")
    fun cachedSongsCount(): Flow<Int>

    @Query("SELECT COUNT(1) FROM Song WHERE id LIKE '$LOCAL_KEY_PREFIX%'")
    fun onDeviceSongsCount(): Flow<Int>

    @Transaction
    @Query("SELECT * FROM Song WHERE artistsText = :name ")
    fun artistSongsByname(name: String): Flow<List<Song>>


    @Query("SELECT id FROM Playlist WHERE name = :playlistName")
    fun playlistExistByName(playlistName: String): Long

    @Query("UPDATE Playlist SET name = :playlistName WHERE id = :playlistId")
    fun updatePlaylistName(playlistName: String, playlistId: Long): Int

    @Transaction
    @Query("UPDATE Song SET title = :title WHERE id = :id")
    fun updateSongTitle(id: String, title: String): Int

    @Transaction
    @Query("UPDATE Song SET artistsText = :artist WHERE id = :id")
    fun updateSongArtist(id: String, artist: String): Int

    @Query("SELECT thumbnailUrl FROM Song WHERE id in (:idsList) ")
    fun getSongsListThumbnailUrls(idsList: List<String>): Flow<List<String?>>

    @Transaction
    @Query("SELECT * FROM Song WHERE ROWID='wooowww' ")
    @RewriteQueriesToDropUnusedColumns
    fun fakeSongsList(): Flow<List<Song>>

    @Transaction
    //@Query("SELECT Playlist.*, (SELECT COUNT(*) FROM SongPlaylistMap WHERE playlistId = Playlist.id) as songCount " +
    //        "FROM Song JOIN SongPlaylistMap ON Song.id = SongPlaylistMap.songId " +
    //        "JOIN Event ON Song.id = Event.songId JOIN Playlist ON Playlist.id = SongPlaylistMap.playlistId " +
    //        "WHERE Event.timestamp BETWEEN :from AND :to GROUP BY Playlist.id ORDER BY Event.timestamp DESC LIMIT :limit")
    @Query("SELECT Playlist.*, (SELECT COUNT(*) FROM SongPlaylistMap WHERE playlistId = Playlist.id) as songCount " +
            "FROM Song JOIN SongPlaylistMap ON Song.id = SongPlaylistMap.songId " +
            "JOIN Event ON Song.id = Event.songId JOIN Playlist ON Playlist.id = SongPlaylistMap.playlistId " +
            "WHERE (:to - Event.timestamp) <= :from GROUP BY Playlist.id ORDER BY SUM(Event.playTime) DESC LIMIT :limit")
    @RewriteQueriesToDropUnusedColumns
    fun playlistsMostPlayedByPeriod(from: Long,to: Long, limit:Int): Flow<List<PlaylistPreview>>

    @Transaction
    //@Query("SELECT Album.* FROM Song JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId " +
    //        "JOIN Event ON Song.id = Event.songId JOIN Album ON Album.id = SongAlbumMap.albumId " +
    //        "WHERE Event.timestamp BETWEEN :from AND :to GROUP BY Album.id ORDER BY Event.timestamp DESC LIMIT :limit")
    @Query("SELECT Album.* FROM Song JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId " +
            "JOIN Event ON Song.id = Event.songId JOIN Album ON Album.id = SongAlbumMap.albumId " +
            "WHERE (:to - Event.timestamp) <= :from GROUP BY Album.id ORDER BY SUM(Event.playTime) DESC LIMIT :limit")
    @RewriteQueriesToDropUnusedColumns
    fun albumsMostPlayedByPeriod(from: Long,to: Long, limit:Int): Flow<List<Album>>

    @Transaction
    //@Query("SELECT Artist.* FROM Song JOIN SongArtistMap ON Song.id = SongArtistMap.songId " +
    //        "JOIN Event ON Song.id = Event.songId JOIN Artist ON Artist.id = SongArtistMap.artistId " +
    //        "WHERE Event.timestamp BETWEEN :from AND :to GROUP BY Artist.id ORDER BY Event.timestamp DESC LIMIT :limit")
    @Query("SELECT Artist.* FROM Song JOIN SongArtistMap ON Song.id = SongArtistMap.songId " +
            "JOIN Event ON Song.id = Event.songId JOIN Artist ON Artist.id = SongArtistMap.artistId " +
            "WHERE (:to - Event.timestamp) <= :from GROUP BY Artist.id ORDER BY SUM(Event.playTime) DESC LIMIT :limit")
    @RewriteQueriesToDropUnusedColumns
    fun artistsMostPlayedByPeriod(from: Long,to: Long, limit:Int): Flow<List<Artist>>

    @Transaction
    //@Query("SELECT Song.* FROM Event JOIN Song ON Song.id = songId WHERE timestamp " +
    //        "BETWEEN :from AND :to GROUP BY songId  ORDER BY timestamp DESC LIMIT :limit")
    @Query("SELECT Song.* FROM Event JOIN Song ON Song.id = songId WHERE " +
            "(:to - Event.timestamp) <= :from GROUP BY songId  ORDER BY SUM(playTime) DESC LIMIT :limit")
    @RewriteQueriesToDropUnusedColumns
    fun songsMostPlayedByPeriod(from: Long, to: Long, limit:Long = Long.MAX_VALUE): Flow<List<Song>>

    @Transaction
    @Query("SELECT Song.* FROM Event JOIN Song ON Song.id = songId WHERE " +
            "CAST(strftime('%m',timestamp / 1000,'unixepoch') AS INTEGER) = :month AND CAST(strftime('%Y',timestamp / 1000,'unixepoch') as INTEGER) = :year " +
            "GROUP BY songId  ORDER BY timestamp DESC LIMIT :limit")
    @RewriteQueriesToDropUnusedColumns
    fun songsMostPlayedByYearMonth(year: Long, month: Long, limit:Long = Long.MAX_VALUE): Flow<List<Song>>

    @Transaction
    @Query("SELECT Song.* FROM Event JOIN Song ON Song.id = songId WHERE " +
            "CAST(strftime('%m',timestamp / 1000,'unixepoch') AS INTEGER) = :month AND CAST(strftime('%Y',timestamp / 1000,'unixepoch') as INTEGER) = :year " +
            "GROUP BY songId  ORDER BY timestamp DESC LIMIT :limit")
    @RewriteQueriesToDropUnusedColumns
    fun songsMostPlayedByYearMonthNoFlow(year: Long, month: Long, limit:Long = Long.MAX_VALUE): List<Song>

    @Transaction
    @Query("SELECT * FROM Song WHERE id LIKE '$LOCAL_KEY_PREFIX%'")
    @RewriteQueriesToDropUnusedColumns
    fun songsOnDevice(): Flow<List<Song>>

    @Transaction
    @Query("SELECT * FROM Song WHERE id LIKE '$LOCAL_KEY_PREFIX%'")
    @RewriteQueriesToDropUnusedColumns
    fun songsEntityOnDevice(): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT * FROM Song WHERE likedAt IS NOT NULL ORDER BY artistsText")
    @RewriteQueriesToDropUnusedColumns
    fun songsFavoritesByArtistAsc(): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT * FROM Song WHERE likedAt IS NOT NULL ORDER BY artistsText DESC")
    @RewriteQueriesToDropUnusedColumns
    fun songsFavoritesByArtistDesc(): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT * FROM Song WHERE likedAt IS NOT NULL ORDER BY totalPlayTimeMs")
    @RewriteQueriesToDropUnusedColumns
    fun songsFavoritesByPlayTimeAsc(): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT * FROM Song WHERE likedAt IS NOT NULL ORDER BY totalPlayTimeMs DESC")
    @RewriteQueriesToDropUnusedColumns
    fun songsFavoritesByPlayTimeDesc(): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT * FROM Song WHERE likedAt IS NOT NULL ORDER BY title COLLATE NOCASE ASC")
    @RewriteQueriesToDropUnusedColumns
    fun songsFavoritesByTitleAsc(): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT * FROM Song WHERE likedAt IS NOT NULL ORDER BY title COLLATE NOCASE DESC")
    @RewriteQueriesToDropUnusedColumns
    fun songsFavoritesByTitleDesc(): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT * FROM Song WHERE likedAt IS NOT NULL ORDER BY ROWID")
    @RewriteQueriesToDropUnusedColumns
    fun songsFavoritesByRowIdAsc(): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT * FROM Song WHERE likedAt IS NOT NULL ORDER BY ROWID DESC")
    @RewriteQueriesToDropUnusedColumns
    fun songsFavoritesByRowIdDesc(): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT * FROM Song WHERE likedAt IS NOT NULL ORDER BY likedAt")
    @RewriteQueriesToDropUnusedColumns
    fun songsFavoritesByLikedAtAsc(): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT * FROM Song WHERE likedAt IS NOT NULL ORDER BY likedAt DESC")
    @RewriteQueriesToDropUnusedColumns
    fun songsFavoritesByLikedAtDesc(): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT DISTINCT S.* FROM Song S LEFT JOIN Event E ON E.songId=S.id " +
            "WHERE likedAt IS NOT NULL " +
            "ORDER BY E.timestamp DESC")
    fun songsFavoritesByDatePlayedDesc(): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT DISTINCT S.* FROM Song S LEFT JOIN Event E ON E.songId=S.id " +
            "WHERE likedAt IS NOT NULL " +
            "ORDER BY E.timestamp")
    fun songsFavoritesByDatePlayedAsc(): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT * FROM Song WHERE likedAt IS NOT NULL ORDER BY durationText")
    @RewriteQueriesToDropUnusedColumns
    fun songsFavoritesByDurationAsc(): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT * FROM Song WHERE likedAt IS NOT NULL ORDER BY durationText DESC")
    @RewriteQueriesToDropUnusedColumns
    fun songsFavoritesByDurationDesc(): Flow<List<SongEntity>>

    fun songsFavorites(sortBy: SongSortBy, sortOrder: SortOrder): Flow<List<SongEntity>> {
        return when (sortBy) {
            SongSortBy.PlayTime -> when (sortOrder) {
                SortOrder.Ascending -> songsFavoritesByPlayTimeAsc()
                SortOrder.Descending -> songsFavoritesByPlayTimeDesc()
            }
            SongSortBy.Title, SongSortBy.AlbumName -> when (sortOrder) {
                SortOrder.Ascending -> songsFavoritesByTitleAsc()
                SortOrder.Descending -> songsFavoritesByTitleDesc()
            }
            SongSortBy.DateLiked -> when (sortOrder) {
                SortOrder.Ascending -> songsFavoritesByLikedAtAsc()
                SortOrder.Descending -> songsFavoritesByLikedAtDesc()
            }
            SongSortBy.DatePlayed -> when (sortOrder) {
                SortOrder.Ascending -> songsFavoritesByDatePlayedAsc()
                SortOrder.Descending -> songsFavoritesByDatePlayedDesc()
            }
            SongSortBy.DateAdded -> when (sortOrder) {
                SortOrder.Ascending -> songsFavoritesByRowIdAsc()
                SortOrder.Descending -> songsFavoritesByRowIdDesc()
            }
            SongSortBy.Artist -> when (sortOrder) {
                SortOrder.Ascending -> songsFavoritesByArtistAsc()
                SortOrder.Descending -> songsFavoritesByArtistDesc()
            }
            SongSortBy.Duration -> when (sortOrder) {
                SortOrder.Ascending -> songsFavoritesByDurationAsc()
                SortOrder.Descending -> songsFavoritesByDurationDesc()
            }
        }
    }

    @Query("SELECT thumbnailUrl FROM Song WHERE likedAt IS NOT NULL AND id NOT LIKE '$LOCAL_KEY_PREFIX%'  LIMIT 4")
    fun preferitesThumbnailUrls(): Flow<List<String?>>

    @Transaction
    @Query("SELECT Song.*, contentLength FROM Song LEFT JOIN Format ON id = songId WHERE songId = :songId")
    fun songCached(songId: String): Flow<SongWithContentLength?>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT Song.*, contentLength FROM Song INNER JOIN Format ON id = songId WHERE contentLength IS NOT NULL AND totalPlayTimeMs > 0 ORDER BY totalPlayTimeMs")
    fun songsOfflineByPlayTimeAsc(): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT Song.*, contentLength FROM Song INNER JOIN Format ON id = songId WHERE contentLength IS NOT NULL AND totalPlayTimeMs > 0 ORDER BY totalPlayTimeMs DESC")
    fun songsOfflineByPlayTimeDesc(): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT Song.*, contentLength FROM Song INNER JOIN Format ON id = songId WHERE contentLength IS NOT NULL AND totalPlayTimeMs > 0 ORDER BY Song.title")
    fun songsOfflineByTitleAsc(): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT Song.*, contentLength FROM Song INNER JOIN Format ON id = songId WHERE contentLength IS NOT NULL AND totalPlayTimeMs > 0 ORDER BY Song.title DESC")
    fun songsOfflineByTitleDesc(): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT Song.*, contentLength FROM Song INNER JOIN Format ON id = songId WHERE contentLength IS NOT NULL AND totalPlayTimeMs > 0 ORDER BY Song.ROWID")
    fun songsOfflineByRowIdAsc(): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT Song.*, contentLength FROM Song INNER JOIN Format ON id = songId WHERE contentLength IS NOT NULL AND totalPlayTimeMs > 0 ORDER BY Song.ROWID DESC")
    fun songsOfflineByRowIdDesc(): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT Song.*, contentLength FROM Song INNER JOIN Format ON id = songId WHERE contentLength IS NOT NULL AND totalPlayTimeMs > 0 ORDER BY Song.likedAt")
    fun songsOfflineByLikedAtAsc(): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT Song.*, contentLength FROM Song INNER JOIN Format ON id = songId WHERE contentLength IS NOT NULL AND totalPlayTimeMs > 0 ORDER BY Song.likedAt DESC")
    fun songsOfflineByLikedAtDesc(): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT Song.*, contentLength FROM Song INNER JOIN Format ON id = songId WHERE contentLength IS NOT NULL AND totalPlayTimeMs > 0 ORDER BY Song.artistsText")
    fun songsOfflineByArtistAsc(): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT Song.*, contentLength FROM Song INNER JOIN Format ON id = songId WHERE contentLength IS NOT NULL AND totalPlayTimeMs > 0 ORDER BY Song.artistsText DESC")
    fun songsOfflineByArtistDesc(): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT Song.*, contentLength FROM Song INNER JOIN Format ON id = songId WHERE contentLength IS NOT NULL AND totalPlayTimeMs > 0 ORDER BY Song.durationText")
    fun songsOfflineByDurationAsc(): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT Song.*, contentLength FROM Song INNER JOIN Format ON id = songId WHERE contentLength IS NOT NULL AND totalPlayTimeMs > 0 ORDER BY Song.durationText DESC")
    fun songsOfflineByDurationDesc(): Flow<List<SongEntity>>

    fun songsOffline(sortBy: SongSortBy, sortOrder: SortOrder): Flow<List<SongEntity>> {
        return when (sortBy) {
            SongSortBy.PlayTime, SongSortBy.DatePlayed -> when (sortOrder) {
                SortOrder.Ascending -> songsOfflineByPlayTimeAsc()
                SortOrder.Descending -> songsOfflineByPlayTimeDesc()
            }
            SongSortBy.Title, SongSortBy.AlbumName -> when (sortOrder) {
                SortOrder.Ascending -> songsOfflineByTitleAsc()
                SortOrder.Descending -> songsOfflineByTitleDesc()
            }
            SongSortBy.DateAdded -> when (sortOrder) {
                SortOrder.Ascending -> songsOfflineByRowIdAsc()
                SortOrder.Descending -> songsOfflineByRowIdDesc()
            }
            SongSortBy.DateLiked -> when (sortOrder) {
                SortOrder.Ascending -> songsOfflineByLikedAtAsc()
                SortOrder.Descending -> songsOfflineByLikedAtDesc()
            }
            SongSortBy.Artist -> when (sortOrder) {
                SortOrder.Ascending -> songsOfflineByArtistAsc()
                SortOrder.Descending -> songsOfflineByArtistDesc()
            }
            SongSortBy.Duration -> when (sortOrder) {
                SortOrder.Ascending -> songsOfflineByDurationAsc()
                SortOrder.Descending -> songsOfflineByDurationDesc()
            }
        }
    }

    @Query("SELECT thumbnailUrl FROM Song JOIN Format ON id = songId WHERE contentLength IS NOT NULL AND totalPlayTimeMs > 0  LIMIT 4")
    fun offlineThumbnailUrls(): Flow<List<String?>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT Song.*, Album.title as albumTitle FROM Song LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId  " +
            "LEFT JOIN Album ON Album.id = SongAlbumMap.albumId " +
            "WHERE (Song.totalPlayTimeMs > :showHiddenSongs OR Song.likedAt NOT NULL) AND Song.id NOT LIKE '$LOCAL_KEY_PREFIX%' ORDER BY Song.ROWID ASC")
    @RewriteQueriesToDropUnusedColumns
    fun songsByRowIdAsc(showHiddenSongs: Int = 0): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT Song.*, Album.title as albumTitle FROM Song LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId  " +
            "LEFT JOIN Album ON Album.id = SongAlbumMap.albumId " +
            "WHERE (Song.totalPlayTimeMs > :showHiddenSongs OR Song.likedAt NOT NULL) AND Song.id NOT LIKE '$LOCAL_KEY_PREFIX%' ORDER BY Song.ROWID DESC")
    @RewriteQueriesToDropUnusedColumns
    fun songsByRowIdDesc(showHiddenSongs: Int = 0): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT Song.*, Album.title as albumTitle FROM Song LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId  " +
            "LEFT JOIN Album ON Album.id = SongAlbumMap.albumId " +
            "WHERE (Song.totalPlayTimeMs > :showHiddenSongs OR Song.likedAt NOT NULL) AND Song.id NOT LIKE '$LOCAL_KEY_PREFIX%' ORDER BY Song.title COLLATE NOCASE ASC")
    @RewriteQueriesToDropUnusedColumns
    fun songsByTitleAsc(showHiddenSongs: Int = 0): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT Song.*, Album.title as albumTitle FROM Song LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId  " +
            "LEFT JOIN Album ON Album.id = SongAlbumMap.albumId " +
            "WHERE (Song.totalPlayTimeMs > :showHiddenSongs OR Song.likedAt NOT NULL) AND Song.id NOT LIKE '$LOCAL_KEY_PREFIX%' ORDER BY Song.title COLLATE NOCASE DESC")
    @RewriteQueriesToDropUnusedColumns
    fun songsByTitleDesc(showHiddenSongs: Int = 0): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT Song.*, Album.title as albumTitle FROM Song LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId  " +
            "LEFT JOIN Album ON Album.id = SongAlbumMap.albumId " +
            "WHERE (Song.totalPlayTimeMs > :showHiddenSongs OR Song.likedAt NOT NULL) AND Song.id NOT LIKE '$LOCAL_KEY_PREFIX%' ORDER BY Song.totalPlayTimeMs ASC")
    @RewriteQueriesToDropUnusedColumns
    fun songsByPlayTimeAsc(showHiddenSongs: Int = 0): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT Song.*, Album.title as albumTitle FROM Song LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId  " +
            "LEFT JOIN Album ON Album.id = SongAlbumMap.albumId " +
            "WHERE (Song.totalPlayTimeMs > :showHiddenSongs OR Song.likedAt NOT NULL) AND Song.id NOT LIKE '$LOCAL_KEY_PREFIX%' ORDER BY Song.totalPlayTimeMs DESC")
    @RewriteQueriesToDropUnusedColumns
    fun songsByPlayTimeDesc(showHiddenSongs: Int = 0): Flow<List<SongEntity>>

    @Transaction
    @Query(
        """
        SELECT * FROM Song
        WHERE id NOT LIKE '$LOCAL_KEY_PREFIX%'
        ORDER BY totalPlayTimeMs DESC
        LIMIT :limit
        """
    )
    @RewriteQueriesToDropUnusedColumns
    fun songsByPlayTimeWithLimitDesc(limit: Int = -1): Flow<List<Song>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query(
        """
        SELECT * FROM Song
        WHERE id NOT LIKE '$LOCAL_KEY_PREFIX%'
        ORDER BY totalPlayTimeMs DESC
        LIMIT :limit
        """
    )
    @RewriteQueriesToDropUnusedColumns
    fun songsEntityByPlayTimeWithLimitDesc(limit: Int = -1): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT DISTINCT Song.*, Album.title as albumTitle FROM Song " +
            "LEFT JOIN Event E ON E.songId=Song.id LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId " +
            "LEFT JOIN Album ON Album.id = SongAlbumMap.albumId " +
            "WHERE (Song.totalPlayTimeMs > :showHiddenSongs OR Song.likedAt NOT NULL) AND Song.id NOT LIKE '$LOCAL_KEY_PREFIX%' " +
            "ORDER BY E.timestamp DESC")
    fun songsByDatePlayedDesc(showHiddenSongs: Int = 0): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT DISTINCT Song.*, Album.title as albumTitle FROM Song " +
            "LEFT JOIN Event E ON E.songId=Song.id LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId " +
            "LEFT JOIN Album ON Album.id = SongAlbumMap.albumId " +
            "WHERE (Song.totalPlayTimeMs > :showHiddenSongs OR Song.likedAt NOT NULL) AND Song.id NOT LIKE '$LOCAL_KEY_PREFIX%' " +
            "ORDER BY E.timestamp")
    @RewriteQueriesToDropUnusedColumns
    fun songsByDatePlayedAsc(showHiddenSongs: Int = 0): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT Song.*, Album.title as albumTitle FROM Song LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId  " +
            "LEFT JOIN Album ON Album.id = SongAlbumMap.albumId " +
            "WHERE (Song.totalPlayTimeMs > :showHiddenSongs OR Song.likedAt NOT NULL) AND Song.id NOT LIKE '$LOCAL_KEY_PREFIX%' ORDER BY Song.likedAt ASC")
    @RewriteQueriesToDropUnusedColumns
    fun songsByLikedAtAsc(showHiddenSongs: Int = 0): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT Song.*, Album.title as albumTitle FROM Song LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId  " +
            "LEFT JOIN Album ON Album.id = SongAlbumMap.albumId " +
            "WHERE (Song.totalPlayTimeMs > :showHiddenSongs OR Song.likedAt NOT NULL) AND Song.id NOT LIKE '$LOCAL_KEY_PREFIX%' ORDER BY Song.likedAt DESC")
    @RewriteQueriesToDropUnusedColumns
    fun songsByLikedAtDesc(showHiddenSongs: Int = 0): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT Song.*, Album.title as albumTitle FROM Song LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId  " +
            "LEFT JOIN Album ON Album.id = SongAlbumMap.albumId " +
            "WHERE (Song.totalPlayTimeMs > :showHiddenSongs OR Song.likedAt NOT NULL) AND Song.id NOT LIKE '$LOCAL_KEY_PREFIX%' ORDER BY Song.artistsText ASC")
    @RewriteQueriesToDropUnusedColumns
    fun songsByArtistAsc(showHiddenSongs: Int = 0): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT Song.*, Album.title as albumTitle FROM Song LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId  " +
            "LEFT JOIN Album ON Album.id = SongAlbumMap.albumId " +
            "WHERE (Song.totalPlayTimeMs > :showHiddenSongs OR Song.likedAt NOT NULL) AND Song.id NOT LIKE '$LOCAL_KEY_PREFIX%' ORDER BY Song.artistsText DESC")
    @RewriteQueriesToDropUnusedColumns
    fun songsByArtistDesc(showHiddenSongs: Int = 0): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Query("SELECT Song.*, Album.title as albumTitle FROM Song LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId  " +
            "LEFT JOIN Album ON Album.id = SongAlbumMap.albumId " +
            "WHERE (Song.totalPlayTimeMs > :showHiddenSongs OR Song.likedAt NOT NULL) AND Song.id NOT LIKE '$LOCAL_KEY_PREFIX%' ORDER BY Song.durationText ASC")
    @RewriteQueriesToDropUnusedColumns
    fun songsByDurationAsc(showHiddenSongs: Int = 0): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Query("SELECT Song.*, Album.title as albumTitle FROM Song LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId  " +
            "LEFT JOIN Album ON Album.id = SongAlbumMap.albumId " +
            "WHERE (Song.totalPlayTimeMs > :showHiddenSongs OR Song.likedAt NOT NULL) AND Song.id NOT LIKE '$LOCAL_KEY_PREFIX%' ORDER BY Song.durationText DESC")
    @RewriteQueriesToDropUnusedColumns
    fun songsByDurationDesc(showHiddenSongs: Int = 0): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT Song.*, Album.title as albumTitle FROM Song LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId  " +
            "LEFT JOIN Album ON Album.id = SongAlbumMap.albumId " +
            "WHERE (Song.totalPlayTimeMs > :showHiddenSongs OR Song.likedAt NOT NULL) AND Song.id NOT LIKE '$LOCAL_KEY_PREFIX%' ORDER BY Album.title COLLATE NOCASE ASC")
    @RewriteQueriesToDropUnusedColumns
    fun songsByAlbumNameAsc(showHiddenSongs: Int = 0): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT Song.*, Album.title as albumTitle FROM Song LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId  " +
            "LEFT JOIN Album ON Album.id = SongAlbumMap.albumId " +
            "WHERE (Song.totalPlayTimeMs > :showHiddenSongs OR Song.likedAt NOT NULL) AND Song.id NOT LIKE '$LOCAL_KEY_PREFIX%' ORDER BY Album.title COLLATE NOCASE DESC")
    @RewriteQueriesToDropUnusedColumns
    fun songsByAlbumNameDesc(showHiddenSongs: Int = 0): Flow<List<SongEntity>>

    fun songs(sortBy: SongSortBy, sortOrder: SortOrder, showHiddenSongs: Int): Flow<List<SongEntity>> {
        return when (sortBy) {
            SongSortBy.AlbumName -> when (sortOrder) {
                SortOrder.Ascending -> songsByAlbumNameAsc(showHiddenSongs)
                SortOrder.Descending -> songsByAlbumNameDesc(showHiddenSongs)
            }
            SongSortBy.PlayTime -> when (sortOrder) {
                SortOrder.Ascending -> songsByPlayTimeAsc(showHiddenSongs)
                SortOrder.Descending -> songsByPlayTimeDesc(showHiddenSongs)
            }
            SongSortBy.Title -> when (sortOrder) {
                SortOrder.Ascending -> songsByTitleAsc(showHiddenSongs)
                SortOrder.Descending -> songsByTitleDesc(showHiddenSongs)
            }
            SongSortBy.DateAdded -> when (sortOrder) {
                SortOrder.Ascending -> songsByRowIdAsc(showHiddenSongs)
                SortOrder.Descending -> songsByRowIdDesc(showHiddenSongs)
            }
            SongSortBy.DatePlayed -> when (sortOrder) {
                SortOrder.Ascending -> songsByDatePlayedAsc(showHiddenSongs)
                SortOrder.Descending -> songsByDatePlayedDesc(showHiddenSongs)
            }
            SongSortBy.DateLiked -> when (sortOrder) {
                SortOrder.Ascending -> songsByLikedAtAsc(showHiddenSongs)
                SortOrder.Descending -> songsByLikedAtDesc(showHiddenSongs)
            }
            SongSortBy.Artist -> when (sortOrder) {
                SortOrder.Ascending -> songsByArtistAsc(showHiddenSongs)
                SortOrder.Descending -> songsByArtistDesc(showHiddenSongs)
            }
            SongSortBy.Duration -> when (sortOrder) {
                SortOrder.Ascending -> songsByDurationAsc(showHiddenSongs)
                SortOrder.Descending -> songsByDurationDesc(showHiddenSongs)
            }
        }
    }


    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT Song.*, Album.title as albumTitle FROM Song JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId  " +
            "JOIN Album ON Album.id = SongAlbumMap.albumId " +
            "WHERE (Song.totalPlayTimeMs > :showHiddenSongs OR Song.likedAt NOT NULL) AND Song.id NOT LIKE '$LOCAL_KEY_PREFIX%' ORDER BY Song.artistsText DESC")
    @RewriteQueriesToDropUnusedColumns
    fun songsWithAlbumByPlayTimeDesc(showHiddenSongs: Int = 0): Flow<List<SongEntity>>

    @Query("SELECT * FROM QueuedMediaItem")
    fun queue(): List<QueuedMediaItem>

    @Query("DELETE FROM QueuedMediaItem")
    fun clearQueue()

    @Query("UPDATE Playlist SET name = '${PINNED_PREFIX}'||name WHERE id = :playlistId")
    fun pinPlaylist(playlistId: Long): Int
    @Query("UPDATE Playlist SET name = REPLACE(name,'${PINNED_PREFIX}','') WHERE id = :playlistId")
    fun unPinPlaylist(playlistId: Long): Int

    @Query("SELECT count(id) FROM Song WHERE id = :songId and likedAt IS NOT NULL")
    fun songliked(songId: String): Int

    @Query("SELECT * FROM Song WHERE id = :id")
    fun song(id: String?): Flow<Song?>

    @Query("SELECT count(id) FROM Song WHERE id = :id")
    fun songExist(id: String): Int

    @Query("SELECT likedAt FROM Song WHERE id = :songId")
    fun likedAt(songId: String): Flow<Long?>

    @Query("UPDATE Song SET likedAt = :likedAt WHERE id = :songId")
    fun like(songId: String, likedAt: Long?): Int

    @Query("UPDATE Song SET durationText = :durationText WHERE id = :songId")
    fun updateDurationText(songId: String, durationText: String): Int

    @Query("SELECT * FROM Lyrics WHERE songId = :songId")
    fun lyrics(songId: String): Flow<Lyrics?>

    @Query("SELECT * FROM Artist WHERE bookmarkedAt IS NOT NULL ORDER BY name")
    fun preferitesArtistsByName(): Flow<List<Artist>>

    @Query("SELECT * FROM Artist WHERE bookmarkedAt IS NOT NULL ORDER BY name DESC")
    fun artistsByNameDesc(): Flow<List<Artist>>

    @Query("SELECT * FROM Artist WHERE bookmarkedAt IS NOT NULL ORDER BY name ASC")
    fun artistsByNameAsc(): Flow<List<Artist>>

    @Query("SELECT * FROM Artist WHERE bookmarkedAt IS NOT NULL ORDER BY bookmarkedAt DESC")
    fun artistsByRowIdDesc(): Flow<List<Artist>>

    @Query("SELECT * FROM Artist WHERE bookmarkedAt IS NOT NULL ORDER BY bookmarkedAt ASC")
    fun artistsByRowIdAsc(): Flow<List<Artist>>

    fun artists(sortBy: ArtistSortBy, sortOrder: SortOrder): Flow<List<Artist>> {
        return when (sortBy) {
            ArtistSortBy.Name -> when (sortOrder) {
                SortOrder.Ascending -> artistsByNameAsc()
                SortOrder.Descending -> artistsByNameDesc()
            }
            ArtistSortBy.DateAdded -> when (sortOrder) {
                SortOrder.Ascending -> artistsByRowIdAsc()
                SortOrder.Descending -> artistsByRowIdDesc()
            }
        }
    }

    @Transaction
    @Query("SELECT *, (SELECT SUM(CAST(REPLACE(durationText, ':', '') AS INTEGER)) FROM Song JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId WHERE SongAlbumMap.albumId = Album.id AND position IS NOT NULL) as totalDuration " +
            "FROM Album WHERE bookmarkedAt IS NOT NULL ORDER BY totalDuration ASC" )
    @RewriteQueriesToDropUnusedColumns
    fun albumsByTotalDurationAsc(): Flow<List<Album>>

    @Transaction
    @Query("SELECT *, (SELECT SUM(CAST(REPLACE(durationText, ':', '') AS INTEGER)) FROM Song JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId WHERE SongAlbumMap.albumId = Album.id AND position IS NOT NULL) as totalDuration " +
            "FROM Album WHERE bookmarkedAt IS NOT NULL ORDER BY totalDuration DESC" )
    @RewriteQueriesToDropUnusedColumns
    fun albumsByTotalDurationDesc(): Flow<List<Album>>

    @Transaction
    @Query("SELECT *, (SELECT COUNT(*) FROM SongAlbumMap WHERE albumId = Album.id) as songCount " +
            "FROM Album WHERE bookmarkedAt IS NOT NULL ORDER BY songCount ASC" )
    @RewriteQueriesToDropUnusedColumns
    fun albumsBySongsCountAsc(): Flow<List<Album>>

    @Transaction
    @Query("SELECT *, (SELECT COUNT(*) FROM SongAlbumMap WHERE albumId = Album.id) as songCount " +
            "FROM Album WHERE bookmarkedAt IS NOT NULL ORDER BY songCount DESC" )
    @RewriteQueriesToDropUnusedColumns
    fun albumsBySongsCountDesc(): Flow<List<Album>>

    @Query("SELECT * FROM Album WHERE bookmarkedAt IS NOT NULL ORDER BY authorsText COLLATE NOCASE ASC")
    fun albumsByArtistAsc(): Flow<List<Album>>

    @Query("SELECT * FROM Album WHERE bookmarkedAt IS NOT NULL ORDER BY authorsText COLLATE NOCASE DESC")
    fun albumsByArtistDesc(): Flow<List<Album>>

    @Query("SELECT * FROM Album WHERE bookmarkedAt IS NOT NULL ORDER BY title COLLATE NOCASE ASC")
    fun albumsByTitleAsc(): Flow<List<Album>>

    @Query("SELECT * FROM Album WHERE bookmarkedAt IS NOT NULL ORDER BY year ASC")
    fun albumsByYearAsc(): Flow<List<Album>>

    @Query("SELECT * FROM Album WHERE bookmarkedAt IS NOT NULL ORDER BY bookmarkedAt ASC")
    fun albumsByRowIdAsc(): Flow<List<Album>>

    @Query("SELECT * FROM Album WHERE bookmarkedAt IS NOT NULL ORDER BY title COLLATE NOCASE DESC")
    fun albumsByTitleDesc(): Flow<List<Album>>

    @Query("SELECT * FROM Album WHERE bookmarkedAt IS NOT NULL ORDER BY year DESC")
    fun albumsByYearDesc(): Flow<List<Album>>

    @Query("SELECT * FROM Album WHERE bookmarkedAt IS NOT NULL ORDER BY bookmarkedAt DESC")
    fun albumsByRowIdDesc(): Flow<List<Album>>

    fun albums(sortBy: AlbumSortBy, sortOrder: SortOrder): Flow<List<Album>> {
        return when (sortBy) {
            AlbumSortBy.Title -> when (sortOrder) {
                SortOrder.Ascending -> albumsByTitleAsc()
                SortOrder.Descending -> albumsByTitleDesc()
            }
            AlbumSortBy.Year -> when (sortOrder) {
                SortOrder.Ascending -> albumsByYearAsc()
                SortOrder.Descending -> albumsByYearDesc()
            }
            AlbumSortBy.DateAdded -> when (sortOrder) {
                SortOrder.Ascending -> albumsByRowIdAsc()
                SortOrder.Descending -> albumsByRowIdDesc()
            }
            AlbumSortBy.Artist -> when (sortOrder) {
                SortOrder.Ascending -> albumsByArtistAsc()
                SortOrder.Descending -> albumsByArtistDesc()
            }
            AlbumSortBy.Songs -> when (sortOrder) {
                SortOrder.Ascending -> albumsBySongsCountAsc()
                SortOrder.Descending -> albumsBySongsCountDesc()
            }
            AlbumSortBy.Duration -> when (sortOrder) {
                SortOrder.Ascending -> albumsByTotalDurationAsc()
                SortOrder.Descending -> albumsByTotalDurationDesc()
            }
        }
    }

    @Transaction
    @Query("SELECT max(position) maxPos FROM SongPlaylistMap WHERE playlistId = :id")
    fun getSongMaxPositionToPlaylist(id: Long): Int

    @Transaction
    @Query("SELECT PM.playlistId FROM SongPlaylistMap PM WHERE PM.songId = :id")
    fun getPlaylistsWithSong(id: String): Flow<List<Long>>

    @Transaction
    @Query("SELECT max(position) maxPos FROM SongPlaylistMap WHERE playlistId = :id")
    fun updateSongMaxPositionToPlaylist(id: Long): Int

    @Transaction
    @Query("SELECT * FROM Playlist WHERE id = :id")
    fun playlistWithSongs(id: Long): Flow<PlaylistWithSongs?>

    @Transaction
    @Query("SELECT * FROM Playlist WHERE trim(name) COLLATE NOCASE = trim(:name) COLLATE NOCASE")
    fun playlistWithSongsNoFlow(name: String): PlaylistWithSongs?

    @Transaction
    @Query("SELECT * FROM Playlist WHERE trim(name) COLLATE NOCASE = trim(:name) COLLATE NOCASE")
    fun playlistWithSongs(name: String): Flow<PlaylistWithSongs?>

    @Transaction
    @Query("SELECT * FROM Playlist WHERE name LIKE '${MONTHLY_PREFIX}' || :name || '%'  ")
    fun monthlyPlaylists(name: String?): Flow<List<PlaylistWithSongs?>>

    @Transaction
    @Query("SELECT id, name, browseId, (SELECT COUNT(*) FROM SongPlaylistMap WHERE playlistId = id) as songCount FROM Playlist WHERE name LIKE '${MONTHLY_PREFIX}' || :name || '%'  ")
    fun monthlyPlaylistsPreview(name: String?): Flow<List<PlaylistPreview>>

    @RewriteQueriesToDropUnusedColumns
    @Transaction
    @Query(
        """
        SELECT * FROM SortedSongPlaylistMap SPLM
        INNER JOIN Song on Song.id = SPLM.songId
        WHERE playlistId = :id
        ORDER BY SPLM.position
        """
    )
    fun playlistSongs(id: Long): Flow<List<Song>?>

    @RewriteQueriesToDropUnusedColumns
    @Transaction
    @Query(
        """
        SELECT * FROM SortedSongPlaylistMap SPLM
        INNER JOIN Song on Song.id = SPLM.songId
        WHERE playlistId = :id
        ORDER BY SPLM.position
        """
    )
    fun songsInPlaylist(id: Long): Flow<List<SongEntity>?>

    @Transaction
    @Query("SELECT DISTINCT S.* FROM Song S INNER JOIN SongAlbumMap SM ON S.id=SM.songId " +
            "INNER JOIN Album A ON A.id=SM.albumId WHERE A.bookmarkedAt IS NOT NULL")
    fun songsInAllBookmarkedAlbums(): Flow<List<Song>?>

    @Transaction
    @Query("SELECT DISTINCT S.* FROM Song S INNER JOIN SongArtistMap SM ON S.id=SM.songId " +
            "INNER JOIN Artist A ON A.id=SM.artistId WHERE A.bookmarkedAt IS NOT NULL")
    fun songsInAllFollowedArtists(): Flow<List<Song>?>

    @Transaction
    @Query("SELECT DISTINCT S.* FROM Song S INNER JOIN songplaylistmap SM ON S.id=SM.songId")
    fun songsInAllPlaylists(): Flow<List<Song>?>

    @Transaction
    @Query("SELECT DISTINCT S.* FROM Song S INNER JOIN songplaylistmap SM ON S.id=SM.songId " +
            "INNER JOIN Playlist P ON P.id=SM.playlistId WHERE P.name LIKE '${PIPED_PREFIX}' || '%'")
    fun songsInAllPipedPlaylists(): Flow<List<Song>?>

    @Transaction
    @Query("SELECT DISTINCT S.* FROM Song S INNER JOIN songplaylistmap SM ON S.id=SM.songId " +
            "INNER JOIN Playlist P ON P.id=SM.playlistId WHERE P.name LIKE '${PINNED_PREFIX}' || '%'")
    fun songsInAllPinnedPlaylists(): Flow<List<Song>?>

    @Transaction
    @Query("SELECT DISTINCT S.* FROM Song S INNER JOIN songplaylistmap SM ON S.id=SM.songId " +
            "INNER JOIN Playlist P ON P.id=SM.playlistId WHERE P.name LIKE '${MONTHLY_PREFIX}' || '%'")
    fun songsInAllMonthlyPlaylists(): Flow<List<Song>?>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT S.*, Album.title as albumTitle FROM Song S INNER JOIN songplaylistmap SP ON S.id=SP.songId " +
            "LEFT JOIN SongAlbumMap ON SongAlbumMap.songId = S.id LEFT JOIN Album ON Album.id = SongAlbumMap.albumId " +
            "WHERE SP.playlistId=:id ORDER BY S.artistsText COLLATE NOCASE ASC")
    fun songsPlaylistByArtistAsc(id: Long): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT S.*, Album.title as albumTitle FROM Song S INNER JOIN songplaylistmap SP ON S.id=SP.songId " +
            "LEFT JOIN SongAlbumMap ON SongAlbumMap.songId = S.id LEFT JOIN Album ON Album.id = SongAlbumMap.albumId " +
            "WHERE SP.playlistId=:id ORDER BY S.artistsText COLLATE NOCASE DESC")
    fun songsPlaylistByArtistDesc(id: Long): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT S.*, Album.title as albumTitle FROM Song S INNER JOIN songplaylistmap SP ON S.id=SP.songId " +
            "LEFT JOIN SongAlbumMap ON SongAlbumMap.songId = S.id LEFT JOIN Album ON Album.id = SongAlbumMap.albumId " +
            "WHERE SP.playlistId=:id ORDER BY S.title COLLATE NOCASE ASC")
    fun songsPlaylistByTitleAsc(id: Long): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT S.*, Album.title as albumTitle FROM Song S INNER JOIN songplaylistmap SP ON S.id=SP.songId " +
            "LEFT JOIN SongAlbumMap ON SongAlbumMap.songId = S.id LEFT JOIN Album ON Album.id = SongAlbumMap.albumId " +
            "WHERE SP.playlistId=:id ORDER BY S.title COLLATE NOCASE DESC")
    fun songsPlaylistByTitleDesc(id: Long): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT S.*, Album.title as albumTitle FROM Song S INNER JOIN songplaylistmap SP ON S.id=SP.songId " +
            "LEFT JOIN SongAlbumMap ON SongAlbumMap.songId = S.id LEFT JOIN Album ON Album.id = SongAlbumMap.albumId " +
            "WHERE SP.playlistId=:id ORDER BY SP.position")
    fun songsPlaylistByPositionAsc(id: Long): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT S.*, Album.title as albumTitle FROM Song S INNER JOIN songplaylistmap SP ON S.id=SP.songId " +
            "LEFT JOIN SongAlbumMap ON SongAlbumMap.songId = S.id LEFT JOIN Album ON Album.id = SongAlbumMap.albumId " +
            "WHERE SP.playlistId=:id ORDER BY SP.position DESC")
    fun songsPlaylistByPositionDesc(id: Long): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT S.*, Album.title as albumTitle FROM Song S INNER JOIN songplaylistmap SP ON S.id=SP.songId " +
            "LEFT JOIN SongAlbumMap ON SongAlbumMap.songId = S.id LEFT JOIN Album ON Album.id = SongAlbumMap.albumId " +
            "WHERE SP.playlistId=:id ORDER BY S.totalPlayTimeMs")
    fun songsPlaylistByPlayTimeAsc(id: Long): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT S.*, Album.title as albumTitle FROM Song S INNER JOIN songplaylistmap SP ON S.id=SP.songId " +
            "LEFT JOIN SongAlbumMap ON SongAlbumMap.songId = S.id LEFT JOIN Album ON Album.id = SongAlbumMap.albumId " +
            "WHERE SP.playlistId=:id ORDER BY S.totalPlayTimeMs DESC")
    fun songsPlaylistByPlayTimeDesc(id: Long): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT DISTINCT S.*, Album.title as albumTitle FROM Song S INNER JOIN songplaylistmap SP ON S.id=SP.songId " +
            "LEFT JOIN Event E ON E.songId=S.id " +
            "LEFT JOIN SongAlbumMap ON SongAlbumMap.songId = S.id LEFT JOIN Album ON Album.id = SongAlbumMap.albumId " +
            "WHERE SP.playlistId=:id " +
            "ORDER BY E.timestamp")
    fun songsPlaylistByDatePlayedAsc(id: Long): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT DISTINCT S.*, Album.title as albumTitle FROM Song S INNER JOIN songplaylistmap SP ON S.id=SP.songId " +
            "LEFT JOIN Event E ON E.songId=S.id " +
            "LEFT JOIN SongAlbumMap ON SongAlbumMap.songId = S.id LEFT JOIN Album ON Album.id = SongAlbumMap.albumId " +
            "WHERE SP.playlistId=:id " +
            "ORDER BY E.timestamp DESC")
    fun songsPlaylistByDatePlayedDesc(id: Long): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT DISTINCT S.*, A.title as albumTitle FROM Song S INNER JOIN songplaylistmap SP ON S.id=SP.songId " +
            "LEFT JOIN songalbummap SA ON SA.songId=SP.songId " +
            "LEFT JOIN Album A ON A.Id=SA.albumId " +
            "WHERE SP.playlistId=:id " +
            "ORDER BY CAST(A.year AS INTEGER) DESC")
    fun songsPlaylistByAlbumYearDesc(id: Long): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT DISTINCT S.*, A.title as albumTitle FROM Song S INNER JOIN songplaylistmap SP ON S.id=SP.songId " +
            "LEFT JOIN songalbummap SA ON SA.songId=SP.songId " +
            "LEFT JOIN Album A ON A.Id=SA.albumId " +
            "WHERE SP.playlistId=:id " +
            "ORDER BY CAST(A.year AS INTEGER)")
    fun songsPlaylistByAlbumYearAsc(id: Long): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT S.*, Album.title as albumTitle FROM Song S INNER JOIN songplaylistmap SP ON S.id=SP.songId " +
            "LEFT JOIN SongAlbumMap ON SongAlbumMap.songId = S.id LEFT JOIN Album ON Album.id = SongAlbumMap.albumId " +
            "WHERE SP.playlistId=:id ORDER BY S.durationText")
    fun songsPlaylistByDurationAsc(id: Long): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT S.*, Album.title as albumTitle FROM Song S INNER JOIN songplaylistmap SP ON S.id=SP.songId " +
            "LEFT JOIN SongAlbumMap ON SongAlbumMap.songId = S.id LEFT JOIN Album ON Album.id = SongAlbumMap.albumId " +
            "WHERE SP.playlistId=:id ORDER BY S.durationText DESC")
    fun songsPlaylistByDurationDesc(id: Long): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT DISTINCT S.*, A.title as albumTitle FROM Song S INNER JOIN songplaylistmap SP ON S.id=SP.songId " +
            "LEFT JOIN songalbummap SA ON SA.songId=SP.songId " +
            "LEFT JOIN Album A ON A.Id=SA.albumId " +
            "WHERE SP.playlistId=:id " +
            "ORDER BY S.artistsText COLLATE NOCASE ASC, A.title COLLATE NOCASE ASC")
    fun songsPlaylistByArtistAndAlbumAsc(id: Long): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT DISTINCT S.*, A.title as albumTitle FROM Song S INNER JOIN songplaylistmap SP ON S.id=SP.songId " +
            "LEFT JOIN songalbummap SA ON SA.songId=SP.songId " +
            "LEFT JOIN Album A ON A.Id=SA.albumId " +
            "WHERE SP.playlistId=:id " +
            "ORDER BY S.artistsText COLLATE NOCASE DESC, A.title COLLATE NOCASE DESC")
    fun songsPlaylistByArtistAndAlbumDesc(id: Long): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT DISTINCT S.*, A.title as albumTitle FROM Song S INNER JOIN songplaylistmap SP ON S.id=SP.songId " +
            "LEFT JOIN songalbummap SA ON SA.songId=SP.songId " +
            "LEFT JOIN Album A ON A.Id=SA.albumId " +
            "WHERE SP.playlistId=:id " +
            "ORDER BY A.title COLLATE NOCASE ASC")
    fun songsPlaylistByAlbumAsc(id: Long): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT DISTINCT S.*, A.title as albumTitle FROM Song S INNER JOIN songplaylistmap SP ON S.id=SP.songId " +
            "LEFT JOIN songalbummap SA ON SA.songId=SP.songId " +
            "LEFT JOIN Album A ON A.Id=SA.albumId " +
            "WHERE SP.playlistId=:id " +
            "ORDER BY A.title COLLATE NOCASE DESC")
    fun songsPlaylistByAlbumDesc(id: Long): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT S.*, Album.title as albumTitle FROM Song S INNER JOIN songplaylistmap SP ON S.id=SP.songId " +
            "LEFT JOIN SongAlbumMap ON SongAlbumMap.songId = S.id LEFT JOIN Album ON Album.id = SongAlbumMap.albumId " +
            "WHERE SP.playlistId=:id ORDER BY S.ROWID")
    fun songsPlaylistByRowIdAsc(id: Long): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT S.*, Album.title as albumTitle FROM Song S INNER JOIN songplaylistmap SP ON S.id=SP.songId " +
            "LEFT JOIN SongAlbumMap ON SongAlbumMap.songId = S.id LEFT JOIN Album ON Album.id = SongAlbumMap.albumId " +
            "WHERE SP.playlistId=:id ORDER BY S.ROWID DESC")
    fun songsPlaylistByRowIdDesc(id: Long): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT S.*, Album.title as albumTitle FROM Song S INNER JOIN songplaylistmap SP ON S.id=SP.songId " +
            "LEFT JOIN SongAlbumMap ON SongAlbumMap.songId = S.id LEFT JOIN Album ON Album.id = SongAlbumMap.albumId " +
            "WHERE SP.playlistId=:id ORDER BY S.LikedAt COLLATE NOCASE ASC")
    fun songsPlaylistByDateLikedAsc(id: Long): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT S.*, Album.title as albumTitle FROM Song S INNER JOIN songplaylistmap SP ON S.id=SP.songId " +
            "LEFT JOIN SongAlbumMap ON SongAlbumMap.songId = S.id LEFT JOIN Album ON Album.id = SongAlbumMap.albumId " +
            "WHERE SP.playlistId=:id ORDER BY S.LikedAt COLLATE NOCASE DESC")
    fun songsPlaylistByDateLikedDesc(id: Long): Flow<List<SongEntity>>

    fun songsPlaylist(id: Long, sortBy: PlaylistSongSortBy, sortOrder: SortOrder): Flow<List<SongEntity>> {
        return when (sortBy) {
            PlaylistSongSortBy.PlayTime -> when (sortOrder) {
                SortOrder.Ascending -> songsPlaylistByPlayTimeAsc(id)
                SortOrder.Descending -> songsPlaylistByPlayTimeDesc(id)
            }
            PlaylistSongSortBy.Title -> when (sortOrder) {
                SortOrder.Ascending -> songsPlaylistByTitleAsc(id)
                SortOrder.Descending -> songsPlaylistByTitleDesc(id)
            }
            PlaylistSongSortBy.Artist -> when (sortOrder) {
                SortOrder.Ascending -> songsPlaylistByArtistAsc(id)
                SortOrder.Descending -> songsPlaylistByArtistDesc(id)
            }
            PlaylistSongSortBy.Position -> when (sortOrder) {
                SortOrder.Ascending -> songsPlaylistByPositionAsc(id)
                SortOrder.Descending -> songsPlaylistByPositionDesc(id)
            }
            PlaylistSongSortBy.DateLiked -> when (sortOrder) {
                SortOrder.Ascending -> songsPlaylistByDateLikedAsc(id)
                SortOrder.Descending -> songsPlaylistByDateLikedDesc(id)
            }
            PlaylistSongSortBy.DatePlayed -> when (sortOrder) {
                SortOrder.Ascending -> songsPlaylistByDatePlayedAsc(id)
                SortOrder.Descending -> songsPlaylistByDatePlayedDesc(id)
            }
            PlaylistSongSortBy.AlbumYear -> when (sortOrder) {
                SortOrder.Ascending -> songsPlaylistByAlbumYearAsc(id)
                SortOrder.Descending -> songsPlaylistByAlbumYearDesc(id)
            }
            PlaylistSongSortBy.Duration -> when (sortOrder) {
                SortOrder.Ascending -> songsPlaylistByDurationAsc(id)
                SortOrder.Descending -> songsPlaylistByDurationDesc(id)
            }
            PlaylistSongSortBy.ArtistAndAlbum -> when (sortOrder) {
                SortOrder.Ascending -> songsPlaylistByArtistAndAlbumAsc(id)
                SortOrder.Descending -> songsPlaylistByArtistAndAlbumDesc(id)
            }
            PlaylistSongSortBy.Album -> when (sortOrder) {
                SortOrder.Ascending -> songsPlaylistByAlbumAsc(id)
                SortOrder.Descending -> songsPlaylistByAlbumDesc(id)
            }
            PlaylistSongSortBy.DateAdded -> when (sortOrder) {
                SortOrder.Ascending -> songsPlaylistByRowIdAsc(id)
                SortOrder.Descending -> songsPlaylistByRowIdDesc(id)
            }

        }
    }

    @Transaction
    @Query("SELECT S.* FROM Song S INNER JOIN songplaylistmap SP ON S.id=SP.songId WHERE SP.playlistId=:id ORDER BY SP.position LIMIT 4")
    fun songsPlaylistTop4Positions(id: Long): Flow<List<Song>>

    @Transaction
    @Query("SELECT SP.position FROM Song S INNER JOIN songplaylistmap SP ON S.id=SP.songId WHERE SP.playlistId=:id AND S.id NOT LIKE '$LOCAL_KEY_PREFIX%' ORDER BY SP.position")
    fun songsPlaylistMap(id: Long): Flow<List<Int>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT id, name, browseId, (SELECT COUNT(*) FROM SongPlaylistMap WHERE playlistId = id) as songCount FROM Playlist WHERE id=:id")
    fun singlePlaylistPreview(id: Long): Flow<PlaylistPreview?>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT id, name, browseId, (SELECT COUNT(*) FROM SongPlaylistMap WHERE playlistId = id) as songCount FROM Playlist WHERE name LIKE '${PINNED_PREFIX}%' ORDER BY name COLLATE NOCASE ASC")
    fun playlistPinnedPreviewsByNameAsc(): Flow<List<PlaylistPreview>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT id, name, browseId, (SELECT COUNT(*) FROM SongPlaylistMap WHERE playlistId = id) as songCount FROM Playlist ORDER BY name COLLATE NOCASE ASC")
    fun playlistPreviewsByNameAsc(): Flow<List<PlaylistPreview>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT id, name, browseId, (SELECT COUNT(*) FROM SongPlaylistMap WHERE playlistId = id) as songCount FROM Playlist ORDER BY ROWID ASC")
    fun playlistPreviewsByDateAddedAsc(): Flow<List<PlaylistPreview>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT id, name, browseId, (SELECT COUNT(*) FROM SongPlaylistMap WHERE playlistId = id) as songCount FROM Playlist ORDER BY songCount ASC")
    fun playlistPreviewsByDateSongCountAsc(): Flow<List<PlaylistPreview>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT id, name, browseId, (SELECT COUNT(*) FROM SongPlaylistMap WHERE playlistId = id) as songCount FROM Playlist ORDER BY name COLLATE NOCASE DESC")
    fun playlistPreviewsByNameDesc(): Flow<List<PlaylistPreview>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT id, name, browseId, (SELECT COUNT(*) FROM SongPlaylistMap WHERE playlistId = id) as songCount FROM Playlist ORDER BY ROWID DESC")
    fun playlistPreviewsByDateAddedDesc(): Flow<List<PlaylistPreview>>
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT id, name, browseId, (SELECT COUNT(*) FROM SongPlaylistMap WHERE playlistId = id) as songCount FROM Playlist ORDER BY songCount DESC")
    fun playlistPreviewsByDateSongCountDesc(): Flow<List<PlaylistPreview>>
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT id, name, browseId, " +
            "(SELECT COUNT(*) FROM SongPlaylistMap WHERE playlistId = id) as songCount, " +
            "(SELECT SUM(Song.totalPlayTimeMs) FROM Song " +
            "JOIN SongPlaylistMap ON Song.id = SongPlaylistMap.songId " +
            "WHERE SongPlaylistMap.playlistId = Playlist.id ) as TotPlayTime " +
            "FROM Playlist " +
            "ORDER BY 4")
    fun playlistPreviewsByMostPlayedSongsAsc(): Flow<List<PlaylistPreview>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT id, name, browseId, " +
            "(SELECT COUNT(*) FROM SongPlaylistMap WHERE playlistId = id) as songCount, " +
            "(SELECT SUM(Song.totalPlayTimeMs) FROM Song " +
            "JOIN SongPlaylistMap ON Song.id = SongPlaylistMap.songId " +
            "WHERE SongPlaylistMap.playlistId = Playlist.id ) as TotPlayTime " +
            "FROM Playlist " +
            "ORDER BY 4 DESC")
    fun playlistPreviewsByMostPlayedSongsDesc(): Flow<List<PlaylistPreview>>

    fun playlistPreviews(
        sortBy: PlaylistSortBy,
        sortOrder: SortOrder
    ): Flow<List<PlaylistPreview>> {
        return when (sortBy) {
            PlaylistSortBy.Name -> when (sortOrder) {
                SortOrder.Ascending -> playlistPreviewsByNameAsc()
                SortOrder.Descending -> playlistPreviewsByNameDesc()
            }
            PlaylistSortBy.SongCount -> when (sortOrder) {
                SortOrder.Ascending -> playlistPreviewsByDateSongCountAsc()
                SortOrder.Descending -> playlistPreviewsByDateSongCountDesc()
            }
            PlaylistSortBy.DateAdded -> when (sortOrder) {
                SortOrder.Ascending -> playlistPreviewsByDateAddedAsc()
                SortOrder.Descending -> playlistPreviewsByDateAddedDesc()
            }
            PlaylistSortBy.MostPlayed -> when (sortOrder) {
                SortOrder.Ascending -> playlistPreviewsByMostPlayedSongsAsc()
                SortOrder.Descending -> playlistPreviewsByMostPlayedSongsDesc()
            }
        }
    }

    @Query("SELECT thumbnailUrl FROM Song JOIN SongPlaylistMap ON id = songId WHERE playlistId = :id ORDER BY position LIMIT 4")
    fun playlistThumbnailUrls(id: Long): Flow<List<String>>

    @Transaction
    @Query("""
        UPDATE SongPlaylistMap SET position = 
          CASE 
            WHEN position < :fromPosition THEN position + 1
            WHEN position > :fromPosition THEN position - 1
            ELSE :toPosition
          END 
        WHERE playlistId = :playlistId AND position BETWEEN MIN(:fromPosition,:toPosition) and MAX(:fromPosition,:toPosition)
    """)
    fun move(playlistId: Long, fromPosition: Int, toPosition: Int)

    @Transaction
    @Query("UPDATE SongPlaylistMap SET position = :toPosition WHERE playlistId = :playlistId and songId = :songId")
    fun updateSongPosition(playlistId: Long, songId: String, toPosition: Int)

    @Query("DELETE FROM SongPlaylistMap WHERE playlistId = :id")
    fun clearPlaylist(id: Long)

    @Query("DELETE FROM SongPlaylistMap WHERE songId = :id")
    fun deleteSongFromPlaylists(id: String)

    @Query("SELECT * FROM Song WHERE title LIKE :query OR artistsText LIKE :query")
    fun search(query: String): Flow<List<Song>>

    @Query("SELECT albumId AS id, NULL AS name, 0 AS size FROM SongAlbumMap WHERE songId = :songId")
    fun songAlbumInfo(songId: String): Info?

    @Query("SELECT id, name, 0 AS size FROM Artist LEFT JOIN SongArtistMap ON id = artistId WHERE songId = :songId")
    fun songArtistInfo(songId: String): List<Info>

    /*
        @Transaction
        @Query("SELECT Song.* FROM Event JOIN Song ON Song.id = songId GROUP BY songId ORDER BY SUM(CAST(playTime AS REAL) / (((:now - timestamp) / 86400000) + 1)) DESC LIMIT 1")
    //    @Query("SELECT Song.* FROM Event JOIN Song ON Song.id = songId GROUP BY songId ORDER BY timestamp DESC LIMIT 1")
        @RewriteQueriesToDropUnusedColumns
        fun trending(now: Long = System.currentTimeMillis()): Flow<Song?>
    //    fun trending(): Flow<Song?>
     */

    @Transaction
    @Query("SELECT Song.* FROM Event JOIN Song ON Song.id = songId WHERE Song.id NOT LIKE '$LOCAL_KEY_PREFIX%' GROUP BY songId ORDER BY SUM(CAST(playTime AS REAL) / (((:now - timestamp) / 86400000) + 1)) DESC LIMIT 1")
    @RewriteQueriesToDropUnusedColumns
    fun trendingReal(now: Long = System.currentTimeMillis()): Flow<List<Song>>

    @Transaction
    @Query("SELECT Song.* FROM Event JOIN Song ON Song.id = songId WHERE Song.id NOT LIKE '$LOCAL_KEY_PREFIX%' GROUP BY songId ORDER BY SUM(playTime) DESC LIMIT :limit")
    @RewriteQueriesToDropUnusedColumns
    fun trending(limit: Int = 3): Flow<List<Song>>

    @Transaction
    @Query("SELECT Song.* FROM Event JOIN Song ON Song.id = songId WHERE Song.id NOT LIKE '$LOCAL_KEY_PREFIX%' GROUP BY songId ORDER BY SUM(playTime) DESC LIMIT :limit")
    @RewriteQueriesToDropUnusedColumns
    fun trendingSongEntity(limit: Int = 3): Flow<List<SongEntity>>

    @Transaction
    @Query("SELECT Song.* FROM Event JOIN Song ON Song.id = songId WHERE (:now - Event.timestamp) <= :period AND Song.id NOT LIKE '$LOCAL_KEY_PREFIX%' GROUP BY songId ORDER BY SUM(playTime) DESC LIMIT :limit")
    @RewriteQueriesToDropUnusedColumns
    fun trending(
        limit: Int = 3,
        now: Long = System.currentTimeMillis(),
        period: Long
    ): Flow<List<Song>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT Song.* FROM Event JOIN Song ON Song.id = songId WHERE (:now - Event.timestamp) <= :period AND Song.id NOT LIKE '$LOCAL_KEY_PREFIX%' GROUP BY songId ORDER BY SUM(playTime) DESC LIMIT :limit")
    @RewriteQueriesToDropUnusedColumns
    fun trendingSongEntity(
        limit: Int = 3,
        now: Long = System.currentTimeMillis(),
        period: Long
    ): Flow<List<SongEntity>>

    @Transaction
    @Query("SELECT Song.* FROM Event JOIN Song ON Song.id = songId WHERE playTime > 0 and Song.id NOT LIKE '$LOCAL_KEY_PREFIX%' GROUP BY songId ORDER BY timestamp DESC LIMIT :limit")
    @RewriteQueriesToDropUnusedColumns
    fun lastPlayed( limit: Int = 10 ): Flow<List<Song>>

    @Query("SELECT COUNT (*) FROM Event")
    fun eventsCount(): Flow<Int>

    @Query("DELETE FROM Event")
    fun clearEvents()

    @Query("DELETE FROM Event WHERE songId = :songId")
    fun clearEventsFor(songId: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    @Throws(SQLException::class)
    fun insert(event: Event)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(searchQuery: SearchQuery)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(playlist: Playlist): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(songPlaylistMap: SongPlaylistMap): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insert(songArtistMap: SongArtistMap): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insert(queuedMediaItems: List<QueuedMediaItem>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertSongPlaylistMaps(songPlaylistMaps: List<SongPlaylistMap>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert( songAlbumMap: SongAlbumMap )

    @Transaction
    fun insert(
        mediaItem: MediaItem,
        block: (Song) -> Song = { it }
    ) = Database.transaction {
        val metadata = mediaItem.mediaMetadata
        val extras = metadata.extras
        val song = Song(
            id = mediaItem.mediaId,
            title = metadata.title!!.toString(),
            artistsText = metadata.artist?.toString(),
            durationText = extras?.getString("durationText"),
            thumbnailUrl = metadata.artworkUri?.toString()
        )

        block( song )

        try {
            // Attempt to put song to database
            this@Database.song.insert( song )
        } catch ( e: SQLiteConstraintException ) {
            // Stop here if it's already in database
            return@transaction
        }

        val albumId = extras?.getString( "albumId" )
        val artistNames = extras?.getStringArrayList("artistNames")
        val artistIds = extras?.getStringArrayList("artistIds")

        // Return if any of those variables is null
        if( albumId == null || artistNames == null || artistIds == null )
            return@transaction

        album.insert(
            Album(
                id = albumId,
                title = metadata.albumTitle?.toString()
            )
        )

        insert(
            SongAlbumMap(
                songId = song.id,
                albumId = albumId,
                position = null
            )
        )

        if( artistNames.size == artistIds.size ) {
            artistNames.mapIndexed { index, artistName ->
                Artist(id = artistIds[index], name = artistName)
            }.onEach( artist::safeUpsert )

            artistIds.map { artistId ->
                SongArtistMap(songId = song.id, artistId = artistId)
            }.onEach( ::insert )
        }
    }

    @Update
    fun update(playlist: Playlist)

    @Upsert
    fun upsert(lyrics: Lyrics)

    @Upsert
    fun upsert(songAlbumMap: SongAlbumMap)

    @Delete
    fun delete(playlist: Playlist)

    @Delete
    fun delete(songPlaylistMap: SongPlaylistMap)

    @RawQuery
    fun raw(supportSQLiteQuery: SupportSQLiteQuery): Int

    fun checkpoint() {
        raw(SimpleSQLiteQuery("PRAGMA wal_checkpoint(FULL)"))
    }

    fun close() = DatabaseInitializer.Instance.close()

    /**
     * Commit statements in BULK. If anything goes wrong during the
     * transaction, other statements will be cancelled and reversed
     * to preserve database's integrity. [Read more](https://sqlite.org/lang_transaction.html)
     *
     * [transaction] runs all statements on non-blocking thread
     * to prevent UI from going unresponsive.
     *
     * ### Best use cases:
     * - Commit multiple write statements that require data integrity
     * - Processes that take longer time to complete
     *
     * # Do NOT use this to retrieve data from the database.
     *
     * @param block of statements to write to database
     */
    @WorkerThread
    fun transaction( block: Database.() -> Unit ) = DatabaseInitializer.Instance
                                                                       .transactionExecutor
                                                                       .execute { this@Database.block() }

    /**
     * Access and retrieve from database.
     *
     * [query] runs all statements asynchronously to 
     * prevent blocking UI thread from freezing
     * 
     * ### Best use cases:
     * - Background data retrieval
     * - Non-immediate UI component update (i.e. count number of songs)
     *
     * # Do NOT use this method to write data to database because it offers no fail-safe during write.
     *
     * @param block of statements to retrieve data from database
     */
    fun query( block: Database.() -> Unit ) = DatabaseInitializer.Instance
                                                                 .queryExecutor
                                                                 .execute { this@Database.block() }
}

@androidx.room.Database(
    entities = [
        Song::class,
        SongPlaylistMap::class,
        Playlist::class,
        Artist::class,
        SongArtistMap::class,
        Album::class,
        SongAlbumMap::class,
        SearchQuery::class,
        QueuedMediaItem::class,
        Format::class,
        Event::class,
        Lyrics::class,
    ],
    views = [
        SortedSongPlaylistMap::class
    ],
    version = 23,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4, spec = From3To4Migration::class),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7),
        AutoMigration(from = 7, to = 8, spec = From7To8Migration::class),
        AutoMigration(from = 9, to = 10),
        AutoMigration(from = 11, to = 12, spec = From11To12Migration::class),
        AutoMigration(from = 12, to = 13),
        AutoMigration(from = 13, to = 14),
        AutoMigration(from = 15, to = 16),
        AutoMigration(from = 16, to = 17),
        AutoMigration(from = 17, to = 18),
        AutoMigration(from = 18, to = 19),
        AutoMigration(from = 19, to = 20),
        AutoMigration(from = 20, to = 21, spec = From20To21Migration::class),
        AutoMigration(from = 21, to = 22, spec = From21To22Migration::class),
    ],
)
@TypeConverters(Converters::class)
abstract class DatabaseInitializer protected constructor() : RoomDatabase() {
    abstract val database: Database
    abstract val song: SongTable
    abstract val artist: ArtistTable
    abstract val album: AlbumTable
    abstract val format: FormatTable
    abstract val event: EventTable
    abstract val searchQuery: SearchQueryTable

    companion object {

        lateinit var Instance: DatabaseInitializer

        private fun getDatabase() = Room
            .databaseBuilder(appContext(), DatabaseInitializer::class.java, "data.db")
            .addMigrations(
                From8To9Migration(),
                From10To11Migration(),
                From14To15Migration(),
                From22To23Migration()
            )
            .build()


        //context(Context)
        operator fun invoke() {
            if (!::Instance.isInitialized) reload()
        }

        fun reload() = synchronized(this) {
            Instance = getDatabase()
        }
    }
}
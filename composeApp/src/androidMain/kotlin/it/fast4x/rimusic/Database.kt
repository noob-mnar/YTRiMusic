package it.fast4x.rimusic

import android.database.sqlite.SQLiteConstraintException
import androidx.annotation.WorkerThread
import androidx.media3.common.MediaItem
import androidx.room.AutoMigration
import androidx.room.Dao
import androidx.room.RawQuery
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.TypeConverters
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import it.fast4x.rimusic.database.Converters
import it.fast4x.rimusic.models.Album
import it.fast4x.rimusic.models.Artist
import it.fast4x.rimusic.models.Event
import it.fast4x.rimusic.models.Format
import it.fast4x.rimusic.models.Lyrics
import it.fast4x.rimusic.models.Playlist
import it.fast4x.rimusic.models.QueuedMediaItem
import it.fast4x.rimusic.models.SearchQuery
import it.fast4x.rimusic.models.Song
import it.fast4x.rimusic.models.SongAlbumMap
import it.fast4x.rimusic.models.SongArtistMap
import it.fast4x.rimusic.models.SongPlaylistMap
import it.fast4x.rimusic.models.SortedSongPlaylistMap
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
import me.knighthat.database.table.LyricsTable
import me.knighthat.database.table.PlaylistTable
import me.knighthat.database.table.QueuedMediaItemTable
import me.knighthat.database.table.SearchQueryTable
import me.knighthat.database.table.SongAlbumMapTable
import me.knighthat.database.table.SongArtistMapTable
import me.knighthat.database.table.SongPlaylistMapTable
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
    val playlist: PlaylistTable
        get() = DatabaseInitializer.Instance.playlist
    val songPlaylistMap: SongPlaylistMapTable
        get() = DatabaseInitializer.Instance.songPlaylistMap
    val lyrics: LyricsTable
        get() = DatabaseInitializer.Instance.lyrics
    val songArtistMap: SongArtistMapTable
        get() = DatabaseInitializer.Instance.songArtistMap
    val songAlbumMap: SongAlbumMapTable
        get() = DatabaseInitializer.Instance.songAlbumMap
    val queuedMediaItem: QueuedMediaItemTable
        get() = DatabaseInitializer.Instance.queuedMediaItem

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

        songAlbumMap.safeUpsert(
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
            }.onEach( songArtistMap::safeUpsert )
        }
    }

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
    abstract val playlist: PlaylistTable
    abstract val songPlaylistMap: SongPlaylistMapTable
    abstract val lyrics: LyricsTable
    abstract val songArtistMap: SongArtistMapTable
    abstract val songAlbumMap: SongAlbumMapTable
    abstract val queuedMediaItem: QueuedMediaItemTable

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
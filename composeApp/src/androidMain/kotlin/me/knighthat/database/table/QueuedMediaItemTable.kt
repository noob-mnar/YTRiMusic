package me.knighthat.database.table

import androidx.room.Dao
import androidx.room.Query
import it.fast4x.rimusic.models.QueuedMediaItem
import me.knighthat.database.DatabaseTable

@Dao
@DatabaseTable("QueuedMediaItem")
interface QueuedMediaItemTable: Table<QueuedMediaItem, Unit> {

    @Query("SELECT * FROM QueuedMediaItem")
    fun all(): List<QueuedMediaItem>
}
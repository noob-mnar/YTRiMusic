package me.knighthat.database.table

import androidx.room.Dao
import androidx.room.Query
import it.fast4x.rimusic.models.Lyrics
import kotlinx.coroutines.flow.Flow

@Dao
interface LyricsTable: Table<Lyrics, Unit> {

    @Query("SELECT * FROM Lyrics WHERE songId = :songId")
    fun flowFindBySongId( songId: String ): Flow<Lyrics?>
}
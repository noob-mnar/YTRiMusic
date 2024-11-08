package me.knighthat.database.table

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import it.fast4x.rimusic.models.SearchQuery
import kotlinx.coroutines.flow.Flow
import me.knighthat.database.DatabaseTable

@Dao
@RewriteQueriesToDropUnusedColumns
@DatabaseTable("SearchQuery")
interface SearchQueryTable: Table<SearchQuery, Unit> {

    @Query("""
        SELECT * 
        FROM SearchQuery 
        WHERE `query` LIKE :query 
        ORDER BY id DESC
    """)
    fun flowFindAllContain( query: String ): Flow<List<SearchQuery>>

    @Query("SELECT COUNT (*) FROM SearchQuery")
    fun flowCountAll(): Flow<Int>
}
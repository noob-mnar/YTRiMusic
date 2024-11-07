package me.knighthat.database.table

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Transaction
import androidx.room.Upsert
import it.fast4x.rimusic.Database
import it.fast4x.rimusic.DatabaseInitializer
import java.sql.SQLException


/**
 * Represents the default operations of a table in SQLite.
 *
 * @param E is the entity represents a record
 * @param K is the return type from inserting a single record
 */
interface Table<E, K> {

    /**
     * Attempt to add a record to the database with no safeguard
     * or failsafe inplace.
     *
     * In the event of error such as [SQLiteConstraintException],
     * will be thrown to signify the problem, and the data within
     * the database remains untouched
     */
    @Insert
    @Throws(SQLException::class)
    fun insert( entity: E ): K

    /**
     * Attempt to add a record to the database.
     *
     * In the event of error, the old record (which exists on the database)
     * will be updated to match [entity]'s values
     *
     * **NOTE:** This method does NOT have [SQLiteConstraintException]
     * failsafe. Meaning, if the record violates the constraint(s),
     * [SQLiteConstraintException] will be thrown.
     */
    @Upsert
    fun upsert( entity: E ): K

    /**
     * Attempt to add a record to the database.
     *
     * In the event of error, such as when the record already
     * exists in the database. This method will attempt to apply
     * [entity]'s values to the existed record.
     *
     * Other problems of [SQLException] will be ignored
     */
    @Transaction
    fun safeUpsert( entity: E ) = Database.transaction {
        try {
            upsert( entity )
        } catch ( _: SQLException ) {}
    }

    /**
     * Attempt to add records to the database with no safeguard
     * or failsafe inplace.
     *
     * In the event of error such as [SQLiteConstraintException],
     * will be thrown to signify the problem, and the data within
     * the database remains untouched.
     */
    @Insert
    @Throws(SQLException::class)
    fun insert( entities: Collection<E> )

    /**
     * Attempt to add records to the database.
     *
     * In the event of error, old records (which exist on the database)
     * will be updated to match new values
     *
     * **NOTE:** This method does NOT have [SQLiteConstraintException]
     * failsafe. Meaning, if the record violates the constraint(s),
     * [SQLiteConstraintException] will be thrown.
     */
    @Upsert
    fun upsert( entities: Collection<E> )

    /**
     * Attempt to add a record to the database.
     *
     * In the event of error, such as when the record already
     * exists in the database. This method will attempt to apply
     * [entities]'s values to the existed record.
     *
     * Other problems of [SQLException] will be ignored
     */
    @Transaction
    fun safeUpsert( entities: Collection<E> ) = Database.transaction {
        try {
            upsert( entities )
        } catch ( _: SQLException ) {}
    }

    /**
     * Attempt to delete a row from the database.
     *
     * @return number of rows affected by this operation
     */
    @Delete
    fun delete( entity: E ): Int

    /**
     * Attempt to delete rows from the database.
     *
     * @return number of rows affected by this operation
     */
    @Delete
    fun delete( entities: Collection<E> ): Int

    /**
     * Delete all records
     */
    @Transaction
    fun clear() = Database.transaction {
        val className = this@Table::class.java.simpleName

        DatabaseInitializer.Instance.query(
            "DELETE FROM $className",
            arrayOf()
        )
    }
}
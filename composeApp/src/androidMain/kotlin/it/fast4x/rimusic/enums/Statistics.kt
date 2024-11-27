package it.fast4x.rimusic.enums

import androidx.annotation.DrawableRes
import androidx.annotation.IntRange
import androidx.annotation.StringRes
import it.fast4x.rimusic.R
import me.knighthat.enums.Drawable
import me.knighthat.enums.TextView


enum class StatisticsType(
    @field:DrawableRes override val iconId: Int,
    @field:StringRes override val textId: Int
): Drawable, TextView {
    // Do NOT change the order of this enum
    // [StatisticsType] relies on its ordinal

    Today( R.drawable.stat_today, R.string.today ),

    OneWeek( R.drawable.stat_week, R.string._1_week ),

    OneMonth( R.drawable.stat_month, R.string._1_month ),

    ThreeMonths( R.drawable.stat_3months, R.string._3_month ),

    SixMonths( R.drawable.stat_6months, R.string._6_month ),

    OneYear( R.drawable.stat_year, R.string._1_year ),

    All( R.drawable.calendar_clear, R.string.all );

    companion object {
        /**
         * Lookup [StatisticsType] value by its ordinal [index].
         *
         * [index] must be a positive number within the range
         * specified by [IntRange] annotation.
         *
         * If [index] is a number outside of range,
         * exception [ArrayIndexOutOfBoundsException] will be thrown.
         *
         * @param index the ordinal of value in [StatisticsType]
         *
         * @return [StatisticsType] if value is found
         *
         * @throws ArrayIndexOutOfBoundsException if [index] is outside available range
         */
        @JvmStatic
        fun fromTabIndex(
            // Update [to]'s value when new value is added/deleted
            @IntRange( from = 0, to = 7 ) index: Int
        ): StatisticsType = entries[index]
    }
}

enum class StatisticsCategory {
    Songs,
    Artists,
    Albums,
    Playlists
}
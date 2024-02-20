package it.vfsfitvnm.vimusic.models

import androidx.room.Entity
import androidx.room.PrimaryKey

data class OnDeviceBlacklistPath(
    val path: String
) {
    fun test(relativePath: String): Boolean {
        return relativePath.startsWith(path)
    }
}
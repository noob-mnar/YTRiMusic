package it.fast4x.rimusic.enums

enum class ExoPlayerDiskCacheMaxSize( val megaBytes: Int ) {
    Disabled( 1 ),
    `32MB`( 32 ),
    `512MB`( 512 ),
    `1GB`( 1024 ),
    `2GB`( 2048 ),
    `4GB`( 4096 ),
    `8GB`( 8192 ),
    Unlimited( 0 ),
    Custom( 1_000_000 );        // YES! It is valid in Kotlin

    val bytes: Long
        get() = this.megaBytes * 1000 * 1000L
}

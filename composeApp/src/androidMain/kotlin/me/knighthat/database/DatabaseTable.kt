package me.knighthat.database

@Target(AnnotationTarget.CLASS)
@Retention(value = AnnotationRetention.BINARY)
annotation class DatabaseTable(
    val value: String
)

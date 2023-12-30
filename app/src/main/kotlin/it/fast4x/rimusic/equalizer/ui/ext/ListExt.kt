package it.fast4x.rimusic.equalizer.ui.ext

fun <T> List<T>.repeat(times: Int): MutableList<T> {
    val result = mutableListOf<T>()
    repeat(times) {
        result.addAll(this)
    }
    return result
}
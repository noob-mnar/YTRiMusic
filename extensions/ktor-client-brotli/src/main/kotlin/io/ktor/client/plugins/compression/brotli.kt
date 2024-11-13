package io.ktor.client.plugins.compression
//import io.ktor.client.engine.cio.*

fun ContentEncodingConfig.brotli(quality: Float? = null) {
    customEncoder(BrotliEncoder, quality)
}

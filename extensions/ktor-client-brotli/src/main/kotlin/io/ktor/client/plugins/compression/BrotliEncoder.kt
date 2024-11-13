package io.ktor.client.plugins.compression

import io.ktor.util.ContentEncoder
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.jvm.javaio.toByteReadChannel
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.CoroutineScope
import org.brotli.dec.BrotliInputStream
import kotlin.coroutines.CoroutineContext

internal object BrotliEncoder : ContentEncoder {
    override val name: String = "br"
    override fun decode(
        source: ByteReadChannel,
        coroutineContext: CoroutineContext
    ): ByteReadChannel =
        BrotliInputStream(source.toInputStream()).toByteReadChannel()


    override fun encode(
        source: ByteReadChannel,
        coroutineContext: CoroutineContext
    ): ByteReadChannel {
        TODO("Not yet implemented")
    }

    override fun encode(
        source: ByteWriteChannel,
        coroutineContext: CoroutineContext
    ): ByteWriteChannel {
        TODO("Not yet implemented")
    }

//    override fun CoroutineScope.encode(source: ByteReadChannel) =
//        error("BrotliOutputStream not available (https://github.com/google/brotli/issues/715)")
//
//    override fun CoroutineScope.decode(source: ByteReadChannel): ByteReadChannel =
//        BrotliInputStream(source.toInputStream()).toByteReadChannel()
}

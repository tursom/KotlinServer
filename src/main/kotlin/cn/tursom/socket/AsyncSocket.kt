package cn.tursom.socket

import java.io.Closeable
import java.nio.ByteBuffer

interface AsyncSocket : Closeable {
	suspend fun write(buffer: ByteBuffer, timeout: Long = 0L): Int
	suspend fun read(buffer: ByteBuffer, timeout: Long = 0L): Int
	override fun close()
}
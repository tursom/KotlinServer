package cn.tursom.socket

import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel

interface IAsyncNioSocket : Closeable {
	val channel: SocketChannel
	val key: SelectionKey
	suspend fun read(buffer: ByteBuffer): Int
	suspend fun write(buffer: ByteBuffer): Int
}
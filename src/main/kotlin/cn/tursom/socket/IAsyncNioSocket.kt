package cn.tursom.socket

import java.io.Closeable
import java.net.SocketException
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel

interface IAsyncNioSocket : Closeable {
	val channel: SocketChannel
	val key: SelectionKey
	suspend fun read(buffer: ByteBuffer): Int
	suspend fun write(buffer: ByteBuffer): Int
	/**
	 * 如果通道已断开则会抛出异常
	 */
	suspend fun recv(buffer: ByteBuffer): Int {
		val readSize = read(buffer)
		if (readSize < 0) {
			throw SocketException("channel closed")
		}
		return readSize
	}
}
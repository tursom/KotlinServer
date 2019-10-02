package cn.tursom.socket

import cn.tursom.socket.niothread.INioThread
import cn.tursom.utils.bytebuffer.AdvanceByteBuffer
import cn.tursom.utils.bytebuffer.readNioBuffer
import cn.tursom.utils.bytebuffer.writeNioBuffer
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import java.net.SocketException
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel

interface IAsyncNioSocket : AsyncSocket {
	val channel: SocketChannel
	val key: SelectionKey
	val nioThread: INioThread

	fun waitMode() {
		nioThread.execute { if (key.isValid) key.interestOps(0) }
		nioThread.wakeup()
	}

	fun readMode() {
		nioThread.execute { if (key.isValid) key.interestOps(SelectionKey.OP_READ) }
		nioThread.wakeup()
	}

	fun writeMode() {
		nioThread.execute { if (key.isValid) key.interestOps(SelectionKey.OP_WRITE) }
		nioThread.wakeup()
	}

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

	override suspend fun read(buffer: ByteBuffer, timeout: Long): Int = if (timeout > 0) withTimeout(timeout) {
		try {
			read(buffer)
		} catch (e: TimeoutCancellationException) {
			waitMode()
			throw e
		}
	} else read(buffer)

	override suspend fun write(buffer: ByteBuffer, timeout: Long): Int = if (timeout > 0) withTimeout(timeout) {
		try {
			write(buffer)
		} catch (e: TimeoutCancellationException) {
			waitMode()
			throw e
		}
	} else write(buffer)

	suspend fun recv(buffer: ByteBuffer, timeout: Long): Int = if (timeout > 0) withTimeout(timeout) {
		try {
			recv(buffer)
		} catch (e: TimeoutCancellationException) {
			waitMode()
			throw e
		}
	} else recv(buffer)

	suspend fun read(buffer: AdvanceByteBuffer, timeout: Long = 0): Int {
		return buffer.writeNioBuffer {
			read(it, timeout)
		}
	}

	suspend fun write(buffer: AdvanceByteBuffer, timeout: Long = 0): Int {
		return buffer.readNioBuffer {
			write(it, timeout)
		}
	}

	suspend fun recv(buffer: AdvanceByteBuffer, timeout: Long = 0): Int {
		return buffer.writeNioBuffer {
			recv(it, timeout)
		}
	}
}
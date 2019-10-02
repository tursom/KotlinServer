package cn.tursom.socket

import cn.tursom.socket.niothread.INioThread
import cn.tursom.utils.bytebuffer.AdvanceByteBuffer
import cn.tursom.utils.bytebuffer.readNioBuffer
import cn.tursom.utils.bytebuffer.writeNioBuffer
import java.net.SocketException
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel

interface IAsyncNioSocket : AsyncSocket {
	val channel: SocketChannel
	val key: SelectionKey
	val nioThread: INioThread

	fun waitMode() {
		if (Thread.currentThread() == nioThread.thread) {
			if (key.isValid) key.interestOps(SelectionKey.OP_WRITE)
		} else {
			nioThread.execute { if (key.isValid) key.interestOps(0) }
			nioThread.wakeup()
		}
	}

	fun readMode() {
		if (Thread.currentThread() == nioThread.thread) {
			if (key.isValid) key.interestOps(SelectionKey.OP_WRITE)
		} else {
			nioThread.execute { if (key.isValid) key.interestOps(SelectionKey.OP_READ) }
			nioThread.wakeup()
		}
	}

	fun writeMode() {
		if (Thread.currentThread() == nioThread.thread) {
			if (key.isValid) key.interestOps(SelectionKey.OP_WRITE)
		} else {
			nioThread.execute { if (key.isValid) key.interestOps(SelectionKey.OP_WRITE) }
			nioThread.wakeup()
		}
	}

	suspend fun read(buffer: ByteBuffer): Int
	suspend fun write(buffer: ByteBuffer): Int
	/**
	 * 如果通道已断开则会抛出异常
	 */
	suspend fun recv(buffer: ByteBuffer): Int {
		if (buffer.remaining() == 0) return 0
		val readSize = read(buffer)
		if (readSize < 0) {
			throw SocketException("channel closed")
		}
		return readSize
	}

	suspend fun recv(buffer: ByteBuffer, timeout: Long): Int {
		if (buffer.remaining() == 0) return 0
		val readSize = read(buffer, timeout)
		if (readSize < 0) {
			throw SocketException("channel closed")
		}
		return readSize
	}

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
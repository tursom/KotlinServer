package cn.tursom.socket

import cn.tursom.socket.AsyncSocket.Companion.defaultTimeout
import cn.tursom.utils.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.InterruptedByTimeoutException
import java.util.concurrent.TimeUnit

class AsyncCachedSocket(socketChannel: AsynchronousSocketChannel, readBuffer: ByteBuffer, val writeBuffer: ByteBuffer) : AsyncSocket(socketChannel) {
	val readBuffer = AdvanceByteBuffer(readBuffer)
	
	constructor(socketChannel: AsynchronousSocketChannel) : this(socketChannel, ByteBuffer.allocate(1024), ByteBuffer.allocate(8))
	
	suspend fun write(timeout: Long = defaultTimeout, timeUnit: TimeUnit = TimeUnit.MILLISECONDS): Int {
		return write(writeBuffer, timeout, timeUnit)
	}
	
	suspend fun read(timeout: Long = defaultTimeout, timeUnit: TimeUnit = TimeUnit.MILLISECONDS): Int {
		return read(readBuffer.buffer, timeout, timeUnit)
	}
}

suspend inline fun AsyncCachedSocket.recv(
	readTimeout: Long = 100L,
	firstTimeout: Long = defaultTimeout
): ByteArray {
	val byteStream = ByteArrayOutputStream()
	readBuffer.reset(byteStream)
	
	try {
		read(firstTimeout)
		readBuffer.reset(byteStream)
		
		while (read(readTimeout) > 0) {
			readBuffer.reset(byteStream)
		}
	} catch (e: SocketTimeoutException) {
	} catch (e: InterruptedByTimeoutException) {
	}
	
	return byteStream.toByteArray()
}

suspend inline fun AsyncCachedSocket.recvStr(
	readTimeout: Long = 100L,
	firstTimeout: Long = defaultTimeout
) = String(recv(readTimeout, firstTimeout))

suspend inline fun AsyncCachedSocket.recvInt(
	readTimeout: Long = 100L
): Int {
	while (readBuffer.readSize < 4) read(readTimeout)
	return readBuffer.getInt()
}

suspend inline fun AsyncCachedSocket.recvLong(
	readTimeout: Long = 100L
): Long {
	while (readBuffer.readSize < 8) read(readTimeout)
	return readBuffer.getLong()
}

@Suppress("UNCHECKED_CAST")
suspend inline fun <T> AsyncCachedSocket.recvObject(
	readTimeout: Long = 100L,
	firstTimeout: Long = defaultTimeout
): T? {
	return unSerialize(recv(readTimeout, firstTimeout)) as T?
}

suspend inline fun AsyncCachedSocket.send(message: Int) {
	writeBuffer.clear()
	writeBuffer.array().push(message, writeBuffer.arrayOffset())
	write()
}

suspend inline fun AsyncCachedSocket.send(message: Long) {
	writeBuffer.clear()
	writeBuffer.array().push(message, writeBuffer.arrayOffset())
	write()
}

inline fun <T> AsyncCachedSocket.use(crossinline block: suspend AsyncCachedSocket.() -> T): T {
	var exception: Throwable? = null
	try {
		return runBlocking { block() }
	} catch (e: Throwable) {
		exception = e
		throw e
	} finally {
		when (exception) {
			null -> close()
			else -> try {
				close()
			} catch (closeException: Throwable) {
				// cause.addSuppressed(closeException) // ignored here
			}
		}
	}
}

inline infix fun AsyncCachedSocket.useCachedNonBlock(crossinline block: suspend AsyncCachedSocket.() -> Unit) =
	GlobalScope.launch {
		try {
			block()
		} finally {
			try {
				close()
			} catch (closeException: Throwable) {
				// cause.addSuppressed(closeException) // ignored here
			}
		}
	}


suspend inline infix operator fun <T> AsyncCachedSocket.invoke(
	@Suppress("REDUNDANT_INLINE_SUSPEND_FUNCTION_TYPE") block: suspend AsyncCachedSocket.() -> T
): T {
	var exception: Throwable? = null
	try {
		return block()
	} catch (e: Throwable) {
		exception = e
		throw e
	} finally {
		when (exception) {
			null -> close()
			else -> try {
				close()
			} catch (closeException: Throwable) {
				// cause.addSuppressed(closeException) // ignored here
			}
		}
	}
}

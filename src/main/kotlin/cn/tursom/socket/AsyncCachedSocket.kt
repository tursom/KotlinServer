package cn.tursom.socket

import cn.tursom.socket.AsyncSocket.Companion.defaultTimeout
import cn.tursom.utils.*
import cn.tursom.utils.bytebuffer.AdvanceByteBuffer
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream
import java.io.OutputStream
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
	outputStream: OutputStream,
	readTimeout: Long = 100L,
	firstTimeout: Long = defaultTimeout
) {
	readBuffer.reset(outputStream)
	
	try {
		read(firstTimeout)
		readBuffer.reset(outputStream)
		
		while (read(readTimeout) > 0) {
			readBuffer.reset(outputStream)
		}
	} catch (e: SocketTimeoutException) {
	} catch (e: InterruptedByTimeoutException) {
	}
}

suspend inline fun AsyncCachedSocket.recv(
	readTimeout: Long = 100L,
	firstTimeout: Long = defaultTimeout
): ByteArray {
	val byteStream = ByteArrayOutputStream()
	recv(byteStream, readTimeout, firstTimeout)
	return byteStream.toByteArray()
}

suspend inline fun AsyncCachedSocket.recvStr(
	charset: String = "utf-8",
	readTimeout: Long = 100L,
	firstTimeout: Long = defaultTimeout
): String {
	val byteStream = ByteArrayOutputStream()
	recv(byteStream, readTimeout, firstTimeout)
	return byteStream.toString(charset)
}

suspend inline fun AsyncCachedSocket.recvChar(
	readTimeout: Long = 100L
): Char {
	readBuffer.requireAvailableSize(2)
	while (readBuffer.readSize < 4) read(readTimeout)
	return readBuffer.getChar()
}

suspend inline fun AsyncCachedSocket.recvShort(
	readTimeout: Long = 100L
): Short {
	readBuffer.requireAvailableSize(2)
	while (readBuffer.readSize < 8) read(readTimeout)
	return readBuffer.getShort()
}

suspend inline fun AsyncCachedSocket.recvInt(
	readTimeout: Long = 100L
): Int {
	readBuffer.requireAvailableSize(4)
	while (readBuffer.readSize < 4) read(readTimeout)
	return readBuffer.getInt()
}

suspend inline fun AsyncCachedSocket.recvLong(
	readTimeout: Long = 100L
): Long {
	readBuffer.requireAvailableSize(8)
	while (readBuffer.readSize < 8) read(readTimeout)
	return readBuffer.getLong()
}

suspend inline fun AsyncCachedSocket.recvFloat(
	readTimeout: Long = 100L
): Float {
	readBuffer.requireAvailableSize(4)
	while (readBuffer.readSize < 4) read(readTimeout)
	return readBuffer.getFloat()
}

suspend inline fun AsyncCachedSocket.recvDouble(
	readTimeout: Long = 100L
): Double {
	readBuffer.requireAvailableSize(8)
	while (readBuffer.readSize < 8) read(readTimeout)
	return readBuffer.getDouble()
}

@Suppress("UNCHECKED_CAST")
suspend inline fun <T> AsyncCachedSocket.unSerializeObject(
	readTimeout: Long = 100L,
	firstTimeout: Long = defaultTimeout
): T? {
	val byteArrayOutputStream = ByteArrayOutputStream()
	recv(byteArrayOutputStream, readTimeout, firstTimeout)
	return unSerialize(byteArrayOutputStream.buf, 0, byteArrayOutputStream.count) as T?
}

suspend inline fun AsyncCachedSocket.send(message: Int) {
	writeBuffer.clear()
	writeBuffer.array().put(message, writeBuffer.arrayOffset())
	writeBuffer.limit(4)
	write()
}

suspend inline fun AsyncCachedSocket.send(message: Long) {
	writeBuffer.clear()
	writeBuffer.array().put(message, writeBuffer.arrayOffset())
	writeBuffer.limit(8)
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

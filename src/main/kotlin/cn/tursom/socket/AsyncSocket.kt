package cn.tursom.socket

import cn.tursom.utils.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.nio.channels.InterruptedByTimeoutException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

open class AsyncSocket(private val socketChannel: AsynchronousSocketChannel) : Closeable {
	suspend fun write(buffer: ByteBuffer, timeout: Long = defaultTimeout, timeUnit: TimeUnit = TimeUnit.MILLISECONDS): Int {
		return suspendCoroutine { cont ->
			this.socketChannel.write(buffer, timeout, timeUnit, cont, awaitHandler)
		}
	}
	
	suspend fun read(buffer: ByteBuffer, timeout: Long = defaultTimeout, timeUnit: TimeUnit = TimeUnit.MILLISECONDS): Int {
		return suspendCoroutine { cont ->
			this.socketChannel.read(buffer, timeout, timeUnit, cont, awaitHandler)
		}
	}
	
	override fun close() {
		socketChannel.close()
	}
	
	suspend inline infix operator fun <T> invoke(@Suppress("REDUNDANT_INLINE_SUSPEND_FUNCTION_TYPE") block: suspend AsyncSocket.() -> T): T {
		@Suppress("ConvertTryFinallyToUseCall")
		try {
			return this.block()
		} finally {
			close()
		}
	}
	
	companion object {
		const val defaultTimeout = 60_000L
		
		@JvmStatic
		private val awaitHandler =
			object : CompletionHandler<Int, Continuation<Int>> {
				override fun completed(result: Int, attachment: Continuation<Int>) {
					attachment.resume(result)
				}
				
				override fun failed(exc: Throwable, attachment: Continuation<Int>) {
					attachment.resumeWithException(exc)
				}
			}
	}
}

@Suppress("unused")
suspend fun AsyncSocket.write(message: String, timeout: Long = 0L, timeUnit: TimeUnit = TimeUnit.MILLISECONDS) =
	write(ByteBuffer.wrap(message.toByteArray()), timeout, timeUnit)


@Suppress("unused")
suspend fun AsyncSocket.recvStr(buffer: ByteBuffer, timeout: Long = 0L, timeUnit: TimeUnit = TimeUnit.MILLISECONDS): String {
	//buffer.clear()
	read(buffer, timeout, timeUnit)
	return String(buffer.array(), buffer.arrayOffset(), buffer.position())
}


suspend fun AsyncSocket.recv(
	readTimeout: Long = 100L,
	firstTimeout: Long = AsyncSocket.defaultTimeout,
	buffer: ByteBuffer = ByteBuffer.allocate(1024)
): ByteArray {
	buffer.clear()
	val byteStream = ByteArrayOutputStream()
	
	try {
		read(buffer, firstTimeout)
		@Suppress("BlockingMethodInNonBlockingContext")
		byteStream.write(buffer.array(), buffer.arrayOffset(), buffer.position())
		buffer.clear()
		
		while (read(buffer, readTimeout) > 0) {
			@Suppress("BlockingMethodInNonBlockingContext")
			byteStream.write(buffer.array(), buffer.arrayOffset(), buffer.position())
			buffer.clear()
		}
	} catch (e: SocketTimeoutException) {
	} catch (e: InterruptedByTimeoutException) {
	}
	
	return byteStream.toByteArray()
}

suspend fun AsyncSocket.recvStr(
	readTimeout: Long = 100L,
	firstTimeout: Long = AsyncSocket.defaultTimeout
) = String(recv(readTimeout, firstTimeout))

suspend fun AsyncSocket.recvInt(
	readTimeout: Long = 100L,
	firstTimeout: Long = AsyncSocket.defaultTimeout,
	buffer: ByteBuffer = ByteBuffer.allocate(4)
): Int {
	buffer.clear().limit(4)
	var readSize = read(buffer, firstTimeout)
	while (readSize < 8) {
		readSize += read(buffer, readTimeout)
	}
	return buffer.array().toInt(buffer.arrayOffset())
}

suspend fun AsyncSocket.recvLong(
	readTimeout: Long = 100L,
	firstTimeout: Long = AsyncSocket.defaultTimeout,
	buffer: ByteBuffer = ByteBuffer.allocate(8)
): Long {
	buffer.clear().limit(8)
	var readSize = read(buffer, firstTimeout)
	while (readSize < 8) {
		readSize += read(buffer, readTimeout)
	}
	return buffer.array().toLong(buffer.arrayOffset())
}

@Suppress("UNCHECKED_CAST")
suspend fun <T> AsyncSocket.recvObject(
	readTimeout: Long = 100L,
	firstTimeout: Long = AsyncSocket.defaultTimeout
): T? {
	return unSerialize(recv(readTimeout, firstTimeout)) as T?
}

suspend fun AsyncSocket.send(message: ByteArray?) {
	write(ByteBuffer.wrap(message ?: return))
}

suspend fun AsyncSocket.send(message: String?) {
	send((message ?: return).toByteArray())
}

suspend fun AsyncSocket.send(message: Int) {
	val buffer = ByteArray(4)
	buffer.push(message)
	send(buffer)
}

suspend fun AsyncSocket.send(message: Long) {
	val buffer = ByteArray(8)
	buffer.push(message)
	send(buffer)
}

suspend fun AsyncSocket.sendObject(obj: Any?): Boolean {
	send(serialize(obj ?: return false) ?: return false)
	return true
}

fun <T> AsyncSocket.use(block: suspend AsyncSocket.() -> T): T {
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

inline fun AsyncSocket.useNonBlock(crossinline block: suspend AsyncSocket.() -> Unit) =
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

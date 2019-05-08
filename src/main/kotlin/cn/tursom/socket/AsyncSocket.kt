package cn.tursom.socket

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.util.concurrent.TimeUnit
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

open class AsyncSocket(private val socketChannel: AsynchronousSocketChannel) : Closeable {
//	private val channel = Channel<Throwable?>()

//	suspend fun send(buffer: ByteBuffer, timeout: Long = 60_000L, timeUnit: TimeUnit = TimeUnit.MILLISECONDS) {
//		this.socketChannel.write(buffer, timeout, timeUnit, channel, channelHandler)
//		channel.receive()?.let { throw it }
//	}
	
	suspend fun send(buffer: ByteBuffer, timeout: Long = 60_000L, timeUnit: TimeUnit = TimeUnit.MILLISECONDS) {
		suspendCoroutine<Throwable?> { cont ->
			this.socketChannel.write(buffer, timeout, timeUnit, cont, awaitHandler)
		}?.let { throw it }
	}


//	suspend fun recv(buffer: ByteBuffer, timeout: Long = 60_000L, timeUnit: TimeUnit = TimeUnit.MILLISECONDS) {
//		this.socketChannel.read(buffer, timeout, timeUnit, channel, channelHandler)
//		channel.receive()?.let { throw it }
//	}
	
	suspend fun recv(buffer: ByteBuffer, timeout: Long = 60_000L, timeUnit: TimeUnit = TimeUnit.MILLISECONDS) {
		suspendCoroutine<Throwable?> { cont ->
			this.socketChannel.read(buffer, timeout, timeUnit, cont, awaitHandler)
		}?.let { throw it }
	}
	
	override fun close() {
		socketChannel.close()
//		channel.close()
	}
	
	fun <T> use(block: suspend AsyncSocket.() -> T): T {
		var exception: Throwable? = null
		try {
			return runBlocking { this@AsyncSocket.block() }
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
	
	fun useNonBlock(block: suspend AsyncSocket.() -> Unit): Job {
		return GlobalScope.launch {
			try {
				this@AsyncSocket.block()
			} finally {
				try {
					close()
				} catch (closeException: Throwable) {
					// cause.addSuppressed(closeException) // ignored here
				}
			}
		}
	}
	
	companion object {
//		@JvmStatic
//		private val channelHandler =
//			object : CompletionHandler<Int, Channel<Throwable?>> {
//				override fun completed(result: Int?, attachment: Channel<Throwable?>?) {
//					GlobalScope.launch {
//						attachment?.send(null)
//					}
//				}
//
//				override fun failed(exc: Throwable?, attachment: Channel<Throwable?>?) {
//					GlobalScope.launch {
//						attachment?.send(exc)
//					}
//				}
//			}
		
		@JvmStatic
		private val awaitHandler =
			object : CompletionHandler<Int, Continuation<Throwable?>> {
				override fun completed(result: Int, attachment: Continuation<Throwable?>) {
					attachment.resume(null)
				}
				
				override fun failed(exc: Throwable, attachment: Continuation<Throwable?>) {
					attachment.resume(exc)
				}
			}
	}
}

@Suppress("unused")
suspend fun AsyncSocket.send(message: String, timeout: Long = 0L, timeUnit: TimeUnit = TimeUnit.MILLISECONDS) {
	send(ByteBuffer.wrap(message.toByteArray()), timeout, timeUnit)
}

@Suppress("unused")
suspend fun AsyncSocket.recvStr(buffer: ByteBuffer, timeout: Long = 0L, timeUnit: TimeUnit = TimeUnit.MILLISECONDS): String {
	buffer.clear()
	recv(buffer, timeout, timeUnit)
	return String(buffer.array(), 0, buffer.position())
}
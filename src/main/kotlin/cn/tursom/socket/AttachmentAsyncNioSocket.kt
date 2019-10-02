package cn.tursom.socket

import cn.tursom.socket.niothread.INioThread
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel
import java.util.concurrent.TimeoutException
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class AttachmentAsyncNioSocket(override val key: SelectionKey, override val nioThread: INioThread) : IAsyncNioSocket {
	override val channel = key.channel() as SocketChannel
	var attachment: Any?
		get() = (key.attachment() as NioAttachment).attachment
		set(value) {
			(key.attachment() as NioAttachment).attachment = value
		}

	init {
		key.attach(NioAttachment(null, nioSocketProtocol))
	}

	override suspend fun read(buffer: ByteBuffer): Int {
		if (buffer.remaining() == 0) return -1
		return try {
			suspendCoroutine {
				attachment = Context(buffer, it)
				readMode()
				key.selector().wakeup()
			}
		} catch (e: Exception) {
			waitMode()
			throw RuntimeException(e)
		}
	}

	override suspend fun write(buffer: ByteBuffer): Int {
		if (buffer.remaining() == 0) return -1
		return try {
			suspendCoroutine {
				attachment = Context(buffer, it)
				writeMode()
				key.selector().wakeup()
			}
		} catch (e: Exception) {
			waitMode()
			throw Exception(e)
		}
	}


	override suspend fun read(buffer: ByteBuffer, timeout: Long): Int {
		if (timeout <= 0) return read(buffer)
		if (buffer.remaining() == 0) return -1
		return try {
			var finished = false
			val result: Int = suspendCoroutine {
				GlobalScope.launch {
					delay(timeout)
					if (!finished) it.resumeWithException(TimeoutException())
				}
				attachment = Context(buffer, it)
				readMode()
				nioThread.wakeup()
			}
			finished = true
			result
		} catch (e: Exception) {
			waitMode()
			throw RuntimeException(e)
		}
	}

	override suspend fun write(buffer: ByteBuffer, timeout: Long): Int {
		if (timeout <= 0) return write(buffer)
		if (buffer.remaining() == 0) return -1
		return try {
			var finished = false
			val result: Int = suspendCoroutine {
				GlobalScope.launch {
					delay(timeout)
					if (!finished) it.resumeWithException(TimeoutException())
				}
				attachment = Context(buffer, it)
				writeMode()
				nioThread.wakeup()
			}
			finished = true
			result
		} catch (e: Exception) {
			waitMode()
			throw Exception(e)
		}
	}

	override fun close() {
		channel.close()
		key.cancel()
	}

	data class Context(val buffer: ByteBuffer, val cont: Continuation<Int>)

	companion object {
		@Suppress("DuplicatedCode")
		val nioSocketProtocol = object : INioProtocol {
			override fun handleRead(key: SelectionKey, nioThread: INioThread) {
				key.interestOps(0)
				val attachment = key.attachment() as NioAttachment
				val context = attachment.attachment as Context
				val channel = key.channel() as SocketChannel
				val readSize = channel.read(context.buffer)
				context.cont.resume(readSize)
				attachment.attachment = null
			}

			override fun handleWrite(key: SelectionKey, nioThread: INioThread) {
				key.interestOps(0)
				val attachment = key.attachment() as NioAttachment
				val context = attachment.attachment as Context
				val channel = key.channel() as SocketChannel
				val readSize = channel.write(context.buffer)
				context.cont.resume(readSize)
				attachment.attachment = null
			}

			override fun exceptionCause(key: SelectionKey, nioThread: INioThread, e: Throwable) {
				key.interestOps(0)
				val attachment = key.attachment() as NioAttachment
				val context = attachment.attachment as Context?
				if (context != null)
					context.cont.resumeWithException(e)
				else {
					key.cancel()
					key.channel().close()
					e.printStackTrace()
				}
			}
		}
	}
}
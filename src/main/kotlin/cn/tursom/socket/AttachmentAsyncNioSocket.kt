package cn.tursom.socket

import cn.tursom.socket.niothread.INioThread
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class AttachmentAsyncNioSocket(override val key: SelectionKey, val nioThread: INioThread) : IAsyncNioSocket {
	override val channel = key.channel() as SocketChannel
	var attachment: Any?
		get() = (key.attachment() as NioAttachment).attachment
		set(value) {
			(key.attachment() as NioAttachment).attachment = value
		}

	override suspend fun read(buffer: ByteBuffer): Int {
		return try {
			suspendCoroutine {
				key.attach(Context(buffer, it))
				nioThread.execute { if (key.isValid) key.interestOps(SelectionKey.OP_READ) }
				key.selector().wakeup()
			}
		} catch (e: Exception) {
			throw RuntimeException(e)
		}
	}

	override suspend fun write(buffer: ByteBuffer): Int {
		return try {
			suspendCoroutine {
				key.attach(Context(buffer, it))
				nioThread.execute { if (key.isValid) key.interestOps(SelectionKey.OP_WRITE) }
				key.selector().wakeup()
			}
		} catch (e: Exception) {
			throw Exception(e)
		}
	}

	override fun close() {
		channel.close()
		key.cancel()
	}

	data class Context(val buffer: ByteBuffer, val cont: Continuation<Int>)

	companion object {
		val nioSocketProtocol = object : INioProtocol {
			override fun handleAccept(key: SelectionKey, nioThread: INioThread) {}

			override fun handleRead(key: SelectionKey, nioThread: INioThread) {
				val attachment = key.attachment() as NioAttachment
				val context = attachment.attachment as Context
				val channel = key.channel() as SocketChannel
				val readSize = channel.read(context.buffer)
				key.interestOps(0)
				context.cont.resume(readSize)
				attachment.attachment = null
			}

			override fun handleWrite(key: SelectionKey, nioThread: INioThread) {
				val attachment = key.attachment() as NioAttachment
				val context = attachment.attachment as Context
				val channel = key.channel() as SocketChannel
				val readSize = channel.write(context.buffer)
				key.interestOps(0)
				context.cont.resume(readSize)
				attachment.attachment = null
			}

			override fun exceptionCause(key: SelectionKey, nioThread: INioThread, e: Throwable) {
				val attachment = key.attachment() as NioAttachment
				val context = attachment.attachment as Context
				context.cont.resumeWithException(e)
			}
		}
	}
}
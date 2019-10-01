package cn.tursom.socket

import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * 利用 SelectionKey 的 attachment 进行状态的传输
 * 导致该类无法利用 SelectionKey 的 attachment
 * 但是对于一般的应用而言是足够使用的
 */
class ProtocolAsyncNioSocket(override val key: SelectionKey) : IAsyncNioSocket {
	override val channel: SocketChannel = key.channel() as SocketChannel

	override suspend fun read(buffer: ByteBuffer): Int {
		key.interestOps(SelectionKey.OP_READ)
		return suspendCoroutine {
			key.attach(Context(buffer, it))
		}
	}

	override suspend fun write(buffer: ByteBuffer): Int {
		key.interestOps(SelectionKey.OP_WRITE)
		return suspendCoroutine {
			key.attach(Context(buffer, it))
		}
	}

	data class Context(val buffer: ByteBuffer, val cont: Continuation<Int>)

	companion object {
		val nioSocketProtocol = object : INioProtocol {
			override fun handleAccept(key: SelectionKey) {}

			override fun handleRead(key: SelectionKey) {
				val context = key.attachment() as Context
				val channel = key.channel() as SocketChannel
				val readSize = channel.read(context.buffer)
				key.interestOps(0)
				context.cont.resume(readSize)
			}

			override fun handleWrite(key: SelectionKey) {
				val context = key.attachment() as Context
				val channel = key.channel() as SocketChannel
				val readSize = channel.write(context.buffer)
				key.interestOps(0)
				context.cont.resume(readSize)
			}

			override fun exceptionCause(key: SelectionKey, e: Throwable) {
				val context = key.attachment() as Context
				context.cont.resumeWithException(e)
			}
		}
	}
}
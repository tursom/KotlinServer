package cn.tursom.socket

import cn.tursom.socket.niothread.INioThread
import cn.tursom.utils.timer.TimerTask
import cn.tursom.utils.timer.WheelTimer
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel
import java.util.concurrent.TimeoutException
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * 利用 SelectionKey 的 attachment 进行状态的传输
 * 导致该类无法利用 SelectionKey 的 attachment
 * 但是对于一般的应用而言是足够使用的
 */
class ProtocolAsyncNioSocket(override val key: SelectionKey, override val nioThread: INioThread) : IAsyncNioSocket {
	override val channel: SocketChannel = key.channel() as SocketChannel

	override suspend fun read(buffer: ByteBuffer): Int {
		if (buffer.remaining() == 0) return -1
		return try {
			suspendCoroutine {
				key.attach(Context(buffer, it))
				readMode()
				nioThread.wakeup()
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
				key.attach(Context(buffer, it))
				writeMode()
				nioThread.wakeup()
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
			var timeoutTask: TimerTask? = null
			val result: Int = suspendCoroutine {
				timeoutTask = timer.exec(timeout) {
					it.resumeWithException(TimeoutException())
				}
				key.attach(Context(buffer, it))
				readMode()
				nioThread.wakeup()
			}
			timeoutTask?.cancel()
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
			var timeoutTask: TimerTask? = null
			val result: Int = suspendCoroutine {
				timeoutTask = timer.exec(timeout) {
					it.resumeWithException(TimeoutException())
				}
				key.attach(Context(buffer, it))
				writeMode()
				nioThread.wakeup()
			}
			timeoutTask?.cancel()
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
		val nioSocketProtocol = object : INioProtocol {
			override fun handleConnect(key: SelectionKey, nioThread: INioThread) {}

			override fun handleRead(key: SelectionKey, nioThread: INioThread) {
				key.interestOps(0)
				val context = key.attachment() as Context
				val channel = key.channel() as SocketChannel
				val readSize = channel.read(context.buffer)
				context.cont.resume(readSize)
			}

			override fun handleWrite(key: SelectionKey, nioThread: INioThread) {
				key.interestOps(0)
				val context = key.attachment() as Context
				val channel = key.channel() as SocketChannel
				val readSize = channel.write(context.buffer)
				context.cont.resume(readSize)
			}

			override fun exceptionCause(key: SelectionKey, nioThread: INioThread, e: Throwable) {
				key.interestOps(0)
				val context = key.attachment() as Context?
				if (context != null)
					context.cont.resumeWithException(e)
				else {
					key.cancel()
					key.channel().close()
					e.printStackTrace()
				}
			}
		}

		//val timer = StaticWheelTimer.timer
		val timer = WheelTimer.timer
	}
}
package cn.tursom.socket.client

import cn.tursom.socket.AsyncNioSocket
import cn.tursom.socket.niothread.WorkerLoopNioThread
import cn.tursom.utils.timer.TimerTask
import java.net.InetSocketAddress
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel
import java.util.concurrent.TimeoutException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object AsyncNioClient {
	private const val TIMEOUT = 1000L
	private val protocol = AsyncNioSocket.nioSocketProtocol
	private val nioThread = WorkerLoopNioThread("nioClient") { nioThread ->
		val selector = nioThread.selector
		//logE("AsyncNioClient selector select")
		if (selector.select(TIMEOUT) != 0) {
			//logE("AsyncNioClient selector select successfully")
			val keyIter = selector.selectedKeys().iterator()
			while (keyIter.hasNext()) {
				val key = keyIter.next()
				keyIter.remove()
				try {
					when {
						!key.isValid -> {
						}
						key.isReadable -> {
							protocol.handleRead(key, nioThread)
						}
						key.isWritable -> {
							protocol.handleWrite(key, nioThread)
						}
						key.isConnectable -> {
							protocol.handleConnect(key, nioThread)
						}
					}
				} catch (e: Throwable) {
					try {
						protocol.exceptionCause(key, nioThread, e)
					} catch (e1: Throwable) {
						e.printStackTrace()
						e1.printStackTrace()
					}
				}
			}
		}
		//logE("AsyncNioClient selector select end")
	}

	@Suppress("DuplicatedCode")
	fun getConnection(host: String, port: Int): AsyncNioSocket {
		nioThread // 确保 nio thread 已经加载，防止虚断

		val selector = nioThread.selector
		val channel = SocketChannel.open()
		channel.connect(InetSocketAddress(host, port))
		channel.configureBlocking(false)
		val f = nioThread.submit<SelectionKey> {
			channel.register(selector, 0)
		}
		selector.wakeup()
		val key: SelectionKey = f.get()
		return AsyncNioSocket(key, nioThread)
	}

	@Suppress("DuplicatedCode")
	suspend fun getSuspendConnection(host: String, port: Int): AsyncNioSocket {
		nioThread // 确保 nio thread 已经加载，防止虚断

		val key: SelectionKey = suspendCoroutine { cont ->
			try {
				val channel = SocketChannel.open()
				channel.connect(InetSocketAddress(host, port))
				channel.configureBlocking(false)
				nioThread.submit {
					nioThread.register(channel, 0) { key ->
						cont.resume(key)
					}
				}
				nioThread.wakeup()
			} catch (e: Exception) {
				cont.resumeWithException(e)
			}
		}
		return AsyncNioSocket(key, nioThread)
	}

	@Suppress("DuplicatedCode")
	suspend fun getSuspendConnection(host: String, port: Int, timeout: Long): AsyncNioSocket {
		if (timeout <= 0) return getSuspendConnection(host, port)
		nioThread // 确保 nio thread 已经加载，防止虚断

		var timeoutTask: TimerTask? = null
		val key: SelectionKey = suspendCoroutine { cont ->
			val channel = SocketChannel.open()
			channel.connect(InetSocketAddress(host, port))
			channel.configureBlocking(false)
			timeoutTask = AsyncNioSocket.timer.exec(timeout) {
				channel.close()
				cont.resumeWithException(TimeoutException())
			}
			try {
				nioThread.register(channel, 0) { key ->
					cont.resume(key)
				}
				nioThread.wakeup()
			} catch (e: Exception) {
				cont.resumeWithException(e)
			}
		}
		timeoutTask?.cancel()
		return AsyncNioSocket(key, nioThread)
	}
}
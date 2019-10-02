package cn.tursom.socket.client

import cn.tursom.socket.ProtocolAsyncNioSocket
import cn.tursom.socket.niothread.WorkerLoopNioThread
import java.net.InetSocketAddress
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object ProtocolAsyncNioClient {
	private const val TIMEOUT = 3000L
	private val protocol = ProtocolAsyncNioSocket.nioSocketProtocol
	private val nioThread = WorkerLoopNioThread { nioThread ->
		val selector = nioThread.selector
		if (selector.select(TIMEOUT) != 0) {
			val keyIter = synchronized(selector) { selector.selectedKeys() }.iterator()
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
	}

	@Suppress("DuplicatedCode")
	fun getConnection(host: String, port: Int): ProtocolAsyncNioSocket {
		val selector = nioThread.selector
		val channel = SocketChannel.open()
		channel.connect(InetSocketAddress(host, port))
		channel.configureBlocking(false)
		val f = nioThread.submit<SelectionKey> {
			channel.register(selector, 0)
		}
		selector.wakeup()
		val key: SelectionKey = f.get()
		return ProtocolAsyncNioSocket(key, nioThread)
	}

	@Suppress("DuplicatedCode")
	suspend fun getSuspendConnection(host: String, port: Int): ProtocolAsyncNioSocket {
		val selector = nioThread.selector
		val key: SelectionKey = suspendCoroutine { cont ->
			nioThread.submit {
				val channel = SocketChannel.open()
				channel.connect(InetSocketAddress(host, port))
				channel.configureBlocking(false)
				channel.register(selector, 0)
				nioThread.register(channel, 0) { key ->
					cont.resume(key)
				}
			}
			selector.wakeup()
		}
		return ProtocolAsyncNioSocket(key, nioThread)
	}
}
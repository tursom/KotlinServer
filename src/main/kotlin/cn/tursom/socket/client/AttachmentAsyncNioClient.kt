package cn.tursom.socket.client

import cn.tursom.socket.AttachmentAsyncNioSocket
import cn.tursom.socket.niothread.ThreadPoolNioThread
import java.net.InetSocketAddress
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel

object AttachmentAsyncNioClient {
	private const val TIMEOUT = 3000L
	private val protocol = AttachmentAsyncNioSocket.nioSocketProtocol
	private val nioThread = ThreadPoolNioThread { nioThread ->
		val selector = nioThread.selector
		if (selector.select(TIMEOUT) != 0) {
			val keyIter = selector.selectedKeys().iterator()
			while (keyIter.hasNext()) {
				val key = keyIter.next()
				try {
					when {
						key.isAcceptable -> {
							protocol.handleConnect(key, nioThread)
						}
						key.isReadable -> {
							protocol.handleRead(key, nioThread)
						}
						key.isValid && key.isWritable -> {
							protocol.handleWrite(key, nioThread)
						}
					}
				} catch (e: Throwable) {
					try {
						protocol.exceptionCause(key, nioThread, e)
					} catch (e1: Throwable) {
						e.printStackTrace()
						e1.printStackTrace()
					}
				} finally {
					keyIter.remove()
				}
			}
		}
	}

	@Suppress("DuplicatedCode")
	fun getConnection(host: String, port: Int): AttachmentAsyncNioSocket {
		val selector = nioThread.selector
		val channel = SocketChannel.open()
		channel.connect(InetSocketAddress(host, port))
		channel.configureBlocking(false)
		val f = nioThread.submit<SelectionKey> {
			channel.register(selector, 0)
		}
		selector.wakeup()
		val key: SelectionKey = f.get()
		return AttachmentAsyncNioSocket(key, nioThread)
	}
}
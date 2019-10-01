package cn.tursom.socket.client

import cn.tursom.socket.ProtocolAsyncNioSocket
import java.net.InetSocketAddress
import java.nio.channels.Selector
import java.nio.channels.SocketChannel
import kotlin.concurrent.thread

object AsyncProtocolNioClient {
	private const val TIMEOUT = 3000L
	private val selector = Selector.open()
	private val protocol = ProtocolAsyncNioSocket.nioSocketProtocol

	fun getConnection(host: String, port: Int): ProtocolAsyncNioSocket {
		val channel = SocketChannel.open()
		channel.connect(InetSocketAddress(host, port))
		channel.configureBlocking(false)
		val key = channel.register(selector, 0)
		return ProtocolAsyncNioSocket(key)
	}

	init {
		thread {
			while (true) {
				if (selector.select(TIMEOUT) == 0) continue

				val keyIter = selector.selectedKeys().iterator()
				while (keyIter.hasNext()) {
					val key = keyIter.next()
					try {
						when {
							key.isAcceptable -> {
								protocol.handleAccept(key)
							}
							key.isReadable -> {
								protocol.handleRead(key)
							}
							key.isValid && key.isWritable -> {
								protocol.handleWrite(key)
							}
						}
					} catch (e: Throwable) {
						try {
							protocol.exceptionCause(key, e)
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
	}
}
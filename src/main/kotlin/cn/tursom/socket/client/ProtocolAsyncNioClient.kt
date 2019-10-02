package cn.tursom.socket.client

import cn.tursom.socket.ProtocolAsyncNioSocket
import cn.tursom.socket.niothread.ThreadPoolNioThread
import java.net.InetSocketAddress
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel

object ProtocolAsyncNioClient {
	private const val TIMEOUT = 3000L
	private val selector: Selector = Selector.open()
	private val protocol = ProtocolAsyncNioSocket.nioSocketProtocol
	private val nioThread = ThreadPoolNioThread()

	@Suppress("DuplicatedCode")
	fun getConnection(host: String, port: Int): ProtocolAsyncNioSocket {
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

	init {
		nioThread.execute(object : Runnable {
			override fun run() {
				if (selector.select(TIMEOUT) != 0) {
					val keyIter = synchronized(selector) { selector.selectedKeys() }.iterator()
					while (keyIter.hasNext()) {
						val key = keyIter.next()
						try {
							when {
								!key.isValid -> {
									System.err.println("$key is not valid")
								}
								key.isReadable -> {
									protocol.handleRead(key, nioThread)
								}
								key.isWritable -> {
									protocol.handleWrite(key, nioThread)
								}
								key.isConnectable -> {
									protocol.handleAccept(key, nioThread)
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
				nioThread.execute(this)
			}
		})
	}
}
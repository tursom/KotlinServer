package cn.tursom.socket.client

import cn.tursom.socket.AttachmentAsyncNioSocket
import java.net.InetSocketAddress
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel
import java.util.concurrent.Executors

object AttachmentAsyncNioClient {
	private const val TIMEOUT = 3000L
	private val selector = Selector.open()
	private val protocol = AttachmentAsyncNioSocket.nioSocketProtocol
	private val threadPool = Executors.newSingleThreadExecutor {
		val thread = Thread(it)
		thread.isDaemon = true
		thread
	}

	@Suppress("DuplicatedCode")
	fun getConnection(host: String, port: Int): AttachmentAsyncNioSocket {
		val channel = SocketChannel.open()
		channel.connect(InetSocketAddress(host, port))
		channel.configureBlocking(false)
		val f = threadPool.submit<SelectionKey> {
			channel.register(selector, 0)
		}
		selector.wakeup()
		val key: SelectionKey = f.get()
		return AttachmentAsyncNioSocket(key)
	}

	init {
		threadPool.execute(object : Runnable {
			override fun run() {
				if (selector.select(TIMEOUT) != 0) {

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

				threadPool.execute(this)
			}
		})
	}
}
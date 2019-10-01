package cn.tursom.socket.server.nio

import cn.tursom.socket.INioProtocol
import cn.tursom.socket.NioAttachment
import cn.tursom.socket.server.ISocketServer
import java.net.InetSocketAddress
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.util.concurrent.ConcurrentLinkedDeque

class AttachmentNioServer(val port: Int, var protocol: INioProtocol) : ISocketServer {
	private val listenChannel = ServerSocketChannel.open()
	private val selectorList = ConcurrentLinkedDeque<Selector>()

	init {
		listenChannel.socket().bind(InetSocketAddress(port))
		listenChannel.configureBlocking(false)
	}

	override fun run() {
		val selector = Selector.open()
		selectorList.add(selector)
		listenChannel.register(selector, SelectionKey.OP_ACCEPT)
		while (true) {
			if (selector.select(TIMEOUT) == 0) continue

			val keyIter = selector.selectedKeys().iterator()
			while (keyIter.hasNext()) {
				val key = keyIter.next()
				try {
					@Suppress("UNCHECKED_CAST")
					when {
						key.isAcceptable -> {
							val serverChannel = key.channel() as ServerSocketChannel
							val channel = serverChannel.accept()
							channel.configureBlocking(false)
							key.interestOps(0)
							val socketKey = channel.register(selector, 0)
							socketKey.attach(NioAttachment(null, protocol))
							protocol.handleAccept(socketKey)
						}
						key.isReadable -> {
							(key.attachment() as NioAttachment).protocol.handleAccept(key)
						}
						key.isValid && key.isWritable -> {
							(key.attachment() as NioAttachment).protocol.handleAccept(key)
						}
					}
				} catch (e: Exception) {
					try {
						(key.attachment() as NioAttachment).protocol.exceptionCause(key, e)
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

	override fun close() {
		listenChannel.close()
		selectorList.forEach { selector -> selector.close() }
	}

	companion object {
		private const val TIMEOUT = 3000L
	}
}


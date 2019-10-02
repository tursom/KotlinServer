package cn.tursom.socket.server.nio

import cn.tursom.socket.INioProtocol
import cn.tursom.socket.NioAttachment
import cn.tursom.socket.niothread.INioThread
import cn.tursom.socket.niothread.ThreadPoolNioThread
import cn.tursom.socket.server.ISocketServer
import java.net.InetSocketAddress
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.util.concurrent.ConcurrentLinkedDeque

@Suppress("MemberVisibilityCanBePrivate")
class AttachmentNioServer(
	val port: Int,
	var protocol: INioProtocol,
	val nioThreadGenerator: (threadName: String, workLoop: (thread: INioThread) -> Unit) -> INioThread = { name, workLoop ->
		ThreadPoolNioThread(name, workLoop = workLoop)
	}
) : ISocketServer {
	private val listenChannel = ServerSocketChannel.open()
	private val selectorList = ConcurrentLinkedDeque<Selector>()

	init {
		listenChannel.socket().bind(InetSocketAddress(port))
		listenChannel.configureBlocking(false)
	}

	override fun run() {
		val nioThread: INioThread = nioThreadGenerator("nio worker") { nioThread ->
			val selector = nioThread.selector
			if (selector.isOpen) {
				if (selector.select(TIMEOUT) != 0) {
					val keyIter = selector.selectedKeys().iterator()
					while (keyIter.hasNext()) run whileBlock@{
						val key = keyIter.next()
						keyIter.remove()
						try {
							when {
								key.isAcceptable -> {
									val serverChannel = key.channel() as ServerSocketChannel
									val channel = serverChannel.accept() ?: return@whileBlock
									channel.configureBlocking(false)
									nioThread.register(channel) {
										it.attach(NioAttachment(null, protocol))
										protocol.handleConnect(it, nioThread)
									}
								}
								key.isReadable -> {
									(key.attachment() as NioAttachment).protocol.handleConnect(key, nioThread)
								}
								key.isValid && key.isWritable -> {
									(key.attachment() as NioAttachment).protocol.handleConnect(key, nioThread)
								}
							}
						} catch (e: Throwable) {
							try {
								protocol.exceptionCause(key, nioThread, e)
							} catch (e1: Throwable) {
								e.printStackTrace()
								e1.printStackTrace()
								key.cancel()
								key.channel().close()
							}
						}
					}
				}
			}
		}
		val selector = Selector.open()
		selectorList.add(selector)
		listenChannel.register(selector, SelectionKey.OP_ACCEPT)
		nioThread.wakeup()
	}

	override fun close() {
		listenChannel.close()
		selectorList.forEach { selector ->
			selector.close()
			selector.wakeup()
		}
	}

	companion object {
		private const val TIMEOUT = 3000L
	}
}


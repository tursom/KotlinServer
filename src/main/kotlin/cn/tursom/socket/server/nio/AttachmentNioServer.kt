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

class AttachmentNioServer(
	val port: Int,
	var protocol: INioProtocol,
	val nioThread: Class<*> = ThreadPoolNioThread::class.java
) : ISocketServer {
	private val listenChannel = ServerSocketChannel.open()
	private val selectorList = ConcurrentLinkedDeque<Selector>()

	init {
		listenChannel.socket().bind(InetSocketAddress(port))
		listenChannel.configureBlocking(false)
	}

	override fun run() {
		val nioThread: INioThread = nioThread.newInstance() as INioThread
		val selector = Selector.open()
		selectorList.add(selector)
		listenChannel.register(selector, SelectionKey.OP_ACCEPT)
		nioThread.execute(Handler(selector, protocol, nioThread))
	}

	override fun close() {
		listenChannel.close()
		selectorList.forEach { selector ->
			selector.close()
			selector.wakeup()
		}
	}


	@Suppress("MemberVisibilityCanBePrivate")
	class Handler(val selector: Selector, val protocol: INioProtocol, val nioThread: INioThread) : Runnable {
		override fun run() {
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
									val socketKey = channel.register(selector, 0)
									socketKey.attach(NioAttachment(null, protocol))
									protocol.handleAccept(socketKey, nioThread)
								}
								key.isReadable -> {
									(key.attachment() as NioAttachment).protocol.handleAccept(key, nioThread)
								}
								key.isValid && key.isWritable -> {
									(key.attachment() as NioAttachment).protocol.handleAccept(key, nioThread)
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
				nioThread.execute(this)
			}
		}
	}

	companion object {
		private const val TIMEOUT = 3000L
	}
}


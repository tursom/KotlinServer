package cn.tursom.socket.server.nio

import cn.tursom.socket.INioProtocol
import cn.tursom.socket.niothread.INioThread
import cn.tursom.socket.niothread.IWorkerGroup
import cn.tursom.socket.niothread.SingleThreadNioThread
import cn.tursom.socket.niothread.ThreadPoolWorkerGroup
import cn.tursom.socket.server.ISocketServer
import java.net.InetSocketAddress
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.util.concurrent.LinkedBlockingDeque

@Suppress("MemberVisibilityCanBePrivate")
class ProtocolGroupNioServer(
	val port: Int,
	val threads: Int = Runtime.getRuntime().availableProcessors(),
	private val protocol: INioProtocol,
	val nioThreadGenerator: (
		threadName: String,
		threads: Int,
		worker: (thread: INioThread) -> Unit
	) -> IWorkerGroup = { name, _, worker ->
		ThreadPoolWorkerGroup(threads, name, worker)
	}
) : ISocketServer {
	private val listenChannel = ServerSocketChannel.open()
	private val listenThreads = LinkedBlockingDeque<INioThread>()
	private val workerGroupList = LinkedBlockingDeque<IWorkerGroup>()

	init {
		listenChannel.socket().bind(InetSocketAddress(port))
		listenChannel.configureBlocking(false)
	}

	override fun run() {
		val workerGroup = nioThreadGenerator("nioWorkerGroup", threads) { nioThread ->
			val selector = nioThread.selector
			if (selector.isOpen) {
				forEachKey(selector) { key ->
					try {
						when {
							key.isAcceptable -> {
								val serverChannel = key.channel() as ServerSocketChannel
								val channel = serverChannel.accept() ?: return@forEachKey
								channel.configureBlocking(false)
								nioThread.register(channel) {
									protocol.handleAccept(it, nioThread)
								}
							}
							key.isReadable -> {
								protocol.handleRead(key, nioThread)
							}
							key.isWritable -> {
								protocol.handleWrite(key, nioThread)
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
		workerGroupList.add(workerGroup)

		val nioThread = SingleThreadNioThread("nioAccepter")
		listenThreads.add(nioThread)
		listenChannel.register(nioThread.selector, SelectionKey.OP_ACCEPT)
		nioThread.execute(AcceptHandler(protocol, nioThread, workerGroup))
	}

	override fun close() {
		listenChannel.close()
		listenThreads.forEach { it.close() }
		workerGroupList.forEach { it.close() }
	}

	class AcceptHandler(val protocol: INioProtocol, val nioThread: INioThread, val nioGroup: IWorkerGroup) : Runnable {
		val selector = nioThread.selector
		override fun run() {
			if (selector.isOpen) {
				forEachKey(selector) { key ->
					try {
						when {
							key.isAcceptable -> {
								val serverChannel = key.channel() as ServerSocketChannel
								val channel = serverChannel.accept() ?: return@forEachKey
								channel.configureBlocking(false)
								nioGroup.register(channel) { (key, thread) ->
									protocol.handleAccept(key, thread)
								}
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
					nioThread.execute(this)
				}
			}
		}
	}

	companion object {
		const val TIMEOUT = 3000L

		inline fun forEachKey(selector: Selector, action: (key: SelectionKey) -> Unit) {
			if (selector.select(TIMEOUT) != 0) {
				val keyIter = selector.selectedKeys().iterator()
				while (keyIter.hasNext()) run whileBlock@{
					val key = keyIter.next()
					keyIter.remove()
					action(key)
				}
			}
		}
	}
}
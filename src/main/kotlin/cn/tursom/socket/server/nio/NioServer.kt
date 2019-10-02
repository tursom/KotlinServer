package cn.tursom.socket.server.nio

import cn.tursom.socket.INioProtocol
import cn.tursom.socket.niothread.INioThread
import cn.tursom.socket.niothread.WorkerLoopNioThread
import cn.tursom.socket.server.ISocketServer
import java.net.InetSocketAddress
import java.nio.channels.SelectionKey
import java.nio.channels.ServerSocketChannel
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * 工作在单线程上的 Nio 服务器。
 */
class NioServer(
	val port: Int,
	private val protocol: INioProtocol,
	backLog: Int = 50,
	val nioThreadGenerator: (threadName: String, workLoop: (thread: INioThread) -> Unit) -> INioThread = { name, workLoop ->
		WorkerLoopNioThread(name, workLoop = workLoop)
	}
) : ISocketServer {
	private val listenChannel = ServerSocketChannel.open()
	private val threadList = ConcurrentLinkedDeque<INioThread>()

	init {
		listenChannel.socket().bind(InetSocketAddress(port), backLog)
		listenChannel.configureBlocking(false)
	}

	override fun run() {
		val nioThread = nioThreadGenerator("nio worker", LoopHandler(protocol)::handle)
		nioThread.register(listenChannel, SelectionKey.OP_ACCEPT) {}
		threadList.add(nioThread)
	}

	override fun close() {
		listenChannel.close()
		threadList.forEach {
			it.close()
		}
	}

	class LoopHandler(val protocol: INioProtocol) {
		fun handle(nioThread: INioThread) {
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
									nioThread.register(channel, 0) {
										protocol.handleConnect(it, nioThread)
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
		}
	}

	companion object {
		private const val TIMEOUT = 1000L
	}
}

//fun main() {
//	val port = 12345
//	//val server = NioServer(port, object : NioProtocol {
//	//	override fun handleAccept(key: SelectionKey, selector: Selector) {
//	//		val serverChannel = key.channel() as ServerSocketChannel
//	//		val channel = serverChannel.accept()
//	//		channel.configureBlocking(false)
//	//		channel.register(selector, SelectionKey.OP_READ)
//	//	}
//	//
//	//	override fun handleRead(key: SelectionKey, selector: Selector) {
//	//		val channel = key.channel() as SocketChannel
//	//		val buffer = ByteBuffer.allocate(1024)
//	//		channel.read(buffer)
//	//		key.attach(buffer)
//	//		key.interestOps(SelectionKey.OP_WRITE)
//	//		buffer.flip()
//	//		println("recv from ${channel.remoteAddress}: ${String(buffer.array(), 0, buffer.limit(), Charsets.UTF_8)}")
//	//	}
//	//
//	//	override fun handleWrite(key: SelectionKey, selector: Selector) {
//	//		val channel = key.channel() as SocketChannel
//	//		val buffer = key.attachment() as ByteBuffer
//	//		println("write to ${channel.remoteAddress}: ${String(buffer.array(), buffer.position(), buffer.limit(), Charsets.UTF_8)}")
//	//		while (buffer.remaining() != 0) {
//	//			channel.write(buffer)
//	//		}
//	//		channel.register(selector, SelectionKey.OP_READ)
//	//	}
//	//})
//	val server = AsyncNioServer(port) {
//		val buffer = ByteBuffer.allocate(1024)
//		read(buffer)
//		println("recv from ${channel.remoteAddress}: ${String(buffer.array(), 0, buffer.position(), Charsets.UTF_8)}")
//		buffer.flip()
//		write(buffer)
//	}
//	Thread(server).start()
//	runBlocking {
//		val client = AsyncClient.connect("127.0.0.1", port)
//		val sendBuffer = ByteBuffer.wrap("hello!".toByteArray())
//		client.write(sendBuffer)
//		val readBuffer = ByteBuffer.allocate(1024)
//		client.read(readBuffer)
//		println("client recv from server: ${String(readBuffer.array(), 0, readBuffer.position(), Charsets.UTF_8)}")
//	}
//}
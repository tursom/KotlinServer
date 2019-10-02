package cn.tursom.socket.server.nio

import cn.tursom.socket.INioProtocol
import cn.tursom.socket.ProtocolAsyncNioSocket
import cn.tursom.socket.niothread.INioThread
import cn.tursom.socket.server.ISocketServer
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.nio.channels.SelectionKey

/**
 * 只有一个工作线程的协程套接字服务器
 * 不过因为结构更加简单，所以性能实际上比多线程的 ProtocolGroupAsyncNioServer 高
 * 而且协程是天生多线程，并不需要太多的接受线程来处理，所以一般只需要用本服务器即可
 */
class ProtocolAsyncNioServer(
	val port: Int,
	val handler: suspend ProtocolAsyncNioSocket.() -> Unit
) : ISocketServer by ProtocolNioServer(port, object : INioProtocol by ProtocolAsyncNioSocket.nioSocketProtocol {
	override fun handleConnect(key: SelectionKey, nioThread: INioThread) {
		GlobalScope.launch {
			val socket = ProtocolAsyncNioSocket(key, nioThread)
			try {
				socket.handler()
			} catch (e: Exception) {
				e.printStackTrace()
			} finally {
				try {
					@Suppress("BlockingMethodInNonBlockingContext")
					socket.channel.close()
					socket.key.cancel()
				} catch (e: Exception) {
				}
			}
		}
	}
}) {
	/**
	 * 次要构造方法，为使用Spring的同学们准备的
	 */
	constructor(
		port: Int,
		handler: Handler
	) : this(port, {
		handler.handle(this)
	})

	interface Handler {
		fun handle(socket: ProtocolAsyncNioSocket)
	}
}

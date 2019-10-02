package cn.tursom.socket.server.nio

import cn.tursom.socket.INioProtocol
import cn.tursom.socket.ProtocolAsyncNioSocket
import cn.tursom.socket.niothread.INioThread
import cn.tursom.socket.server.ISocketServer
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.nio.channels.SelectionKey

class ProtocolAsyncNioServer(val port: Int, val handler: suspend ProtocolAsyncNioSocket.() -> Unit)
	: ISocketServer by ProtocolNioServer(port, object : INioProtocol by ProtocolAsyncNioSocket.nioSocketProtocol {
	override fun handleAccept(key: SelectionKey, nioThread: INioThread) {
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
	constructor(port: Int, handler: Handler) : this(port, {
		handler.handle(this)
	})

	interface Handler {
		fun handle(socket: ProtocolAsyncNioSocket)
	}
}
package cn.tursom.socket.server.nio

import cn.tursom.socket.AttachmentAsyncNioSocket
import cn.tursom.socket.INioProtocol
import cn.tursom.socket.niothread.INioThread
import cn.tursom.socket.niothread.ThreadPoolNioThread
import cn.tursom.socket.server.ISocketServer
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.nio.channels.SelectionKey

class AttachmentAsyncNioServer(
	val port: Int,
	val nioThread: Class<*> = ThreadPoolNioThread::class.java,
	val handler: suspend AttachmentAsyncNioSocket.() -> Unit)
	: ISocketServer by AttachmentNioServer(port, object : INioProtocol by AttachmentAsyncNioSocket.nioSocketProtocol {
	override fun handleAccept(key: SelectionKey, nioThread: INioThread) {
		GlobalScope.launch {
			val socket = AttachmentAsyncNioSocket(key, nioThread)
			socket.handler()
			try {
				@Suppress("BlockingMethodInNonBlockingContext")
				socket.channel.close()
				socket.key.cancel()
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}
	}
}, nioThread) {
	/**
	 * 次要构造方法，为使用Spring的同学们准备的
	 */
	constructor(
		port: Int,
		nioThread: Class<*> = ThreadPoolNioThread::class.java,
		handler: Handler
	) : this(port, nioThread, {
		handler.handle(this)
	})

	interface Handler {
		suspend fun handle(socket: AttachmentAsyncNioSocket)
	}
}
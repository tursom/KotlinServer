package cn.tursom.socket.server.nio

import cn.tursom.socket.INioProtocol
import cn.tursom.socket.ProtocolAsyncNioSocket
import cn.tursom.socket.niothread.INioThread
import cn.tursom.socket.server.ISocketServer
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.nio.channels.SelectionKey

/**
 * 有多个工作线程的协程套接字服务器
 * 不过因为结构复杂，所以性能实际上比多线程的 ProtocolAsyncNioServer 低
 */
@Suppress("MemberVisibilityCanBePrivate")
class ProtocolGroupAsyncNioServer(
	val port: Int,
	val threads: Int = Runtime.getRuntime().availableProcessors(),
	val handler: suspend ProtocolAsyncNioSocket.() -> Unit
) : ISocketServer by ProtocolGroupNioServer(port, threads, object : INioProtocol by ProtocolAsyncNioSocket.nioSocketProtocol {
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
})
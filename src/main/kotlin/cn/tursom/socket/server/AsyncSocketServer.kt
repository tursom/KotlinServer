package cn.tursom.socket.server

import cn.tursom.socket.AsyncSocket
import java.io.Closeable
import java.net.InetSocketAddress
import java.nio.channels.AsynchronousCloseException
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler

class AsyncSocketServer(
	port: Int,
	private val handler: suspend AsyncSocket.() -> Unit
) : Runnable, Closeable {
	private val server = AsynchronousServerSocketChannel
		.open()
		.bind(InetSocketAddress("0.0.0.0", port))
	
	
	override fun run() {
		server.accept(0, object : CompletionHandler<AsynchronousSocketChannel, Int> {
			override fun completed(result: AsynchronousSocketChannel?, attachment: Int) {
				try {
					server.accept(attachment + 1, this)
				} catch (e: Throwable) {
				}
				result ?: return
				AsyncSocket(result).useNonBlock {
					handler()
				}
			}
			
			override fun failed(exc: Throwable?, attachment: Int?) {
				when (exc) {
					is AsynchronousCloseException -> {
					}
					else -> exc?.printStackTrace()
				}
			}
		})
	}
	
	override fun close() {
		server.close()
	}
}
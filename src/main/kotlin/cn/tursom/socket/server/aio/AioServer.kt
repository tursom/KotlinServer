package cn.tursom.socket.server.aio

import cn.tursom.socket.AioSocket
import java.io.Closeable
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler

class AioServer(
	port: Int,
	val bufferSize: Int = 4096,
	val handler: AioSocket.() -> Unit
) : Runnable, Closeable {
	
	private val server = AsynchronousServerSocketChannel
		.open()
		.bind(InetSocketAddress("0.0.0.0", port))
	
	override fun run() {
		while (true) {
			server.accept(0, object : CompletionHandler<AsynchronousSocketChannel, Int> {
				override fun completed(result: AsynchronousSocketChannel?, attachment: Int?) {
					val socket = AioSocket(result!!, ByteBuffer.allocate(bufferSize))
					socket.handler()
					socket.run()
				}
				
				override fun failed(exc: Throwable?, attachment: Int?) {
					exc?.printStackTrace()
				}
			})
		}
	}
	
	override fun close() {
		server.close()
	}
}
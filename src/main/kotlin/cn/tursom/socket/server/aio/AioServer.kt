package cn.tursom.socket.server.aio

import cn.tursom.socket.AioSocket
import java.io.Closeable
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.*

class AioServer(
	port: Int,
	val bufferSize: Int = 4096,
	val handler: AioSocket.() -> Unit
) : Runnable, Closeable {
	
	private val server = AsynchronousServerSocketChannel
		.open()
		.bind(InetSocketAddress("0.0.0.0", port))
	
	init {
		run()
	}
	
	override fun run() {
		server.accept(0, object : CompletionHandler<AsynchronousSocketChannel, Int> {
			override fun completed(result: AsynchronousSocketChannel?, attachment: Int?) {
				try {
					server.accept(0, this)
				} catch (e: Throwable) {
				}
				val socket = AioSocket(result!!, ByteBuffer.allocate(bufferSize))
				socket.tryCatch {
					when (this) {
						is StringIndexOutOfBoundsException -> {
						}
						is ClosedChannelException -> {
						}
						is InterruptedByTimeoutException -> {
						}
						else -> {
							System.err.println("AioServer caused an exception:")
							printStackTrace()
						}
					}
					socket.close()
				}
				socket.handler()
				socket.run()
			}
			
			override fun failed(exc: Throwable?, attachment: Int?) {
				when (exc) {
					is AsynchronousCloseException -> {
					}
					else -> {
						exc?.printStackTrace()
					}
				}
			}
		})
	}
	
	override fun close() {
		server.close()
	}
}
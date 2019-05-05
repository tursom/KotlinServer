package cn.tursom.socket.client

import cn.tursom.socket.AioSocket
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.nio.channels.InterruptedByTimeoutException


class AioClient(
	host: String,
	port: Int,
	buffer: ByteBuffer = ByteBuffer.allocate(4096),
	process: AioClient.() -> Unit
) : AioSocket(AsynchronousSocketChannel.open()!!, buffer) {
	constructor(
		host: String,
		port: Int,
		bufferSize: Int,
		process: AioClient.() -> Unit
	) : this(host, port, ByteBuffer.allocate(bufferSize), process)
	
	init {
		tryCatch {
			System.err.println("AioClient caused an exception:")
			printStackTrace()
			close()
		}
		process()
		channel.connect(InetSocketAddress(host, port), 0, object : CompletionHandler<Void, Int> {
			override fun completed(result: Void?, attachment: Int?) {
				run()
			}
			
			override fun failed(exc: Throwable?, attachment: Int?) {
				when (exc) {
					is InterruptedByTimeoutException -> {
					}
					else -> exc?.printStackTrace()
				}
			}
		})
	}
	
	override fun toString() =
		"AioClient(processLength=$processLength)"
}
package cn.tursom.socket.client

import java.io.Closeable
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler

interface AioClientInterface : Closeable {
	val buffer: ByteBuffer
	infix fun send(bufferGetter: AioClientInterface.() -> ByteBuffer)
	fun recv(
		bufferGetter: AioClientInterface.() -> ByteBuffer,
		handler: AioClientInterface.(size: Int, buffer: ByteBuffer) -> Unit
	)
	infix fun tryCatch(exceptionHandler: Throwable.() -> Unit)
	infix fun run(runBlock: AioClientInterface.() -> Unit)
}

infix fun AioClientInterface.recv(a: AioClientInterface.(size: Int, buffer: ByteBuffer) -> Unit) {
	recv({ buffer }, a)
}

class AioClient(
	host: String,
	port: Int,
	override val buffer: ByteBuffer = ByteBuffer.allocate(4096),
	process: AioClient.() -> Unit
) : AioClientInterface {
	private val channel = AsynchronousSocketChannel.open()!!
	private val processList = ArrayList<(index: Int) -> Unit>()
	private var failed: Throwable.() -> Unit = {
		throw this
	}
	
	constructor(
		host: String,
		port: Int,
		bufferSize: Int,
		process: AioClient.() -> Unit
	) : this(host, port, ByteBuffer.allocate(bufferSize), process)
	
	init {
		process()
		channel.connect(InetSocketAddress(host, port), 0, object : CompletionHandler<Void, Int> {
			override fun completed(result: Void?, attachment: Int?) {
				processList[0](0)
			}
			
			override fun failed(exc: Throwable?, attachment: Int?) {
				exc?.printStackTrace()
			}
		})
	}
	
	
	override fun send(bufferGetter: AioClientInterface.() -> ByteBuffer) {
		val failed = this.failed
		val handler = Handler<Int>({ failed() }) { _, index ->
			doNext(index + 1)
		}
		processList.add {
			channel.write(bufferGetter(), it, handler)
		}
	}
	
	override fun recv(bufferGetter: AioClientInterface.() -> ByteBuffer, handler: AioClientInterface.(size: Int, buffer: ByteBuffer) -> Unit) {
		val failed = this.failed
		processList.add {
			val recvBuffer = bufferGetter()
			recvBuffer.clear()
			channel.read(recvBuffer, it, Handler({ this.failed() }) { size, index ->
				handler(size, recvBuffer)
				doNext(index + 1)
			})
		}
	}
	
	override fun tryCatch(exceptionHandler: Throwable.() -> Unit) {
		failed = exceptionHandler
	}
	
	override fun run(runBlock: AioClientInterface.() -> Unit) {
		val failed = this.failed
		processList.add { index ->
			try {
				runBlock()
			} catch (e: Throwable) {
				e.failed()
			}
			doNext(index + 1)
		}
	}
	
	override fun close() {
		try {
			channel.close()
		} catch (e: Exception) {
		}
	}
	
	override fun toString() =
		"AioClient(processLength=${processList.size})"
	
	private fun doNext(index: Int) {
		if (index < processList.size) processList[index](index)
	}
	
	private class Handler<T>(
		val failed: Throwable.(buffer: T) -> Unit = { printStackTrace() },
		val handler: (result: Int, attachment: T) -> Unit
	) : CompletionHandler<Int, T> {
		override fun completed(result: Int?, a: T) {
			handler(result!!, a)
		}
		
		override fun failed(e: Throwable, a: T) {
			e.failed(a)
		}
	}
	
	
}
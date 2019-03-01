package cn.tursom.socket.client

import java.io.Closeable
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler

class NioClient(
	host: String,
	port: Int,
	bufferSize: Int = 4096,
	onWriteComplete: (result: Int, buffer: ByteBuffer) -> Unit = { _, _ -> },
	onReadComplete: ((result: Int, buffer: ByteBuffer) -> Unit)? = null
) : Closeable {
	
	private val channel = AsynchronousSocketChannel.open()!!
	private val writeHandler = Handler(onWriteComplete)
	private val readHandler by lazy {
		Handler(onReadComplete ?: return@lazy null)
	}
	private val buffer = ByteBuffer.allocate(bufferSize)
	
	init {
		channel.connect(InetSocketAddress(host, port))
	}
	
	fun write(buffer: ByteBuffer) {
		channel.write(buffer, buffer, writeHandler)
	}
	
	fun write(data: ByteArray) {
		val buffer = ByteBuffer.allocate(data.size)
		buffer.put(data)
		buffer.flip()
		channel.write(buffer, buffer, writeHandler)
	}
	
	fun write(buffer: ByteBuffer, onWriteComplete: (result: Int, buffer: ByteBuffer) -> Unit) {
		channel.write(buffer, buffer, Handler(onWriteComplete))
	}
	
	fun write(data: ByteArray, onWriteComplete: (result: Int, buffer: ByteBuffer) -> Unit) {
		val buffer = ByteBuffer.allocate(data.size)
		buffer.put(data)
		buffer.flip()
		channel.write(buffer, buffer, Handler(onWriteComplete))
	}
	
	fun read() {
		channel.read(buffer, buffer, readHandler!!)
	}
	
	fun read(onReadComplete: (result: Int, buffer: ByteBuffer) -> Unit) {
		channel.read(buffer, buffer, Handler(onReadComplete))
	}
	
	fun read(onReadComplete: (message: ByteArray) -> Unit) {
		channel.read(buffer, buffer, Handler { _, buffer ->
			buffer.flip()
			val limits = buffer.limit()
			val bytes = ByteArray(limits)
			buffer.get(bytes, 0, limits)
			onReadComplete(bytes)
		})
	}
	
	override fun close() {
		try {
			channel.close()
		} catch (e: Exception) {
		}
	}
	
	private class Handler<T>(
		val handler: (result: Int, buffer: T) -> Unit
	) : CompletionHandler<Int, T> {
		override fun completed(result: Int?, buffer: T) {
			handler(result!!, buffer)
		}
		
		override fun failed(e: Throwable, buffer: T) {
			e.printStackTrace()
		}
	}
}
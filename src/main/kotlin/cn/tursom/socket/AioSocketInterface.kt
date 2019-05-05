package cn.tursom.socket

import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.channels.CompletionHandler


interface AioSocketInterface : Closeable, Runnable {
	//	val buffer: ByteBuffer
	infix fun send(bufferGetter: AioSocketInterface.() -> ByteBuffer)
	
	fun recv(
		bufferGetter: AioSocketInterface.() -> ByteBuffer,
		handler: (size: Int, buffer: ByteBuffer) -> Unit
	)
	
	infix fun tryCatch(exceptionHandler: Throwable.() -> Unit)
	infix fun run(runBlock: AioSocketInterface.() -> Unit)
}

fun AioSocketInterface.recv(
	buffer: ByteBuffer,
	a: (size: Int, buffer: ByteBuffer) -> Unit
) {
	recv({ buffer }, a)
}

fun AioSocketInterface.recvStr(bufferGetter: AioSocketInterface.() -> ByteBuffer, handler: (String) -> Unit) {
	recv(bufferGetter) { size, buffer ->
		handler(String(buffer.array(), 0, size))
	}
}

fun AioSocketInterface.recvStr(buffer: ByteBuffer, handler: (String) -> Unit) {
	recv(buffer) { size, recvBuffer ->
		handler(String(recvBuffer.array(), 0, size))
	}
}

infix fun AioSocketInterface.sendStr(str: String) {
	send { ByteBuffer.wrap(str.toByteArray()) }
}

infix fun AioSocketInterface.sendStr(str: () -> String) {
	send { ByteBuffer.wrap(str().toByteArray()) }
}

class AioHandler<T>(
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
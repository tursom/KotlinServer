package cn.tursom.socket

import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel

open class AioSocket(
	protected val channel: AsynchronousSocketChannel,
	val buffer: ByteBuffer
) : AioSocketInterface {
	private val processList = ArrayList<(index: Int) -> Unit>()
	private var failed: Throwable.() -> Unit = {
		throw this
	}
	val processLength
		get() = processList.size
	
	override fun send(bufferGetter: AioSocketInterface.() -> ByteBuffer) {
		val failed = this.failed
		val handler = AioHandler<Int>({ failed() }) { _, index ->
			doNext(index + 1)
		}
		processList.add {
			val buffer = bufferGetter()
			channel.write(buffer, it, handler)
		}
	}
	
	override fun recv(bufferGetter: AioSocketInterface.() -> ByteBuffer, handler: (size: Int, buffer: ByteBuffer) -> Unit) {
		val failed = this.failed
		processList.add {
			val recvBuffer = bufferGetter()
			recvBuffer.clear()
			channel.read(recvBuffer, it, AioHandler({ this.failed() }) { size, index ->
				handler(size, recvBuffer)
				doNext(index + 1)
			})
		}
	}
	
	infix fun recv(a: (size: Int, buffer: ByteBuffer) -> Unit) {
		recv({ buffer }, a)
	}
	
	infix fun recvStr(a: (String) -> Unit) {
		recvStr({ buffer }, a)
	}
	
	override fun tryCatch(exceptionHandler: Throwable.() -> Unit) {
		failed = exceptionHandler
	}
	
	override fun run(runBlock: AioSocketInterface.() -> Unit) {
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
	
	override fun run() {
		processList[0](0)
	}
	
	override fun toString() =
		"AioClient(processLength=${processList.size})"
	
	private fun doNext(index: Int) {
		if (index < processList.size) processList[index](index)
	}
}

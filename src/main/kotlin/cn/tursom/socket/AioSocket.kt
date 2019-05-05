package cn.tursom.socket

import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.util.concurrent.TimeUnit

open class AioSocket(
	protected val channel: AsynchronousSocketChannel,
	val buffer: ByteBuffer,
	override var timeout: Long = 0L,
	override var timeUnit: TimeUnit = TimeUnit.MILLISECONDS
) : AioSocketInterface {
	private val processList = ArrayList<(index: Int) -> Unit>()
	private var failed: Throwable.() -> Unit = {
		throw this
	}
	val processLength
		get() = processList.size
	
	override fun send(
		next: (Int) -> Int,
		bufferGetter: () -> ByteBuffer
	): Int {
		val failed = this.failed
		val handler = AioHandler<Int>({ failed() }) { _, index ->
			doNext(next(index))
		}
		processList.add {
			val buffer = bufferGetter()
			channel.write(buffer, timeout, timeUnit, it, handler)
		}
		return processList.size - 1
	}
	
	override fun recv(
		bufferGetter: () -> ByteBuffer,
		next: (Int) -> Int,
		handler: (size: Int, buffer: ByteBuffer, failed: Throwable.() -> Unit) -> Unit
	): Int {
		val failed = this.failed
		processList.add {
			val recvBuffer = bufferGetter()
			recvBuffer.clear()
			channel.read(recvBuffer, timeout, timeUnit, it, AioHandler({ this.failed() }) { size, index ->
				handler(size, recvBuffer, failed)
				doNext(next(index))
			})
		}
		return processList.size - 1
	}
	
	infix fun recv(
		a: (size: Int, buffer: ByteBuffer,
		    failed: Throwable.() -> Unit) -> Unit
	) = recv({ buffer }, a)
	
	
	infix fun recvStr(
		a: (String) -> Unit
	) = recvStr({ buffer }, { it + 1 }, a)
	
	
	override fun tryCatch(exceptionHandler: Throwable.() -> Unit) {
		failed = exceptionHandler
	}
	
	override fun run(
		next: (Int) -> Int,
		runBlock: () -> Unit
	): Int {
		val failed = this.failed
		processList.add { index ->
			try {
				runBlock()
			} catch (e: Throwable) {
				e.failed()
			}
			doNext(next(index))
		}
		return processList.size - 1
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

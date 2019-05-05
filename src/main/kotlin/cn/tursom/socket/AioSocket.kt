package cn.tursom.socket

import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.util.concurrent.TimeUnit

open class AioSocket(
	protected val channel: AsynchronousSocketChannel,
	val buffer: ByteBuffer,
	override var timeout: Long = 0L,
	override var timeUnit: TimeUnit = TimeUnit.MILLISECONDS,
	private val processList: ArrayList<(index: Int) -> Unit> = ArrayList()
) : AioSocketInterface {
	private var failed: Throwable.() -> Unit = {
		throw this
	}
	val processLength
		get() = processList.size
	val id = size
	
	override fun send(
		next: (Int) -> Int,
		bufferGetter: () -> ByteBuffer
	): Int {
		val failed = this.failed
		val handler = AioHandler<Int>({ failed(this) }) { _, index ->
			doNext(next(index))
		}
		processList.add { index: Int ->
			val buffer = bufferGetter()
			channel.write(buffer, timeout, timeUnit, index, handler)
		}
		return processList.size - 1
	}
	
	override fun recv(
		bufferGetter: () -> ByteBuffer,
		next: (Int) -> Int,
		handler: (
			size: Int,
			buffer: ByteBuffer
		) -> Unit
	): Int {
		val failed = this.failed
		processList.add { index: Int ->
			val recvBuffer = bufferGetter()
			recvBuffer.clear()
			channel.read(recvBuffer, timeout, timeUnit, index, AioHandler({ failed(this) }) { size, _ ->
				try {
					handler(size, recvBuffer)
					doNext(next(index))
				} catch (e: Throwable) {
					failed(e)
				}
			})
		}
		return processList.size - 1
	}
	
	infix fun recv(
		handler: (size: Int, buffer: ByteBuffer) -> Unit
	) = recv({ buffer }, handler)
	
	
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
		processList.add { index: Int ->
			try {
				runBlock()
			} catch (e: Throwable) {
				failed(e)
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
		"AioSocket(id=$id, processLength=${processList.size})"
	
	override fun hashCode() = id
	
	
	private fun doNext(index: Int) {
		if (index < processList.size) {
			processList[index](index)
		}
	}
	
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false
		
		other as AioSocket
		
		if (channel != other.channel) return false
		if (buffer != other.buffer) return false
		if (timeout != other.timeout) return false
		if (timeUnit != other.timeUnit) return false
		if (processList != other.processList) return false
		if (failed != other.failed) return false
		if (id != other.id) return false
		
		return true
	}
	
	companion object {
		private var size = 0
			get() = synchronized(field) { field++ }
		
		val maxId: Int
			get() {
				val id = size
				size = id
				return id
			}
	}
}

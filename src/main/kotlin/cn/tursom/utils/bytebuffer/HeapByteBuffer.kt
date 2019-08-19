package cn.tursom.utils.bytebuffer

import java.nio.ByteBuffer

/**
 * HOOK java.nio.HeapByteBuffer
 */
object HeapByteBuffer {
	private val field = ByteBuffer::class.java.getDeclaredField("offset")

	init {
		field.isAccessible = true
	}

	fun wrap(array: ByteArray, size: Int = array.size, offset: Int = 0): ByteBuffer {
		val buffer = ByteBuffer.wrap(array, 0, size)
		field.set(buffer, offset)
		return buffer
	}

	fun wrap(string: String) = wrap(string.toByteArray())
}
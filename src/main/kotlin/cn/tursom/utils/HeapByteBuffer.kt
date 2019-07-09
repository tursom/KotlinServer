package cn.tursom.utils

import java.nio.ByteBuffer

/**
 * HOOK java.nio.HeapByteBuffer
 */
object HeapByteBuffer {
	private val constructor = ByteBuffer.wrap(ByteArray(0)).javaClass.getDeclaredConstructor(
		ByteArray::class.java,
		Int::class.java,
		Int::class.java,
		Int::class.java,
		Int::class.java,
		Int::class.java
	)
	
	init {
		constructor.isAccessible = true
	}
	
	fun wrap(array: ByteArray, size: Int = array.size, offset: Int = 0): ByteBuffer = constructor.newInstance(array, -1, 0, size, size, offset)
}
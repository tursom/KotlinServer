package cn.tursom.utils.bytebuffer

import java.nio.ByteBuffer

/**
 * HOOK java.nio.HeapByteBuffer
 */
object HeapByteBuffer {
	//private var initType = 0
	//
	//private val constructor = ByteBuffer.wrap(ByteArray(0)).javaClass.let {
	//	try {
	//		// jdk constructor
	//		it.getDeclaredConstructor(
	//			ByteArray::class.java,
	//			Int::class.java,
	//			Int::class.java,
	//			Int::class.java,
	//			Int::class.java,
	//			Int::class.java
	//		)
	//	} catch (e: Exception) {
	//		// android constructor
	//		initType = 1
	//		it.getDeclaredConstructor(
	//			ByteArray::class.java,
	//			Int::class.java,
	//			Int::class.java,
	//			Int::class.java,
	//			Int::class.java,
	//			Int::class.java,
	//			Boolean::class.java
	//		)
	//	}
	//}
	
	private val field = ByteBuffer::class.java.getDeclaredField("offset")
	
	init {
		//constructor.isAccessible = true
		field.isAccessible = true
	}
	
	fun wrap(array: ByteArray, size: Int = array.size, offset: Int = 0): ByteBuffer {
		val buffer = ByteBuffer.wrap(array, 0, size)
		field.set(buffer, offset)
		return buffer
		//println("$array $size $offset")
		//return when (initType) {
		//	0 -> constructor.newInstance(array, -1, 0, size, size, offset)
		//	else -> constructor.newInstance(array, -1, 0, size, size, offset, false)
		//}
	}
}
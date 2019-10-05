package cn.tursom.utils.pool

import cn.tursom.utils.bytebuffer.AdvanceByteBuffer
import cn.tursom.utils.bytebuffer.NioAdvanceByteBuffer
import java.nio.ByteBuffer

interface MemoryPool {
	fun allocate(): Int
	fun free(token: Int)
	fun getMemory(token: Int): ByteBuffer?
	fun getAdvanceByteBuffer(token: Int): AdvanceByteBuffer? {
		val buffer = getMemory(token)
		return if (buffer != null) {
			NioAdvanceByteBuffer(buffer)
		} else {
			null
		}
	}
}


inline fun MemoryPool.usingMemory(action: (ByteBuffer?) -> Unit) {
	val token = allocate()
	try {
		action(getMemory(token))
	} finally {
		free(token)
	}
}

inline fun MemoryPool.usingAdvanceByteBuffer(action: (AdvanceByteBuffer?) -> Unit) {
	val token = allocate()
	try {
		action(getAdvanceByteBuffer(token))
	} finally {
		free(token)
	}
}
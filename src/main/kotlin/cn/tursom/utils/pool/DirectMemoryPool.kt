package cn.tursom.utils.pool

import cn.tursom.utils.bytebuffer.AdvanceByteBuffer
import cn.tursom.utils.bytebuffer.NioAdvanceByteBuffer
import cn.tursom.utils.datastruct.ArrayBitSet
import java.nio.ByteBuffer

class DirectMemoryPool(val blockSize: Int = 1024, val blockCount: Int = 16) {
	private val memoryPool = ByteBuffer.allocateDirect(blockSize * blockCount)
	private val bitMap = ArrayBitSet(blockCount.toLong())

	/**
	 * @return token
	 */
	fun allocate(): Int = synchronized(this) {
		val index = bitMap.firstDown()
		if (index in 0 until blockCount) {
			bitMap.up(index)
			index.toInt()
		} else {
			-1
		}
	}

	fun free(token: Int) {
		if (token >= 0) synchronized(this) {
			bitMap.down(token.toLong())
		}
	}

	fun getMemory(token: Int): ByteBuffer? = if (token in 0 until blockCount) {
		synchronized(this) {
			memoryPool.position(token * blockSize)
			memoryPool.limit((token + 1) * blockSize)
			return memoryPool.slice()
		}
	} else {
		null
	}

	fun getAdvanceByteBuffer(token: Int): AdvanceByteBuffer? = if (token in 0 until blockCount) {
		synchronized(this) {
			memoryPool.position(token * blockSize)
			memoryPool.limit((token + 1) * blockSize)
			return NioAdvanceByteBuffer(memoryPool.slice())
		}
	} else {
		null
	}

	inline fun usingMemory(action: (ByteBuffer?) -> Unit) {
		val token = allocate()
		try {
			action(getMemory(token))
		} finally {
			free(token)
		}
	}

	inline fun usingAdvanceByteBuffer(action: (AdvanceByteBuffer?) -> Unit) {
		val token = allocate()
		try {
			action(getAdvanceByteBuffer(token))
		} finally {
			free(token)
		}
	}
}
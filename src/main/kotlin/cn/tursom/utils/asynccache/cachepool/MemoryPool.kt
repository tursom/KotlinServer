package cn.tursom.utils.asynccache.cachepool

import cn.tursom.utils.asynccache.interfaces.CachePool
import cn.tursom.utils.bytebuffer.HeapByteBuffer
import cn.tursom.utils.datastruct.ArrayBitSet
import java.nio.ByteBuffer

@Suppress("CanBeParameter", "MemberVisibilityCanBePrivate")
class MemoryPool(val blockSize: Int = 1024, val blockCount: Int = 16) : CachePool<ByteBuffer> {
	private val memoryPool = ByteArray(blockSize * blockCount)
	private val bitSet = ArrayBitSet(blockCount.toLong())
	
	override fun put(cache: ByteBuffer): Boolean {
		if (cache.array() !== memoryPool) return false
		
		val index = (cache.arrayOffset() / blockSize).toLong()
		synchronized(bitSet) {
			bitSet.down(index)
		}
		
		cache.asCharBuffer()
		return true
	}
	
	override fun get(): ByteBuffer? {
		val index = synchronized(bitSet) {
			val index = bitSet.firstDown()
			if (index >= 0) bitSet.up(index)
			index
		}.toInt()
		return if (index < 0 || index > blockCount) null
		else HeapByteBuffer.wrap(memoryPool, blockSize * index, blockSize)
	}
	
	fun contain(buffer: ByteBuffer) = buffer.array() === memoryPool
}
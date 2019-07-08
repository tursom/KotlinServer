package cn.tursom.utils.cache

import kotlinx.coroutines.channels.Channel
import java.nio.ByteBuffer

interface AsyncCacheChannel<T> {
	suspend fun put(cache: T): Boolean
	suspend fun get(): T
}

@Suppress("MemberVisibilityCanBePrivate", "CanBeParameter")
class AsyncMemoryPoolChanel(val blockSize: Int = 1024, val blockCount: Int = 16) : AsyncCacheChannel<ByteBuffer> {
	private val memoryPool = AsyncMemoryPool(blockSize, blockCount)
	private val channel = Channel<ByteBuffer>(blockCount)
	
	override suspend fun put(cache: ByteBuffer): Boolean {
		return if (memoryPool.contain(cache)) {
			cache.clear()
			channel.send(cache)
			true
		} else {
			false
		}
	}
	
	override suspend fun get(): ByteBuffer {
		return memoryPool.get() ?: channel.receive()
	}
}
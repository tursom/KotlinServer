package cn.tursom.utils.asynccache.cachepool

import cn.tursom.utils.asynccache.cachepool.AsyncMemoryPool
import cn.tursom.utils.asynccache.interfaces.AsyncCacheChannel
import kotlinx.coroutines.channels.Channel
import java.nio.ByteBuffer

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
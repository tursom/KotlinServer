package cn.tursom.asynclock

import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 读优化锁
 */
@Suppress("MemberVisibilityCanBePrivate")
class WriteAsyncLock(val maxReadOperatorTime: Long, val delayTime: Long = (maxReadOperatorTime shr 2) or 1) : AsyncLock {
	
	private val lock = AtomicBoolean(false)
	
	private suspend fun getWriteLock() {
		while (!lock.compareAndSet(false, true)) {
			delay(delayTime)
		}
		delay(maxReadOperatorTime)
	}
	
	private fun releaseLock() {
		lock.set(false)
	}
	
	suspend fun getReadLock() {
		while (lock.get()) {
			delay(delayTime)
		}
	}
	
	override suspend fun <T> doRead(block: suspend () -> T): T {
		getReadLock()
		return block()
	}
	
	override suspend fun doWrite(block: suspend () -> Unit) {
		getWriteLock()
		block()
		releaseLock()
	}
	
	override fun toString(): String {
		return "WriteAsyncLock(maxReadOperatorTime=$maxReadOperatorTime, delayTime=$delayTime, lock=${lock.get()})"
	}
}

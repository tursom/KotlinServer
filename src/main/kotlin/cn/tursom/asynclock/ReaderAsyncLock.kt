package cn.tursom.asynclock

import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicReference

/**
 * 读优化锁
 */
@Suppress("MemberVisibilityCanBePrivate")
class ReaderAsyncLock(val maxOperatorTime: Long, val delayTime: Long = (maxOperatorTime shr 2) or 1) : AsyncLock {
	private val lock = AtomicReference<LockState>(LockState.FREE)
	
	private suspend fun getWriteLock() {
		while (lock.compareAndSet(LockState.FREE, LockState.LOCK)) {
			delay(delayTime)
		}
		delay(maxOperatorTime)
	}
	
	private fun releaseLock() {
		lock.set(LockState.FREE)
	}
	
	suspend fun getReadLock() {
		while (lock.get() != LockState.FREE) {
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
		return "ReaderAsyncLock(maxOperatorTime=$maxOperatorTime, delayTime=$delayTime, lock=${lock.get()})"
	}
}

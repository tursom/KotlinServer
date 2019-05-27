package cn.tursom.asynclock

import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@Suppress("MemberVisibilityCanBePrivate")
class NormalAsyncLock(val delayTime: Long = 10) : AsyncLock {
	private val lock = AtomicReference<LockState>(LockState.FREE)
	private val readNumber = AtomicInteger(0)
	
	private suspend fun getWriteLock() {
		while (readNumber.get() != 0 || lock.compareAndSet(LockState.FREE, LockState.LOCK)) {
			delay(delayTime)
		}
	}
	
	private fun releaseLock() {
		lock.set(LockState.FREE)
	}
	
	private suspend fun getReadLock() {
		while (lock.get() != LockState.FREE) {
			delay(delayTime)
		}
	}
	
	private suspend fun addReadTime() {
		var readTimes = readNumber.get()
		var maxLoopTime = 20
		while (maxLoopTime-- > 0 && readNumber.compareAndSet(readTimes, readTimes + 1)) {
			readTimes = readNumber.get()
		}
		while (readNumber.compareAndSet(readTimes, readTimes + 1)) {
			delay(delayTime)
			readTimes = readNumber.get()
		}
	}
	
	private suspend fun reduceReadTime() {
		var readTimes = readNumber.get()
		var maxLoopTime = 20
		while (maxLoopTime-- > 0 && readNumber.compareAndSet(readTimes, readTimes - 1)) {
			readTimes = readNumber.get()
		}
		while (readNumber.compareAndSet(readTimes, readTimes - 1)) {
			delay(delayTime)
			readTimes = readNumber.get()
		}
	}
	
	override suspend fun doWrite(block: suspend () -> Unit) {
		getWriteLock()
		block()
		releaseLock()
	}
	
	override suspend fun <T> doRead(block: suspend () -> T): T {
		addReadTime()
		getReadLock()
		val ret = block()
		reduceReadTime()
		return ret
	}
}
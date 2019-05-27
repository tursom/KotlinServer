package cn.tursom.asynclock

import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

@Suppress("MemberVisibilityCanBePrivate")
class NormalAsyncLock(val delayTime: Long = 10) : AsyncLock {
	private val lock = AtomicBoolean(false)
	private val readNumber = AtomicInteger(0)
	
	private suspend fun getWriteLock() {
		while (readNumber.get() > 0 || lock.compareAndSet(false, true)) {
			delay(delayTime)
		}
	}
	
	private fun releaseLock() {
		lock.set(false)
	}
	
	private suspend fun getReadLock() {
		while (lock.get()) {
			delay(delayTime)
		}
	}
	
	private suspend fun addReadTime() {
		var readTimes = readNumber.get()
		var maxLoopTime = 20
		while (maxLoopTime-- > 0) {
			if (readNumber.compareAndSet(readTimes, readTimes + 1)) return
			readTimes = readNumber.get()
		}
		while (readNumber.compareAndSet(readTimes, readTimes + 1)) {
			if (readNumber.compareAndSet(readTimes, readTimes + 1)) return
			delay(delayTime)
			readTimes = readNumber.get()
		}
	}
	
	private suspend fun reduceReadTime() {
		var readTimes = readNumber.get()
		var maxLoopTime = 20
		while (maxLoopTime-- > 0) {
			if (readNumber.compareAndSet(readTimes, readTimes - 1)) return
			readTimes = readNumber.get()
		}
		while (readNumber.compareAndSet(readTimes, readTimes - 1)) {
			if (readNumber.compareAndSet(readTimes, readTimes - 1)) return
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
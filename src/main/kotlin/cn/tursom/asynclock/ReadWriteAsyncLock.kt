package cn.tursom.asynclock

import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

@Suppress("MemberVisibilityCanBePrivate")
class ReadWriteAsyncLock(val delayTime: Long = 10) : AsyncLock {
	private val writeLock = AtomicBoolean(false)
	private val lock = AtomicBoolean(false)
	private val readNumber = AtomicInteger(0)
	
	suspend fun AtomicBoolean.lock() {
		while (!compareAndSet(false, true)) {
			delay(delayTime)
		}
	}
	
	fun AtomicBoolean.release() {
		set(false)
	}
	
	suspend fun AtomicBoolean.wait() {
		while (get()) {
			delay(delayTime)
		}
	}
	
	suspend fun AtomicInteger.wait() {
		while (get() > 0) {
			delay(delayTime)
		}
	}
	
	private suspend fun addReadTime() {
		var readTimes = readNumber.get()
		
		// 如果得不到锁，先自旋20次
		var maxLoopTime = 20
		while (maxLoopTime-- > 0) {
			if (readNumber.compareAndSet(readTimes, readTimes + 1)) return
			readTimes = readNumber.get()
		}
		while (!readNumber.compareAndSet(readTimes, readTimes + 1)) {
			delay(delayTime)
			readTimes = readNumber.get()
		}
	}
	
	private suspend fun reduceReadTime() {
		var readTimes = readNumber.get()
		
		// 如果得不到锁，先自旋20次
		var maxLoopTime = 20
		while (maxLoopTime-- > 0) {
			if (readNumber.compareAndSet(readTimes, readTimes - 1)) return
			readTimes = readNumber.get()
		}
		while (!readNumber.compareAndSet(readTimes, readTimes - 1)) {
			delay(delayTime)
			readTimes = readNumber.get()
		}
	}
	
	override suspend fun doWrite(block: suspend () -> Unit) {
		// 先通知所有人，我要进行写操作了，不要再有新的读操作了
		lock.lock()
		
		// 然后等待现有的读操作全部退出完成
		readNumber.wait()
		
		// 最后挂起强制锁
		writeLock.lock()
		
		block()
		
		lock.release()
		writeLock.release()
	}
	
	override suspend fun <T> doRead(block: suspend () -> T): T {
		// 先等待通知锁关闭
		lock.wait()
		// 添加读计数
		addReadTime()
		// 等待强制锁关闭
		writeLock.wait()
		
		val ret = block()
		
		// 减少读计数
		reduceReadTime()
		
		return ret
	}
}

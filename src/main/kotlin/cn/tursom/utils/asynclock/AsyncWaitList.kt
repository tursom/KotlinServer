package cn.tursom.utils.asynclock

import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AsyncWaitList {
	val empty: Boolean get() = lockList == null
	val notEmpty: Boolean get() = lockList != null
	
	suspend fun wait() = listLock {
		suspendCoroutine<Int> { cont ->
			lockList = LockNode(cont, lockList)
		}
	}
	
	suspend fun notify() = listLock {
		val node = lockList ?: return@listLock
		lockList = node.next
		node.cont.resume(0)
	}
	
	@Volatile
	private var lockList: LockNode? = null
	private val listLock = AsyncLoopLock()
	
	private data class LockNode(val cont: Continuation<Int>, val next: LockNode? = null)
}
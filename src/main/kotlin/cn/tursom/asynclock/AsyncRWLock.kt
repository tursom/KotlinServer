package cn.tursom.asynclock

interface AsyncRWLock : AsyncLock {
	suspend fun <T> doRead(block: suspend () -> T): T
	suspend fun doWrite(block: suspend () -> Unit)
}
package cn.tursom.asynclock

interface AsyncLock {
	suspend fun <T> doRead(block: suspend () -> T): T
	suspend fun doWrite(block: suspend () -> Unit)
}
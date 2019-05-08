package cn.tursom.asyncstream

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

interface AsyncStream {
	companion object {
		val threadPool = ThreadPoolExecutor(
			0,
			Runtime.getRuntime().availableProcessors() * 2,
			60_000L,
			TimeUnit.MILLISECONDS,
			ArrayBlockingQueue(128)
		)
	}
}

@Suppress("unused")
suspend fun <T> AsyncStream.run(action: () -> T): T = suspendCoroutine { cont ->
	AsyncStream.threadPool.execute {
		try {
			cont.resume(action())
		} catch (e: Throwable) {
			cont.resumeWithException(e)
		}
	}
}

/**
 * 饮鸩止渴
 */
@Suppress("unused")
suspend fun <T> runBlock(action: () -> T): T = suspendCoroutine { cont ->
	AsyncStream.threadPool.execute {
		try {
			cont.resume(action())
		} catch (e: Throwable) {
			cont.resumeWithException(e)
		}
	}
}
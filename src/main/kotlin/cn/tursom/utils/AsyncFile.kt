package cn.tursom.utils

import java.nio.ByteBuffer
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.nio.channels.AsynchronousFileChannel
import java.nio.channels.CompletionHandler
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


class AsyncFile(val path: Path, vararg standardOpenOption: StandardOpenOption) {
	var fileChannel = AsynchronousFileChannel.open(path, *standardOpenOption)

	fun write(buffer: ByteBuffer, position: Long = 0) {
		fileChannel.write(buffer, position)
	}

	suspend fun writeAndWait(buffer: ByteBuffer, position: Long = 0): Int {
		return suspendCoroutine {
			fileChannel.write(buffer, position, it, handler)
		}
	}

	suspend fun read(buffer: ByteBuffer, position: Long = 0): Int {
		return suspendCoroutine {
			fileChannel.read(buffer, position, it, handler)
		}
	}

	companion object {
		@JvmStatic
		val handler = object : CompletionHandler<Int, Continuation<Int>> {
			override fun completed(result: Int, attachment: Continuation<Int>) {
				attachment.resume(result)
			}

			override fun failed(exc: Throwable, attachment: Continuation<Int>) {
				attachment.resumeWithException(exc)
			}
		}
	}
}
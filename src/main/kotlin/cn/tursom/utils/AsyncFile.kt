package cn.tursom.utils

import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel
import java.nio.channels.CompletionHandler
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


@Suppress("MemberVisibilityCanBePrivate")
class AsyncFile(val path: Path) {
	constructor(path: String) : this(Paths.get(path))

	var exists = false
	val writeChannel: AsynchronousFileChannel by lazy { AsynchronousFileChannel.open(path, StandardOpenOption.WRITE) }
	val readChannel: AsynchronousFileChannel by lazy { AsynchronousFileChannel.open(path, StandardOpenOption.READ) }

	fun write(buffer: ByteBuffer, position: Long = 0) {
		create()
		writeChannel.write(buffer, position)
	}

	suspend fun writeAndWait(buffer: ByteBuffer, position: Long = 0): Int {
		create()
		return suspendCoroutine {
			writeChannel.write(buffer, position, it, handler)
		}
	}

	fun append(buffer: ByteBuffer, position: Long = size()) {
		create()
		writeChannel.write(buffer, position)
	}

	suspend fun appendAndWait(buffer: ByteBuffer, position: Long = size()): Int {
		create()
		return suspendCoroutine {
			writeChannel.write(buffer, position, it, handler)
		}
	}

	suspend fun read(buffer: ByteBuffer, position: Long = size()): Int {
		return suspendCoroutine {
			readChannel.read(buffer, position, it, handler)
		}
	}

	fun create() = if (exists || !Files.exists(path)) {
		Files.createFile(path)
		exists = true
		true
	} else {
		false
	}

	fun delete(): Boolean {
		exists = false
		return Files.deleteIfExists(path)
	}

	fun size() = Files.size(path)

	companion object {
		@JvmStatic
		val handler = object : CompletionHandler<Int, Continuation<Int>> {
			override fun completed(result: Int, attachment: Continuation<Int>) = attachment.resume(result)
			override fun failed(exc: Throwable, attachment: Continuation<Int>) = attachment.resumeWithException(exc)
		}
	}
}

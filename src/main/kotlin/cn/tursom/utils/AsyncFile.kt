package cn.tursom.utils

import cn.tursom.utils.bytebuffer.AdvanceByteBuffer
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

	private var existsCache = false

	val exists get() = Files.exists(path)
	val size get() = if (existsCache && exists) Files.size(path) else 0

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

	suspend fun write(buffer: AdvanceByteBuffer, position: Long = 0): Int {
		buffer.readMode()
		return try {
			writeAndWait(buffer.buffer, position)
		} finally {
			buffer.resumeWriteMode()
		}
	}

	fun append(buffer: ByteBuffer, position: Long = size) {
		write(buffer, position)
	}

	suspend fun appendAndWait(buffer: ByteBuffer, position: Long = size): Int {
		return writeAndWait(buffer, position)
	}

	suspend fun append(buffer: AdvanceByteBuffer, position: Long = size): Int {
		buffer.readMode()
		return try {
			appendAndWait(buffer.buffer, position)
		} finally {
			buffer.resumeWriteMode()
		}
	}

	suspend fun read(buffer: ByteBuffer, position: Long = 0): Int {
		return suspendCoroutine {
			readChannel.read(buffer, position, it, handler)
		}
	}

	suspend fun read(buffer: AdvanceByteBuffer, position: Long = 0): Int {
		return read(buffer.buffer, position)
	}

	fun create() = if (existsCache || !exists) {
		Files.createFile(path)
		existsCache = true
		true
	} else {
		false
	}

	fun delete(): Boolean {
		existsCache = false
		return Files.deleteIfExists(path)
	}

	companion object {
		@JvmStatic
		val handler = object : CompletionHandler<Int, Continuation<Int>> {
			override fun completed(result: Int, attachment: Continuation<Int>) = attachment.resume(result)
			override fun failed(exc: Throwable, attachment: Continuation<Int>) = attachment.resumeWithException(exc)
		}
	}
}
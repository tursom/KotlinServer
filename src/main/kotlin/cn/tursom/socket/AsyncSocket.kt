package cn.tursom.socket

import cn.tursom.utils.bytebuffer.AdvanceByteBuffer
import cn.tursom.utils.bytebuffer.readNioBuffer
import java.io.Closeable
import java.nio.ByteBuffer

interface AsyncSocket : Closeable {
	suspend fun write(buffer: ByteBuffer, timeout: Long = 0L): Int
	suspend fun read(buffer: ByteBuffer, timeout: Long = 0L): Int
	override fun close()

	suspend fun writeBuffers(buffer: Array<out ByteBuffer>, timeout: Long = 0L): Int {
		var writeSize = 0
		buffer.forEach {
			val s = write(it, timeout)
			if (s < 0) return writeSize
			writeSize += s
		}
		return writeSize
	}

	suspend fun write(buffer: AdvanceByteBuffer, timeout: Long = 0L): Int {
		return if (buffer.singleBuffer) {
			buffer.readNioBuffer {
				write(it, timeout)
			}
		} else {
			writeBuffers(buffer.nioBuffers)
		}
	}

	suspend fun read(buffer: AdvanceByteBuffer, timeout: Long = 0L): Int {
		return buffer.readNioBuffer {
			read(it, timeout)
		}
	}
}
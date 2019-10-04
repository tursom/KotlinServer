package cn.tursom.socket

import cn.tursom.utils.bytebuffer.AdvanceByteBuffer
import cn.tursom.utils.bytebuffer.writeNioBuffer
import java.io.Closeable
import java.nio.ByteBuffer

interface AsyncSocket : Closeable {
	suspend fun write(buffer: Array<out ByteBuffer>, timeout: Long = 0L): Long
	suspend fun read(buffer: Array<out ByteBuffer>, timeout: Long = 0L): Long
	suspend fun write(buffer: ByteBuffer, timeout: Long = 0L): Int = write(arrayOf(buffer)).toInt()
	suspend fun read(buffer: ByteBuffer, timeout: Long = 0L): Int = read(arrayOf(buffer)).toInt()
	override fun close()

	suspend fun write(buffer: AdvanceByteBuffer, timeout: Long = 0): Int {
		return if (buffer.bufferCount == 1) {
			buffer.writeNioBuffer {
				write(it, timeout)
			}
		} else {
			write(buffer.nioBuffers, timeout).toInt()
		}
	}

	suspend fun read(buffer: AdvanceByteBuffer, timeout: Long = 0): Int {
		return if (buffer.bufferCount == 1) {
			buffer.writeNioBuffer {
				read(it, timeout)
			}
		} else {
			read(buffer.nioBuffers, timeout).toInt()
		}
	}
}
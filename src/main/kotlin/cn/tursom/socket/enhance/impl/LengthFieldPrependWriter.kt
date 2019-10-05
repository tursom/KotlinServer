package cn.tursom.socket.enhance.impl

import cn.tursom.socket.IAsyncNioSocket
import cn.tursom.socket.enhance.SocketWriter
import cn.tursom.utils.bytebuffer.AdvanceByteBuffer
import cn.tursom.utils.bytebuffer.ByteArrayAdvanceByteBuffer
import cn.tursom.utils.bytebuffer.MultiAdvanceByteBuffer
import cn.tursom.utils.bytebuffer.NioAdvanceByteBuffer
import cn.tursom.utils.datastruct.ArrayBitSet
import java.nio.ByteBuffer


class LengthFieldPrependWriter(
	val prevWriter: SocketWriter<AdvanceByteBuffer>
) : SocketWriter<AdvanceByteBuffer> {
	constructor(socket: IAsyncNioSocket) : this(SimpSocketWriter(socket))

	override suspend fun put(value: AdvanceByteBuffer, timeout: Long) {
		val buffer = synchronized(bitMap) {
			val firstDown = bitMap.firstDown()
			if (firstDown < 0 || firstDown >= 1024) {
				ByteArrayAdvanceByteBuffer(4)
			} else {
				bitMap.up(firstDown)
				NioAdvanceByteBuffer(getMemory(firstDown.toInt()))
			}
		}
		buffer.put(value.readableSize)
		prevWriter.put(MultiAdvanceByteBuffer(buffer, value))
	}

	override fun close() {
		prevWriter.close()
	}

	companion object {
		@JvmStatic
		private val memoryPool = ByteBuffer.allocateDirect(4096)
		@JvmStatic
		private val bitMap = ArrayBitSet(1024)

		private fun getMemory(index: Int): ByteBuffer {
			memoryPool.position(index shr 2)
			memoryPool.limit((index shr 2) + 4)
			return memoryPool.slice()
		}
	}
}


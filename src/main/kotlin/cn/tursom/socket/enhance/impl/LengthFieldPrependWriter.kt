package cn.tursom.socket.enhance.impl

import cn.tursom.socket.IAsyncNioSocket
import cn.tursom.socket.enhance.SocketWriter
import cn.tursom.utils.bytebuffer.AdvanceByteBuffer
import cn.tursom.utils.bytebuffer.ByteArrayAdvanceByteBuffer
import cn.tursom.utils.bytebuffer.MultiAdvanceByteBuffer
import cn.tursom.utils.pool.DirectMemoryPool


class LengthFieldPrependWriter(
	val prevWriter: SocketWriter<AdvanceByteBuffer>
) : SocketWriter<AdvanceByteBuffer> {
	constructor(socket: IAsyncNioSocket) : this(SimpSocketWriter(socket))

	override suspend fun put(value: AdvanceByteBuffer, timeout: Long) {
		val memToken = directMemoryPool.allocate()
		val buffer = directMemoryPool.getAdvanceByteBuffer(memToken) ?: ByteArrayAdvanceByteBuffer(4)
		buffer.put(value.readableSize)
		prevWriter.put(MultiAdvanceByteBuffer(buffer, value))
		directMemoryPool.free(memToken)
	}

	override fun close() {
		prevWriter.close()
	}

	companion object {
		@JvmStatic
		private val directMemoryPool = DirectMemoryPool(4, 1024)
	}
}


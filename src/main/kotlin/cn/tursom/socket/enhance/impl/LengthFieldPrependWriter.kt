package cn.tursom.socket.enhance.impl

import cn.tursom.socket.IAsyncNioSocket
import cn.tursom.socket.enhance.SocketWriter
import cn.tursom.utils.bytebuffer.AdvanceByteBuffer
import cn.tursom.utils.bytebuffer.ByteArrayAdvanceByteBuffer
import cn.tursom.utils.bytebuffer.MultiAdvanceByteBuffer


class LengthFieldPrependWriter(
	val prevWriter: SocketWriter<AdvanceByteBuffer>
) : SocketWriter<AdvanceByteBuffer> {
	constructor(socket: IAsyncNioSocket) : this(SimpSocketWriter(socket))

	override suspend fun put(value: AdvanceByteBuffer, timeout: Long) {
		val buffer = ByteArrayAdvanceByteBuffer(4)
		buffer.put(value.readableSize)
		prevWriter.put(MultiAdvanceByteBuffer(buffer, value))
	}

	override fun close() {
		prevWriter.close()
	}
}


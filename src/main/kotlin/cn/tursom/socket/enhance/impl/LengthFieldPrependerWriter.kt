package cn.tursom.socket.enhance.impl

import cn.tursom.socket.IAsyncNioSocket
import cn.tursom.socket.enhance.SocketWriter
import cn.tursom.utils.bytebuffer.AdvanceByteBuffer
import cn.tursom.utils.bytebuffer.ByteArrayAdvanceByteBuffer


class LengthFieldPrependerWriter(
	val prevWriter: SocketWriter<AdvanceByteBuffer>
) : SocketWriter<AdvanceByteBuffer> {
	constructor(socket: IAsyncNioSocket) : this(SimpSocketWriter(socket))

	override suspend fun put(value: AdvanceByteBuffer, timeout: Long) {
		if (value.readableSize < 1024) {
			val buffer = ByteArrayAdvanceByteBuffer(value.readableSize + 4)
			buffer.put(value.readableSize)
			value.writeTo(buffer)
			prevWriter.put(buffer)
		} else {
			val buffer = ByteArrayAdvanceByteBuffer(1024)
			buffer.put(value.readableSize)
			while (value.readableSize != 0) {
				value.writeTo(buffer)
				prevWriter.put(value)
				buffer.clear()
			}
		}
	}

	override fun close() {
		prevWriter.close()
	}
}


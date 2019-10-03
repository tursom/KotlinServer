package cn.tursom.socket.enhance.impl

import cn.tursom.socket.AsyncNioSocket
import cn.tursom.socket.enhance.SocketWriter
import cn.tursom.utils.bytebuffer.AdvanceByteBuffer
import cn.tursom.utils.bytebuffer.ByteArrayAdvanceByteBuffer


class LengthFieldPrependerWriter(
    val prevWriter: SocketWriter<AdvanceByteBuffer>
) : SocketWriter<AdvanceByteBuffer> {
    override val socket: AsyncNioSocket get() = prevWriter.socket

    constructor(socket: AsyncNioSocket) : this(SimpSocketWriter(socket))

    override suspend fun write(value: AdvanceByteBuffer, timeout: Long) {
        if (value.readableSize < 1024) {
            val buffer = ByteArrayAdvanceByteBuffer(value.readableSize + 4)
            buffer.put(value.readableSize)
            value.writeTo(buffer)
            prevWriter.write(buffer)
        } else {
            val buffer = ByteArrayAdvanceByteBuffer(1024)
            buffer.put(value.readableSize)
            while (value.readableSize != 0) {
                value.writeTo(buffer)
                prevWriter.write(value)
                buffer.clear()
            }
        }
    }
}


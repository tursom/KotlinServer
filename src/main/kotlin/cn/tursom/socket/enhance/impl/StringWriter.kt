package cn.tursom.socket.enhance.impl

import cn.tursom.socket.AsyncNioSocket
import cn.tursom.socket.enhance.SocketWriter
import cn.tursom.utils.bytebuffer.AdvanceByteBuffer
import cn.tursom.utils.bytebuffer.ByteArrayAdvanceByteBuffer

class StringWriter(
    val prevWriter: SocketWriter<AdvanceByteBuffer>
) : SocketWriter<String> {
    override val socket: AsyncNioSocket get() = prevWriter.socket

    constructor(socket: AsyncNioSocket) : this(LengthFieldPrependerWriter(socket))

    override suspend fun write(value: String, timeout: Long) {
        val buf = ByteArrayAdvanceByteBuffer(value.toByteArray())
        buf.writePosition = buf.limit
        prevWriter.write(buf, timeout)
    }
}
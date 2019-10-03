package cn.tursom.socket.enhance.impl

import cn.tursom.socket.AsyncNioSocket
import cn.tursom.socket.enhance.SocketWriter
import cn.tursom.utils.bytebuffer.AdvanceByteBuffer

class SimpSocketWriter(
    override val socket: AsyncNioSocket
) : SocketWriter<AdvanceByteBuffer> {
    override suspend fun write(value: AdvanceByteBuffer, timeout: Long) {
        socket.write(value, timeout)
    }
}
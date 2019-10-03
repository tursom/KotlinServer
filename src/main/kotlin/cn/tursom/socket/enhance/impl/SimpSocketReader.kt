package cn.tursom.socket.enhance.impl

import cn.tursom.socket.AsyncNioSocket
import cn.tursom.socket.enhance.SocketReader
import cn.tursom.utils.bytebuffer.AdvanceByteBuffer
import cn.tursom.utils.bytebuffer.ByteArrayAdvanceByteBuffer

class SimpSocketReader(
    override val socket: AsyncNioSocket,
    val buffer: AdvanceByteBuffer = ByteArrayAdvanceByteBuffer(1024)
) : SocketReader<AdvanceByteBuffer> {
    override suspend fun read(timeout: Long): AdvanceByteBuffer {
        socket.read(buffer)
        return buffer
    }
}
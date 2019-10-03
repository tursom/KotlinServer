package cn.tursom.socket.enhance.impl

import cn.tursom.socket.AsyncNioSocket
import cn.tursom.socket.enhance.SocketReader
import cn.tursom.utils.bytebuffer.AdvanceByteBuffer
import cn.tursom.utils.bytebuffer.ByteArrayAdvanceByteBuffer

class LengthFieldBasedFrameReader(
    val prevReader: SocketReader<AdvanceByteBuffer>
) : SocketReader<AdvanceByteBuffer> {
    override val socket: AsyncNioSocket get() = prevReader.socket

    constructor(socket: AsyncNioSocket) : this(SimpSocketReader(socket))

    override suspend fun read(timeout: Long): AdvanceByteBuffer {
        val buffer1 = prevReader.read(timeout)
        val blockSize = buffer1.getInt()
        val targetBuffer = ByteArrayAdvanceByteBuffer(blockSize)
        buffer1.writeTo(targetBuffer)

        while (targetBuffer.writeableSize != 0) {
            val buf = prevReader.read(timeout)
            if (buf.readableSize == 0) return targetBuffer
            buf.writeTo(targetBuffer)
        }
        return targetBuffer
    }
}



package cn.tursom.socket.enhance.impl

import cn.tursom.socket.AsyncNioSocket
import cn.tursom.socket.enhance.SocketReader
import cn.tursom.utils.bytebuffer.AdvanceByteBuffer

class StringReader(
    val prevReader: SocketReader<AdvanceByteBuffer>
) : SocketReader<String> {
    override val socket: AsyncNioSocket get() = prevReader.socket

    constructor(socket: AsyncNioSocket) : this(LengthFieldBasedFrameReader(socket))

    override suspend fun read(timeout: Long): String {
        return prevReader.read(timeout).getString()
    }
}
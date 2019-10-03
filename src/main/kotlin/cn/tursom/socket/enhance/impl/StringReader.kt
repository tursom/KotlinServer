package cn.tursom.socket.enhance.impl

import cn.tursom.socket.IAsyncNioSocket
import cn.tursom.socket.enhance.SocketReader
import cn.tursom.utils.bytebuffer.AdvanceByteBuffer

class StringReader(
    val prevReader: SocketReader<AdvanceByteBuffer>
) : SocketReader<String> {
    override val socket: IAsyncNioSocket get() = prevReader.socket

    constructor(socket: IAsyncNioSocket) : this(LengthFieldBasedFrameReader(socket))

    override suspend fun read(timeout: Long): String {
        return prevReader.read(timeout).getString()
    }
}
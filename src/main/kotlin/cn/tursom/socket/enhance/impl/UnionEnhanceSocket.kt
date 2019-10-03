package cn.tursom.socket.enhance.impl

import cn.tursom.socket.AsyncNioSocket
import cn.tursom.socket.enhance.EnhanceSocket
import cn.tursom.socket.enhance.SocketReader
import cn.tursom.socket.enhance.SocketWriter

@Suppress("MemberVisibilityCanBePrivate")
class UnionEnhanceSocket<Read, Write>(
    val prevReader: SocketReader<Read>,
    val prevWriter: SocketWriter<Write>
) : EnhanceSocket<Read, Write>,
    SocketReader<Read> by prevReader,
    SocketWriter<Write> by prevWriter {
    override val socket: AsyncNioSocket get() = prevReader.socket
}
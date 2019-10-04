package cn.tursom.socket.enhance.impl

import cn.tursom.socket.IAsyncNioSocket
import cn.tursom.socket.enhance.SocketReader
import cn.tursom.utils.bytebuffer.AdvanceByteBuffer

class StringReader(
	val prevReader: SocketReader<AdvanceByteBuffer>
) : SocketReader<String> {
	constructor(socket: IAsyncNioSocket) : this(LengthFieldBasedFrameReader(socket))

	override suspend fun readSocket(buffer: AdvanceByteBuffer, timeout: Long): String {
		return prevReader.readSocket(buffer, timeout).getString()
	}

	override fun close() {
		prevReader.close()
	}
}
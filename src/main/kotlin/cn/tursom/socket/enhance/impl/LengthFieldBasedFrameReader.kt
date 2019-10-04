package cn.tursom.socket.enhance.impl

import cn.tursom.socket.IAsyncNioSocket
import cn.tursom.socket.enhance.SocketReader
import cn.tursom.utils.bytebuffer.AdvanceByteBuffer
import cn.tursom.utils.bytebuffer.ByteArrayAdvanceByteBuffer

class LengthFieldBasedFrameReader(
	val prevReader: SocketReader<AdvanceByteBuffer>
) : SocketReader<AdvanceByteBuffer> {
	constructor(socket: IAsyncNioSocket) : this(SimpSocketReader(socket))

	override suspend fun readSocket(buffer: AdvanceByteBuffer, timeout: Long): AdvanceByteBuffer {
		val rBuf = prevReader.readSocket(buffer, timeout)
		val blockSize = rBuf.getInt()
		if (rBuf.readableSize == blockSize) return rBuf

		val targetBuffer = ByteArrayAdvanceByteBuffer(blockSize)
		rBuf.writeTo(targetBuffer)
		while (targetBuffer.writeableSize != 0) {
			val rBuf2 = prevReader.readSocket(buffer, timeout)
			if (rBuf2.readableSize == 0) return targetBuffer
			rBuf2.writeTo(targetBuffer)
		}
		return targetBuffer
	}

	override fun close() {
		prevReader.close()
	}
}



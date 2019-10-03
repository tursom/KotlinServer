package cn.tursom.socket.enhance.impl

import cn.tursom.socket.IAsyncNioSocket
import cn.tursom.socket.enhance.SocketReader
import cn.tursom.utils.bytebuffer.AdvanceByteBuffer
import cn.tursom.utils.bytebuffer.ByteArrayAdvanceByteBuffer

class SimpSocketReader(
	override val socket: IAsyncNioSocket,
	val buffer: AdvanceByteBuffer = ByteArrayAdvanceByteBuffer(1024)
) : SocketReader<AdvanceByteBuffer> {
	override suspend fun read(timeout: Long): AdvanceByteBuffer {
		if (socket.read(buffer) < 0) {
			socket.close()
		}
		return buffer
	}
}
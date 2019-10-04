package cn.tursom.socket.enhance

import cn.tursom.utils.bytebuffer.AdvanceByteBuffer
import java.io.Closeable

interface SocketReader<T> : Closeable {
	suspend fun get(buffer: AdvanceByteBuffer, timeout: Long = 0): T
	override fun close()
}


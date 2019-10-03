package cn.tursom.socket.enhance

import cn.tursom.socket.AsyncNioSocket

interface SocketReader<T> {
	val socket: AsyncNioSocket
	suspend fun read(timeout: Long = 0): T
}


package cn.tursom.socket.enhance

import cn.tursom.socket.IAsyncNioSocket

interface SocketReader<T> {
	val socket: IAsyncNioSocket
	suspend fun read(timeout: Long = 0): T
}


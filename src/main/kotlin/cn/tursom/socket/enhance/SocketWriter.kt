package cn.tursom.socket.enhance

import cn.tursom.socket.AsyncNioSocket

interface SocketWriter<T> {
	val socket: AsyncNioSocket
	suspend fun write(value: T, timeout: Long = 0)
}
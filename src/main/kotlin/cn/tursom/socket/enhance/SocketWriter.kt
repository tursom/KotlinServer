package cn.tursom.socket.enhance

import cn.tursom.socket.IAsyncNioSocket

interface SocketWriter<T> {
	val socket: IAsyncNioSocket
	suspend fun write(value: T, timeout: Long = 0)
}
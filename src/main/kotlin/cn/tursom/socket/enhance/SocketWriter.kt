package cn.tursom.socket.enhance

import java.io.Closeable

interface SocketWriter<T> : Closeable {
	suspend fun write(value: T, timeout: Long = 0)
}
package cn.tursom.socket.server

import cn.tursom.socket.BaseSocket
import java.io.Closeable

abstract class SocketServer(val handler: BaseSocket.() -> Unit) : Runnable, Closeable {
	companion object {
		val cpuNumber = Runtime.getRuntime().availableProcessors() //CPU处理器的个数
	}
}
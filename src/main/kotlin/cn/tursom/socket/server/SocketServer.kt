package cn.tursom.socket.server

import java.io.Closeable
import java.net.Socket

abstract class SocketServer(val handler: Socket.() -> Unit) : Runnable, Closeable {
	companion object {
		val cpuNumber = Runtime.getRuntime().availableProcessors() //CPU处理器的个数
	}
}
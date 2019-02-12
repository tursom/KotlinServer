package cn.tursom.socket.server

import java.net.ServerSocket
import java.net.Socket

class SingleThreadSocketServer(
	private val serverSocket: ServerSocket,
	val exception: Exception.() -> Unit = {
		printStackTrace()
	},
	handler: Socket.() -> Unit
) : SocketServer(handler) {
	
	constructor(
		port: Int,
		exception: Exception.() -> Unit = {
			printStackTrace()
		},
		handler: Socket.() -> Unit
	) : this(ServerSocket(port), exception, handler)
	
	override fun run() {
		while (!serverSocket.isClosed) {
			serverSocket.accept().use {
				try {
					it.handler()
				} catch (e: Exception) {
					e.exception()
				}
			}
		}
	}
	
	override fun close() {
		try {
			serverSocket.close()
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}
}
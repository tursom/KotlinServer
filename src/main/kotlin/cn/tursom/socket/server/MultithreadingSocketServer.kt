package cn.tursom.socket.server

import java.net.ServerSocket
import java.net.Socket

class MultithreadingSocketServer(
	val serverSocket: ServerSocket,
	val threadNumber: Int = cpuNumber,
	val exception: Exception.() -> Unit = {
		printStackTrace()
	},
	handler: Socket.() -> Unit
) : SocketServer(handler) {
	
	constructor(
		port: Int,
		threadNumber: Int = cpuNumber,
		exception: Exception.() -> Unit = {
			printStackTrace()
		},
		handler: Socket.() -> Unit
	) : this(ServerSocket(port), threadNumber, exception, handler)
	
	private val threadList = ArrayList<Thread>()
	
	override fun run() {
		for (i in 1..threadNumber) {
			val thread = Thread {
				while (true) {
					serverSocket.accept().use {
						try {
							it.handler()
						} catch (e: Exception) {
							e.exception()
						}
					}
				}
			}
			thread.start()
			threadList.add(thread)
		}
	}
	
	override fun close() {
		serverSocket.close()
	}
}
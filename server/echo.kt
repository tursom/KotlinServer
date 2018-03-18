package server

import tools.Passcode

val passcode = Passcode()

fun main(args: Array<String>) {
	
	val server = object : server.SocketServer(12345, SocketServer.cpuNumber) {
		
		override val handler: Runnable
			get() = object : server.ServerHandler(socket!!) {
				override fun handle() {
					val recv = recv(1024)
					when (recv) {
						passcode.toString() -> {
							close()
							throw object : ServerException("server closed") {
								override val code: ByteArray
									get() = "server closed".toByteArray()
							}
						}
						else -> outputStream.write(recv?.toByteArray())
					}
				}
			}
	}
	
	passcode.showPasscode()
	server.start()
	server.close()
}
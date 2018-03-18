package server

val exitCode = RandomCode()
const val port=12346

fun main(args: Array<String>) {
	
	val server = object : server.SocketServer(port, SocketServer.cpuNumber) {
		
		override val handler: Runnable
			get() = object : server.ServerHandler(socket!!) {
				override fun handle() {
					val recv = recv(1024)
					when (recv) {
						exitCode.toString() -> {
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
	
	val interactiveCommand = object : HashMap<String, () -> Unit>() {
		init {
			this[exitCode.toString()] = {
				server.close()
				throw Interactive.CloseException()
			}
			this["close"] = {
				server.close()
				throw Interactive.CloseException()
			}
			this["exitcode"] = { println(exitCode) }
		}
	}
	object : Interactive(interactiveCommand) {
		override fun run() {
			println("server running in port $port")
			register.server.passcode.showPasscode("passcode")
			super.run()
		}
	}.start()
	
	exitCode.showCode()
	server.start()
}

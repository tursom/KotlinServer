package cn.tursom.socket.server

import java.net.InetAddress

val exitCode = RandomCode()
const val port = 12346

fun main(args: Array<String>) {
	object : SocketServer(port, cpuNumber * 2) {
		override val handler: Runnable
			get() = object : ServerHandler(socketQueue.poll()!!) {
				override fun handle() {
					println("connection from $address, local port=$localport")
					var recv: ByteArray
					var srecv = ""
					while (srecv != exitCode.toString()) {
						recv = recvByteArray(102400, 2, 2) ?: break
						outputStream.write(recv)
						srecv = String(recv)
						println("recv from $address: $srecv")
					}
					socket.close()
					throw object : ServerException("socket closed") {
						override val code: ByteArray
							get() = "socket closed".toByteArray()
					}
				}
			}
	}.start()
	println("server running in ${InetAddress.getLocalHost().hostAddress}:$port")
	exitCode.showCode(codeName = "exit code", filepath = "passcode")
}
package cn.tursom.socket.client

const val localhost = "127.0.0.1"
const val localport = 12346

fun main(args: Array<String>) {
	val client = SocketClient(localhost, localport)
	client.execute {
		val bufferedReader = System.`in`.bufferedReader()
		while (client.isConnected()) {
			val message = bufferedReader.readLine() ?: ""
			client.send(message)
			val timeRecvStart = System.currentTimeMillis()
			val recv = client.recvByteArray(102400)
			val timeRecvEnd = System.currentTimeMillis()
			if (recv?.size == 102400) {
				client.clean()
			}
//			println("recv from server ${client.address}:${String(recv ?: "".toByteArray())}")
			println("recv size: ${recv?.size}")
			println("recv using time: ${timeRecvEnd - timeRecvStart}ms")
			if (message == "exit client") throw SocketClient.ClientException("we are exiting socket client")
		}
	}
}
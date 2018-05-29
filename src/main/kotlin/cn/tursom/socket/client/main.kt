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
			val time3 = System.currentTimeMillis()
			val recv = client.recvByteArray(102400, 2, 2)
			val time4 = System.currentTimeMillis()
			println("recv from server ${client.address}:${String(recv ?: "".toByteArray())}")
			println("size: ${recv?.size}")
			println("recv using time: ${time4 - time3}ms")
			if (message == "exit client") throw SocketClient.ClientException("we are exiting socket client")
		}
	}
}
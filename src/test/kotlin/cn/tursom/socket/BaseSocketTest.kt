package cn.tursom.socket

import cn.tursom.socket.server.SingleThreadSocketServer
import org.junit.Test
import java.net.Socket

class BaseSocketTest {
	@Test
	fun baseSocketTest() {
		val port = 12345
		SingleThreadSocketServer(port) {
			send(recvString())
		}.use { server ->
			Thread(server).start()
			val socket = BaseSocket(Socket("127.0.0.1", 12345))
			socket.send("hello")
			assert(socket.recvString() == "hello")
		}
	}
}
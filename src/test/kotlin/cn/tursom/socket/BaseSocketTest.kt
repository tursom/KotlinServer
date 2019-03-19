package cn.tursom.socket

import org.junit.Test
import java.net.Socket

class BaseSocketTest {
	@Test
	fun baseSocketTest() {
		val socket = BaseSocket(Socket("127.0.0.1", 12345))
		socket.send("hello")
		println(socket.recvString())
	}
}
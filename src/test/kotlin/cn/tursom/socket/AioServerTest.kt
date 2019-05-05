package cn.tursom.socket

import cn.tursom.socket.client.SocketClient
import cn.tursom.socket.server.aio.AioServer
import org.junit.Test

class AioServerTest {
	@Test
	fun testServer() {
		val port = 12345
		val server = AioServer(port) {
			recvStr { str -> println("server recved $str") }
			send {
				buffer.flip()
				buffer
			}
		}
		Thread(server).start()
		val client = SocketClient("127.0.0.1", port)
		client.send("hi")
		println("client recving: ${client.recvString()}")
		server.close()
	}
}
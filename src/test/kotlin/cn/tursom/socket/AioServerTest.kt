package cn.tursom.socket

import cn.tursom.socket.client.SocketClient
import cn.tursom.socket.server.aio.AioServer
import org.junit.Test

class AioServerTest {
	@Test
	fun testServer() {
		val port = 12345
		val server = AioServer(port) {
			timeout = 1000L
			val startIndex = recvStr { str -> println("${System.currentTimeMillis()} server recved $str") }
			// 执行完应当循环执行第一步
			send(next = { startIndex }) {
				buffer.flip()
				buffer
			}
		}
		server.run()
		
		for (i in 1..2) {
			SocketClient("127.0.0.1", port).use {
				for (j in 1..3) {
					send("client $i, loop $j")
					println("${System.currentTimeMillis()} client recving: ${recvString()}")
				}
			}
		}
		server.close()
	}
}
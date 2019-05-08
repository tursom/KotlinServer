package cn.tursom.socket

import cn.tursom.socket.client.AsyncClient
import cn.tursom.socket.server.AsyncSocketServer
import org.junit.Test
import java.lang.Thread.sleep
import java.nio.ByteBuffer


class AsyncClientTest {
	
	@Test
	fun test() {
		val port = 12345
		val server = AsyncSocketServer(port) {
			val buffer = ByteBuffer.allocate(100)
			recv(buffer)
			buffer.flip()
			send(buffer)
		}
		Thread(server).start()
		println("server started")
		val t1 = System.currentTimeMillis()
		var t2 = System.currentTimeMillis()
		var e = 0
		for (i in 1..1_0000) {
			try {
				AsyncClient.connect("127.0.0.1", port).useNonBlock {
					val buffer = ByteBuffer.allocate(100)
					send("client $i say hello!")
					buffer.clear()
					recv(buffer)
					//				println(String(buffer.array(), 0, buffer.position()))
					val t = System.currentTimeMillis()
					close()
					synchronized(t2) {
						if (t2 < t) {
							t2 = t
						}
					}
				}
			} catch (_: Exception) {
				e++
			}
		}
		sleep(20000)
		println(t2 - t1)
		println(e)
		server.close()
	}
}
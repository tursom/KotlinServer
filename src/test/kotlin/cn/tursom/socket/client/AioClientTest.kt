package cn.tursom.socket.client

import cn.tursom.socket.server.SingleThreadSocketServer
import org.junit.Test
import java.lang.Thread.sleep
import java.nio.ByteBuffer

class AioClientTest {
	@Test
	fun testAioClient() {
		val port = 12345
		val server = SingleThreadSocketServer(port) {
			val recv = recvString()
			println("${System.currentTimeMillis()}: server recved: $recv")
			send(recv)
		}
		Thread(server).start()
		AioClient("127.0.0.1", port) {
			tryCatch { printStackTrace() }
			send { ByteBuffer.wrap("Hello".toByteArray()) }
			recv { size, buffer ->
				println("${System.currentTimeMillis()}: ${String(buffer.array(), 0, size)}")
			}
			run {
				println("${System.currentTimeMillis()}: client run()")
				close()
				server.close()
			}
			println("${System.currentTimeMillis()}: end process: $this")
		}
		
		sleep(1000)
	}
}
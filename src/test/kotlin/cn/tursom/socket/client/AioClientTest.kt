package cn.tursom.socket.client

import cn.tursom.socket.recvStr
import cn.tursom.socket.sendStr
import cn.tursom.socket.server.SingleThreadSocketServer
import org.junit.Test
import java.lang.Thread.sleep
import java.nio.ByteBuffer

class AioClientTest {
	@Test
	fun testAioClient() {
		val port = 12345
		val server = SingleThreadSocketServer(port) {
			while (true) {
				val recv = recvString()
				println("${System.currentTimeMillis()}: server recved: $recv")
				send(recv)
			}
		}
		Thread(server).start()
		AioClient("127.0.0.1", port) {
			tryCatch { printStackTrace() }
			this sendStr "Hello, NIO!"
			val recvBuffer = ByteBuffer.allocate(4096)
			recvStr(recvBuffer) { str ->
				println("${System.currentTimeMillis()}: read 1: $str")
			}
			send {
				recvBuffer.flip()
				recvBuffer
			}
			recvStr(recvBuffer) { str ->
				println("${System.currentTimeMillis()}: read 2: $str")
			}
			run {
				println("${System.currentTimeMillis()}: client run()")
				close()
				server.close()
			}
			println("${System.currentTimeMillis()}: end process: $this")
		}
		
		sleep(2000)
	}
}
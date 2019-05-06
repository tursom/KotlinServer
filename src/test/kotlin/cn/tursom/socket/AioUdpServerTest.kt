package cn.tursom.socket

import cn.tursom.datagram.client.UdpClient
import cn.tursom.datagram.server.AioUdpServer
import org.junit.Test

class AioUdpServerTest {
	@Test
	fun test() {
		val port = 12345
		val server = AioUdpServer(port) { channel, address, buffer ->
			buffer.flip()
			send(channel, address, buffer)
			read(address, 1000L) {
				it.flip()
				send(channel, address, it)
			}
		}
		Thread(server).start()
		UdpClient("127.0.0.1", port).use {
			it.send("hello".toByteArray()) { bytes, size ->
				println(String(bytes, 0, size))
			}
			it.send("hello2".toByteArray()) { bytes, size ->
				println(String(bytes, 0, size))
			}
			server.close()
		}
	}
}
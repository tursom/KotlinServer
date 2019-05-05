package cn.tursom.socket

import cn.tursom.socket.client.AioClient
import cn.tursom.socket.server.aio.AioServer
import org.junit.Test

class AioServerTest {
	@Test
	fun testServer() {
		val port = 12345
		val server = AioServer(port) {
			timeout = 1000L
			recvStr { str ->
				val time = System.currentTimeMillis()
				println("$time: server recved:\"$str\"")
			}
			// 执行完应当循环执行第一步
			send(next = { 0 }) {
				buffer.flip()
				buffer
			}
		}
		
		
		for (i in 1..10000) {
			var j = 1
			AioClient("127.0.0.1", port) {
				sendStr { "client $i loop $j" }
				recvStr({ buffer }, {
					if (j <= 10) 0
					else 100
				}) { recv ->
					println("${System.currentTimeMillis()}: client recving: $recv")
					j++
				}
			}
		}

//		for (i in 1..500) {
//			Thread({
//				for (k in 1..200) {
//					SocketClient("127.0.0.1", port).use {
//						for (j in 1..1) {
//							send("client $i$k, loop $j")
//							val recv = recvString()
////						println("${System.currentTimeMillis()}: client recving: $recv")
//						}
//					}
//				}
//			}, "SocketClient$i").start()
//		}

//		sleep(15000)
		println(AioSocket.maxId)
		server.close()
	}
}
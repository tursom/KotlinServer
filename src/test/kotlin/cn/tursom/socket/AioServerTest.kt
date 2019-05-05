package cn.tursom.socket

import cn.tursom.socket.client.AioClient
import cn.tursom.socket.server.aio.AioServer
import org.junit.Test
import java.lang.Thread.sleep

class AioServerTest {
	@Test
	fun testServer() {
		val port = 12345
		var beg: Long = Long.MAX_VALUE
		var end = 0L
		val server = AioServer(port) {
			timeout = 1000L
			val startIndex = recvStr { str ->
				val time = System.currentTimeMillis()
				if (time < beg) beg = time
				if (time > end) end = time
				println("$time: ${Thread.currentThread()}: server recved $str")
			}
			// 执行完应当循环执行第一步
			send(next = { startIndex }) {
				buffer.flip()
				buffer
			}
		}
		
		
		for (i in 1..100) {
			AioClient("127.0.0.1", port) {
				var j = 1
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

//		for (i in 1..100) {
//			Thread({
//				SocketClient("127.0.0.1", port).use {
//					for (j in 1..10) {
//						send("client $i, loop $j")
//						val recv = recvString()
//						println("${System.currentTimeMillis()}: client recving: $recv")
//					}
//				}
//			}, "SocketClient$i").start()
//		}
		sleep(2000)
		println(end - beg)
		server.close()
	}
}
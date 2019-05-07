package cn.tursom.socket

import cn.tursom.datagram.client.UdpClient
import cn.tursom.datagram.server.MultiThreadUDPServer
import io.netty.util.HashedWheelTimer
import org.junit.Test
import java.lang.Thread.sleep
import java.util.concurrent.TimeUnit

class UdpServerTest {
	@Test
	fun test() {
		val port = 12345
		@Suppress("NAME_SHADOWING") val server = MultiThreadUDPServer(port) { address, buffer, size ->
			// 先设定下一次接收数据的处理方法
			recv(address) { address, buffer, size ->
				println("server recv twice: ${String(buffer, 0, size)}\n" +
					"server thread: ${Thread.currentThread()}")
				send(address, buffer, size)
			}
			
			// 然后处理数据
			println("server first recv: ${String(buffer, 0, size)}\nserver thread: ${Thread.currentThread()}")
			send(address, buffer, size)
		}
		
		// 启动服务
		server.start()
		println("UDP Server started")
		
		UdpClient("127.0.0.1", port).use {
			for (i in 1..3) {
				it.send("single thread test: $i".toByteArray()) { bytes, size ->
					println(String(bytes, 0, size))
				}
			}
		}
		
		for (j in 1..3) {
			Thread {
				UdpClient("127.0.0.1", port).use {
					for (i in 1..3) {
						it.send("thread $j test $i".toByteArray()) { bytes, size ->
							println(String(bytes, 0, size))
						}
					}
				}
			}.start()
		}
		
		sleep(1000)
		server.close()
	}
	
	@Test
	fun multiThreadTest() {
		val port = 12345
		@Suppress("NAME_SHADOWING") val server = MultiThreadUDPServer(port) { address, buffer, size ->
			// 先设定下一次接收数据的处理方法
			recv(address) { address, buffer, size ->
				println("server recv twice: ${String(buffer, 0, size)}\n" +
					"server thread: ${Thread.currentThread()}")
				send(address, buffer, size)
			}
			
			// 然后处理数据
			println("server first recv: ${String(buffer, 0, size)}\n" +
				"server thread: ${Thread.currentThread()}")
			send(address, buffer, size)
		}
		
		// 启动多线程服务
		for (i in 1..4) {
			Thread(server, "ThreadPoolUDPServer$i").start()
			println("UDP Server $i started")
		}
		
		UdpClient("127.0.0.1", port).use {
			for (i in 1..3) {
				it.send("single thread test: $i".toByteArray()) { bytes, size ->
					println(String(bytes, 0, size))
				}
			}
		}
		
		for (j in 1..3) {
			Thread {
				UdpClient("127.0.0.1", port).use {
					for (i in 1..3) {
						it.send("thread $j test $i".toByteArray()) { bytes, size ->
							println(String(bytes, 0, size))
						}
					}
				}
			}.start()
		}
		
		sleep(1000)
		server.close()
	}
	
	@Test
	fun timerTest() {
		val excWheelTimer = HashedWheelTimer()
		var time = 0
		for (i in 1..100) {
			Thread {
				sleep(100)
				excWheelTimer.newTimeout({
					synchronized(time) { time++ }
				}, 10L, TimeUnit.MILLISECONDS)
			}.start()
		}
		sleep(3000)
		println(time)
	}
}
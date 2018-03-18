package server

import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors

/*
 * SocketServer多线程服务器
 * 每当有新连接接入时就会将handler:Runnable加入线程池的任务队列中运行
 * 通过重载handler:Runnable的getter实现多态
 * start()函数实现无限循环监听，同时自动处理异常
 * 最新接入的套接字出存在socket变量中
 * 通过调用close()或closeServer()关闭服务器，造成的异常会被自动处理
 * cpuNumber是CPU处理器的个数
 */
open class SocketServer(port: Int, threads: Int = 1):Thread() {
	var socket: Socket? = null
	private val pool = Executors.newFixedThreadPool(threads)!!
	private var server = ServerSocket(port)
	
	override fun run() {
		while (true) {
			try {
				socket = server.accept()
				pool.execute(handler)
			} catch (e: IOException) {
				if (pool.isShutdown || server.isClosed) {
					close()
					System.err.println("server closed")
					return
				}
				e.printStackTrace()
			}
		}
	}
	
	open val handler: Runnable
		get() = Runnable {
			socket?.close()
		}
	
	fun closeServer() {
		if (!server.isClosed)
			server.close()
	}
	
	fun shutdownPool() {
		if (!pool.isShutdown)
			pool.shutdown()
	}
	
	fun close() {
		shutdownPool()
		closeServer()
	}
	
	companion object {
		val cpuNumber = Runtime.getRuntime().availableProcessors()
	}
}

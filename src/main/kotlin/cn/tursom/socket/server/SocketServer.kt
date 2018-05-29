package cn.tursom.socket.server

import cn.tursom.tools.getTAG
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * SocketServer多线程服务器
 * 每当有新连接接入时就会将handler:Runnable加入线程池的任务队列中运行
 * 通过重载handler:Runnable的getter实现多态
 * start()函数实现无限循环监听，同时自动处理异常
 * 最新接入的套接字出存在socket变量中
 * 通过调用close()或closeServer()关闭服务器，造成的异常会被自动处理
 * cpuNumber是CPU处理器的个数
 */
open class SocketServer(
		port: Int, threads: Int = 1,
		queueSize: Int = 2147483647,
		timeout: Long = 0L,
		timeUnit: TimeUnit = TimeUnit.MILLISECONDS,
		startImmediately: Boolean = false) : Thread() {

	val socketQueue: Queue<Socket> = LinkedList<Socket>()
	private val pool = ThreadPoolExecutor(threads, threads, timeout, timeUnit, LinkedBlockingQueue(queueSize))
	private var serverSocket: ServerSocket = ServerSocket(port)

	init {
		if (startImmediately) {
			start()
		}
	}

	/**
	 * 主要作用：封闭start()，防止用户重载start()
	 */
	final override fun start() {
		super.start()
	}

	/**
	 * 主要作用：
	 * 循环接受连接请求
	 * 讲接收的连接交给handler处理
	 * 连接初期异常处理
	 * 自动关闭套接字服务器与线程池
	 */
	override fun run() {
		var socket = Socket()
		while (!serverSocket.isClosed) {
			try {
				socket = serverSocket.accept()
				socketQueue.offer(socket)
				println("$TAG: run(): get connect: $socket")
				pool.execute(handler)
			} catch (e: IOException) {
				if (pool.isShutdown || serverSocket.isClosed) {
					System.err.println("server closed")
					break
				}
				e.printStackTrace()
			} catch (e: SocketException) {
				e.printStackTrace()
				break
			} catch (e: RejectedExecutionException) {
				socket.getOutputStream()?.write(poolIsFull)
			} catch (e: Exception) {
				e.printStackTrace()
				break
			}
		}
		whenClose()
		close()
		System.err.println("server closed")
	}

	/**
	 * 交给用户重载的属性，用于处理连接请求
	 * 默认是立即关闭套接字
	 */
	open val handler: Runnable
		get() = Runnable {
			socketQueue.poll()?.close()
		}

	fun closeServer() {
		if (!serverSocket.isClosed)
			serverSocket.close()
	}

	fun shutdownPool() {
		if (!pool.isShutdown)
			pool.shutdown()
	}

	fun isClosed() = pool.isShutdown || serverSocket.isClosed

	open fun close() {
		shutdownPool()
		closeServer()
	}

	open fun whenClose() {
	}

	fun setPort(port: Int) {
		serverSocket = ServerSocket(port)
	}

	open val poolIsFull
		get() = Companion.poolIsFull

	companion object {
		val TAG = getTAG(this::class.java)
		val cpuNumber = Runtime.getRuntime().availableProcessors()
		val poolIsFull = "server pool is full".toByteArray()
	}
}
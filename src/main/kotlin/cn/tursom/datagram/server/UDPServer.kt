package cn.tursom.datagram.server

import java.io.Closeable

interface UDPServer : Runnable, Closeable {
	val port: Int
	
	fun start()
	
	@Suppress("MemberVisibilityCanBePrivate")
	companion object {
		//定义不同环境下数据报的最大大小
		const val LANNetLen = 1472
		const val internetLen = 548
		const val defaultLen = internetLen
	}
}

object UdpPackageSize {
	//定义不同环境下数据报的最大大小
	const val LANNetLen = 1472
	const val internetLen = 548
	const val defaultLen = internetLen
}
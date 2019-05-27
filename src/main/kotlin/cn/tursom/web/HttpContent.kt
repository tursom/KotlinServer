package cn.tursom.web

import java.io.OutputStream
import java.net.SocketAddress

interface HttpContent {
	val uri: String
	var responseCode: Int
	var responseMessage: String?
	val body: ByteArray?
	val bodyOffSet: Int
	val readableBytes: Int
	val clientIp: SocketAddress
	val method: String
	val responseBody: OutputStream
	
	fun getHeader(header: String): String?
	fun getHeaders(): List<Map.Entry<String, String>>
	
	fun getParam(param: String): String?
	fun getParams(): Map<String, List<String>>
	fun getParams(param: String): List<String>?
	
	fun setResponseHeader(name: String, value: Any)
	
	fun write(message: String)
	fun write(byte: Int)
	fun write(bytes: ByteArray)
	fun reset()
	
	fun finish()
}
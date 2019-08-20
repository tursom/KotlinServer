package cn.tursom.web

import cn.tursom.utils.bytebuffer.AdvanceByteBuffer
import java.io.OutputStream
import java.net.SocketAddress

interface HttpContent {
	val uri: String
	var responseCode: Int
	var responseMessage: String?
	val body: AdvanceByteBuffer?
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
	fun write(bytes: ByteArray, offset: Int = 0, size: Int = 0)
	fun write(buffer: AdvanceByteBuffer)
	fun reset()

	fun finish()

	fun finish(code: Int) = finishHtml(code)

	fun finishHtml(code: Int = responseCode) {
		responseCode = code
		setResponseHeader("content-type", "text/html; charset=UTF-8")
		finish()
	}

	fun finishText(code: Int = responseCode) {
		responseCode = code
		setResponseHeader("content-type", "text/plain; charset=UTF-8")
		finish()
	}

	fun finishJson(code: Int = responseCode) {
		responseCode = code
		setResponseHeader("content-type", "application/json; charset=UTF-8")
		finish()
	}
}
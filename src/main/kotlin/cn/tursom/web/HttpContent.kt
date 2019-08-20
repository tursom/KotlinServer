package cn.tursom.web

import cn.tursom.utils.buf
import cn.tursom.utils.bytebuffer.AdvanceByteBuffer
import cn.tursom.utils.count
import java.io.ByteArrayOutputStream
import java.net.SocketAddress

interface HttpContent {
	val uri: String
	var responseCode: Int
	var responseMessage: String?
	val body: AdvanceByteBuffer?
	val clientIp: SocketAddress
	val method: String
	val responseBody: ByteArrayOutputStream

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

	fun finish() {
		finish(responseBody.buf, 0, responseBody.count)
	}

	fun finish(response: ByteArray, offset: Int = 0, size: Int = response.size - offset)

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

	fun finishHtml(code: Int = responseCode, response: ByteArray) {
		responseCode = code
		setResponseHeader("content-type", "text/html; charset=UTF-8")
		finish(response)
	}

	fun finishText(code: Int = responseCode, response: ByteArray) {
		responseCode = code
		setResponseHeader("content-type", "text/plain; charset=UTF-8")
		finish(response)
	}

	fun finishJson(code: Int = responseCode, response: ByteArray) {
		responseCode = code
		setResponseHeader("content-type", "application/json; charset=UTF-8")
		finish(response)
	}
}
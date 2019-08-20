package cn.tursom.web.utils

import cn.tursom.utils.bytebuffer.AdvanceByteBuffer
import cn.tursom.web.HttpContent
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.SocketAddress

class EmptyHttpContent(
	override val uri: String = "/",
	override var responseCode: Int = 200,
	override var responseMessage: String? = null,
	override val body: AdvanceByteBuffer? = null,
	override val clientIp: SocketAddress = InetSocketAddress(0),
	override val method: String = "GET",
	override val responseBody: OutputStream = ByteArrayOutputStream(0)
) : HttpContent {
	override fun getHeader(header: String): String? = null
	override fun getHeaders(): List<Map.Entry<String, String>> = listOf()
	override fun getParam(param: String): String? = null
	override fun getParams(): Map<String, List<String>> = mapOf()
	override fun getParams(param: String): List<String>? = null
	override fun setResponseHeader(name: String, value: Any) {}
	override fun write(message: String) {}
	override fun write(byte: Int) {}
	override fun write(bytes: ByteArray, offset: Int, size: Int) {}
	override fun write(buffer: AdvanceByteBuffer) {}
	override fun reset() {}
	override fun finish() {}
}


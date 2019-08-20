package cn.tursom.web.netty

import cn.tursom.utils.bytebuffer.AdvanceByteBuffer
import cn.tursom.utils.bytebuffer.NettyAdvanceByteBuffer
import cn.tursom.web.AdvanceHttpContent
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.*
import java.io.ByteArrayOutputStream
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set


@Suppress("MemberVisibilityCanBePrivate")
open class NettyHttpContent(
	val ctx: ChannelHandlerContext,
	val msg: FullHttpRequest
) : AdvanceHttpContent {
	override val uri: String get() = msg.uri()
	val headers: HttpHeaders get() = msg.headers()
	private val paramMap by lazy { RequestParser.parse(msg) }
	val responseMap = HashMap<String, Any>()

	override var responseCode: Int = 200
	override var responseMessage: String? = null
	override val clientIp get() = ctx.channel().remoteAddress()!!
	override val method: String get() = httpMethod.name()

	override val responseBody = ByteArrayOutputStream()
	val httpMethod get() = msg.method()
	val protocolVersion get() = msg.protocolVersion()

	override val body = msg.content()?.let { NettyAdvanceByteBuffer(it) }

	override fun getHeader(header: String): String? {
		return headers[header]
	}

	override fun getHeaders(): List<Map.Entry<String, String>> {
		return headers.toList()
	}

	override fun getParam(param: String): String? {
		return paramMap[param]?.get(0)
	}

	override fun getParams(): Map<String, List<String>> {
		return paramMap
	}

	override fun getParams(param: String): List<String>? {
		return paramMap[param]
	}

	override fun addParam(key: String, value: String) {
		if (!paramMap.containsKey(key)) {
			paramMap[key] = ArrayList()
		}
		(paramMap[key] as ArrayList).add(value)
	}

	override fun setResponseHeader(name: String, value: Any) {
		responseMap[name] = value
	}

	override fun write(message: String) {
		responseBody.write(message.toByteArray())
	}

	override fun write(byte: Int) {
		responseBody.write(byte)
	}

	override fun write(bytes: ByteArray, offset: Int, size: Int) {
		responseBody.write(bytes, offset, size)
	}

	override fun write(buffer: AdvanceByteBuffer) {
		buffer.writeTo(responseBody)
	}

	override fun reset() {
		responseBody.reset()
	}

	override fun finish(response: ByteArray, offset: Int, size: Int) {
		val response1 = DefaultFullHttpResponse(
			HttpVersion.HTTP_1_1,
			if (responseMessage == null)
				HttpResponseStatus.valueOf(responseCode)
			else
				HttpResponseStatus.valueOf(responseCode, responseMessage),
			Unpooled.wrappedBuffer(response, offset, size)
		)
		finish(response1)
	}

	fun finish(response: ByteBuf) = finish(response, HttpResponseStatus.valueOf(responseCode))
	fun finish(response: ByteBuf, responseCode: HttpResponseStatus) {
		val response1 = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, responseCode, response)
		finish(response1)
	}

	fun finish(response: DefaultFullHttpResponse) {
		val heads = response.headers()

		heads.set(HttpHeaderNames.CONTENT_TYPE, "$contentType; charset=UTF-8")
		heads.set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes())
		heads.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE)

		responseMap.forEach { (t, u) ->
			heads.set(t, u)
		}

		ctx.writeAndFlush(response)
	}

	companion object {
		private val contentType = HttpHeaderValues.TEXT_PLAIN
	}
}


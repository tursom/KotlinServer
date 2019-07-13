package cn.tursom.web.netty

import cn.tursom.web.AdvanceHttpContent
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.*
import io.netty.handler.codec.http.multipart.Attribute
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder
import java.io.ByteArrayOutputStream
import java.util.HashMap
import kotlin.collections.ArrayList
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set


open class NettyHttpContent(
	private val ctx: ChannelHandlerContext,
	private val msg: FullHttpRequest
) : AdvanceHttpContent {
	override val uri: String = msg.uri()
	private val headers by lazy { msg.headers() }
	private val paramMap by lazy { RequestParser.parse(msg) }
	private val responseMap = HashMap<String, Any>()
	
	override var responseCode: Int = 200
	override var responseMessage: String? = null
	override val clientIp = ctx.channel().remoteAddress()!!
	override val method: String = msg.method().name()
	
	override val responseBody = ByteArrayOutputStream()
	val httpMethod = msg.method()
	val protocolVersion = msg.protocolVersion()
	
	val buf = msg.content()
	
	override val body = when {
		buf.readableBytes() == 0 -> null
		buf.hasArray() -> buf.array()
		else -> {
			val bytes = ByteArray(buf.readableBytes())
			buf.getBytes(buf.readerIndex(), bytes)
			bytes
		}
	}
	
	override val bodyOffSet = if (buf.hasArray()) {
		buf.arrayOffset()
	} else {
		0
	}
	
	override val readableBytes: Int = buf.readableBytes()
	
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
	
	override fun write(bytes: ByteArray) {
		responseBody.write(bytes)
	}
	
	override fun reset() {
		responseBody.reset()
	}
	
	override fun finish() {
		val response = DefaultFullHttpResponse(
			HttpVersion.HTTP_1_1,
			if (responseMessage == null)
				HttpResponseStatus.valueOf(responseCode)
			else
				HttpResponseStatus.valueOf(responseCode, responseMessage),
			Unpooled.wrappedBuffer(responseBody.toByteArray())
		)
		
		val heads = response.headers()
		
		heads.add(HttpHeaderNames.CONTENT_TYPE, "$contentType; charset=UTF-8")
		heads.add(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes())
		heads.add(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE)
		
		responseMap.forEach { (t, u) ->
			heads.add(t, u)
		}
		
		ctx.writeAndFlush(response)
	}
	
	companion object {
		private val contentType = HttpHeaderValues.TEXT_PLAIN
	}
}

/**
 * HTTP请求参数解析器, 支持GET, POST
 */
object RequestParser {
	fun parse(fullReq: FullHttpRequest): HashMap<String, List<String>> {
		val method = fullReq.method()
		
		val paramMap = HashMap<String, List<String>>()
		
		when {
			HttpMethod.GET === method -> try {
				// 是GET请求
				val decoder = QueryStringDecoder(fullReq.uri())
				decoder.parameters().entries.forEach { entry ->
					paramMap[entry.key] = entry.value
				}
			} catch (e: Exception) {
			}
			HttpMethod.POST === method -> try {
				// 是POST请求
				val decoder = HttpPostRequestDecoder(fullReq)
				decoder.offer(fullReq)
				
				val paramList = decoder.bodyHttpDatas
				
				for (param in paramList) {
					val data = param as Attribute
					if (!paramMap.containsKey(data.name)) {
						paramMap[data.name] = ArrayList()
					}
					(paramMap[data.name] as ArrayList).add(data.value)
				}
			} catch (e: Exception) {
			}
			else -> // 不支持其它方法
				throw Exception("") // 这是个自定义的异常, 可删掉这一行
		}
		
		return paramMap
	}
}

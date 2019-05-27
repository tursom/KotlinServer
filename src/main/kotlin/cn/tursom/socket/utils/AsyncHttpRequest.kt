package cn.tursom.socket.utils

import okhttp3.*
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URLEncoder
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


@Suppress("unused", "MemberVisibilityCanBePrivate")
object AsyncHttpRequest {
	
	val defaultClient = OkHttpClient()
	val socketClient = proxyClient()
	val httpProxyClient = proxyClient(port = 8080, type = Proxy.Type.HTTP)
	
	fun proxyClient(
		host: String = "127.0.0.1",
		port: Int = 1080,
		type: Proxy.Type = Proxy.Type.SOCKS
	): OkHttpClient = OkHttpClient().newBuilder()
		.proxy(Proxy(type, InetSocketAddress(host, port)))
		.build()
	
	private suspend fun sendRequest(call: Call): Response = suspendCoroutine {
		call.enqueue(object : Callback {
			override fun onFailure(call: Call, e: IOException) {
				it.resumeWithException(e)
			}
			
			override fun onResponse(call: Call, response: Response) {
				it.resume(response)
			}
		})
	}
	
	private suspend fun requestString(call: Call): String = suspendCoroutine {
		call.enqueue(object : Callback {
			override fun onFailure(call: Call, e: IOException) {
				it.resumeWithException(e)
			}
			
			override fun onResponse(call: Call, response: Response) {
				try {
					it.resume(response.body()!!.string())
				} catch (e: Throwable) {
					it.resumeWithException(e)
				}
			}
		})
	}
	
	suspend fun get(
		url: String,
		param: Map<String, String>? = null,
		headers: Map<String, String>? = null,
		client: OkHttpClient = defaultClient
	): Response {
		val paramSB = StringBuilder()
		param?.forEach {
			paramSB.append("${URLEncoder.encode(it.key, "UTF-8")}=${URLEncoder.encode(it.value, "UTF-8")}&")
		}
		if (paramSB.isNotEmpty())
			paramSB.deleteCharAt(paramSB.length - 1)
		
		val requestBuilder = Request.Builder().get()
			.url("$url?$paramSB")
		
		headers?.forEach { t, u ->
			requestBuilder.addHeader(t, u)
		}
		
		return sendRequest(client.newCall(requestBuilder.build()))
	}
	
	private suspend fun post(
		url: String,
		body: RequestBody,
		headers: Map<String, String>? = null,
		client: OkHttpClient = defaultClient
	): Response {
		val requestBuilder = Request.Builder()
			.post(body)
			.url(url)
		
		headers?.forEach { t, u ->
			requestBuilder.addHeader(t, u)
		}
		
		return sendRequest(client.newCall(requestBuilder.build()))
	}
	
	suspend fun post(
		url: String,
		param: Map<String, String>,
		headers: Map<String, String>? = null,
		client: OkHttpClient = defaultClient
	): Response {
		val formBuilder = FormBody.Builder()
		param.forEach { (t, u) ->
			formBuilder.add(t, u)
		}
		return post(url, formBuilder.build(), headers, client)
	}
	
	suspend fun post(
		url: String,
		body: String,
		headers: Map<String, String>? = null,
		client: OkHttpClient = defaultClient
	) = post(
		url,
		RequestBody.create(MediaType.parse("text/plain;charset=utf-8"), body),
		headers,
		client
	)
	
	suspend fun post(
		url: String,
		body: File,
		headers: Map<String, String>? = null,
		client: OkHttpClient = defaultClient
	) = post(
		url,
		RequestBody.create(MediaType.parse("application/octet-stream"), body),
		headers,
		client
	)
	
	suspend fun post(
		url: String,
		body: ByteArray,
		headers: Map<String, String>? = null,
		client: OkHttpClient = defaultClient
	) = post(
		url,
		RequestBody.create(MediaType.parse("application/octet-stream"), body),
		headers,
		client
	)
	
	suspend fun getStr(
		url: String,
		param: Map<String, String>? = null,
		headers: Map<String, String>? = null,
		client: OkHttpClient = defaultClient
	): String {
		val paramSB = StringBuilder()
		param?.forEach {
			paramSB.append("${URLEncoder.encode(it.key, "UTF-8")}=${URLEncoder.encode(it.value, "UTF-8")}&")
		}
		if (paramSB.isNotEmpty())
			paramSB.deleteCharAt(paramSB.length - 1)
		
		val requestBuilder = Request.Builder().get()
			.url("$url?$paramSB")
		
		headers?.forEach { t, u ->
			requestBuilder.addHeader(t, u)
		}
		
		return requestString(client.newCall(requestBuilder.build()))
	}
	
	private suspend fun postStr(
		url: String,
		body: RequestBody,
		headers: Map<String, String>? = null,
		client: OkHttpClient = defaultClient
	): String {
		val requestBuilder = Request.Builder()
			.post(body)
			.url(url)
		
		headers?.forEach { t, u ->
			requestBuilder.addHeader(t, u)
		}
		
		return requestString(client.newCall(requestBuilder.build()))
	}
	
	suspend fun postStr(
		url: String,
		param: Map<String, String>,
		headers: Map<String, String>? = null,
		client: OkHttpClient = defaultClient
	): String {
		val formBuilder = FormBody.Builder()
		param.forEach { (t, u) ->
			formBuilder.add(t, u)
		}
		return postStr(url, formBuilder.build(), headers, client)
	}
	
	suspend fun postStr(
		url: String,
		body: String,
		headers: Map<String, String>? = null,
		client: OkHttpClient = defaultClient
	): String = postStr(
		url,
		RequestBody.create(MediaType.parse("text/plain;charset=utf-8"), body),
		headers,
		client
	)
	
	suspend fun postStr(
		url: String,
		body: File,
		headers: Map<String, String>? = null,
		client: OkHttpClient = defaultClient
	): String = postStr(
		url,
		RequestBody.create(MediaType.parse("application/octet-stream"), body),
		headers,
		client
	)
	
}

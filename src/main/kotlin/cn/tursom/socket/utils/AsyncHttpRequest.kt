package cn.tursom.socket.utils

import okhttp3.*
import java.io.File
import java.io.IOException
import java.net.URLEncoder
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


@Suppress("unused")
object AsyncHttpRequest {
	private val client = OkHttpClient()
	
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
		headers: Map<String, String>? = null
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
		headers: Map<String, String>? = null
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
		headers: Map<String, String>? = null
	): Response {
		val formBuilder = FormBody.Builder()
		param.forEach { (t, u) ->
			formBuilder.add(t, u)
		}
		return post(url, formBuilder.build(), headers)
	}
	
	suspend fun post(
		url: String,
		body: String,
		headers: Map<String, String>? = null
	) = post(
		url,
		RequestBody.create(MediaType.parse("text/plain;charset=utf-8"), body),
		headers
	)
	
	suspend fun post(
		url: String,
		body: File,
		headers: Map<String, String>? = null
	) = post(
		url,
		RequestBody.create(MediaType.parse("application/octet-stream"), body),
		headers
	)
	
	suspend fun getStr(
		url: String,
		param: Map<String, String>? = null,
		headers: Map<String, String>? = null
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
		headers: Map<String, String>? = null
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
		headers: Map<String, String>? = null
	): String {
		val formBuilder = FormBody.Builder()
		param.forEach { (t, u) ->
			formBuilder.add(t, u)
		}
		return postStr(url, formBuilder.build(), headers)
	}
	
	suspend fun postStr(
		url: String,
		body: String,
		headers: Map<String, String>? = null
	): String = postStr(
		url,
		RequestBody.create(MediaType.parse("text/plain;charset=utf-8"), body),
		headers
	)
	
	suspend fun postStr(
		url: String,
		body: File,
		headers: Map<String, String>? = null
	): String = postStr(
		url,
		RequestBody.create(MediaType.parse("application/octet-stream"), body),
		headers
	)
}

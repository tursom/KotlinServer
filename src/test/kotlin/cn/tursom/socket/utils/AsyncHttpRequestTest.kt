package cn.tursom.socket.utils

import kotlinx.coroutines.runBlocking
import org.junit.Test

class AsyncHttpRequestTest {
	@Test
	fun test() {
		runBlocking {
			val response = AsyncHttpRequest.get(
				url = "https://gayhub.com",
				client = AsyncHttpRequest.socketClient
			)
			println(response)
			@Suppress("BlockingMethodInNonBlockingContext")
			println(response.body()?.string())
		}
	}
}
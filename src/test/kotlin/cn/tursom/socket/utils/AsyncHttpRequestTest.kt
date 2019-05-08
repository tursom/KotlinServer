package cn.tursom.socket.utils

import kotlinx.coroutines.runBlocking
import org.junit.Test

class AsyncHttpRequestTest {
	@Test
	fun test() {
		runBlocking {
			val response = AsyncHttpRequest.getStr(
				url = "https://github.com/tursom/KotlinServer/blob/master/src/main/kotlin/cn/tursom/socket/AsyncSocket.kt",
				client = AsyncHttpRequest.defaultClient
			)
			println(response)
		}
	}
}
package cn.tursom.socket.utils

import kotlinx.coroutines.runBlocking
import org.junit.Test

class AsyncHttpRequestTest {
	@Test
	fun test() {
		runBlocking {
			val response = AsyncHttpRequest.getStr("https://www.baidu.com")
			println(response)
		}
	}
}
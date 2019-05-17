package cn.tursom.socket

import cn.tursom.database.async.vertx
import cn.tursom.socket.utils.sendGet
import io.vertx.ext.web.Router
import org.junit.Test

class VertxTest {
	@Test
	fun testHttpServer() {
		val server = vertx.createHttpServer()
		val router = Router.router(vertx)
		router.route("/echo/:message").handler { context ->
			val response = context.response()
			response.putHeader("content-type", "text/plain")
			response.end("Hello, ${context.request().getParam("message")}!")
		}
		server.requestHandler(router::handle)
		server.listen(8086)
		
		println(sendGet("http://127.0.0.1:8086/echo/hi"))
	}
}
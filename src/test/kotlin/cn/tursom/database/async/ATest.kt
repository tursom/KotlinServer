package cn.tursom.database.async

import cn.tursom.database.annotation.*
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.jdbc.JDBCClient
import io.vertx.ext.sql.SQLConnection
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


@TableName("Test")
data class TestClass(
	@Default("1") @NotNull @Check(">0") @FieldName("id") val _id: Int?,
	@NotNull @FieldType("DATE") @Getter("ele2") val ele2: Long = 0,
	@TextLength(50) val text: String? = ""
) {
	fun ele2() = System.currentTimeMillis()
}

class ATest {
	@Test
	fun test(): Unit = runBlocking {
		val config = JsonObject()
		config.put("url", "jdbc:sqlite:test.db")
		config.put("_driver class", "org.sqlite.JDBC")
		val jdbc = JDBCClient.createShared(Vertx.vertx(), config)
		val connection: SQLConnection = suspendCoroutine { cont ->
			jdbc.getConnection {
				if (!it.failed()) {
					cont.resume(it.result())
				} else {
					cont.resumeWithException(it.cause())
				}
			}
		}
		println("成功连接数据库")
		suspendCoroutine<Int> { cont ->
			connection.query("select * from test limit 100;") {
				if (!it.failed()) {
					val adapter = AsyncSqlAdapter(TestClass::class.java)
					adapter.adapt(it.result())
					println(adapter.size)
					println(adapter)
				} else {
					it.cause().printStackTrace()
				}
				cont.resume(1)
			}
		}
		Unit
	}
}
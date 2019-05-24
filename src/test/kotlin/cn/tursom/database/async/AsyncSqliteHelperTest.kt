package cn.tursom.database.async

import cn.tursom.database.annotation.*
import cn.tursom.database.async.sqlite.AsyncSqliteHelper
import kotlinx.coroutines.runBlocking
import org.junit.Test

class AsyncSqliteHelperTest {
	
	@TableName("Test")
	data class TestClass(
		@Default("1") @NotNull @Check(">0") @FieldName("id") val _id: Int?,
		@NotNull @FieldType("DATE") @Getter("ele2") val ele2: Long = 0,
		@TextLength(50) val text: String? = ""
	) {
		fun ele2() = System.currentTimeMillis()
	}
	
	@Test
	fun test(): Unit = runBlocking {
		val helper = AsyncSqliteHelper("test.db")
		println(helper.select(AsyncSqlAdapter(TestClass::class.java), maxCount = 10).size)
		Unit
	}
}
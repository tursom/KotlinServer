package cn.tursom.database.sqlite

import cn.tursom.database.*
import cn.tursom.database.SQLHelper.*
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.*

@FieldType("DATE")
class TDate : SqlField<String> {
	override val sqlValue: String
		get() = obj
	
	private var obj: String = Date().toString()
	
	override fun adapt(obj: Any) {
		this.obj = obj.toString()
	}
	
	override fun get(): String {
		return obj
	}
	
	override fun toString(): String {
		return obj
	}
}

@TableName("Test")
data class TestClass(
	@Default("1") @NotNullField @Check("id > 0") val id: Int,
	@NotNullField @FieldType("DATE") val ele2: TDate,
	@NotNullField @TextLength(50) val text: String
)

class SqliteTest {
	@Test
	fun sqliteTest() {
		SQLiteHelper("test.db").use { sqLiteHelper ->
			SQLiteHelper("../KotlinServer/test.db").use { sqLiteHelper2 ->
				println(sqLiteHelper == sqLiteHelper2)
			}
//			for (i in 1..10) {
//				sqLiteHelper.insert(TestClass(i, TDate(), "233"))
//			}
			println(sqLiteHelper.select<TestClass>(maxCount = 500))
		}
	}
}
package cn.tursom.database.sqlite

import cn.tursom.database.*
import org.junit.Test
import java.util.*

@SqlFieldType("DATE")
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

data class TestClass(
	@Default("1")
	@NotNullField @Check("ele1 > 0") val ele1: Int,
	@NotNullField val ele2: TDate,
	@NotNullField @TextLength(50) val text: String
)

class SqliteTest {
	@Test
	fun sqliteTest() {
		val sqLiteHelper = SQLiteHelper("test.db")
		sqLiteHelper.insert("tab", TestClass(1, TDate(), "233"))
		val adapter = SQLAdapter(TestClass::class.java)
		sqLiteHelper.select(adapter, "tab")
		println(adapter)
	}
}
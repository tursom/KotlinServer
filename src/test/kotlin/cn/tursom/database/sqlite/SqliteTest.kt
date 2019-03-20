package cn.tursom.database.sqlite

import cn.tursom.database.*
import cn.tursom.database.SQLHelper.*
import org.junit.Test

@FieldType("DATE")
class TTime : SqlField<Long> {
	private var obj: Long = System.currentTimeMillis()
	
	override val sqlValue: String
		get() = obj.toString()
	
	override fun adapt(obj: Any) {
		this.obj = when (obj) {
			is Long -> obj
			is Int -> obj.toLong()
			is Short -> obj.toLong()
			is Byte -> obj.toLong()
			else -> obj.toString().toLong()
		}
	}
	
	override fun get() = obj
	
	override fun toString() = sqlValue
}

@TableName("Test")
data class TestClass(
	@Default("1") @NotNull @Check("id > 0") val id: Int,
	@NotNull @FieldType("DATE") val ele2: TTime,
	@NotNull @TextLength(50) val text: String = ""
)

class SqliteTest {
	@Test
	fun sqliteTest() {
		SQLiteHelper("test.db").use { sqLiteHelper ->
			SQLiteHelper("../KotlinServer/test.db").use { sqLiteHelper2 ->
				println(sqLiteHelper == sqLiteHelper2)
			}
			sqLiteHelper.delete("Test")
//			val fieldList = ArrayList<TestClass>()
//			for (i in 1..1000) {
//				fieldList.add(TestClass(i, TTime(), "233"))
//			}
			println(System.currentTimeMillis())
//			sqLiteHelper.insert(fieldList)
			val id2 = listOf(EqualWhere(TestClass::id, "2"))
			sqLiteHelper.update(TestClass(2, TTime(), "还行"), id2)
			println(System.currentTimeMillis())
			println(sqLiteHelper.select<TestClass>(where = id2).size)
			println(System.currentTimeMillis())
		}
	}
}
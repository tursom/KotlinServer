package cn.tursom.database.sqlite

import cn.tursom.database.*
import cn.tursom.database.SQLHelper.*
import org.junit.Test
import java.sql.ResultSet
import kotlin.reflect.jvm.javaField

@FieldType("DATE")
data class TTime(private var obj: Long = System.currentTimeMillis()) : SqlField<Long>, SQLAdapter.ResultSetReadable {
	override val sqlValue: String
		get() = obj.toString()
	
	override fun adapt(fieldName: String, resultSet: ResultSet) {
		obj = resultSet.getLong(fieldName)
	}
	
	override fun get() = obj
}

@TableName("Test")
data class TestClass(
	@Default("1") @NotNull @Check("id > 0") @FieldName("id") val o: Int?,
	@NotNull @FieldType("DATE") val ele2: TTime,
	@NotNull @TextLength(50) val text: String = ""
)

class SqliteTest {
	@Test
	fun sqliteTest() {
		
		SQLiteHelper("test.db").use { sqLiteHelper ->
			//测试同一性
			SQLiteHelper("../KotlinServer/test.db").use { sqLiteHelper2 ->
				println(sqLiteHelper == sqLiteHelper2)
			}
			
			val id2 = listOf(EqualWhere(TestClass::o.javaField!!, "20"))
			
			val fieldList = ArrayList<TestClass>()
			for (i in 1..10000) {
				fieldList.add(TestClass(i, TTime(), "233"))
			}
			
			println("insert: ${System.currentTimeMillis()}")
			sqLiteHelper.insert(fieldList)
			println("update: ${System.currentTimeMillis()}")
			sqLiteHelper.update(TestClass(20, TTime(), "还行"), id2)
			println("select: ${System.currentTimeMillis()}")
			println(sqLiteHelper.select<TestClass>().size)
			println("select: ${System.currentTimeMillis()}")
			println(sqLiteHelper.select<TestClass>(where = id2))
			println("end: ${System.currentTimeMillis()}")
			
			//清空表
			sqLiteHelper.delete(TestClass::class.java)
		}
	}
}
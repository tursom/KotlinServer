package cn.tursom.database.sqlite

import cn.tursom.database.SQLAdapter
import cn.tursom.database.SQLHelper.SqlField
import cn.tursom.database.annotation.*
import cn.tursom.database.clauses.clause
import cn.tursom.database.select
import cn.tursom.database.tableName
import cn.tursom.database.update
import org.junit.Test
import java.sql.ResultSet

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
	@Default("1") @NotNull @Check(">0") @FieldName("id") val _id: Int?,
	@NotNull @FieldType("DATE") @Getter val ele2: Long = 0,
	@TextLength(50) val text: String? = ""
) {
	fun ele2() = System.currentTimeMillis()
}

class SqliteTest {
	@Test
	fun getterTest() {
		val obj = TestClass(null, 0, "test")
		println(TestClass::class.java.getDeclaredMethod("ele2").invoke(obj))
	}
	
	@Test
	fun tryFinallyTest() {
		var finally = false
		try {
			try {
				throw Exception()
			} catch (e: Exception) {
				throw e
			} finally {
				finally = true
			}
		} catch (e: Exception) {
		}
		assert(finally)
	}
	
	@Test
	fun sqliteTest() {
		
		SQLiteHelper("test.db").use { sqLiteHelper ->
			//测试同一性
//			SQLiteHelper("../KotlinServer/test.db").use { sqLiteHelper2 ->
//				println(sqLiteHelper == sqLiteHelper2)
//			}
			
			// 清空表
			try {
				sqLiteHelper.delete(TestClass::class.java.tableName)
			} catch (e: Exception) {
			}
			val fieldList = ArrayList<TestClass>()
			for (i in 1..10000) {
				fieldList.add(TestClass(i, text = "233"))
			}
			
			println("insert: ${System.currentTimeMillis()}")
			sqLiteHelper.insert(fieldList)
			println("update: ${System.currentTimeMillis()}")
			sqLiteHelper.update(TestClass(20, text = "还行"), clause { !TestClass::_id equal !"20" })
			println("select: ${System.currentTimeMillis()}")
			println(sqLiteHelper.select<TestClass>().size)
			
			sqLiteHelper update {
				this table TestClass::class
				TestClass::text setTo TestClass::_id - 11
				where { !TestClass::_id equal !10 }
			}
			
			println("select: ${System.currentTimeMillis()}")
			val result: SQLAdapter<TestClass> = sqLiteHelper select {
				where { (!TestClass::text equal !"还行") or (!TestClass::_id equal !10) }
				TestClass::_id limit 10
			}
			println(result)
			println("end: ${System.currentTimeMillis()}")
			
			//清空表
//			sqLiteHelper.delete(TestClass::class.java)
		}
	}
}
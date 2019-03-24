package cn.tursom.database.mysql

import cn.tursom.database.annotation.*
import cn.tursom.database.clauses.clause
import cn.tursom.database.delete
import cn.tursom.database.select
import cn.tursom.database.tableName
import cn.tursom.regex.RegexMaker.beg
import cn.tursom.regex.RegexMaker.end
import org.junit.Test
import kotlin.reflect.jvm.javaField

@TableName("TestTable2")
data class TableStruckTestClass(
	@NotNull @AutoIncrement @PrimaryKey @FieldName("field1") @FieldType("INTEGER") val tele1: Int? = null,
	@NotNull @Unique @Check(">0") val ele2: Double = 1.0,
	@TextLength(50) val text: String? = null
)

class MySQLHelperTest {
	@ExperimentalUnsignedTypes
	@Test
	fun createStrTest() {
		println(MySQLHelper.createTableStr(TableStruckTestClass::class.java))
	}
	
	@Test
	fun mysqlHelpertest() {
		val helper = MySQLHelper("127.0.0.1", "test", "test", "test")
		// 清空表
//		try {
//			helper.delete(TableStruckTestClass::class.java.tableName)
//		} catch (e: Exception) {
//		}
//
//		val fieldList = ArrayList<TableStruckTestClass>()
//		for (i in 1..10000) {
//			fieldList.add(TableStruckTestClass(null, i.toDouble(), "233"))
//		}
//
//		println("insert: ${System.currentTimeMillis()}")
//		helper.insert(fieldList)
//		println("update: ${System.currentTimeMillis()}")
//		helper.update(
//			TableStruckTestClass(null, 20.toDouble(), "还行"),
//			ClauseMaker.make { TableStruckTestClass::ele2 equal "20" }
//		)
//		println("select: ${System.currentTimeMillis()}")
//		println(helper.select<TableStruckTestClass>().size)
		
		println("select: ${System.currentTimeMillis()}")
		println(helper.select<TableStruckTestClass> {
			where { !TableStruckTestClass::ele2 equal !10 }
		})
		
		helper.delete {
			TableStruckTestClass::class where {
				!TableStruckTestClass::ele2 equal !10
			}
		}
		
		println(helper.select<TableStruckTestClass> {
			where { !TableStruckTestClass::ele2 equal !10 }
		})
		println("end: ${System.currentTimeMillis()}")
	}
	
	@Test
	fun testIterator() {
		val range = 1..10
		val iterator = range.iterator()
		println(iterator.nextInt())
		for (i in iterator) {
			println("$i:$i")
		}
	}
}
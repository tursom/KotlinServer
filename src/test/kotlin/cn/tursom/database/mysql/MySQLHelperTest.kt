package cn.tursom.database.mysql

import cn.tursom.database.EqualWhere
import cn.tursom.database.SQLHelper.*
import cn.tursom.database.select
import cn.tursom.database.tableName
import org.junit.Test
import kotlin.reflect.jvm.javaField

@TableName("TestTable")
data class TableStruckTestClass(
	@NotNull @AutoIncrement @PrimaryKey @FieldName("field1") @FieldType("INTEGER") val tele1: Int? = null,
	@NotNull @Unique val ele2: Double = 1.0,
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
		//清空表
		try {
			helper.delete(TableStruckTestClass::class.java.tableName)
		} catch (e: Exception) {
		}
		
		val id2 = listOf(EqualWhere(TableStruckTestClass::ele2.javaField!!, "20"))
		
		val fieldList = ArrayList<TableStruckTestClass>()
		for (i in 1..10000) {
			fieldList.add(TableStruckTestClass(null, i.toDouble(), "233"))
		}
		
		println("insert: ${System.currentTimeMillis()}")
		helper.insert(fieldList)
		println("update: ${System.currentTimeMillis()}")
//		helper.update(TableStruckTestClass(null, 20.toDouble(), "还行"), id2)
		println("select: ${System.currentTimeMillis()}")
//		println(helper.select<TableStruckTestClass>().size)
		println("select: ${System.currentTimeMillis()}")
		println(helper.select<TableStruckTestClass>(where = id2, order = TableStruckTestClass::text.javaField, reverse = true))
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
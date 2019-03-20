package cn.tursom.database.mysql

import cn.tursom.database.SQLHelper.*
import org.junit.Test

@TableName("TestTable")
data class TableStruckTestClass(
	@NotNull @AutoIncrement @PrimaryKey @FieldName("field1") @FieldType("INTEGER") val tele1: Int,
	@NotNull @Unique val ele2: Double,
	@NotNull @TextLength(50) val text: String
)

class MySQLHelperTest {
	@ExperimentalUnsignedTypes
	@Test
	fun createStrTest() {
		println(MySQLHelper.createTableStr(TableStruckTestClass::class.java))
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
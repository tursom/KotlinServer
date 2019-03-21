package cn.tursom.database.mysql

import cn.tursom.database.SQLHelper.*
import org.junit.Test

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
		helper.insert(TableStruckTestClass())
		println(helper.select<TableStruckTestClass>())
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
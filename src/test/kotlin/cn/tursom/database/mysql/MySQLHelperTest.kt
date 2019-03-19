package cn.tursom.database.mysql

import cn.tursom.database.*
import org.junit.Test


data class TableStruckTestClass(
	@NotNullField @AutoIncrement @PrimaryKey val tele1: Int,
	@NotNullField @Unique val ele2: Double,
	@NotNullField @TextLength(50) val text: String
)

class MySQLHelperTest {
	@ExperimentalUnsignedTypes
	@Test
	fun createStrTest() {
		println(MySQLHelper.createTableStr("test2", TableStruckTestClass::class.java))
	}
}
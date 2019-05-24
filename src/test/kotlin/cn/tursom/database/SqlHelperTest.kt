package cn.tursom.database

import cn.tursom.database.annotation.FieldName
import cn.tursom.database.annotation.Setter
import cn.tursom.database.clauses.EqualClause
import org.junit.Test


data class TestClass(
	@FieldName("field1") val ele1: Int,
	val field2: Float,
	@Setter("setTime") val time: Long
) {
	fun setTime(obj: Any): Long {
		return obj.toString().toLong()
	}
}

class SqlHelperTest {
	@Test
	fun sqlHelperTest() {
		val a = TestClass::ele1
		println(a.name)
		println(EqualClause(TestClass::ele1, "1").sqlStr)
	}
	
	
	@Test
	fun fieldValueTest() {
		println(1.fieldValue)
		println("123'123".fieldValue)
	}
}
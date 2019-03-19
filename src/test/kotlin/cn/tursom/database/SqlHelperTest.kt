package cn.tursom.database

import org.junit.Test

data class TestClass(val ele1: Int)

class SqlHelperTest {
	@Test
	fun sqlHelperTest() {
		val a = TestClass::ele1
		println(a.name)
		println(EqualWhere(TestClass::ele1, "1").sqlStr)
	}
}
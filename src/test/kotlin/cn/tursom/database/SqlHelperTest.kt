package cn.tursom.database

import org.junit.Test
import java.util.*

data class TestClass(val ele1: Int)

class SqlHelperTest {
	@Test
	fun sqlHelperTest() {
		val a = TestClass::ele1
		println(a.name)
		println(EqualWhere(TestClass::ele1, "1").sqlStr)
	}
	
	@Test
	fun testWhere() {
		val where = object : SQLHelper.Where {
			override val sqlStr: String
				get() = "a=`1`"
		}
		when (where) {
			is SQLHelper.Where -> println("yes")
			else -> println("no")
		}
	}
}
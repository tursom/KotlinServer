package cn.tursom.database

import org.junit.Test
import cn.tursom.database.SQLHelper.*
import kotlin.reflect.jvm.javaField

@FieldType("LONG")
@StringField
class TTime : SqlField<Long>, SQLAdapter.Adaptable {
	private var time: Long = System.currentTimeMillis()
	
	override fun adapt(obj: Any) {
		time = when (obj) {
			is Long -> obj
			is Int -> obj.toLong()
			is Short -> obj.toLong()
			is Byte -> obj.toLong()
			else -> obj.toString().toLong()
		}
	}
	
	override fun get(): Long {
		return time
	}
	
	override val sqlValue: String
		get() = time.toString()
}

data class TestClass(
	@FieldName("field1") val ele1: Int,
	val field2: Float,
	val time: TTime
)

class SqlHelperTest {
	@Test
	fun sqlHelperTest() {
		val a = TestClass::ele1
		println(a.name)
		println(EqualWhere(TestClass::ele1.javaField!!, "1").sqlStr)
	}
	
	@Test
	fun testFieldValue() {
		println(TTime().fieldValue)
		assert(TTime().fieldValue == "'${System.currentTimeMillis()}'")
	}
	
	@Test
	fun fieldValueTest() {
		println(1.fieldValue)
		println("123'123".fieldValue)
		println(TTime().fieldValue)
	}
}
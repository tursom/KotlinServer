package cn.tursom.database.clauses

import cn.tursom.database.annotation.AutoIncrement
import cn.tursom.database.annotation.TableName
import cn.tursom.database.select
import cn.tursom.database.sqlite.SQLiteHelper
import cn.tursom.regex.regex
import org.junit.Test

@TableName("ClausesTestTable")
data class TestClass(@AutoIncrement val id: Int)

class ClausesTest {
	@Test
	fun testAnd() {
		val helper = SQLiteHelper("test.db")
		println(helper.select<TestClass>(where = clause {
			(!TestClass::id equal !20) - (!TestClass::id lessThan !10)
		}))
	}
	
	@Test
	fun regexTest() {
//		println(
//			clause {
//				!TestClass::id regexp {
//					lowercase * uppercase * numbers % (1..2) +
//						any / 3 + caret % 1
//				}
//			}
//		)
		println(regex { beg + uppercase * lowercase * !"_" * 1 + uppercase * lowercase * numbers * !"_" % 0 + end })
		
		Regex("([0-9A-Za-z]{5}){0,2}")
//		val regex = Regex(pattern = ".*((a)).*")
//		Regex("\\\\ \\[][a]")
//		println(regex)
//		println("a".contains(regex))
	}
}
package cn.tursom.database.clauses

import cn.tursom.database.annotation.AutoIncrement
import cn.tursom.database.annotation.TableName
import cn.tursom.database.select
import cn.tursom.database.sqlite.SQLiteHelper
import cn.tursom.regex.RegexMaker.rem
import cn.tursom.regex.regex
import org.junit.Test

@TableName("ClausesTestTable")
data class TestClass(@AutoIncrement val id: Int)

class ClausesTest {
	@Test
	fun testAnd() {
		val helper = SQLiteHelper("test.db")
		println(helper.select<TestClass>(where = clause {
			(+TestClass::id equal "20") or (+TestClass::id lessThan "10")
		}))
	}
	
	@Test
	fun regexTest() {
		println(clause { +TestClass::id regexp { lowercase * uppercase * numbers % (1..2) + any / 3 + caret % 1 } })
		println(regex {
			'1' % '2' * uppercase * !('5' to '7') / 5 + !"ABC\\-\\\\" * lowercase * numbers % (2 to 3) +
				uppercase + any / 3 + caret % 1 + +"还行" * 2
		})
		Regex("[1-2]{0,5}[ABC\\-\\\\a-z0-9]{2,3}[A-Z].{3,}\\^+a{2}")
//		val regex = Regex(pattern = ".*((a)).*")
//		Regex("\\\\ \\[][a]")
//		println(regex)
//		println("a".contains(regex))
	}
}
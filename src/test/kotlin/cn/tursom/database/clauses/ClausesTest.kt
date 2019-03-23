package cn.tursom.database.clauses

import cn.tursom.database.annotation.AutoIncrement
import cn.tursom.database.annotation.TableName
import cn.tursom.database.select
import cn.tursom.database.sqlite.SQLiteHelper
import cn.tursom.regex.*
import org.junit.Test

@TableName("ClausesTestTable")
data class TestClass(@AutoIncrement val id: Int)

class ClausesTest {
	@Test
	fun testAnd() {
		val helper = SQLiteHelper("test.db")
		println(helper.select<TestClass>(where = "(id=1 OR id=2)"))
	}
	
	@Test
	fun regexTest() {
		println(RegexWildcard.make {
			('a'..'z' and '0'..'9')..(1 to 2) link any..3 link +(caret..1)
		})
		Regex("[a-z0-9]?((.{3}){2}(){4}){5,6}")
//		val regex = Regex(pattern = ".*((a)).*")
//		Regex("\\\\ \\[][a]")
//		println(regex)
//		println("a".contains(regex))
	}
}
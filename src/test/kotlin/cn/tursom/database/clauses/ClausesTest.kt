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
			('a'..'z' and '0'..'9').onceBelow and any repeat 3
		})
		Regex("([a-z0-9])*.*")
//		val regex = Regex(pattern = ".*((a)).*")
//		Regex("\\\\ \\[][a]")
//		println(regex)
//		println("a".contains(regex))
	}
}
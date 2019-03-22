package cn.tursom.database.clauses

import org.junit.Test
import cn.tursom.database.annotation.AutoIncrement
import cn.tursom.database.annotation.TableName
import cn.tursom.database.select
import cn.tursom.database.sqlite.SQLiteHelper

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
			charList(('a' to 'z') also ('0' to '9')).anyTime()
		})
		Regex("\\b([a-z]+) \\1\\b")
		val regex = Regex(pattern = ".*((a)).*")
		Regex("\\\\ \\[][a]")
		println(regex)
		println("a".contains(regex))
	}
}
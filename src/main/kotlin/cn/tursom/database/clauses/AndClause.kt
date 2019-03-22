package cn.tursom.database.clauses

class AndClause(val first: Clause, val second: Clause) : Clause {
	override val sqlStr: String
		get() = "(${first.sqlStr} AND ${second.sqlStr})"
}
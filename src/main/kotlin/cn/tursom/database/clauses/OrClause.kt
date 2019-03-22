package cn.tursom.database.clauses

class OrClause(val first: Clause, val second: Clause) : Clause {
	override val sqlStr: String
		get() = "(${first.sqlStr} OR ${second.sqlStr})"
}
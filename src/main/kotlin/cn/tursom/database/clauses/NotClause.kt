package cn.tursom.database.clauses

class NotClause(val clause: Clause) : Clause {
	override val sqlStr: String
		get() = "(NOT ${clause.sqlStr})"
}
package cn.tursom.regex

class UnitListRegexUnit(private val valList: String) : RegexUnit {
	constructor(from: Char, to: Char) : this("$from-$to")
	constructor(range: Pair<Char, Char>) : this("${range.first}-${range.second}")
	constructor(range: CharRange) : this("${range.start}-${range.last}")
	constructor(range: UnitListRegexUnit) : this(range.valList)
	
	val reverse
		get() = UnitListRegexUnit(if (valList.first() == '^') valList.drop(1) else "^$valList")
	
	operator fun not() = reverse
	operator fun plus(unitList: UnitListRegexUnit) = UnitListRegexUnit("$valList${unitList.valList}")
	operator fun plus(unitList: CharRange) = this + UnitListRegexUnit(unitList)
	operator fun plus(unitList: Pair<Char, Char>) = this + UnitListRegexUnit(unitList)
	
	infix fun also(unitList: UnitListRegexUnit) = UnitListRegexUnit("$valList${unitList.valList}")
	infix fun also(unitList: CharRange) = this also UnitListRegexUnit(unitList)
	infix fun also(unitList: Pair<Char, Char>) = this also UnitListRegexUnit(unitList)
	
	infix fun and(unitList: UnitListRegexUnit) = UnitListRegexUnit("$valList${unitList.valList}")
	infix fun and(unitList: CharRange) = this and UnitListRegexUnit(unitList)
	infix fun and(unitList: Pair<Char, Char>) = this and UnitListRegexUnit(unitList)
	
	infix fun link(unitList: UnitListRegexUnit) = UnitListRegexUnit("$valList${unitList.valList}")
	infix fun link(unitList: CharRange) = this also UnitListRegexUnit(unitList)
	infix fun link(unitList: Pair<Char, Char>) = this also UnitListRegexUnit(unitList)
	
	override val unit = "[$valList]"
	override fun toString() = unit
}
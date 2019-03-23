package cn.tursom.regex

@Suppress("MemberVisibilityCanBePrivate")
class RepeatRegexUnit(val repeatUnit: RegexUnit?, val from: Int, val to: Int? = null) : RegexUnit {
	constructor(repeatUnit: RegexUnit?, range: IntRange) : this(repeatUnit, range.start, range.last)
	constructor(repeatUnit: RegexUnit?, range: Pair<Int, Int>) : this(repeatUnit, range.first, range.second)
	
	constructor(repeatUnit: String?, from: Int, to: Int? = null) : this(StringRegexUnit(repeatUnit ?: ""), from, to)
	constructor(repeatUnit: String?, range: IntRange) : this(StringRegexUnit(repeatUnit ?: ""), range.start, range.last)
	constructor(repeatUnit: String?, range: Pair<Int, Int>) :
		this(StringRegexUnit(repeatUnit ?: ""), range.first, range.second)
	
	constructor(repeatUnit: RepeatRegexUnit?) : this(repeatUnit?.repeatUnit, repeatUnit?.from ?: 0, repeatUnit?.to)
	
	val range = when {
		from < 0 -> "*"
		to == null -> if (from != 1) "{$from}" else ""
		to < 0 -> if (from == 1) "+" else "{$from,}"
		to == 1 && from == 0 -> "?"
		else -> "{$from,$to}"
	}
	
	operator fun unaryPlus() = RepeatRegexUnit(repeatUnit, from, -1)
	
	override val unit = repeatUnit?.unit?.let {
		if (it.isNotEmpty()) "${if (repeatUnit is RepeatRegexUnit) "($it)" else it}$range"
		else ""
	} ?: ""
	
	override fun toString() = unit
}
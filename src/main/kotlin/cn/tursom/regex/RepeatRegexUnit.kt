package cn.tursom.regex

@Suppress("MemberVisibilityCanBePrivate")
class RepeatRegexUnit(val repeatUnit: RegexUnit?, val from: Int, val to: Int? = null) : RegexUnit {
	constructor(repeatUnit: RegexUnit?, range: IntRange) : this(repeatUnit, range.start, range.last)
	constructor(repeatUnit: RegexUnit?, range: Pair<Int, Int>) : this(repeatUnit, range.first, range.second)
	
	constructor(repeatUnit: String?, from: Int, to: Int?) : this(StringRegexUnit(repeatUnit ?: ""), from, to)
	constructor(repeatUnit: String?, range: IntRange) : this(StringRegexUnit(repeatUnit ?: ""), range.start, range.last)
	constructor(repeatUnit: String?, range: Pair<Int, Int>) :
		this(StringRegexUnit(repeatUnit ?: ""), range.first, range.second)
	
	constructor(repeatUnit: RepeatRegexUnit?, from: Int, to: Int? = null) : this(repeatUnit?.repeatUnit, from, to)
	
	override val unit: String
		get() = toString()
	
	override fun toString() = when (repeatUnit) {
		null -> ""
		is RepeatRegexUnit -> repeatUnit.unit.let {
			when (it.length) {
				0 -> ""
				else -> "($it){$from${if (to != null) ",$to" else ""}}"
			}
		}
		else -> repeatUnit.unit?.let {
			"$it{$from${to ?: ""}}"
		} ?: ""
	}
}
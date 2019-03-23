package cn.tursom.regex

class CharListRegexUnit(private val valList: String) : RegexUnit {
	constructor(from: Char, to: Char) : this("$from-$to")
	constructor(range: Pair<Char, Char>) : this("${range.first}-${range.second}")
	constructor(range: CharRange) : this("${range.start}-${range.last}")
	constructor(range: CharListRegexUnit) : this(range.valList)
	
	val reverse
		get() = CharListRegexUnit(if (valList.first() == '^') valList.drop(1) else "^$valList")
	
	operator fun not() = reverse
	operator fun plus(charList: CharListRegexUnit) = CharListRegexUnit("$valList${charList.valList}")
	operator fun plus(charList: CharRange) = this + CharListRegexUnit(charList)
	operator fun plus(charList: Pair<Char, Char>) = this + CharListRegexUnit(charList)
	
	infix fun also(charList: CharListRegexUnit) = CharListRegexUnit("$valList${charList.valList}")
	infix fun also(charList: CharRange) = this also CharListRegexUnit(charList)
	infix fun also(charList: Pair<Char, Char>) = this also CharListRegexUnit(charList)
	
	infix fun and(charList: CharListRegexUnit) = CharListRegexUnit("$valList${charList.valList}")
	infix fun and(charList: CharRange) = this and CharListRegexUnit(charList)
	infix fun and(charList: Pair<Char, Char>) = this and CharListRegexUnit(charList)
	
	override val unit = "[$valList]"
	override fun toString() = unit
}
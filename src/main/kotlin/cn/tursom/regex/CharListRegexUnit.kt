package cn.tursom.regex

class CharListRegexUnit(private val valList: String) : RegexUnit {
	constructor(from: Char, to: Char) : this("$from-$to")
	constructor(range: Pair<Char, Char>) : this("${range.first}-${range.second}")
	constructor(range: CharRange) : this("${range.start}-${range.last}")
	constructor(range: CharListRegexUnit) : this(range.valList)
	
	val reverse
		get() = CharListRegexUnit(if (valList[0] == '^') valList.drop(1) else "^$valList")
	
	infix fun also(charList: CharListRegexUnit) = CharListRegexUnit("$valList${charList.valList}")
	infix fun also(charList: CharRange) = this also CharListRegexUnit(charList)
	infix fun also(charList: Pair<Char, Char>) = this also CharListRegexUnit(charList)
	
	infix fun and(charList: CharListRegexUnit) = CharListRegexUnit("$valList${charList.valList}")
	infix fun and(charList: CharRange) = this and CharListRegexUnit(charList)
	infix fun and(charList: Pair<Char, Char>) = this and CharListRegexUnit(charList)
	
	override val unit: String
		get() = "[$valList]"
	
	override fun toString() = "[$valList]"
}

infix fun Char.to(target: Char) = CharListRegexUnit(this, target)

infix fun CharRange.also(charList: CharListRegexUnit) = CharListRegexUnit(this) also charList
infix fun CharRange.also(charList: CharRange) = CharListRegexUnit(this) also charList
infix fun CharRange.also(charList: Pair<Char, Char>) = CharListRegexUnit(this) also charList

infix fun Pair<Char, Char>.also(charList: CharListRegexUnit) = CharListRegexUnit(this) also charList
infix fun Pair<Char, Char>.also(charList: CharRange) = CharListRegexUnit(this) also charList
infix fun Pair<Char, Char>.also(charList: Pair<Char, Char>) = CharListRegexUnit(this) also charList

infix fun CharRange.and(charList: CharListRegexUnit) = CharListRegexUnit(this) and charList
infix fun CharRange.and(charList: CharRange) = CharListRegexUnit(this) and charList
infix fun CharRange.and(charList: Pair<Char, Char>) = CharListRegexUnit(this) and charList

infix fun Pair<Char, Char>.and(charList: CharListRegexUnit) = CharListRegexUnit(this) and charList
infix fun Pair<Char, Char>.and(charList: CharRange) = CharListRegexUnit(this) and charList
infix fun Pair<Char, Char>.and(charList: Pair<Char, Char>) = CharListRegexUnit(this) and charList
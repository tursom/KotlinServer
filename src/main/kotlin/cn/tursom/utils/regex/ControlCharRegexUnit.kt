package cn.tursom.utils.regex

class ControlCharRegexUnit(private val char: Char) : RegexUnit {
	constructor(char: ControlCharRegexUnit) : this(char.char)
	
	override val unit: String = "\\c$char"
	
	override fun toString() = unit
}
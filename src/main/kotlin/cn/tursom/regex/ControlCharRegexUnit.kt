package cn.tursom.regex

class ControlCharRegexUnit(private val char: Char) : RegexUnit {
	override val unit: String
		get() = "\\c$char"
	
	override fun toString() = unit
}
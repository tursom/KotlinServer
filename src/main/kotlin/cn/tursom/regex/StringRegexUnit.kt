package cn.tursom.regex

class StringRegexUnit(private val str: String) : RegexUnit {
	override val unit: String
		get() = when (str.length) {
			0 -> ""
			1 -> str
			else -> "($str)"
		}
	
	override fun toString() = str
}
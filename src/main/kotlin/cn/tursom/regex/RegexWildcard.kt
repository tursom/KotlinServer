package cn.tursom.regex

@Suppress("unused")
object RegexWildcard {
	val pointChar = UnitRegexUnit("\\.")
	val caret = UnitRegexUnit("\\^")
	val dollar = UnitRegexUnit("\\$")
	val plus = UnitRegexUnit("\\+")
	val roundBrackets = UnitRegexUnit("\\(")
	val squareBrackets = UnitRegexUnit("\\[")
	val curlyBrackets = UnitRegexUnit("\\{")
	val backslash = UnitRegexUnit("\\\\")
	val verticalBar = UnitRegexUnit("\\|")
	val questionMark = UnitRegexUnit("\\?")
	val nextPage = UnitRegexUnit("\\f")
	val nextLine = UnitRegexUnit("\\n")
	val enter = UnitRegexUnit("\\r")
	val space = UnitRegexUnit("\\s")
	val nonSpace = UnitRegexUnit("\\S")
	val tab = UnitRegexUnit("\\t")
	val vertical = UnitRegexUnit("\\v")
	val wordBoundary = UnitRegexUnit("\\b")
	val nonWordBoundary = UnitRegexUnit("\\B")
	
	/**
	 * @warning except \n
	 */
	val any = UnitRegexUnit(".")
	val beg = UnitRegexUnit("^")
	val end = UnitRegexUnit("$")
	
	val Char.control
		get() = UnitRegexUnit("\\c$this")
	
	val Iterable<String>.toSet: StringRegexUnit?
		get() {
			val iterator = iterator()
			if (!iterator.hasNext()) return null
			val stringBuilder = StringBuilder()
			stringBuilder.append(iterator.next())
			forEach {
				stringBuilder.append("|$it")
			}
			return StringRegexUnit(stringBuilder.toString())
		}
	
	val Array<out String>.toSet: StringRegexUnit?
		get() {
			val iterator = iterator()
			if (!iterator.hasNext()) return null
			val stringBuilder = StringBuilder()
			stringBuilder.append(iterator.next())
			forEach {
				stringBuilder.append("|$it")
			}
			return StringRegexUnit(stringBuilder.toString())
		}
	
	@Suppress("UNUSED_EXPRESSION")
	fun make(func: RegexWildcard.() -> RegexUnit) = func()
}
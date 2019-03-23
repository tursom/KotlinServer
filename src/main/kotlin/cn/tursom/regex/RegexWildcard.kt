package cn.tursom.regex

@Suppress("unused", "MemberVisibilityCanBePrivate")
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
		get() = ControlCharRegexUnit(this)
	
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
	
	val RegexUnit.onceMore
		get() = UnitRegexUnit(unit?.let { "$it+" } ?: "")
	
	val RegexUnit.anyTime
		get() = UnitRegexUnit(unit?.let { "$it*" } ?: "")
	
	val RegexUnit.onceBelow
		get() = UnitRegexUnit(unit?.let { "$it?" } ?: "")
	
	infix fun RegexUnit.repeat(times: Int) = RepeatRegexUnit(StringRegexUnit(unit ?: ""), times)
	infix fun String.repeat(times: Int) = RepeatRegexUnit(StringRegexUnit(this), times)
	
	infix fun RegexUnit.repeatTime(times: Int) = RepeatRegexUnit(StringRegexUnit(unit ?: ""), times)
	infix fun String.repeatTime(times: Int) = RepeatRegexUnit(StringRegexUnit(this), times)
	
	fun RegexUnit.timeRange(from: Int, to: Int) = RepeatRegexUnit(StringRegexUnit(unit ?: ""), from, to)
	fun String.timeRange(from: Int, to: Int) = RepeatRegexUnit(StringRegexUnit(this), from, to)
	
	infix fun RegexUnit.link(target: RegexUnit) = StringRegexUnit("${unit ?: ""}${target.unit ?: ""}")
	infix fun RegexUnit.link(target: String) = StringRegexUnit("${unit ?: ""}${StringRegexUnit(target).unit}")
	infix fun String.link(target: RegexUnit) = StringRegexUnit("${StringRegexUnit(this).unit}${target.unit}")
	infix fun String.link(target: String) = StringRegexUnit("${StringRegexUnit(this).unit}${StringRegexUnit(target).unit}")
	
	infix fun RegexUnit.also(target: RegexUnit) = StringRegexUnit("${unit ?: ""}${target.unit ?: ""}")
	infix fun RegexUnit.also(target: String) = StringRegexUnit("${unit ?: ""}${StringRegexUnit(target).unit}")
	infix fun String.also(target: RegexUnit) = StringRegexUnit("${StringRegexUnit(this).unit}${target.unit}")
	infix fun String.also(target: String) = StringRegexUnit("${StringRegexUnit(this).unit}${StringRegexUnit(target).unit}")
	
	infix fun RegexUnit.or(target: RegexUnit): StringRegexUnit {
		val unit = this.unit
		val targetUnit = target.unit
		return StringRegexUnit(when {
			unit == null -> targetUnit ?: ""
			targetUnit == null -> unit
			else -> "$unit|$targetUnit"
		})
	}
	
	infix fun String.or(target: RegexUnit): StringRegexUnit {
		val unit = StringRegexUnit(this).unit
		val targetUnit = target.unit
		return StringRegexUnit(when (targetUnit) {
			null -> unit ?: ""
			else -> "$unit|$targetUnit"
		})
	}
	
	infix fun RegexUnit.or(target: String): StringRegexUnit {
		val unit = this.unit
		val targetUnit = StringRegexUnit(target).unit
		return StringRegexUnit(when (unit) {
			null -> targetUnit ?: ""
			else -> "$unit|$targetUnit"
		})
	}
	
	infix fun String.or(target: String): StringRegexUnit {
		val unit = StringRegexUnit(this).unit
		val targetUnit = StringRegexUnit(target).unit
		return StringRegexUnit("$unit|$targetUnit")
	}
	
	infix fun Char.list(target: Char) = CharListRegexUnit(this, target)
	
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
	
	@Suppress("UNUSED_EXPRESSION")
	fun make(func: RegexWildcard.() -> RegexUnit) = func()
}
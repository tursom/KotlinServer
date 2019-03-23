package cn.tursom.regex

@Suppress("unused", "MemberVisibilityCanBePrivate")
object RegexMaker {
	val slush = UnitRegexUnit("\\\\")
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
	val empty = UnitRegexUnit("()")
	
	val uppercase = 'A' % 'Z'
	val lowercase = 'a' % 'z'
	val numbers = '0' % '9'
	
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
		get() = RepeatRegexUnit(this, 1, -1)
	
	val RegexUnit.anyTime
		get() = RepeatRegexUnit(this, -1)
	
	val RegexUnit.onceBelow
		get() = RepeatRegexUnit(this, 0, 1)
	
	infix fun RegexUnit.repeat(times: Int) = RepeatRegexUnit(this, times)
	infix fun String.repeat(times: Int) = RepeatRegexUnit(this, times)
	
	infix fun RegexUnit.repeat(times: IntRange) = RepeatRegexUnit(this, times)
	infix fun String.repeat(times: IntRange) = RepeatRegexUnit(this, times)
	
	infix fun RegexUnit.repeat(times: Pair<Int, Int>) = RepeatRegexUnit(this, times)
	infix fun String.repeat(times: Pair<Int, Int>) = RepeatRegexUnit(this, times)
	
	infix fun RegexUnit.repeatTime(times: Int) = RepeatRegexUnit(this, times)
	infix fun String.repeatTime(times: Int) = RepeatRegexUnit(this, times)
	
	infix fun RegexUnit.repeatLast(times: Int) = RepeatRegexUnit(this, times, -1)
	infix fun String.repeatLast(times: Int) = RepeatRegexUnit(this, times, -1)
	
	infix fun RegexUnit.last(times: Int) = RepeatRegexUnit(this, times, -1)
	infix fun String.last(times: Int) = RepeatRegexUnit(this, times, -1)
	
	operator fun RegexUnit.rem(times: Int) = RepeatRegexUnit(this, times, -1)
	operator fun String.rem(times: Int) = RepeatRegexUnit(this, times, -1)
	
	operator fun RegexUnit.times(times: Int) = RepeatRegexUnit(this, times)
	operator fun String.times(times: Int) = RepeatRegexUnit(this, times)
	
	infix fun RegexUnit.upTo(times: Int) = RepeatRegexUnit(this, 0, times)
	infix fun String.upTo(times: Int) = RepeatRegexUnit(this, 0, times)
	
	operator fun RegexUnit.minus(times: Int) = RepeatRegexUnit(this, 0, times)
	operator fun String.minus(times: Int) = RepeatRegexUnit(this, 0, times)
	
	operator fun RegexUnit.minus(range: IntRange) = RepeatRegexUnit(this, range)
	operator fun String.minus(range: IntRange) = RepeatRegexUnit(this, range)
	
	operator fun RegexUnit.times(range: IntRange) = RepeatRegexUnit(this, range)
	operator fun String.times(range: IntRange) = RepeatRegexUnit(this, range)
	
	fun RegexUnit.timeRange(from: Int, to: Int) = RepeatRegexUnit(this, from, to)
	fun String.timeRange(from: Int, to: Int) = RepeatRegexUnit(this, from, to)
	
	operator fun RegexUnit.rangeTo(range: IntRange) = RepeatRegexUnit(this, range)
	operator fun String.rangeTo(range: IntRange) = RepeatRegexUnit(this, range)
	
	operator fun RegexUnit.rangeTo(range: Pair<Int, Int>) = RepeatRegexUnit(this, range)
	operator fun String.rangeTo(range: Pair<Int, Int>) = RepeatRegexUnit(this, range)
	
	operator fun RegexUnit.times(range: Pair<Int, Int>) = RepeatRegexUnit(this, range)
	operator fun String.times(range: Pair<Int, Int>) = RepeatRegexUnit(this, range)
	
	operator fun RegexUnit.minus(range: Pair<Int, Int>) = RepeatRegexUnit(this, range)
	operator fun String.minus(range: Pair<Int, Int>) = RepeatRegexUnit(this, range)
	
	operator fun RegexUnit.rem(range: Pair<Int, Int>) = RepeatRegexUnit(this, range)
	operator fun String.rem(range: Pair<Int, Int>) = RepeatRegexUnit(this, range)
	
	operator fun RegexUnit.rem(range: IntRange) = RepeatRegexUnit(this, range)
	operator fun String.rem(range: IntRange) = RepeatRegexUnit(this, range)
	
	infix fun RegexUnit.link(target: RegexUnit) = StringRegexUnit("$this$target")
	infix fun RegexUnit.link(target: String) = StringRegexUnit("$this$target")
	infix fun String.link(target: RegexUnit) = StringRegexUnit("$this$target")
	infix fun String.link(target: String) = StringRegexUnit("$this$target")
	
	infix fun RegexUnit.also(target: RegexUnit) = StringRegexUnit("$this$target")
	infix fun RegexUnit.also(target: String) = StringRegexUnit("$this$target")
	infix fun String.also(target: RegexUnit) = StringRegexUnit("$this$target")
	infix fun String.also(target: String) = StringRegexUnit("$this$target")
	
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
	
	infix fun Char.list(target: Char) = UnitListRegexUnit(this, target)
	
	infix fun CharRange.also(unitList: UnitListRegexUnit) = UnitListRegexUnit(this) also unitList
	infix fun CharRange.also(unitList: CharRange) = UnitListRegexUnit(this) also unitList
	infix fun CharRange.also(unitList: Pair<Char, Char>) = UnitListRegexUnit(this) also unitList
	
	infix fun Pair<Char, Char>.also(unitList: UnitListRegexUnit) = UnitListRegexUnit(this) also unitList
	infix fun Pair<Char, Char>.also(unitList: CharRange) = UnitListRegexUnit(this) also unitList
	infix fun Pair<Char, Char>.also(unitList: Pair<Char, Char>) = UnitListRegexUnit(this) also unitList
	
	infix fun CharRange.and(unitList: UnitListRegexUnit) = UnitListRegexUnit(this) and unitList
	infix fun CharRange.and(unitList: CharRange) = UnitListRegexUnit(this) and unitList
	infix fun CharRange.and(unitList: Pair<Char, Char>) = UnitListRegexUnit(this) and unitList
	
	infix fun Pair<Char, Char>.and(unitList: UnitListRegexUnit) = UnitListRegexUnit(this) and unitList
	infix fun Pair<Char, Char>.and(unitList: CharRange) = UnitListRegexUnit(this) and unitList
	infix fun Pair<Char, Char>.and(unitList: Pair<Char, Char>) = UnitListRegexUnit(this) and unitList
	
	operator fun Char.rem(char: Char) = UnitListRegexUnit(this, char)
	
	operator fun RegexUnit.invoke(unit: RegexUnit) = this link unit
	operator fun RegexUnit.invoke(times: Int) = this repeat times
	operator fun RegexUnit.invoke(times: IntRange) = this repeat times
	operator fun RegexUnit.invoke(times: Pair<Int, Int>) = this repeat times
	operator fun RegexUnit.invoke(from: Int, to: Int) = RepeatRegexUnit(this, from, to)
	
	operator fun UnitListRegexUnit.invoke(unitList: UnitListRegexUnit) = this and unitList
	
	object UnitList {
		const val hyphen = "\\-"
		const val slush = "\\\\"
	}
	
	class UnitListCheckException : Exception()
	
	fun list(func: UnitList.() -> String) = UnitList.func().let {
		if (it.filterIndexed { index, c -> c == '-' && (index == 0 || it[index - 1] != '\\') }.isNotEmpty())
			throw UnitListCheckException()
		UnitListRegexUnit(it)
	}
	
	fun str(str: String) = StringRegexUnit(str)
	
	@Suppress("UNUSED_EXPRESSION")
	fun make(func: RegexMaker.() -> RegexUnit) = Regex(func().toString())
}

fun regex(func: RegexMaker.() -> RegexUnit) = Regex(RegexMaker.func().toString())
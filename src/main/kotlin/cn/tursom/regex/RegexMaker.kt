package cn.tursom.regex

/**
 * 使用 regex 函数创建一个正则对象
 * 字符串前加 + 表示一个字符串单元，任何时候都会作为一个独立单元存在
 * 字符串前加 - 表示一个字符串，不会作为一个独立单元处理
 * 注意，我们不支持原始的字符串对象，请使用 + 或 - 将其打包
 * 在 RegexMaker 对象头部的这些对象都是字符转义，请根据需要使用
 * uppercase，lowercase 与 numbers 都是字符列表，用于表示在其范围内的单个字符
 * 使用 Char.control 获得控制字符转义
 * 接下来是连接两个正则单元用的方法，我们可以用 (单元1) link (单元2)，(单元1) also (单元2)，甚至是 (单元1)(单元2) 的形式连接两个单元
 * 接着是创建单元组的方法，toSet 不建议使用，建议使用 ((单元1) or (单元2) or ...) 的形式创建一个单元组
 * 后面跟着的就都是表示重复次数的方法，(单元)-次数n 表示最多重复n次，相应的 * 与 .. 表示精确的 n 次，%表示至少 n 次
 * 我们还可以使用 (单元)-(min..max)的形式指定重复区间，对于这个接口，-、*、% 与 .. 的效果相同，范围区间还可以使用(min to max)的形式
 * 如果我们想匹配属于某一组的单个字符，可以使用 (开始字符 % 结束字符) 的形式，使用 and、also、link 或者 + 将多个字符组单元相连
 * 我们还可以在一个 CharRange 或 Pair<Char, Char> 前面加 + 生成字符组
 * 或者我们也可以手动指定使用哪些字符，使用 list 方法，返回的字符串中的所有字符就会组成一个字符组单元，
 * 不过要注意，- 必须使用 $hyphen 转义，否则会抛出 UnitListCheckException 异常
 */

@Suppress("unused", "MemberVisibilityCanBePrivate")
object RegexMaker {
	operator fun String.unaryMinus() = UnitRegexUnit(this)
	operator fun String.unaryPlus() = StringRegexUnit(this)
	
	val slush = -"\\\\"
	val pointChar = -"\\."
	val caret = -"\\^"
	val dollar = -"\\$"
	val plus = -"\\+"
	val roundBrackets = -"\\("
	val squareBrackets = -"\\["
	val curlyBrackets = -"\\{"
	val backslash = -"\\\\"
	val verticalBar = -"\\|"
	val questionMark = -"\\?"
	val nextPage = -"\\f"
	val nextLine = -"\\n"
	val enter = -"\\r"
	val space = -"\\s"
	val nonSpace = -"\\S"
	val tab = -"\\t"
	val vertical = -"\\v"
	val wordBoundary = -"\\b"
	val nonWordBoundary = -"\\B"
	
	/**
	 * @warning except \n
	 */
	val any = -"."
	val beg = -"^"
	val end = -"$"
	val empty = -"()"
	
	val uppercase = 'A' % 'Z'
	val lowercase = 'a' % 'z'
	val numbers = '0' % '9'
	
	val Char.control
		get() = ControlCharRegexUnit(this)
	
	infix fun RegexUnit.link(target: RegexUnit) = +"$this$target"
	infix fun RegexUnit.also(target: RegexUnit) = +"$this$target"
	operator fun RegexUnit.invoke(unit: RegexUnit) = this link unit
	
	val Iterable<RegexUnit>.toSet: StringRegexUnit?
		get() {
			val iterator = iterator()
			if (!iterator.hasNext()) return null
			val stringBuilder = StringBuilder()
			stringBuilder.append(iterator.next().unit)
			forEach {
				stringBuilder.append("|${it.unit}")
			}
			return StringRegexUnit(stringBuilder.toString())
		}
	
	val Array<out RegexUnit>.toSet: StringRegexUnit?
		get() {
			val iterator = iterator()
			if (!iterator.hasNext()) return null
			val stringBuilder = StringBuilder()
			stringBuilder.append(iterator.next().unit)
			forEach {
				stringBuilder.append("|${it.unit}")
			}
			return StringRegexUnit(stringBuilder.toString())
		}
	
	infix fun RegexUnit.or(target: RegexUnit): StringRegexUnit {
		val unit = this.unit
		val targetUnit = target.unit
		return +when {
			unit == null -> targetUnit ?: ""
			targetUnit == null -> unit
			else -> "$unit|$targetUnit"
		}
	}
	
	val RegexUnit.onceMore
		get() = RepeatRegexUnit(this, 1, -1)
	
	val RegexUnit.anyTime
		get() = RepeatRegexUnit(this, -1)
	
	val RegexUnit.onceBelow
		get() = RepeatRegexUnit(this, 0, 1)
	
	infix fun RegexUnit.repeat(times: Int) = RepeatRegexUnit(this, times)
	infix fun RegexUnit.repeat(times: IntRange) = RepeatRegexUnit(this, times)
	infix fun RegexUnit.repeat(times: Pair<Int, Int>) = RepeatRegexUnit(this, times)
	fun RegexUnit.timeRange(from: Int, to: Int) = RepeatRegexUnit(this, from, to)
	
	operator fun RegexUnit.invoke(times: Int) = this repeat times
	operator fun RegexUnit.invoke(times: IntRange) = this repeat times
	operator fun RegexUnit.invoke(times: Pair<Int, Int>) = this repeat times
	operator fun RegexUnit.invoke(from: Int, to: Int) = this.timeRange(from, to)
	
	infix fun RegexUnit.upTo(times: Int) = RepeatRegexUnit(this, 0, times)
	infix fun RegexUnit.repeatTime(times: Int) = RepeatRegexUnit(this, times)
	infix fun RegexUnit.repeatLast(times: Int) = RepeatRegexUnit(this, times, -1)
	infix fun RegexUnit.last(times: Int) = RepeatRegexUnit(this, times, -1)
	
	operator fun RegexUnit.rem(times: Int) = RepeatRegexUnit(this, times, -1)
	operator fun RegexUnit.times(times: Int) = RepeatRegexUnit(this, times)
	operator fun RegexUnit.minus(times: Int) = RepeatRegexUnit(this, 0, times)
	operator fun RegexUnit.rangeTo(range: Int) = RepeatRegexUnit(this, range)
	
	operator fun RegexUnit.rem(range: Pair<Int, Int>) = RepeatRegexUnit(this, range)
	operator fun RegexUnit.rem(range: IntRange) = RepeatRegexUnit(this, range)
	operator fun RegexUnit.times(range: IntRange) = RepeatRegexUnit(this, range)
	operator fun RegexUnit.times(range: Pair<Int, Int>) = RepeatRegexUnit(this, range)
	operator fun RegexUnit.minus(range: IntRange) = RepeatRegexUnit(this, range)
	operator fun RegexUnit.minus(range: Pair<Int, Int>) = RepeatRegexUnit(this, range)
	operator fun RegexUnit.rangeTo(range: IntRange) = RepeatRegexUnit(this, range)
	operator fun RegexUnit.rangeTo(range: Pair<Int, Int>) = RepeatRegexUnit(this, range)
	
	infix fun Char.list(target: Char) = UnitListRegexUnit(this, target)
	operator fun Char.rem(char: Char) = UnitListRegexUnit(this, char)
	operator fun CharRange.unaryPlus() = UnitListRegexUnit(this)
	operator fun Pair<Char, Char>.unaryPlus() = UnitListRegexUnit(this)
	
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
	
	@Suppress("UNUSED_EXPRESSION")
	fun make(func: RegexMaker.() -> RegexUnit) = Regex(func().toString())
}

fun regex(func: RegexMaker.() -> RegexUnit) = Regex(RegexMaker.func().toString())
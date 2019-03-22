package cn.tursom.database.clauses

import cn.tursom.database.fieldName
import cn.tursom.database.sqlStr
import java.lang.reflect.Field
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaField

@Suppress("unused")
object RegexWildcard {
	const val pointChar = "\\."
	const val caret = "\\^"
	const val dollar = "\\$"
	const val plus = "\\+"
	const val roundBrackets = "\\("
	const val squareBrackets = "\\["
	const val curlyBrackets = "\\{"
	const val backslash = "\\\\"
	const val verticalBar = "\\|"
	const val questionMark = "\\?"
	const val nextPage = "\\f"
	const val nextLine = "\\n"
	const val enter = "\\r"
	const val space = "\\s"
	const val nonSpace = "\\S"
	const val tab = "\\t"
	const val vertical = "\\v"
	const val wordBoundary = "\\b"
	const val nonWordBoundary = "\\B"
	const val single: Char = '.'
	const val beg: Char = '^'
	const val end: Char = '$'
	val Iterable<String>.toSet: String?
		get() {
			val iterator = iterator()
			if (!iterator.hasNext()) return null
			val stringBuilder = StringBuilder()
			stringBuilder.append(iterator.next())
			forEach {
				stringBuilder.append("|$it")
			}
			return "($stringBuilder)"
		}
	val Array<out String>.toSet: String?
		get() {
			val iterator = iterator()
			if (!iterator.hasNext()) return null
			val stringBuilder = StringBuilder()
			stringBuilder.append(iterator.next())
			forEach {
				stringBuilder.append("|$it")
			}
			return "($stringBuilder)"
		}
	val String.toUnit: String
		get() = "($this)"
	val Char.control
		get() = "\\c$this"
	
	fun charList(charList: String) = "[$charList]"
	fun unCharList(charList: String) = "[^$charList]"
	infix operator fun Char.rangeTo(target: Char) = "$this-$target"
	infix fun Char.to(target: Char) = "$this-$target"
	infix fun String.also(target: String) = "$this$target"
	infix fun String.or(target: String) = "$this|$target"
	fun String.onceMore() = "($this)+"
	fun String.anyTime() = "($this)*"
	fun String.noneOrOnce() = "($this)?"
	fun String.repeatTime(times: Int) = "($this){$times}"
	fun String.timeRange(from: Int, to: Int) = "($this){$from,$to}"
	@Suppress("UNUSED_EXPRESSION")
	fun make(func: RegexWildcard.() -> String) = func()
}

class RegexpClauses(val field: String, val value: String) : Clause {
	constructor(field: Field, value: String) : this(field.fieldName, value)
	constructor(field: KProperty<*>, value: String) : this(field.javaField!!, value)
	
	constructor(field: String, value: RegexWildcard.() -> String)
		: this(field, RegexWildcard.value())
	
	constructor(field: Field, value: RegexWildcard.() -> String)
		: this(field, RegexWildcard.value())
	
	constructor(field: KProperty<*>, value: RegexWildcard.() -> String)
		: this(field, RegexWildcard.value())
	
	override val sqlStr: String
		get() = "${this.field} REGEXP '${value.sqlStr}'"
}
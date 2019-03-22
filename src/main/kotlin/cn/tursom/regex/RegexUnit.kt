package cn.tursom.regex

interface RegexUnit {
	val unit: String?
}

val RegexUnit.onceMore
	get() = UnitRegexUnit(unit?.let { "$it+" } ?: "")

val RegexUnit.anyTime
	get() = UnitRegexUnit(unit?.let { "$it*" } ?: "")

val RegexUnit.onceBelow
	get() = UnitRegexUnit(unit?.let { "$it?" } ?: "")

infix fun RegexUnit.repeat(times: Int) = UnitRegexUnit(unit?.let { "$it{$times}" } ?: "")

infix fun RegexUnit.repeatTime(times: Int) = UnitRegexUnit(unit?.let { "$it{$times}" } ?: "")

fun RegexUnit.timeRange(from: Int, to: Int) = UnitRegexUnit(unit?.let { "$it{$from,$to}" } ?: "")

infix fun RegexUnit.also(target: RegexUnit) = UnitRegexUnit("${unit ?: ""}${target.unit ?: ""}")

infix fun RegexUnit.and(target: RegexUnit) = UnitRegexUnit("${unit ?: ""}${target.unit ?: ""}")

infix fun RegexUnit.or(target: RegexUnit): UnitRegexUnit {
	val unit = this.unit
	val targetUnit = target.unit
	return UnitRegexUnit(when {
		unit == null -> targetUnit ?: ""
		targetUnit == null -> unit
		else -> "$unit|$targetUnit"
	})
}
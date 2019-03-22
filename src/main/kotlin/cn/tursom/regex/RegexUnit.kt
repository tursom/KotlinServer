package cn.tursom.regex

interface RegexUnit {
	val unit: String?
}

fun RegexUnit.onceMore() = UnitRegexUnit(unit?.let { "$it+" } ?: "")

fun RegexUnit.anyTime() = UnitRegexUnit(unit?.let { "$it*" } ?: "")

fun RegexUnit.noneOrOnce() = UnitRegexUnit(unit?.let { "$it?" } ?: "")

fun RegexUnit.repeatTime(times: Int) = UnitRegexUnit(unit?.let { "$it{$times}" } ?: "")

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
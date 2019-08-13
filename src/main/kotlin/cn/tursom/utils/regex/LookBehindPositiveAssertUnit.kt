package cn.tursom.utils.regex

class LookBehindPositiveAssertUnit(val subUnit: RegexUnit) : RegexUnit {
    override fun toString(): String = "(?<=$subUnit)"
}
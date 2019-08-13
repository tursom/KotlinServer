package cn.tursom.utils.regex

class LookAheadPositiveAssertUnit(val subUnit: RegexUnit) : RegexUnit {
    override fun toString(): String = "(?=$subUnit)"
}
package cn.tursom.utils.regex

class LookBehindNegativeAssertUnit(val subUnit: RegexUnit) : RegexUnit {
    override fun toString(): String = "(?<!$subUnit)"
}
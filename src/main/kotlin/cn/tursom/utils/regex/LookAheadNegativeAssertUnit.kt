package cn.tursom.utils.regex

class LookAheadNegativeAssertUnit(val subUnit: RegexUnit) : RegexUnit {
    override fun toString(): String =  "(?=$subUnit)"
}
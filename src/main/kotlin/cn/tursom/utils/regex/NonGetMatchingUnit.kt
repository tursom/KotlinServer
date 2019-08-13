package cn.tursom.utils.regex

class NonGetMatchingUnit(val subUnit: RegexUnit) : RegexUnit {
    override fun toString(): String = "(?:$subUnit)"
}
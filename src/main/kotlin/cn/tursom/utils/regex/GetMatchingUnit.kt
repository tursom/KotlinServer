package cn.tursom.utils.regex

class GetMatchingUnit(val subUnit: RegexUnit) : RegexUnit {
    override val unit: String?
        get() = toString()

    override fun toString(): String = "($subUnit)"
}
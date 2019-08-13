package cn.tursom.utils.regex

interface RegexUnit {
    val unit: String? get() = toString()
    val regex get() = toString().toRegex()
    override fun toString(): String
}
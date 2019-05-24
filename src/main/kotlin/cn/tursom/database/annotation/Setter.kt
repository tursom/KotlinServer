package cn.tursom.database.annotation

@MustBeDocumented
@Target(AnnotationTarget.FIELD)
annotation class Setter(val setter: String)
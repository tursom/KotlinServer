package cn.tursom.xml

enum class ElementTarget {
	Attribute, Data
}

@Target(AnnotationTarget.CLASS)
annotation class DefaultTarget(val target: ElementTarget)

/**
 * Short, Int, Long
 * Float, Double
 * Boolean
 * String
 */
@Target(AnnotationTarget.FIELD)
annotation class Attribute

/**
 * Short, Int, Long
 * Float, Double
 * Boolean
 * String
 */
@Target(AnnotationTarget.FIELD)
annotation class Text

/**
 * 指定转换函数的名称
 */
@Target(AnnotationTarget.FIELD)
annotation class Setter(val setter: String)

@Target(AnnotationTarget.FIELD)
annotation class FieldName(val name: String)

@Target(AnnotationTarget.CLASS)
annotation class ElementName(val name: String)

@Target(AnnotationTarget.CLASS, AnnotationTarget.FIELD)
annotation class CompressionXml

/**
 * fun #getter(
 *   obj: FieldType,
 *   elementName: String,
 *   builder: StringBuilder,
 *   indentation: String,
 *   advanceIndentation: String
 * )
 *
 * or
 *
 * fun #getter(
 *   obj: FieldType,
 *   elementName: String,
 *   builder: StringBuilder
 * )
 *
 */
@Target(AnnotationTarget.FIELD)
annotation class ToXml(val getter: String)

package cn.tursom.xml

enum class ElementTarget {
	Attribute, ElementText, SubElement
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
annotation class ElementText

@Target(AnnotationTarget.FIELD)
annotation class SubElement

/**
 * 指定转换函数的名称
 * fun setter(text: String): FieldType
 * or ( advance setter )
 * fun setter(element: Element): FieldType
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
 * simplify:
 *
 * fun #getter(
 *   obj: FieldType,
 *   elementName: String,
 *   indentation: String,
 *   advanceIndentation: String
 * ): Any
 *
 * or
 *
 * fun #getter(
 *   obj: FieldType,
 *   elementName: String
 * ): Any
 */
@Target(AnnotationTarget.FIELD)
annotation class ToXml(val getter: String)

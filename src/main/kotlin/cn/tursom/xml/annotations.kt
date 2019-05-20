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
 * fun callback(text: String): FieldType
 * or ( advance callback )
 * fun callback(element: Element): FieldType
 */
@Target(AnnotationTarget.FIELD)
annotation class Setter(val callback: String)

@Target(AnnotationTarget.FIELD)
annotation class FieldName(val name: String)

@Target(AnnotationTarget.CLASS)
annotation class ElementName(val name: String)

@Target(AnnotationTarget.CLASS, AnnotationTarget.FIELD)
annotation class CompressionXml

/**
 * fun callback(
 *   obj: FieldType,
 *   elementName: String,
 *   builder: StringBuilder,
 *   indentation: String,
 *   advanceIndentation: String
 * )
 *
 * or
 *
 * fun callback(
 *   obj: FieldType,
 *   elementName: String,
 *   builder: StringBuilder
 * )
 *
 * simplify:
 *
 * fun callback(
 *   obj: FieldType,
 *   elementName: String,
 *   indentation: String,
 *   advanceIndentation: String
 * ): Any
 *
 * or
 *
 * fun callback(
 *   obj: FieldType,
 *   elementName: String
 * ): Any
 */
@Target(AnnotationTarget.FIELD)
annotation class ToXml(val callback: String)

/**
 * 数组所有的元素都同名，在同一个父节点下
 */
@Target(AnnotationTarget.FIELD)
annotation class Vararg

package cn.tursom.xml


data class SubElement(
	@Data val data: String
)

@DefaultTarget(ElementTarget.Attribute)
data class Root(
	@Data val data: String,
	val encodePort: Int,
	val decodePort: Int,
	@FieldName("hi") val subElement: SubElement
)

fun main() {
	println(Xml.parse(Root::class.java, """
		<root encodePort="1" decodePort="456">hi<hi>还行</hi> 2</root>
	"""))
}
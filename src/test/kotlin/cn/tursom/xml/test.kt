package cn.tursom.xml


@DefaultTarget(ElementTarget.Attribute)
data class SubElement(
	@Data val data: String
)

@DefaultTarget(ElementTarget.Attribute)
@ElementName("root")
data class Root(
	@Data val data: String,
	val encodePort: Int,
	val decodePort: Int,
	@FieldName("hi") val subElement: SubElement
)

fun main() {
	val xml = Xml.parse(Root::class.java, """
		<root encodePort="1" decodePort="456">hi<hi>还行</hi> 2</root>
	""")
	println(xml)
	println(Xml.toXml(xml, indentation = "\t"))
}
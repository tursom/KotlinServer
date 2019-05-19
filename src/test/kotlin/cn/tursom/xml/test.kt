package cn.tursom.xml

import com.google.gson.Gson
import org.dom4j.Element


@DefaultTarget(ElementTarget.Attribute)
data class SubElement(
	@Text val text: String
)

@Suppress("unused")
@DefaultTarget(ElementTarget.Attribute)
@ElementName("root")
data class Root(
	@Text val text: String,
	val encodePort: Int,
	val decodePort: Int,
	@FieldName("hi") val subElement: SubElement,
	@CompressionXml @Setter("element") @ToXml("toXml") val map: HashMap<String, String>
) {
	fun element(element: Element): HashMap<String, String> {
		val map = HashMap<String, String>()
		element.elements().forEach {
			val ele = it as Element
			map[ele.name] = ele.text
		}
		return map
	}
	
	fun toXml(
		obj: HashMap<String, String>,
		elementName: String,
		builder: StringBuilder,
		indentation: String,
		@Suppress("UNUSED_PARAMETER") advanceIndentation: String
	) {
		builder.append("$indentation<$elementName>${Gson().toJson(obj)}</$elementName>\n")
	}
	
	fun toXml(
		obj: HashMap<String, String>,
		elementName: String,
		builder: StringBuilder
	) {
		builder.append("<$elementName>${Gson().toJson(obj)}</$elementName>")
	}
}

@ElementName("date")
data class Date(
	@Text val time: Long = System.currentTimeMillis()
)

fun main() {
	val xml = Xml.parse(Root::class.java, """
		<root encodePort="1" decodePort="456">hi<hi>还行</hi> <map><a>1</a><b>2</b></map>2</root>
	""")
	println(xml)
	println(Xml.toXml(xml))
	println(Xml.toXmlCom(xml))
//	println(Xml.toXml(mapOf(123 to 456, 1 to 2, 454345 to 12, "abc" to 4), rootName = "map", indentation = "\t"))
}
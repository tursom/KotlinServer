package cn.tursom.xml

import cn.tursom.tools.fromJson
import com.google.gson.Gson
import java.util.concurrent.TimeUnit


@Suppress("unused")
@DefaultTarget(ElementTarget.SubElement)
@ElementName("root")
data class Root(
	@Attribute val encodePort: Int,
	@Attribute val decodePort: Int,
	@Attribute @FieldName("hi") val subElement: String,
	@FieldName("char") val enumTest: TimeUnit,
	@CompressionXml @Setter("element") @ToXml("toXml") val map: HashMap<String, String>
) {
	fun element(text: String) = Gson().fromJson<HashMap<String, String>>(text)
	fun toXml(
		obj: HashMap<String, String>,
		elementName: String
	) = "<$elementName>${Gson().toJson(obj)}</$elementName>"
}

@ElementName("date")
data class Date(
	@ElementText val time: Long = System.currentTimeMillis()
)

fun main() {
	val xml = Xml.parse(Root::class.java, """
		<root encodePort="123" decodePort="456" hi="还行">
    <encodePort>123</encodePort>
    <decodePort>456</decodePort>
    <hi>还行</hi>
		<char>MILLISECONDS</char>
    <map>{"a":"1","b":"2"}</map>
</root>
	""")
	println(xml)
	println(Xml.toXml(xml))
	println(Xml.toXmlCom(xml))
	println(Xml.toXmlCom(arrayOf(1, 2, 3, 4, "海星"), "array"))
	println(Xml.toXml(listOf(9 to 2, 666 to 777, 9 to 3, 4, "海星"), "array"))
	println(Xml.toXml(mapOf(123 to 456, 1 to 2, 454345 to 12, "abc" to "海星"), rootName = "map", indentation = "\t"))
}
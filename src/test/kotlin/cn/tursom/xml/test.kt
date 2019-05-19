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
	@MultipleField @FieldName("hi") val subElement: Array<String>,
	val intArray: Array<Int>,
	@FieldName("char") val enumTest: TimeUnit,
	@CompressionXml @Setter("element") @ToXml("toXml") val map: HashMap<String, String>
) {
	fun element(text: String) = Gson().fromJson<HashMap<String, String>>(text)
	fun toXml(
		obj: HashMap<String, String>,
		elementName: String
	) = "<$elementName>${Gson().toJson(obj)}</$elementName>"
	
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false
		
		other as Root
		
		if (encodePort != other.encodePort) return false
		if (decodePort != other.decodePort) return false
		if (!subElement.contentEquals(other.subElement)) return false
		if (!intArray.contentEquals(other.intArray)) return false
		if (enumTest != other.enumTest) return false
		if (map != other.map) return false
		
		return true
	}
	
	override fun hashCode(): Int {
		var result = encodePort
		result = 31 * result + decodePort
		result = 31 * result + subElement.contentHashCode()
		result = 31 * result + intArray.contentHashCode()
		result = 31 * result + enumTest.hashCode()
		result = 31 * result + map.hashCode()
		return result
	}
}

fun main() {
	val xml = Xml.parse(Root::class.java, """
		<root encodePort="123" decodePort="456" hi="还行">
    <encodePort>123</encodePort>
    <decodePort>456</decodePort>
		<intArray>
			<a>1</a>
			<a>2</a>
			<a>3</a>
			<a>4</a>
			<a>5</a>
		</intArray>
    <hi>还行1</hi>
    <hi>还行2</hi>
    <hi>还行3</hi>
    <hi>还行4</hi>
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
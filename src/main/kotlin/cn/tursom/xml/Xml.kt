package cn.tursom.xml

import org.dom4j.Document
import org.dom4j.Element
import org.dom4j.io.SAXReader
import sun.misc.Unsafe
import java.io.File
import java.io.StringReader
import java.lang.reflect.Field
import java.net.URL

object Xml {
	val saxReader = SAXReader()
	
	//利用Unsafe绕过构造函数获取变量
	private val unsafe by lazy {
		val field = Unsafe::class.java.getDeclaredField("theUnsafe")
		//允许通过反射设置属性的值
		field.isAccessible = true
		field.get(null) as Unsafe
	}
	
	private val parseSet = setOf(
		Short::class.java,
		Int::class.java,
		Long::class.java,
		Float::class.java,
		Double::class.java,
		Boolean::class.java,
		String::class.java,
		
		java.lang.Short::class.java,
		java.lang.Integer::class.java,
		java.lang.Long::class.java,
		java.lang.Float::class.java,
		java.lang.Double::class.java,
		java.lang.Boolean::class.java,
		java.lang.String::class.java
	)
	
	private val Class<*>.defaultTarget
		get() = getAnnotation(DefaultTarget::class.java)?.target ?: ElementTarget.Attribute
	
	private val Class<*>.elementName: String
		get() = getAnnotation(ElementName::class.java)?.name ?: name
	
	private val Class<*>.dataField
		get() = declaredFields.filter {
			if (parseSet.contains(it.type))
				if (defaultTarget != ElementTarget.Data)
					it.getAnnotation(Data::class.java) != null
				else
					it.getAnnotation(Attribute::class.java) == null
			else
				false
		}
	
	private val Class<*>.attributeField
		get() = declaredFields.filter {
			if (parseSet.contains(it.type))
				if (defaultTarget != ElementTarget.Attribute)
					it.getAnnotation(Attribute::class.java) != null
				else
					it.getAnnotation(Data::class.java) == null
			else
				false
		}
	
	private val Class<*>.subElementField
		get() = declaredFields.filter {
			!parseSet.contains(it.type)
		}
	
	private val Field.elementName: String
		get() = getAnnotation(FieldName::class.java)?.name ?: name
	
	fun getData(element: Element, name: String, target: ElementTarget): String? = when (target) {
		ElementTarget.Attribute -> element.attribute(name).value
		ElementTarget.Data -> element.text
	}
	
	@Suppress("UNCHECKED_CAST")
	fun <T> parse(clazz: Class<T>, root: Element): T {
		val defaultTarget = clazz.defaultTarget
		val instance = unsafe.allocateInstance(clazz) as T
		
		clazz.declaredFields.forEach { field ->
			field.isAccessible = true
			
			val target = when {
				field.getAnnotation(Attribute::class.java) != null -> ElementTarget.Attribute
				field.getAnnotation(Data::class.java) != null -> ElementTarget.Data
				else -> defaultTarget
			}
			val fieldName = field.getAnnotation(FieldName::class.java)?.name ?: field.name
			
			val setter = field.getAnnotation(Setter::class.java)
			val value = if (setter != null) {
				val advanceSetMethod = clazz.getDeclaredMethod(setter.setter, Element::class.java)
				if (advanceSetMethod != null) {
					advanceSetMethod.isAccessible = true
					advanceSetMethod.invoke(instance, root.element(fieldName))
				} else {
					val setMethod = clazz.getDeclaredMethod(setter.setter, String::class.java)
					setMethod.isAccessible = true
					setMethod.invoke(instance, getData(root, fieldName, target))
				}
			} else {
				when (field.type) {
					Short::class.java -> getData(root, fieldName, target)?.toShortOrNull()
					Int::class.java -> getData(root, fieldName, target)?.toIntOrNull()
					Long::class.java -> getData(root, fieldName, target)?.toLongOrNull()
					Float::class.java -> getData(root, fieldName, target)?.toFloatOrNull()
					Double::class.java -> getData(root, fieldName, target)?.toDoubleOrNull()
					Boolean::class.java -> getData(root, fieldName, target)?.toBoolean()
					String::class.java -> getData(root, fieldName, target)
					
					java.lang.Short::class.java -> getData(root, fieldName, target)?.toShortOrNull()
					java.lang.Integer::class.java -> getData(root, fieldName, target)?.toIntOrNull()
					java.lang.Long::class.java -> getData(root, fieldName, target)?.toLongOrNull()
					java.lang.Float::class.java -> getData(root, fieldName, target)?.toFloatOrNull()
					java.lang.Double::class.java -> getData(root, fieldName, target)?.toDoubleOrNull()
					java.lang.Boolean::class.java -> getData(root, fieldName, target)?.toBoolean()
					java.lang.String::class.java -> getData(root, fieldName, target)
					
					else -> parse(field.type, root.element(fieldName) ?: return@forEach)
				}
			} ?: return@forEach
			field.set(instance, value)
		}
		
		return instance
	}
	
	fun <T> parse(clazz: Class<T>, document: Document): T = parse(clazz, document.rootElement)
	
	fun <T> parse(clazz: Class<T>, document: String): T = parse(clazz, saxReader.read(StringReader(document)))
	
	fun <T> parse(clazz: Class<T>, url: URL): T = parse(clazz, saxReader.read(url))
	
	fun <T> parse(clazz: Class<T>, file: File): T = parse(clazz, saxReader.read(file))
	
	fun toXml(obj: Any, elementName: String, builder: StringBuilder, indentation: String, advanceIndentation: String) {
		val clazz = obj.javaClass
		val dataFieldList = clazz.dataField
		val attributeField = clazz.attributeField
		val subElement = clazz.subElementField
		val subIndentation = "$advanceIndentation$indentation"
		
		builder.append(indentation)
		builder.append("<$elementName")
		attributeField.forEach {
			it.isAccessible = true
			builder.append("\n$subIndentation${it.elementName}=\"${it.get(obj)}\" ")
		}
		
		when {
			dataFieldList.isEmpty() && subElement.isEmpty() -> {
				builder.append(if (attributeField.isEmpty()) " />\n" else "\n$indentation/>\n")
				return
			}
			attributeField.isNotEmpty() -> {
				builder.append("\n$indentation>\n")
			}
			subElement.isNotEmpty() -> builder.append(">\n")
			else -> builder.append(">")
		}
		
		
		if (dataFieldList.isNotEmpty()) {
			val dataField = dataFieldList[0]
			dataField.isAccessible = true
			val value = dataField.get(obj)
			if (value != null) {
				if (attributeField.isNotEmpty()) builder.append(subIndentation)
				builder.append(value)
				if (attributeField.isNotEmpty()) builder.append("\n")
			}
		}
		
		subElement.forEach {
			it.isAccessible = true
			toXml(it.get(obj), it.elementName, builder, subIndentation, advanceIndentation)
		}
		
		if (attributeField.isNotEmpty() || subElement.isNotEmpty())
			builder.append(indentation)
		builder.append("</$elementName>\n")
	}
	
	fun toXml(obj: Any, rootName: String = obj.javaClass.elementName, indentation: String = "    "): String {
		val stringBuilder = StringBuilder()
		toXml(obj, rootName, stringBuilder, "", indentation)
		return stringBuilder.toString()
	}
}
package cn.tursom.xml

import org.dom4j.Document
import org.dom4j.Element
import org.dom4j.io.SAXReader
import sun.misc.Unsafe
import java.io.File
import java.io.StringReader
import java.lang.reflect.Array
import java.lang.reflect.Field
import java.net.URL

@Suppress("MemberVisibilityCanBePrivate", "unused")
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
		Byte::class.java,
		Short::class.java,
		Int::class.java,
		Long::class.java,
		Float::class.java,
		Double::class.java,
		Boolean::class.java,
		Char::class.java,
		String::class.java,
		
		java.lang.Byte::class.java,
		java.lang.Short::class.java,
		Integer::class.java,
		java.lang.Long::class.java,
		java.lang.Float::class.java,
		java.lang.Double::class.java,
		java.lang.Boolean::class.java,
		Character::class.java,
		java.lang.String::class.java
	)
	
	private val Class<*>.defaultTarget
		get() = getAnnotation(DefaultTarget::class.java)?.target ?: ElementTarget.Attribute
	
	private val Class<*>.elementName: String
		get() = getAnnotation(ElementName::class.java)?.name ?: if (isArray) componentType.name else name
	
	private val Class<*>.dataField
		get() = declaredFields.filter {
			if (parseSet.contains(it.type))
				if (defaultTarget != ElementTarget.Data)
					it.getAnnotation(Text::class.java) != null
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
					it.getAnnotation(Text::class.java) == null
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
				field.getAnnotation(Text::class.java) != null -> ElementTarget.Data
				else -> defaultTarget
			}
			val fieldName = field.getAnnotation(FieldName::class.java)?.name ?: field.name
			
			val setter = field.getAnnotation(Setter::class.java)
			val value = if (setter != null) {
				val advanceSetMethod = clazz.getDeclaredMethod(setter.setter, Element::class.java)
				if (advanceSetMethod != null) {
					advanceSetMethod.isAccessible = true
					advanceSetMethod.invoke(instance, root.element(fieldName) ?: return@forEach)
				} else {
					val setMethod = clazz.getDeclaredMethod(setter.setter, String::class.java)
					setMethod.isAccessible = true
					setMethod.invoke(instance, getData(root, fieldName, target))
				}
			} else {
				when (field.type) {
					Byte::class.java -> getData(root, fieldName, target)?.toByteOrNull()
					Short::class.java -> getData(root, fieldName, target)?.toShortOrNull()
					Int::class.java -> getData(root, fieldName, target)?.toIntOrNull()
					Long::class.java -> getData(root, fieldName, target)?.toLongOrNull()
					Float::class.java -> getData(root, fieldName, target)?.toFloatOrNull()
					Double::class.java -> getData(root, fieldName, target)?.toDoubleOrNull()
					Boolean::class.java -> getData(root, fieldName, target)?.toBoolean()
					Char::class.java -> getData(root, fieldName, target)?.toIntOrNull()?.toChar()
					String::class.java -> getData(root, fieldName, target)
					
					java.lang.Byte::class.java -> getData(root, fieldName, target)?.toByteOrNull()
					java.lang.Short::class.java -> getData(root, fieldName, target)?.toShortOrNull()
					Integer::class.java -> getData(root, fieldName, target)?.toIntOrNull()
					java.lang.Long::class.java -> getData(root, fieldName, target)?.toLongOrNull()
					java.lang.Float::class.java -> getData(root, fieldName, target)?.toFloatOrNull()
					java.lang.Double::class.java -> getData(root, fieldName, target)?.toDoubleOrNull()
					java.lang.Boolean::class.java -> getData(root, fieldName, target)?.toBoolean()
					java.lang.Character::class.java -> getData(root, fieldName, target)?.toIntOrNull()?.toChar()
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
	
	fun arrayXml(obj: Any, elementName: String, builder: StringBuilder, indentation: String, advanceIndentation: String) {
		val clazz = obj.javaClass
		if (clazz.isArray) {
			val subIndentation = "$advanceIndentation$indentation"
			val type = clazz.componentType
			
			if (type.getAnnotation(CompressionXml::class.java) != null) {
				arrayXmlCom(obj, elementName, builder)
				builder.append("\n")
				return
			}
			
			val subElementName = type.elementName
			
			builder.append("$indentation<$elementName>\n")
			for (i in 0 until Array.getLength(obj)) {
				val value = Array.get(obj, i) ?: continue
				if (parseSet.contains(clazz.componentType))
					builder.append("$subIndentation<$subElementName>$value</$subElementName>\n")
				else {
					toXml(value, elementName, builder, subIndentation, advanceIndentation)
				}
			}
			builder.append("$indentation</$elementName>\n")
			
			return
		}
	}
	
	fun iterableXml(obj: Iterable<*>, elementName: String, builder: StringBuilder, indentation: String, advanceIndentation: String) {
		val subIndentation = "$advanceIndentation$indentation"
		builder.append("$indentation<$elementName>\n")
		obj.forEach {
			val type = (it ?: return@forEach).javaClass
			
			if (type.getAnnotation(CompressionXml::class.java) != null) {
				arrayXmlCom(obj, elementName, builder)
				builder.append("\n")
				return@forEach
			}
			
			val subElementName = type.elementName
			if (parseSet.contains(type))
				builder.append("$subIndentation<$subElementName>$it</$subElementName>\n")
			else {
				toXml(it, subElementName, builder, subIndentation, advanceIndentation)
			}
		}
		builder.append("$indentation</$elementName>\n")
	}
	
	fun mapXml(obj: Map<*, *>, elementName: String, builder: StringBuilder, indentation: String, advanceIndentation: String) {
		val subIndentation = "$advanceIndentation$indentation"
		builder.append("$indentation<$elementName>\n")
		obj.forEach { (k, v) ->
			val type = (v ?: return@forEach).javaClass
			
			if (type.getAnnotation(CompressionXml::class.java) != null) {
				mapXmlCom(obj, elementName, builder)
				builder.append("\n")
				return@forEach
			}
			
			if (parseSet.contains(type))
				builder.append("$subIndentation<$k>$v</$k>\n")
			else {
				toXml(v, (k ?: return@forEach).toString(), builder, subIndentation, advanceIndentation)
			}
		}
		builder.append("$indentation</$elementName>\n")
	}
	
	fun normalXml(obj: Any, elementName: String, builder: StringBuilder, indentation: String, advanceIndentation: String) {
		val clazz = obj.javaClass
		
		if (clazz.getAnnotation(CompressionXml::class.java) != null) {
			normalXmlCom(obj, elementName, builder)
			builder.append("\n")
			return
		}
		
		val dataFieldList = clazz.dataField
		val attributeField = clazz.attributeField
		val subElement = clazz.subElementField
		val subIndentation = "$advanceIndentation$indentation"
		
		builder.append("$indentation<$elementName")
		attributeField.forEach {
			it.isAccessible = true
			val value = it.get(obj) ?: return@forEach
			
			builder.append(" ${it.elementName}=\"$value\"")
		}
		
		when {
			dataFieldList.isEmpty() && subElement.isEmpty() -> {
				builder.append(if (attributeField.isEmpty()) " />\n" else "\n$indentation/>\n")
				return
			}
			subElement.isNotEmpty() -> builder.append(">\n")
			else -> builder.append(">")
		}
		
		if (dataFieldList.isNotEmpty()) run {
			val dataField = dataFieldList[0]
			dataField.isAccessible = true
			val value = dataField.get(obj) ?: return@run
			
			if (attributeField.isNotEmpty()) builder.append(subIndentation)
			builder.append(value)
			if (attributeField.isNotEmpty()) builder.append("\n")
		}
		
		subElement.forEach {
			it.isAccessible = true
			val value = it.get(obj) ?: return@forEach
			
			it.getAnnotation(ToXml::class.java)?.let { getter ->
				val method = try {
					clazz.getDeclaredMethod(
						getter.getter,
						it.type,
						String::class.java,
						StringBuilder::class.java,
						String::class.java,
						String::class.java
					)
				} catch (e: NoSuchMethodException) {
					return@let
				}
				method.invoke(obj, value, it.elementName, builder, subIndentation, advanceIndentation)
				return@forEach
			}
			
			if (it.getAnnotation(CompressionXml::class.java) != null) {
				builder.append(subIndentation)
				toXmlCom(value, it.elementName, builder)
				builder.append("\n")
				return@forEach
			}
			
			toXml(value, it.elementName, builder, subIndentation, advanceIndentation)
		}
		
		if (attributeField.isNotEmpty() || subElement.isNotEmpty())
			builder.append(indentation)
		builder.append("</$elementName>\n")
	}
	
	fun toXml(obj: Any, elementName: String, builder: StringBuilder, indentation: String, advanceIndentation: String) {
		val clazz = obj.javaClass
		if (clazz.isArray) {
			arrayXml(obj, elementName, builder, indentation, advanceIndentation)
			return
		}
		
		try {
			mapXml(obj as Map<*, *>, elementName, builder, indentation, advanceIndentation)
			return
		} catch (e: ClassCastException) {
		}
		
		try {
			iterableXml(obj as Iterable<*>, elementName, builder, indentation, advanceIndentation)
			return
		} catch (e: ClassCastException) {
		}
		
		normalXml(obj, elementName, builder, indentation, advanceIndentation)
	}
	
	fun toXml(obj: Any, rootName: String = obj.javaClass.elementName, indentation: String = "    "): String {
		val stringBuilder = StringBuilder()
		toXml(obj, rootName, stringBuilder, "", indentation)
		if (stringBuilder.isNotEmpty()) stringBuilder.deleteCharAt(stringBuilder.length - 1)
		return stringBuilder.toString()
	}
	
	
	fun arrayXmlCom(obj: Any, elementName: String, builder: StringBuilder) {
		val clazz = obj.javaClass
		if (clazz.isArray) {
			val subElementName = clazz.componentType.elementName
			
			builder.append("<$elementName>")
			for (i in 0 until Array.getLength(obj)) {
				val value = Array.get(obj, i) ?: continue
				if (parseSet.contains(clazz.componentType))
					builder.append("<$subElementName>$value</$subElementName>")
				else {
					toXmlCom(value, elementName, builder)
				}
			}
			builder.append("</$elementName>")
			
			return
		}
	}
	
	fun iterableXmlCom(obj: Iterable<*>, elementName: String, builder: StringBuilder) {
		builder.append("<$elementName>\n")
		obj.forEach {
			val type = (it ?: return@forEach).javaClass
			val subElementName = type.elementName
			if (parseSet.contains(type))
				builder.append("<$subElementName>$it</$subElementName>")
			else {
				toXmlCom(it, subElementName, builder)
			}
		}
		builder.append("</$elementName>")
	}
	
	fun mapXmlCom(obj: Map<*, *>, elementName: String, builder: StringBuilder) {
		builder.append("<$elementName>")
		obj.forEach { (k, v) ->
			val type = (v ?: return@forEach).javaClass
			if (parseSet.contains(type))
				builder.append("<$k>$v</$k>")
			else {
				toXmlCom(v, (k ?: return@forEach).toString(), builder)
			}
		}
		builder.append("</$elementName>")
	}
	
	fun normalXmlCom(obj: Any, elementName: String, builder: StringBuilder) {
		val clazz = obj.javaClass
		val dataFieldList = clazz.dataField
		val attributeField = clazz.attributeField
		val subElement = clazz.subElementField
		
		builder.append("<$elementName")
		attributeField.forEach {
			it.isAccessible = true
			builder.append(" ${it.elementName}=\"${it.get(obj)}\"")
		}
		
		when {
			dataFieldList.isEmpty() && subElement.isEmpty() -> {
				builder.append(" />")
				return
			}
			else -> builder.append(">")
		}
		
		
		if (dataFieldList.isNotEmpty()) {
			val dataField = dataFieldList[0]
			dataField.isAccessible = true
			val value = dataField.get(obj)
			if (value != null) {
				builder.append(value)
			}
		}
		
		subElement.forEach {
			it.isAccessible = true
			val value = it.get(obj) ?: return@forEach
			
			it.getAnnotation(ToXml::class.java)?.let { getter ->
				val method = try {
					clazz.getDeclaredMethod(
						getter.getter,
						it.type,
						String::class.java,
						StringBuilder::class.java
					)
				} catch (e: NoSuchMethodException) {
					return@let
				}
				method.invoke(obj, value, it.elementName, builder)
				return@forEach
			}
			toXmlCom(value, it.elementName, builder)
		}
		
		builder.append("</$elementName>")
	}
	
	fun toXmlCom(obj: Any, elementName: String, builder: StringBuilder) {
		val clazz = obj.javaClass
		if (clazz.isArray) {
			arrayXmlCom(obj, elementName, builder)
			return
		}
		
		try {
			mapXmlCom(obj as Map<*, *>, elementName, builder)
			return
		} catch (e: ClassCastException) {
		}
		
		try {
			iterableXmlCom(obj as Iterable<*>, elementName, builder)
			return
		} catch (e: ClassCastException) {
		}
		
		normalXmlCom(obj, elementName, builder)
	}
	
	fun toXmlCom(obj: Any, rootName: String = obj.javaClass.elementName): String {
		val stringBuilder = StringBuilder()
		toXmlCom(obj, rootName, stringBuilder)
		return stringBuilder.toString()
	}
}
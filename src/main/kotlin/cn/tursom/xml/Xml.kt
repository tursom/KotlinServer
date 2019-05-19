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
		java.lang.Integer::class.java,
		java.lang.Long::class.java,
		java.lang.Float::class.java,
		java.lang.Double::class.java,
		java.lang.Boolean::class.java,
		Character::class.java,
		java.lang.String::class.java
	)
	
	private val Class<*>.defaultTarget
		get() = getAnnotation(DefaultTarget::class.java)?.target ?: ElementTarget.SubElement
	
	private val Class<*>.elementName: String
		get() = getAnnotation(ElementName::class.java)?.name ?: if (isArray) componentType.name else name
	
	private val Class<*>.textField
		get() = declaredFields.filter {
			it.target ?: defaultTarget == ElementTarget.ElementText
		}
	
	private val Class<*>.attributeField
		get() = declaredFields.filter {
			it.target ?: defaultTarget == ElementTarget.Attribute
		}
	
	private val Class<*>.subElementField
		get() = declaredFields.filter {
			!parseSet.contains(it.type) ||
				it.target ?: defaultTarget == ElementTarget.SubElement
		}
	
	private val Field.elementName: String
		get() = getAnnotation(FieldName::class.java)?.name ?: name
	
	private val Field.target: ElementTarget?
		get() = when {
			getAnnotation(Attribute::class.java) != null -> ElementTarget.Attribute
			getAnnotation(ElementText::class.java) != null -> ElementTarget.ElementText
			getAnnotation(SubElement::class.java) != null -> ElementTarget.SubElement
			else -> null
		}
	
	fun getData(element: Element, name: String, target: ElementTarget): String? = when (target) {
		ElementTarget.Attribute -> element.attribute(name)?.value
		ElementTarget.ElementText -> element.text
		ElementTarget.SubElement -> element.element(name)?.text
	}
	
	@Suppress("UNCHECKED_CAST")
	fun <T> parse(clazz: Class<T>, root: Element): T {
		val defaultTarget = clazz.defaultTarget
		val instance = unsafe.allocateInstance(clazz) as T
		
		clazz.declaredFields.forEach { field ->
			field.isAccessible = true
			
			val target = field.target ?: defaultTarget
			val fieldName = field.getAnnotation(FieldName::class.java)?.name ?: field.name
			
			val setter = field.getAnnotation(Setter::class.java)
			val value = if (setter != null) {
				val advanceSetMethod = try {
					clazz.getDeclaredMethod(setter.setter, Element::class.java)
				} catch (e: NoSuchMethodException) {
					null
				}
				if (advanceSetMethod != null) {
					advanceSetMethod.isAccessible = true
					advanceSetMethod.invoke(instance, root.element(fieldName) ?: return@forEach)
				} else {
					val setMethod = clazz.getDeclaredMethod(setter.setter, String::class.java)
					setMethod.isAccessible = true
					setMethod.invoke(instance, getData(root, fieldName, target) ?: return@forEach)
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
	
	inline fun <reified T : Any> parse(root: Element): T = parse(T::class.java, root)
	inline fun <reified T : Any> parse(document: Document): T = parse(T::class.java, document.rootElement)
	inline fun <reified T : Any> parse(document: String): T = parse(T::class.java, saxReader.read(StringReader(document)))
	inline fun <reified T : Any> parse(url: URL): T = parse(T::class.java, saxReader.read(url))
	inline fun <reified T : Any> parse(file: File): T = parse(T::class.java, saxReader.read(file))
	
	fun Field.toXml(
		obj: Any,
		value: Any,
		elementName: String,
		builder: StringBuilder,
		indentation: String,
		advanceIndentation: String
	): Boolean {
		val clazz = obj.javaClass
		getAnnotation(ToXml::class.java)?.let { getter ->
			val method = try {
				clazz.getDeclaredMethod(
					getter.getter,
					type,
					String::class.java,
					String::class.java,
					String::class.java
				)
			} catch (e: NoSuchMethodException) {
				null
			}
			if (method != null) {
				method.isAccessible = true
				builder.append("\n")
				builder.append(indentation)
				builder.append(method.invoke(obj, value, elementName, indentation, advanceIndentation) ?: return false)
				return true
			}
			val method2 = try {
				clazz.getDeclaredMethod(
					getter.getter,
					type,
					String::class.java,
					StringBuilder::class.java,
					String::class.java,
					String::class.java
				)
			} catch (e: NoSuchMethodException) {
				null
			}
			if (method2 != null) {
				method2.isAccessible = true
				builder.append("\n")
				builder.append(indentation)
				method2.invoke(obj, value, elementName, builder, indentation, advanceIndentation)
				return true
			}
			val method3 = try {
				clazz.getDeclaredMethod(
					getter.getter,
					type,
					String::class.java
				)
			} catch (e: NoSuchMethodException) {
				null
			}
			if (method3 != null) {
				method3.isAccessible = true
				builder.append("\n")
				builder.append(indentation)
				builder.append(method3.invoke(obj, value, elementName) ?: return false)
			}
			val method4 = try {
				clazz.getDeclaredMethod(
					getter.getter,
					type,
					String::class.java,
					StringBuilder::class.java
				)
			} catch (e: NoSuchMethodException) {
				null
			}
			if (method4 != null) {
				method4.isAccessible = true
				builder.append("\n")
				builder.append(indentation)
				method4.invoke(obj, value, elementName, builder)
			}
			return true
		}
		return false
	}
	
	
	fun arrayXml(
		obj: Any,
		rootName: String = obj.javaClass.elementName,
		indentation: String = "    ",
		fieldName: String? = "i"
	): String {
		val stringBuilder = StringBuilder()
		arrayXml(obj, rootName, stringBuilder, "", indentation, fieldName)
		return stringBuilder.toString()
	}
	
	fun arrayXml(
		obj: Any,
		elementName: String,
		builder: StringBuilder,
		indentation: String,
		advanceIndentation: String,
		fieldName: String? = "i"
	) {
		val clazz = obj.javaClass
		if (clazz.isArray) {
			val subIndentation = "$advanceIndentation$indentation"
			
			builder.append("$indentation<$elementName>")
			(0 until Array.getLength(obj)).forEach { i ->
				val value = Array.get(obj, i) ?: return@forEach
				val type = value.javaClass
				val subElementName = fieldName ?: type.elementName
				
				if (type.getAnnotation(CompressionXml::class.java) != null) {
					builder.append("\n")
					builder.append(subIndentation)
					arrayXmlCom(obj, subElementName, builder)
					return@forEach
				}
				
				if (parseSet.contains(value.javaClass))
					builder.append("\n$subIndentation<$subElementName>$value</$subElementName>")
				else {
					builder.append("\n")
					toXml(value, elementName, builder, subIndentation, advanceIndentation)
				}
			}
			builder.append("\n$indentation</$elementName>")
		}
	}
	
	
	fun iterableXml(
		obj: Iterable<*>,
		rootName: String,
		indentation: String,
		fieldName: String? = "i"
	): String {
		val stringBuilder = StringBuilder()
		iterableXml(obj, rootName, stringBuilder, "", indentation, fieldName)
		return stringBuilder.toString()
	}
	
	fun iterableXml(
		obj: Iterable<*>,
		elementName: String,
		builder: StringBuilder,
		indentation: String,
		advanceIndentation: String,
		fieldName: String? = "i"
	) {
		val subIndentation = "$advanceIndentation$indentation"
		builder.append("$indentation<$elementName>")
		obj.forEach {
			val type = (it ?: return@forEach).javaClass
			
			if (type.getAnnotation(CompressionXml::class.java) != null) {
				builder.append("\n")
				builder.append(subIndentation)
				arrayXmlCom(obj, elementName, builder)
				return@forEach
			}
			
			val subElementName = fieldName ?: type.elementName
			if (parseSet.contains(type))
				builder.append("\n$subIndentation<$subElementName>$it</$subElementName>")
			else {
				builder.append("\n")
				toXml(it, subElementName, builder, subIndentation, advanceIndentation)
			}
		}
		builder.append("\n$indentation</$elementName>")
	}
	
	fun mapXml(
		obj: Map<*, *>,
		rootName: String,
		indentation: String
	): String {
		val stringBuilder = StringBuilder()
		mapXml(obj, rootName, stringBuilder, "", indentation)
		return stringBuilder.toString()
	}
	
	fun mapXml(
		obj: Map<*, *>,
		elementName: String,
		builder: StringBuilder,
		indentation: String,
		advanceIndentation: String
	) {
		val subIndentation = "$advanceIndentation$indentation"
		builder.append("$indentation<$elementName>")
		obj.forEach { (k, v) ->
			val type = (v ?: return@forEach).javaClass
			
			if (type.getAnnotation(CompressionXml::class.java) != null) {
				builder.append("\n")
				builder.append(subIndentation)
				mapXmlCom(obj, elementName, builder)
				return@forEach
			}
			
			if (parseSet.contains(type))
				builder.append("\n$subIndentation<$k>$v</$k>")
			else {
				builder.append("\n")
				toXml(v, (k ?: return@forEach).toString(), builder, subIndentation, advanceIndentation)
			}
		}
		builder.append("\n$indentation</$elementName>")
	}
	
	fun normalXml(obj: Any, elementName: String, builder: StringBuilder, indentation: String, advanceIndentation: String) {
		val clazz = obj.javaClass
		val dataFieldList = clazz.textField
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
				builder.append(if (attributeField.isEmpty()) " />\n" else "\n$indentation/>")
				return
			}
			else -> builder.append(">")
		}
		
		subElement.forEach {
			it.isAccessible = true
			val value = it.get(obj) ?: return@forEach
			val eleName = it.elementName
			
			if (it.toXml(obj, value, eleName, builder, subIndentation, advanceIndentation))
				return@forEach
			
			if (it.getAnnotation(CompressionXml::class.java) != null) {
				builder.append("\n")
				builder.append(subIndentation)
				toXmlCom(value, eleName, builder)
				return@forEach
			}
			
			if (parseSet.contains(it.type)) {
				builder.append("\n$subIndentation<$eleName>$value</$eleName>")
				return@forEach
			}
			
			builder.append("\n")
			toXml(value, eleName, builder, subIndentation, advanceIndentation)
		}
		
		if (dataFieldList.isNotEmpty()) run {
			val dataField = dataFieldList[0]
			dataField.isAccessible = true
			val value = dataField.get(obj) ?: return@run
			builder.append(value)
			builder.append("</$elementName>\n")
		} else
			builder.append("\n$indentation</$elementName>")
	}
	
	fun toXml(obj: Any, elementName: String, builder: StringBuilder, indentation: String, advanceIndentation: String) {
		try {
			obj as Pair<*, *>
			builder.append("$indentation<${obj.first}>${obj.second}</${obj.first}>")
			return
		} catch (e: ClassCastException) {
		}
		
		val clazz = obj.javaClass
		
		if (clazz.getAnnotation(CompressionXml::class.java) != null) {
			toXmlCom(obj, elementName, builder)
			return
		}
		
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
		return stringBuilder.toString()
	}
	
	
	fun arrayXmlCom(obj: Any, elementName: String, builder: StringBuilder, fieldName: String? = "i") {
		val clazz = obj.javaClass
		if (clazz.isArray) {
			
			builder.append("<$elementName>")
			for (i in 0 until Array.getLength(obj)) {
				val value = Array.get(obj, i) ?: continue
				val type = value.javaClass
				val subElementName = fieldName ?: type.elementName
				if (parseSet.contains(type))
					builder.append("<$subElementName>$value</$subElementName>")
				else {
					toXmlCom(value, elementName, builder)
				}
			}
			builder.append("</$elementName>")
			
			return
		}
	}
	
	fun iterableXmlCom(obj: Iterable<*>, elementName: String, builder: StringBuilder, fieldName: String? = "i") {
		builder.append("<$elementName>\n")
		obj.forEach {
			val type = (it ?: return@forEach).javaClass
			val subElementName = fieldName ?: type.elementName
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
		try {
			obj as Pair<*, *>
			builder.append("<${obj.first}>${obj.second}</${obj.first}>")
			return
		} catch (e: ClassCastException) {
		}
		
		val clazz = obj.javaClass
		val dataFieldList = clazz.textField
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
			val eleName = it.elementName
			
			it.getAnnotation(ToXml::class.java)?.let { getter ->
				val method3 = try {
					clazz.getDeclaredMethod(
						getter.getter,
						it.type,
						String::class.java
					)
				} catch (e: NoSuchMethodException) {
					null
				}
				if (method3 != null) {
					method3.isAccessible = true
					builder.append(method3.invoke(obj, value, eleName) ?: return@let)
					return@forEach
				}
				
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
				method.isAccessible = true
				method.invoke(obj, value, eleName, builder)
				return@forEach
			}
			
			if (parseSet.contains(it.type)) {
				builder.append("<$eleName>$value</$eleName>")
				return@forEach
			}
			
			toXmlCom(value, eleName, builder)
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
package cn.tursom.database.async

import cn.tursom.database.SQLAdapter
import cn.tursom.database.annotation.NotNull
import cn.tursom.database.annotation.Setter
import cn.tursom.database.fieldName
import io.vertx.core.json.JsonArray
import io.vertx.ext.sql.ResultSet
import sun.misc.Unsafe
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.sql.SQLException
import java.util.*
import kotlin.collections.HashMap

@Suppress("NON_FINAL_MEMBER_IN_FINAL_CLASS")
class AsyncSqlAdapter<T>(
	@Suppress("MemberVisibilityCanBePrivate") val clazz: Class<T>,
	private val adapter: (ArrayList<T>.(
		resultSet: ResultSet,
		fieldList: List<SQLAdapter.FieldData>
	) -> Unit)? = null
) : ArrayList<T>() {
	val fieldNameMap: Map<String, Field> = run {
		val map = HashMap<String, Field>()
		clazz.declaredFields.forEach {
			map[it.fieldName] = it
		}
		map
	}
	
	open fun adapt(result: ResultSet) {
		val fieldList = ArrayList<FieldData>()
		result.columnNames.forEachIndexed { index, fieldName ->
			try {
				val field = fieldNameMap[fieldName] ?: error("")
				field.isAccessible = true
				val setterAnnotation = field.getAnnotation(Setter::class.java)
				val setter = try {
					clazz.getDeclaredMethod(setterAnnotation.setter, String::class.java)
				} catch (e: Exception) {
					null
				}
				val advanceSetter = try {
					clazz.getDeclaredMethod(setterAnnotation.setter, JsonArray::class.java, Int::class.java)
				} catch (e: Exception) {
					null
				}
				fieldList.add(
					FieldData(
						index,
						field,
						field.type,
						advanceSetter,
						setter
					)
				)
			} catch (e: SQLException) {
				// ignored
			} catch (e: IllegalStateException) {
				// ignored
			}
		}
		result.results.forEach {
			adaptOnce(it, fieldList)
		}
	}
	
	@Suppress("UNCHECKED_CAST")
	open fun adaptOnce(result: JsonArray, fieldList: List<FieldData>) {
		//绕过构造函数获取变量0
		val bean = unsafe.allocateInstance(clazz) as T
		fieldList.forEach { (
			                    index,
			                    field,
			                    beanType,
			                    advanceSetter,
			                    setter
		                    ) ->
			try {
				if (advanceSetter != null) {
					//如果你有能力直接从ResultSet里面取出数据,那就随君便
					val value = try {
						advanceSetter.invoke(bean, result, index)!!
					} catch (e: InvocationTargetException) {
						throw e.targetException
					}
					field.set(bean, value)
				} else {
					result.getValue(index)?.let { value ->
						//让我们把数据喂进去
						field.set(bean, handleCast(bean, field, beanType, value, setter))
					}
				}
			} catch (e: SQLException) {
				e.printStackTrace()
				return@forEach
			}
		}
		add(bean)
	}
	
	private fun handleCast(
		bean: T,
		field: Field,
		beanType: Class<*>,
		value: Any,
		setter: Method?
	): Any? {
		val dbType = value.javaClass // 这里是获取数据库字段的类型
		return if (setter != null) {
			try {
				setter.invoke(bean, value.toString())
			} catch (e: InvocationTargetException) {
				throw e.targetException
			}
		} else if (beanType == java.lang.Float::class.java) {
			if (dbType == java.lang.Double::class.java) {
				(value as Double).toFloat()
			} else {
				//检查是否可以为空
				if (field.getAnnotation(NotNull::class.java) != null) {
					value.toString().toFloat()
				} else {
					value.toString().toFloatOrNull()
				}
			}
		} else if (beanType == java.lang.String::class.java && dbType != java.lang.String::class.java) {
			value.toString()
		} else if (beanType == java.lang.Boolean::class.java) {
			if (field.getAnnotation(NotNull::class.java) != null) {
				value.toString().toBoolean()
			} else {
				try {
					value.toString().toBoolean()
				} catch (e: Exception) {
					null
				}
			}
		} else {
			value
		}
	}
	
	data class FieldData(
		val index: Int,
		val field: Field,
		val beanType: Class<*>,
		val advanceSetter: Method?,
		val setter: Method?
	)
	
	companion object {
		//利用Unsafe绕过构造函数获取变量
		private val unsafe by lazy {
			val field = Unsafe::class.java.getDeclaredField("theUnsafe")
			//允许通过反射设置属性的值
			field.isAccessible = true
			field.get(null) as Unsafe
		}
	}
}
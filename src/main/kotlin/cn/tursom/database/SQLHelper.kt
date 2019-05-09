package cn.tursom.database

import cn.tursom.database.annotation.*
import cn.tursom.database.clauses.Clause
import cn.tursom.database.clauses.ClauseMaker
import java.io.Closeable
import java.lang.reflect.Field
import java.util.AbstractCollection
import kotlin.collections.forEach
import kotlin.reflect.KClass

/**
 * MySQLHelper，SQLite辅助使用类
 * 实现创建表格、查询、插入和更新功能
 */

interface SQLHelper : Closeable {
	/**
	 * 创建表格
	 * @param table: 表格名
	 * @param keys: 属性列表
	 */
	fun createTable(table: String, keys: Iterable<String>)
	
	/**
	 * 根据提供的class对象自动化创建表格
	 * 但是有诸多缺陷，所以不是很建议使用
	 */
	fun createTable(fields: Class<*>)
	
	/**
	 * 删除表格
	 */
	fun deleteTable(table: String)
	
	/**
	 * 删除表格
	 */
	fun dropTable(table: String)
	
	/**
	 * 查询
	 * @param adapter 用于保存查询结果的数据类，由SQLAdapter继承而来
	 * @param fields 查询字段
	 * @param where 指定从一个表或多个表中获取数据的条件,Pair左边为字段名，右边为限定的值
	 * @param maxCount 最大查询数量
	 */
	fun <T : Any> select(
		adapter: SQLAdapter<T>,
		fields: Iterable<String>? = null,
		where: Clause,
		order: Field? = null,
		reverse: Boolean = false,
		maxCount: Int? = null
	): SQLAdapter<T>
	
	/**
	 * 用于支持灵活查询
	 */
	fun <T : Any> select(
		adapter: SQLAdapter<T>,
		fields: String = "*",
		where: String? = null,
		order: String? = null,
		reverse: Boolean = false,
		maxCount: Int? = null
	): SQLAdapter<T>
	
	/**
	 * 插入
	 * @param value 值
	 */
	fun insert(value: Any): Int
	
	fun insert(valueList: Iterable<*>): Int
	
	fun insert(table: String, fields: String, values: String): Int
	
	fun update(table: String, set: String, where: String = ""): Int
	
	fun update(value: Any, where: Clause): Int
	
	fun delete(table: String, where: String? = null): Int
	
	fun delete(table: String, where: Clause?): Int
	
	fun commit()
}

val Any.tableName: String
	get() = javaClass.tableName

val Class<*>.tableName: String
	get() = (getAnnotation<TableName>()?.name ?: name.split('.').last()).toLowerCase()

val KClass<*>.tableName: String
	get() = java.tableName

/**
 * 用于支持灵活查询
 */
inline fun <reified T : Any> SQLHelper.select(
	fields: String = "*",
	where: String? = null,
	order: String? = null,
	reverse: Boolean = false,
	maxCount: Int? = null
): SQLAdapter<T> = select(SQLAdapter(T::class.java), fields, where, order, reverse, maxCount)

inline fun <reified T : Any> SQLHelper.select(
	fields: Iterable<String> = listOf("*"),
	where: Clause,
	order: Field? = null,
	reverse: Boolean = false,
	maxCount: Int? = null
): SQLAdapter<T> = select(SQLAdapter(T::class.java), fields, where, order, reverse, maxCount)

fun <T : Any> SQLHelper.select(
	adapter: SQLAdapter<T>,
	maker: SqlSelector<T>.() -> Unit
): SQLAdapter<T> {
	val selector = SqlSelector(this, adapter)
	selector.maker()
	return selector.select()
}

inline infix fun <reified T : Any> SQLHelper.select(
	noinline maker: SqlSelector<T>.() -> Unit
): SQLAdapter<T> = select(SQLAdapter(T::class.java), maker)

fun <T : Any> SQLHelper.select(
	clazz: Class<T>,
	fields: Iterable<String> = listOf("*"),
	where: Clause,
	order: Field? = null,
	reverse: Boolean = false,
	maxCount: Int? = null
): SQLAdapter<T> = select(SQLAdapter(clazz), fields, where, order, reverse, maxCount)

/**
 * 用于支持灵活查询
 */
fun <T : Any> SQLHelper.select(
	clazz: Class<T>,
	fields: String = "*",
	where: String? = null,
	order: String? = null,
	reverse: Boolean = false,
	maxCount: Int? = null
): SQLAdapter<T> = select(SQLAdapter(clazz), fields, where, order, reverse, maxCount)

fun SQLHelper.delete(
	clazz: Class<*>,
	where: String? = null
) = delete(clazz.tableName, where)

fun SQLHelper.delete(
	table: String,
	where: ClauseMaker.() -> Clause
) = delete(table, ClauseMaker.where())


infix fun SQLHelper.delete(helper: SqlDeleter.() -> Unit): Int {
	val deleter = SqlDeleter(this)
	deleter.helper()
	return deleter.delete()
}

infix fun SQLHelper.update(updater: SqlUpdater.() -> Unit): Int {
	val sqlUpdater = SqlUpdater(this)
	sqlUpdater.updater()
	return sqlUpdater.update()
}

inline fun <reified T : Annotation> Field.getAnnotation(): T? = getAnnotation(T::class.java)
inline fun <reified T : Annotation> Class<*>.getAnnotation(): T? = getAnnotation(T::class.java)

fun Array<out Field>.fieldStr(): String {
	val fields = StringBuilder()
	forEach field@{ field ->
		field.isAccessible = true
		fields.append("${field.fieldName},")
	}
	fields.deleteCharAt(fields.length - 1)
	return fields.toString()
}

fun Iterable<String>.fieldStr(): String {
	val stringBuffer = StringBuffer()
	forEach {
		if (it.isNotEmpty())
			stringBuffer.append("$it,")
	}
	stringBuffer.delete(stringBuffer.length - 1, stringBuffer.length)
	return stringBuffer.toString()
}

fun Iterable<String>.fieldNameStr(): String? {
	val stringBuffer = StringBuffer()
	forEach {
		if (it.isNotEmpty())
			stringBuffer.append("`$it`,")
	}
	return if (stringBuffer.isNotEmpty()) {
		stringBuffer.delete(stringBuffer.length - 1, stringBuffer.length)
		stringBuffer.toString()
	} else {
		null
	}
}

fun Class<*>.valueStr(value: Any): String? {
	val values = StringBuilder()
	declaredFields.forEach field@{ field ->
		field.isAccessible = true
		values.append(field.getAnnotation(Getter::class.java)?.let {
			getDeclaredMethod(field.name).invoke(null) as String
		} ?: field.get(value)?.fieldValue)
		values.append(',')
	}
	if (values.isNotEmpty()) {
		values.deleteCharAt(values.length - 1)
	} else {
		return null
	}
	return values.toString()
}

fun Array<out Field>.valueStr(value: Any): String? {
	val values = StringBuilder()
	forEach field@{ field ->
		field.isAccessible = true
		values.append(field.get(value)?.fieldValue)
		values.append(',')
	}
	if (values.isNotEmpty()) {
		values.deleteCharAt(values.length - 1)
	} else {
		return null
	}
	return values.toString()
}

fun Iterable<*>.valueStr(sqlFieldMap: Array<out Field>): String? {
	val values = StringBuilder()
	forEach { value ->
		value ?: return@forEach
		values.append("(${sqlFieldMap.valueStr(value) ?: return@forEach}),")
	}
	if (values.isNotEmpty()) {
		values.deleteCharAt(values.length - 1)
	} else {
		return null
	}
	return values.toString()
}


fun List<Pair<String, String>>.fieldStr(): Pair<String, String> {
	val first = StringBuilder()
	val second = StringBuilder()
	forEach { (f, s) ->
		first.append("$f,")
		second.append("$s,")
	}
	if (first.isNotEmpty()) first.deleteCharAt(first.length - 1)
	if (second.isNotEmpty()) second.deleteCharAt(second.length - 1)
	return first.toString() to second.toString()
}


fun StringBuilder.appendField(
	field: Field,
	fieldType: Field.() -> String?,
	foreignKeyList: AbstractCollection<Pair<String, String>>,
	autoIncrement: String = "AUTO_INCREMENT",
	primaryKey: Field.() -> Unit
) {
	val fieldName = field.fieldName
	append("`$fieldName` ${field.fieldType() ?: return}")
	field.annotations.forEach annotations@{ annotation ->
		append(" ${when (annotation) {
			is NotNull -> "NOT NULL"
			is AutoIncrement -> autoIncrement
			is Unique -> "UNIQUE"
			is Default -> "DEFAULT ${annotation.default}"
			is Check -> "CHECK(${field.fieldName}${annotation.func})"
			is ExtraAttribute -> annotation.attributes
			is ForeignKey -> {
				foreignKeyList.add(fieldName to if (annotation.target.isNotEmpty()) annotation.target else fieldName)
				return@annotations
			}
			is PrimaryKey -> {
				field.primaryKey()
				return@annotations
			}
			else -> return@annotations
		}}")
	}
	append(',')
}

val String.sqlStr
	get() = "'${replace("'", "''")}'"
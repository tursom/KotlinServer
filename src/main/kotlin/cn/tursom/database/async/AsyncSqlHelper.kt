package cn.tursom.database.async

import cn.tursom.database.*
import cn.tursom.database.clauses.Clause
import cn.tursom.database.clauses.ClauseMaker
import io.vertx.ext.sql.SQLConnection
import kotlinx.coroutines.selects.select
import java.lang.reflect.Field

interface AsyncSqlHelper {
	val connection: SQLConnection
	
	suspend fun doSql(sql: String): Int
	
	suspend fun createTable(table: String, keys: Iterable<String>)
	
	suspend fun createTable(fields: Class<*>)
	
	suspend fun deleteTable(table: String)
	
	suspend fun dropTable(table: String)
	
	suspend fun <T : Any> select(
		adapter: AsyncSqlAdapter<T>,
		fields: Iterable<String>? = null,
		where: Clause,
		order: Field? = null,
		reverse: Boolean = false,
		maxCount: Int? = null
	): AsyncSqlAdapter<T>
	
	suspend fun <T : Any> select(
		adapter: AsyncSqlAdapter<T>,
		fields: String = "*",
		where: String? = null,
		order: String? = null,
		reverse: Boolean = false,
		maxCount: Int? = null
	): AsyncSqlAdapter<T>
	
	suspend fun insert(value: Any): Int
	
	suspend fun insert(valueList: Iterable<*>): Int
	
	suspend fun insert(table: String, fields: String, values: String): Int
	
	suspend fun update(table: String, set: String, where: String = ""): Int
	
	suspend fun update(value: Any, where: Clause): Int
	
	suspend fun delete(table: String, where: String? = null): Int
	
	suspend fun delete(table: String, where: Clause?): Int
	
	suspend fun close()
}

suspend inline fun <reified T : Any> AsyncSqlHelper.select(
	fields: String = "*",
	where: String? = null,
	order: String? = null,
	reverse: Boolean = false,
	maxCount: Int? = null
): AsyncSqlAdapter<T> = select(AsyncSqlAdapter(T::class.java), fields, where, order, reverse, maxCount)

suspend inline fun <reified T : Any> AsyncSqlHelper.select(
	fields: Iterable<String> = listOf("*"),
	where: Clause,
	order: Field? = null,
	reverse: Boolean = false,
	maxCount: Int? = null
): AsyncSqlAdapter<T> = select(AsyncSqlAdapter(T::class.java), fields, where, order, reverse, maxCount)

suspend fun <T : Any> AsyncSqlHelper.select(
	adapter: AsyncSqlAdapter<T>,
	maker: AsyncSqlSelector<T>.() -> Unit
): AsyncSqlAdapter<T> {
	val selector = AsyncSqlSelector(this, adapter)
	selector.maker()
	return selector.select()
}

suspend inline infix fun <reified T : Any> AsyncSqlHelper.select(
	noinline maker: AsyncSqlSelector<T>.() -> Unit
): AsyncSqlAdapter<T> = select(AsyncSqlAdapter(T::class.java), maker)

suspend fun <T : Any> AsyncSqlHelper.select(
	clazz: Class<T>,
	fields: Iterable<String> = listOf("*"),
	where: Clause,
	order: Field? = null,
	reverse: Boolean = false,
	maxCount: Int? = null
): AsyncSqlAdapter<T> = select(AsyncSqlAdapter(clazz), fields, where, order, reverse, maxCount)

/**
 * 用于支持灵活查询
 */
suspend fun <T : Any> AsyncSqlHelper.select(
	clazz: Class<T>,
	fields: String = "*",
	where: String? = null,
	order: String? = null,
	reverse: Boolean = false,
	maxCount: Int? = null
): AsyncSqlAdapter<T> = select(AsyncSqlAdapter(clazz), fields, where, order, reverse, maxCount)

suspend fun AsyncSqlHelper.delete(
	clazz: Class<*>,
	where: String? = null
) = delete(clazz.tableName, where)

suspend fun AsyncSqlHelper.delete(
	table: String,
	where: ClauseMaker.() -> Clause
) = delete(table, ClauseMaker.where())

suspend infix fun AsyncSqlHelper.delete(helper: AsyncSqlDeleter.() -> Unit): Int {
	val deleter = AsyncSqlDeleter(this)
	deleter.helper()
	return deleter.delete()
}

suspend infix fun AsyncSqlHelper.update(updater: AsyncSqlUpdater.() -> Unit): Int {
	val sqlUpdater = AsyncSqlUpdater(this)
	sqlUpdater.updater()
	return sqlUpdater.update()
}

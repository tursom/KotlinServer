package cn.tursom.utils.asynccache

import cn.tursom.database.annotation.NotNull
import cn.tursom.database.annotation.PrimaryKey
import cn.tursom.database.async.AsyncSqlAdapter
import cn.tursom.database.async.AsyncSqlHelper
import cn.tursom.database.clauses.clause
import cn.tursom.utils.asynccache.interfaces.AsyncPotableCacheMap
import cn.tursom.utils.background

@Suppress("CanBeParameter", "MemberVisibilityCanBePrivate", "SpellCheckingInspection")
class AsyncSqlStringCacheMap(
	val db: AsyncSqlHelper,
	val timeout: Long,
	val prevCacheMap: AsyncPotableCacheMap<String, String> = DefaultAsyncPotableCacheMap(timeout)
) : AsyncPotableCacheMap<String, String> by prevCacheMap {

	override suspend fun get(key: String): String? {
		val memCache = prevCacheMap.get(key)
		return if (memCache != null) memCache
		else {
			val storage = db.select(AsyncSqlAdapter(StorageData::class.java), maxCount = 1)
			if (storage.isNotEmpty()) {
				val value = storage[0].value
				set(key, value)
				value
			} else null
		}
	}

	override suspend fun get(key: String, constructor: suspend () -> String): String {
		return prevCacheMap.get(key) {
			val newValue = constructor()
			background { updateStorage(key, newValue) }
			newValue
		}
	}

	override suspend fun set(key: String, value: String): String? {
		prevCacheMap.set(key, value)
		updateStorage(key, value)
		return value
	}

	suspend fun updateStorage(key: String, value: String) {
		if (db.select(AsyncSqlAdapter(StorageData::class.java), maxCount = 1).isNotEmpty()) {
			db.insert(StorageData(key, value))
		} else {
			db.update(StorageData(key, value), where = clause { !StorageData::key equal key })
		}
	}

	data class StorageData(
		@PrimaryKey
		@NotNull
		val key: String,
		@NotNull
		val value: String,
		val cacheTime: Long = System.currentTimeMillis()
	)
}

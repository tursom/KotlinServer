package cn.tursom.utils.cache.interfaces

import cn.tursom.utils.datastruct.async.interfaces.AsyncPotableMap

interface AsyncPotableCacheMap<K, V> : AsyncCacheMap<K, V>, AsyncPotableMap<K, V>
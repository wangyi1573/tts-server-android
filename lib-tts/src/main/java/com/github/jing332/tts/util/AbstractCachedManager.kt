package com.github.jing332.tts.util

import cn.hutool.cache.impl.TimedCache
import java.util.concurrent.ConcurrentHashMap

abstract class AbstractCachedManager<K, V>(timeout: Long, delay: Long) {
    protected val cache = TimedCache<K, V>(timeout, ConcurrentHashMap())

    init {
        cache.schedulePrune(delay)
        cache.setListener { k, v ->
            onCacheRemove(k, v)
            // 移除了潜在的并发修改操作
        }
    }

    /**
     * @return true: cache extend
     */
    open fun onCacheRemove(key: K, value: V): Boolean = false
}
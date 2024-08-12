package com.github.jing332.script_engine.core.type

abstract class JClass {
    var onThrowable: ((t: Throwable) -> Unit)? = null

    fun tryBlock(block: () -> Unit) {
        kotlin.runCatching {
            block.invoke()
        }.onFailure {
            it.printStackTrace()
            onThrowable?.invoke(it)
        }
    }
}
package com.github.jing332.script_engine.core.type.ws.internal

import okhttp3.Response

data class WebSocketException(val response: Response? = null) : Exception()
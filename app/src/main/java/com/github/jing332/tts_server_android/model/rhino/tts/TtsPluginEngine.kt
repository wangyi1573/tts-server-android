package com.github.jing332.tts_server_android.model.rhino.tts

import android.content.Context
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.constant.AppConst
import com.github.jing332.tts_server_android.data.entities.plugin.Plugin
import com.github.jing332.tts_server_android.model.speech.tts.PluginTTS
import com.script.javascript.RhinoScriptEngine
import org.mozilla.javascript.NativeObject
import java.io.ByteArrayInputStream
import java.io.InputStream

open class TtsPluginEngine(
    val pluginTTS: PluginTTS,
    private val context: Context,
    override val rhino: RhinoScriptEngine = RhinoScriptEngine(),
    override val logger: com.github.jing332.script_engine.core.Logger = com.github.jing332.script_engine.core.Logger(),
) : com.github.jing332.script_engine.core.BaseScriptEngine(
    rhino = rhino, logger = logger,
    code = pluginTTS.requirePlugin.code,
    ttsrvObject = EngineContext(
        pluginTTS,
        pluginTTS.plugin!!.userVars,
        context,
        pluginTTS.requirePlugin.pluginId
    )
) {
    private var mPlugin: Plugin
        inline get() = pluginTTS.plugin!!
        inline set(value) {
            pluginTTS.plugin = value
        }

    companion object {
        const val OBJ_PLUGIN_JS = "PluginJS"

        const val FUNC_GET_AUDIO = "getAudio"
        const val FUNC_ON_LOAD = "onLoad"
        const val FUNC_ON_STOP = "onStop"
    }

    // 已弃用, 占位
    @Suppress("unused")
    var extraData: String = ""

    private val pluginJsObject: NativeObject
        get() = findObject(OBJ_PLUGIN_JS)

    @Suppress("UNCHECKED_CAST")
    @Synchronized
    fun evalPluginInfo(): Plugin {
        logger.d("evalPluginInfo()...")
        eval()

        pluginJsObject.apply {
            mPlugin.name = get("name").toString()
            mPlugin.pluginId = get("id").toString()
            mPlugin.author = get("author").toString()

            try {
                mPlugin.defVars = get("vars") as Map<String, Map<String, String>>
            } catch (_: NullPointerException) {
                mPlugin.defVars = emptyMap()
            } catch (t: Throwable) {
                mPlugin.defVars = emptyMap()

                throw ClassCastException("\"vars\" bad format").initCause(t)
            }

            runCatching {
                mPlugin.version = (get("version") as Double).toInt()
            }.onFailure {
                throw NumberFormatException(context.getString(R.string.plugin_bad_format))
            }
        }

        return mPlugin
    }

    @Synchronized
    fun onLoad(): Any? {
        logger.d("onLoad()...")
        eval()
        try {
            return rhino.invokeMethod(pluginJsObject, FUNC_ON_LOAD)
        } catch (_: NoSuchMethodException) {
        }
        return null
    }

    @Synchronized
    fun onStop(): Any? {
        logger.d("onStop()...")
        ttsrvObject.cancelNetwork()
        try {
            return rhino.invokeMethod(pluginJsObject, FUNC_ON_STOP)
        } catch (_: NoSuchMethodException) {
        }
        return null
    }

    @Synchronized
    fun getAudio(
        text: String, rate: Int = 1, pitch: Int = 1
    ): InputStream? {
        logger.d("getAudio()... $pluginTTS")

        return rhino.invokeMethod(
            pluginJsObject,
            FUNC_GET_AUDIO,
            text,
            pluginTTS.locale,
            pluginTTS.voice,
            rate,
            pluginTTS.volume,
            pitch
        )?.run {
            when (this) {
                is ArrayList<*> -> {
                    ByteArrayInputStream(this.map { (it as Double).toInt().toByte() }.toByteArray())
                }

                is InputStream -> this
                else -> ByteArrayInputStream(this as ByteArray)
            }
        }
    }
}
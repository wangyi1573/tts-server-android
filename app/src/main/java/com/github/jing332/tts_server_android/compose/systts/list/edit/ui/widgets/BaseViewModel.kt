package com.github.jing332.tts_server_android.compose.systts.list.edit.ui.widgets

import androidx.lifecycle.ViewModel
import com.github.jing332.tts_server_android.app
import com.github.jing332.common.audio.AudioPlayer

open class BaseViewModel : ViewModel() {
    val audioPlayer by lazy { com.github.jing332.common.audio.AudioPlayer(app) }

    override fun onCleared() {
        super.onCleared()

        audioPlayer.release()
    }
}
package com.github.jing332.script_engine.core.ext

import android.view.View
import android.view.ViewGroup
import com.github.jing332.common.utils.dp
import com.github.jing332.common.utils.longToast
import com.github.jing332.common.utils.toast
import splitties.init.appCtx


interface JsUserInterface {
    fun toast(msg: String) = appCtx.toast(msg)
    fun longToast(msg: String) = appCtx.longToast(msg)

    fun setMargins(v: View, left: Int, top: Int, right: Int, bottom: Int) {
        (v.layoutParams as ViewGroup.MarginLayoutParams).setMargins(
            left.dp,
            top.dp,
            right.dp,
            bottom.dp
        )
    }
//
}
package com.gridsync.mobile.ui.util

import android.content.Context
import android.widget.Toast

/** Short, non-blocking feedback for the outcome of an action (save, approve, failure, …). */
fun Context.toast(message: String, long: Boolean = false) {
    if (message.isBlank()) return
    Toast.makeText(
        applicationContext,
        message,
        if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT,
    ).show()
}

package com.example.geminispotifyapp.core.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.provider.Settings
import android.widget.Toast

fun <T1 : Any, T2 : Any, T3 : Any, R : Any> safeLet(p1: T1?, p2: T2?, p3: T3?, block: (T1, T2, T3) -> R?): R? =
    if (p1 != null && p2 != null && p3 != null) block(p1, p2, p3) else null

fun toast(context: Context?, message: String?, duration: Int = Toast.LENGTH_SHORT) {
    safeLet(context, message, duration) { safeContext, safeMessage, safeDuration ->
        // Fix: Use safeContext directly, Toast doesn't always need Activity
        Toast.makeText(safeContext, safeMessage, safeDuration).show()
    }
}

fun Context.openAppNotificationSettings() {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    }
    startActivity(intent)
}

fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
package com.example.geminispotifyapp.core.utils

import android.app.Activity
import android.content.Context
import android.widget.Toast

fun <T1 : Any, T2 : Any, T3 : Any, R : Any> safeLet(p1: T1?, p2: T2?, p3: T3?, block: (T1, T2, T3) -> R?): R? =
    if (p1 != null && p2 != null && p3 != null) block(p1, p2, p3) else null
// TODO: Check all uses and use activity context to show toast.
// For safety and potential memory leak, try to use application context (as below).
fun toast(context: Context?, message: String?, duration: Int = Toast.LENGTH_SHORT) {
    safeLet(context, message, duration) { safeContext, safeMessage, safeDuration ->
        (safeContext as? Activity)?.runOnUiThread {
            Toast.makeText(safeContext, safeMessage, safeDuration).show()
        }
    }
}

//fun toast(context: Context?, message: String?, duration: Int = Toast.LENGTH_SHORT) {
//    safeLet(context, message, duration) { safeContext, safeMessage, safeDuration ->
//         //Ensure that Toast is shown on the main thread
//        if (Looper.myLooper() == Looper.getMainLooper()) {
//            // If already on the main thread, show directly
//            Toast.makeText(safeContext.applicationContext, safeMessage, safeDuration).show()
//        } else {
//            // If on a background thread, post to the main thread
//            Handler(Looper.getMainLooper()).post {
//                Toast.makeText(safeContext.applicationContext, safeMessage, safeDuration).show()
//            }
//        }
//    }
//}
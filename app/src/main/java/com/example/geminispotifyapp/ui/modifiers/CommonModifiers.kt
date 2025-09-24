package com.example.geminispotifyapp.ui.modifiers

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

/**
 *  Set a modifier for onTap to hide keyboard and clear focus.
 */
fun Modifier.autoCloseKeyboardClearFocus(): Modifier = composed {
    val keyBoardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    pointerInput(this) {
        detectTapGestures(onTap = {
            keyBoardController?.hide()
            focusManager.clearFocus()
        })
    }
}
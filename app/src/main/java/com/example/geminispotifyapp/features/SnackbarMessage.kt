package com.example.geminispotifyapp.features

import androidx.annotation.StringRes
import androidx.compose.material3.SnackbarDuration

sealed class SnackbarMessage {
    data class TextMessage(val message: String, val duration: SnackbarDuration = SnackbarDuration.Short) : SnackbarMessage()
    data class ResourceMessage(@StringRes val resourceId: Int, val duration: SnackbarDuration = SnackbarDuration.Short) : SnackbarMessage()
    data class ExceptionMessage(val exception: Exception, val duration: SnackbarDuration = SnackbarDuration.Short) : SnackbarMessage()
    data class ActionMessage(val message: String, val duration: SnackbarDuration = SnackbarDuration.Short, val actionLabel: String, val onAction: () -> Unit) : SnackbarMessage()
}
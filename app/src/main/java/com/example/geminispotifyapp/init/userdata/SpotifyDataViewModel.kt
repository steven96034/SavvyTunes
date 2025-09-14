package com.example.geminispotifyapp.init.userdata

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geminispotifyapp.ApiError
import com.example.geminispotifyapp.DownLoadState
import com.example.geminispotifyapp.SpotifyRepository
import com.example.geminispotifyapp.auth.AuthManager
import com.example.geminispotifyapp.features.UiEvent
import com.example.geminispotifyapp.features.userdatadetail.FetchResult
import com.example.geminispotifyapp.utils.GlobalErrorHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SpotifyDataViewModel @Inject constructor(
    private val spotifyRepository: SpotifyRepository, // 使用介面
    private val authManager: AuthManager,
    private val globalErrorHandler: GlobalErrorHandler // 注入 GlobalErrorHandler
) : ViewModel() {

    private val _downLoadState: MutableStateFlow<DownLoadState> = MutableStateFlow(DownLoadState.Initial)
    val downLoadState: StateFlow<DownLoadState> = _downLoadState.asStateFlow()

    private val tag = "SpotifyDataViewModel"

    fun startAuthentication() {
        viewModelScope.launch {
            authManager.startAuthentication()
        }
    }

    fun fetchData() {
        if (_downLoadState.value is DownLoadState.Loading || _downLoadState.value is DownLoadState.Success) {
            Log.d(tag, "Fetch data called but current state is Loading or Success. Skipping.")
            return
        }

        _downLoadState.value = DownLoadState.Loading
        Log.d(tag, "Fetching user profile...")

        viewModelScope.launch {
            val result = spotifyRepository.getUserProfile()

            when (result) {
                is FetchResult.Success -> {
                    _downLoadState.value = DownLoadState.Success(result.data)
                    Log.i(tag, "User profile fetched successfully.")
                }
                is FetchResult.Error -> {
                    Log.e(tag, "Error fetching user profile from repository: ${result.errorData.javaClass.simpleName} - ${result.errorData.message}")
                    processApiErrorWithGlobalHandler(result.errorData)
                }
                FetchResult.Initial -> {
                    Log.w(tag, "Received FetchResult.Initial from repository, treating as an error.")
                    _downLoadState.value = DownLoadState.Error("無法初始化資料載入。")
                }
                FetchResult.Loading -> {
                    Log.d(tag, "Received FetchResult.Loading from repository while already in Loading state.")
                    // 保持目前的 Loading 狀態
                }
            }
        }
    }

    private suspend fun processApiErrorWithGlobalHandler(apiError: ApiError) {
        val uiEvent = globalErrorHandler.processError(apiError, tag)
        Log.d(tag, "GlobalErrorHandler processed error: ${apiError.javaClass.simpleName}, result UiEvent: ${uiEvent.javaClass.simpleName}")

        when (uiEvent) {
            is UiEvent.Unauthorized -> {
                Log.w(tag, "GlobalErrorHandler indicated Unauthorized: ${uiEvent.message}. Performing logout and setting ReAuthenticationRequired state.")
                spotifyRepository.performLogOutAndCleanUp()
                // 這個特定的錯誤訊息會被 SpotifyData.kt 用來判斷是否需要重新認證
                _downLoadState.value = DownLoadState.Error("ReAuthenticationRequired")
            }
            is UiEvent.ShowSnackbar -> {
                Log.e(tag, "GlobalErrorHandler indicated ShowSnackbar: ${uiEvent.message}")
                _downLoadState.value = DownLoadState.Error(uiEvent.message)
            }
            is UiEvent.ShowSnackbarDetail -> {
                val errorMessage = "${uiEvent.message}${if (uiEvent.detail != null) " (${uiEvent.detail})" else ""}"
                Log.e(tag, "GlobalErrorHandler indicated ShowSnackbarDetail: $errorMessage")
                _downLoadState.value = DownLoadState.Error(errorMessage)
            }
            is UiEvent.Navigate -> {
                // 對於 SpotifyDataViewModel，初始載入失敗時若收到 Navigate 事件，
                // 尤其是非 Unauthorized 相關的 Navigate，可能表示一個更普遍的錯誤。
                // 我們主要依賴 Unauthorized 事件來觸發 ReAuthenticationRequired。
                // 其他 Navigate 事件在此 ViewModel 中也應視為一種錯誤狀態，因為它中斷了正常的初始資料載入流程。
                Log.w(tag, "GlobalErrorHandler indicated Navigate to ${uiEvent.route}. Setting generic error for DownLoadState as initial data load failed.")
                // 使用 apiError 的原始訊息可能更合適，因為 Navigate 事件的 message 可能不適用於 DownLoadState
                val originalErrorMessage = apiError.message ?: "初始資料載入失敗，需要導航至 ${uiEvent.route}"
                _downLoadState.value = DownLoadState.Error(originalErrorMessage)
            }
            // 若 GlobalErrorHandler 未來有其他 UiEvent 類型，可以在此處擴展
            // else -> {
            //     Log.e(tag, "GlobalErrorHandler returned unhandled UiEvent type: ${uiEvent.javaClass.simpleName}. Defaulting to generic error from ApiError.")
            //     _downLoadState.value = DownLoadState.Error(apiError.message ?: "發生未處理的 UI 事件類型錯誤。")
            // }
        }
    }
}

package com.example.geminispotifyapp.features.userdatadetail

import android.util.Log
import com.example.geminispotifyapp.ErrorData
import com.example.geminispotifyapp.features.SnackbarMessage
import com.example.geminispotifyapp.features.UiEventManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.supervisorScope
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class ApiExecutionHelper @Inject constructor(private val uiEventManager: UiEventManager) {

    /**
     * Executes a list of asynchronous API operations and transforms their results.
     *
     * This function is designed to handle multiple API calls concurrently. It uses a `supervisorScope`
     * to ensure that if one operation fails, the others can still complete. However, if any
     * operation throws an exception, the entire function will result in an error state.
     *
     * @param ResultType The type of data expected from each individual API operation.
     * @param SuccessDataType The type of data after successfully transforming the results of all operations.
     * @param operations A suspend lambda function that returns a list of `Deferred<ResultType>`.
     *                   Each `Deferred` represents an asynchronous API operation.
     *                   This lambda is executed within a `CoroutineScope`.
     * @param transformSuccess A function that takes a list of `ResultType` (the results from all
     *                         successful operations) and transforms it into the final `SuccessDataType`.
     *                         This is only called if all operations complete successfully.
     * @return A [FetchResult] which can be either:
     *         - [FetchResult.Success] containing the `SuccessDataType` if all operations succeed.
     *         - [FetchResult.Error] containing an [ErrorData] object if any operation fails
     *           due to an `HttpException`, `IOException`, or any other `Exception`.
     *           In case of an error, a Snackbar message will also be displayed via `uiEventManager`.
     *
     * @throws Exception If an individual operation within the `operations` block throws an exception
     *                   that is not caught internally, it will be rethrown and caught by the
     *                   outer try-catch block, resulting in a [FetchResult.Error].
     */
    suspend fun <ResultType, SuccessDataType> executeApiOperations(
        operations: suspend CoroutineScope.() -> List<Deferred<ResultType>>,
        transformSuccess: (List<ResultType>) -> SuccessDataType// Transform List<ResultType> into final SuccessDataType
    ): FetchResult<SuccessDataType> { // Return result to caller to update StateFlow

        return try {
            val results = supervisorScope {
                val deferredTasks = operations()
                deferredTasks.map {
                    try {
                        it.await() // 等待每個異步操作
                    } catch (e: Exception) {
                        Log.d("MainViewModel", "Single operation failed: $e")
                        throw e // 重新拋出，讓外層 try-catch 處理
                    }
                }
            }
            val successData = transformSuccess(results)
            Log.d("MainViewModel", "All operations succeeded")
            FetchResult.Success(successData)
        } catch (e: HttpException) {
            val errorData = ErrorData(e.response()?.code(), e.message(), e)
            Log.e("MainViewModel", "HttpException: ${e.response()?.code()}, ${e.message}", e)
            uiEventManager.showSnackbar(SnackbarMessage.ExceptionMessage(e))
            FetchResult.Error(errorData)
        } catch (e: IOException) {
            val errorData = ErrorData(null, "Network Error: ${e.message}", e)
            Log.e("MainViewModel", "IOException: ${e.message}", e)
            uiEventManager.showSnackbar(SnackbarMessage.ExceptionMessage(e))
            FetchResult.Error(errorData)
        } catch (e: Exception) {
            val errorData = ErrorData(null, "Failed to load data: ${e.message}", e)
            Log.e("MainViewModel", "Generic Exception: ${e.message}", e)
            uiEventManager.showSnackbar(SnackbarMessage.ExceptionMessage(e))
            FetchResult.Error(errorData)
        }
    }
}

// 定義一個通用的結果狀態，類似於您目前的 DownLoadState，但更通用
sealed interface FetchResult<out T> {
    data object Initial : FetchResult<Nothing>
    data object Loading : FetchResult<Nothing>
    data class Success<T>(val data: T) : FetchResult<T>
    data class Error(val errorData: ErrorData) : FetchResult<Nothing> // ErrorData 來自您的項目
}
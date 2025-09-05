package com.example.geminispotifyapp.features.userdatadetail

import android.util.Log
import com.example.geminispotifyapp.ApiError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.supervisorScope
import retrofit2.Response
import javax.inject.Inject

class ApiExecutionHelper @Inject constructor() {

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
                    it.await() // Wait for each async operation
                }
            }
            val firstError = results.firstOrNull { it is FetchResult.Error }
            if (firstError != null) {
                return firstError as FetchResult.Error // If any operation fails, return first error
            }
            val successData = transformSuccess(results)
            Log.d("MainViewModel", "All operations succeeded")
            FetchResult.Success(successData)
        } catch (e: Exception) {
            // Other exceptions caused from this fetch.
            throw e
        }
    }

    /**
     * Executes an operation that supports ETag-based caching.
     *
     * This function performs an API call and handles different HTTP response codes:
     * - **200 (OK):** The operation was successful. The response body is transformed into the desired
     *   `SuccessDataType`, and the ETag from the response headers is extracted.
     * - **304 (Not Modified):** The resource has not changed since the last request (identified by
     *   the ETag provided in the request). No data is returned.
     * - **Other codes:** An error is considered to have occurred.
     *
     * The function returns a [FetchResultWithEtag] to indicate the outcome, which can be used to
     * update a StateFlow or other reactive streams.
     *
     * @param ResultType The type of the raw data returned by the API operation.
     * @param SuccessDataType The type of the data after successful transformation.
     * @param operation A suspend function that executes the actual network request and returns a Retrofit [Response].
     *                  This operation is expected to handle potential network exceptions internally or allow them to propagate.
     * @param transformSuccess A function that takes the raw `ResultType` from a successful (200) API response
     *                         and transforms it into the final `SuccessDataType`.
     * @return A [FetchResultWithEtag] object representing the outcome of the operation:
     *         - [FetchResultWithEtag.Success]: If the operation was successful (HTTP 200) and the response body is not null.
     *           Contains the transformed data and the ETag.
     *         - [FetchResultWithEtag.NotModified]: If the server responded with HTTP 304.
     *         - [FetchResultWithEtag.Error]: If the response code is not 200 or 304, or if the response body is null for a 200 response.
     *           Contains an [ApiError] detailing the issue.
     * @throws Exception If the `operation` suspend function throws an exception (e.g., network issues not handled by Retrofit).
     *                   This exception will be re-thrown by `executeEtaggedOperation`.
     */
    suspend fun <ResultType, SuccessDataType> executeEtaggedOperation(
        operation: suspend () -> Response<ResultType>,
        transformSuccess: (ResultType) -> SuccessDataType// Transform ResultType into final SuccessDataType
        ): FetchResultWithEtag<SuccessDataType> { // Return result to caller to update StateFlow
        return try {
            val response = operation()
            when (response.code()) {
                200 -> {
                    val result = response.body()
                    if (result != null) {
                        val successData = transformSuccess(result)
                        Log.d("MainViewModel", "All operations succeeded")
                        Log.d("MainViewModel", "${response.headers()}")
                        Log.d("MainViewModel", "ETag: ${response.headers()["ETag"]}")
                        FetchResultWithEtag.Success(successData, response.headers()["ETag"])
                    } else {
                        Log.d("MainViewModel", "Response body is null")
                        FetchResultWithEtag.Error(ApiError.UnknownError("Response body is null"))
                    }
                }
                304 -> {
                    Log.d("MainViewModel", "Not Modified")
                    FetchResultWithEtag.NotModified(response.headers()["ETag"])
                }
                else -> {
                    Log.d("MainViewModel", "Response code is not 200 or 304")
                    FetchResultWithEtag.Error(ApiError.UnknownError("Response code is not 200 or 304"))
                }
            }
        } catch (e: Exception) {
            throw e
        }
    }
}



// Define a generic result state
sealed interface FetchResult<out T> {
    data object Initial : FetchResult<Nothing>
    data object Loading : FetchResult<Nothing>
    data class Success<T>(val data: T) : FetchResult<T>
    data class Error(val errorData: ApiError) : FetchResult<Nothing>
}

sealed interface FetchResultWithEtag<out T> {
    data object Initial : FetchResultWithEtag<Nothing>
    data object Loading : FetchResultWithEtag<Nothing>
    data class Success<T>(val data: T, val eTag: String?) : FetchResultWithEtag<T>
    data class NotModified(val eTag: String?) : FetchResultWithEtag<Nothing>
    data class Error(val errorData: ApiError) : FetchResultWithEtag<Nothing>
}
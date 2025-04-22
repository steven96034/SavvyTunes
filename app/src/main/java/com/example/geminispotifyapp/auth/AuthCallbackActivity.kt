package com.example.geminispotifyapp.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.geminispotifyapp.MainActivity
import com.example.geminispotifyapp.SpotifyApiService
import com.example.geminispotifyapp.SpotifyDataManager
import com.example.geminispotifyapp.utils.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

class AuthCallbackActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "AuthCallbackActivity"
        private const val PREFS_NAME = "spotify_token_prefs"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate called")
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent called")
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        Log.d(TAG, "handleIntent called with intent: $intent")
        val uri = intent?.data
        val expectedScheme = "geminispotifyapp"
        val expectedHost = "callback"

        if (uri == null || uri.scheme != expectedScheme || uri.host != expectedHost) {
            Log.w(TAG, "Received Intent Data is invalid or null")
            showErrorAndFinish("Invalid callback URI")
            return
        }

        val code = uri.getQueryParameter("code")
        val error = uri.getQueryParameter("error")
        val state = uri.getQueryParameter("state") // Consider using this if your auth flow uses state

        when {
            error != null -> {
                Log.e(TAG, "Spotify Authentication Error: $error")
                showErrorAndFinish("Spotify authentication error: $error")
                return
            }
            code != null -> {
                Log.d(TAG, "Successfully received Authorization Code: $code")
                exchangeCodeForToken(code)
            }
            else -> {
                Log.w(TAG, "Callback URI does not contain code or error")
                showErrorAndFinish("Missing code or error in callback URI")
                return
            }
        }
    }

    private fun exchangeCodeForToken(code: String) {
        val codeVerifier = AuthManager.getSavedCodeVerifier(this)
        if (codeVerifier == null) {
            Log.e(TAG, "Saved Code Verifier not found!")
            showErrorAndFinish("Code Verifier not found")
            return
        }
        Log.d(TAG, "Preparing to exchange Token... Code: $code, Verifier: $codeVerifier")

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = SpotifyApiService.getAccessToken(
                    grantType = "authorization_code",
                    code = code,
                    redirectUri = AuthManager.REDIRECT_URI,
                    clientId = AuthManager.CLIENT_ID,
                    codeVerifier = codeVerifier
                )

                Log.d(TAG, "Successfully received Access Token: ${response.accessToken}")
                saveTokens(response)
                SpotifyDataManager(applicationContext).updateAccessToken(response.accessToken)

                withContext(Dispatchers.Main) {
                    navigateToMainActivity()
                    finish()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error exchanging Token", e)
                withContext(Dispatchers.Main) {
                    showErrorAndFinish("Error exchanging token")
                }
            }
        }
    }

    private fun saveTokens(tokenResponse: SpotifyTokenResponse) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("access_token", tokenResponse.accessToken)
            putString("refresh_token", tokenResponse.refreshToken)
            putString("token_type", tokenResponse.tokenType)
            putLong("expires_at", System.currentTimeMillis() + (tokenResponse.expiresIn * 1000))
            putString("scope", tokenResponse.scope)
            apply()
        }
        Log.d(
            TAG,
            "Token saved to SharedPreferences. Expires at: ${Date(System.currentTimeMillis() + (tokenResponse.expiresIn * 1000))}"
        )
    }

    private fun navigateToMainActivity() {
        Log.d(TAG, "Navigating to MainActivity...")
        toast(this, "Spotify authorization successful")
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }
    private fun showErrorAndFinish(errorMessage: String) {
        Log.e(TAG,"showErrorAndFinish：$errorMessage")
        // Displaying the error message in a dialog, then finishing the activity
        AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage(errorMessage)
            .setPositiveButton("OK") { _, _ -> finish() }
            .setCancelable(false) // Prevent dismissing without acknowledgment
            .show()
    }
}

//class AuthCallbackActivity : AppCompatActivity() {
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        // 這個 Activity 通常不需要顯示 UI，主要用來處理 Intent
//        // setContentView(R.layout.activity_auth_callback) // 可以不需要
//        val context: Context = applicationContext
//
//        handleIntent(intent, context)
//    }
//
//    override fun onNewIntent(intent: Intent) {
//        super.onNewIntent(intent)
//        val context: Context = applicationContext
//        // 如果 Activity 是 singleTask 或 singleInstance，後續的 Intent 會透過這裡傳遞
//        handleIntent(intent, context)
//    }
//
//    private fun handleIntent(intent: Intent?, context: Context) {
//        val uri: Uri? = intent?.data
//        val expectedScheme = "geminispotifyapp" // 與 REDIRECT_URI 的 scheme 相同
//        val expectedHost = "callback"        // 與 REDIRECT_URI 的 host 相同
//
//        if (uri != null && uri.scheme == expectedScheme && uri.host == expectedHost) {
//            val code = uri.getQueryParameter("code")
//            val error = uri.getQueryParameter("error")
//            val state = uri.getQueryParameter("state") // 如果您有傳送 state，也要接收
//
//            if (code != null) {
//                Log.d("SpotifyAuth", "成功獲取授權碼 (Authorization Code): $code")
//                // --- 下一步：交換 Token ---
//                // 1. 從 SharedPreferences 取得之前儲存的 codeVerifier
//                val codeVerifier = AuthManager.getSavedCodeVerifier(this)
//                if (codeVerifier != null) {
//                    // 2. 使用 code 和 codeVerifier 向 Spotify 的 Token 端點請求 Access Token
//                    //    這一步需要發起網路請求 (POST)，必須在背景執行緒完成 (例如使用 Coroutines, Retrofit, Volley 等)
//                    exchangeCodeForToken(code, codeVerifier, context)
//                } else {
//                    Log.e("SpotifyAuth", "找不到儲存的 Code Verifier！")
//                    // 處理錯誤，例如提示使用者重試
//                    finish() // 關閉此 Activity
//                }
//            } else if (error != null) {
//                Log.e("SpotifyAuth", "Spotify 認證錯誤: $error")
//                // 處理錯誤，例如向使用者顯示錯誤訊息
//                finish() // 關閉此 Activity
//            } else {
//                Log.w("SpotifyAuth", "收到的回調 URI 不包含 code 或 error")
//                finish() // 關閉此 Activity
//            }
//        } else {
//            Log.w("SpotifyAuth", "收到的 Intent Data 不符預期或為空")
//            finish() // 關閉此 Activity
//        }
//    }
//
//    // --- 交換 Token 的範例函式 (需在背景執行緒呼叫) ---
//    private fun exchangeCodeForToken(code: String, codeVerifier: String, context: Context) {
//        Log.d("SpotifyAuth", "準備交換 Token... Code: $code, Verifier: $codeVerifier")
//
//        // 這裡需要實作網路請求邏輯
//        // 例如使用 Kotlin Coroutines + Retrofit:
//        lifecycleScope.launch(Dispatchers.IO) {
//            try {
//                val response = SpotifyApiService.getAccessToken(
//                    grantType = "authorization_code",
//                    code = code,
//                    redirectUri = AuthManager.REDIRECT_URI, // 確保與請求時相同
//                    clientId = AuthManager.CLIENT_ID,
//                    codeVerifier = codeVerifier
//                )
//                // 處理成功的回應 (response.accessToken, response.refreshToken etc.)
//                Log.d("SpotifyAuth", "成功獲取 Access Token: ${response.accessToken}")
//
//                saveTokens(response)
//                SpotifyDataManager(context).updateAccessToken(response.accessToken)
//
//                // 儲存 Token，導航到主畫面等
//                withContext(Dispatchers.Main) {
//                    // 更新 UI 或導航
//                    navigateToMainActivity()
//                    finish()
//                }
//
//            } catch (e: Exception) {
//                // 處理網路請求錯誤
//                Log.e("SpotifyAuth", "交換 Token 時發生錯誤", e)
////                 withContext(Dispatchers.Main) {
////                    // 提示使用者錯誤
////                    showErrorDialog()
////                 }
//            }
////            finally {
////                // 無論成功失敗都關閉此 Activity
////                finish()
////            }
//        }
//        // 暫時先關閉
//        //finish()
//    }
//
//    // 儲存 Token 的輔助函數
//    private fun saveTokens(tokenResponse: SpotifyTokenResponse) {
//        val prefs = getSharedPreferences("spotify_token_prefs", Context.MODE_PRIVATE)
//        with(prefs.edit()) {
//            putString("access_token", tokenResponse.accessToken)
//            putString("refresh_token", tokenResponse.refreshToken)
//            putString("token_type", tokenResponse.tokenType)
//            putLong("expires_at", System.currentTimeMillis() + (tokenResponse.expiresIn * 1000))
//            putString("scope", tokenResponse.scope)
//            apply()
//        }
//
//        Log.d("SpotifyAuth", "Token 已儲存至 SharedPreferences")
//        Log.d("SpotifyAuth", "Token 已儲存至 SharedPreferences，有效期至：" +
//                Date(System.currentTimeMillis() + (tokenResponse.expiresIn * 1000))
//        )
//    }
//
//    // --- 導航或錯誤處理的輔助函式 ---
//    private fun navigateToMainActivity() {
//        // 實作導航到您的 App 主畫面的邏輯
//        Log.d("SpotifyAuth", "導航到主畫面...")
//        toast(this, "成功授權Spotify")
//        val intent = Intent(this, MainActivity::class.java)
//        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
//        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP))
//    }
//
//    private fun showErrorDialog() {
//        // 實作顯示錯誤訊息給使用者的邏輯
//        Log.e("SpotifyAuth", "顯示錯誤對話框...")
//        AlertDialog.Builder(this).show()
//    }
//}
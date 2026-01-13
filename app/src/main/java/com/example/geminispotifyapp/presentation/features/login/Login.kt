package com.example.geminispotifyapp.presentation.features.login

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.geminispotifyapp.R
import androidx.core.net.toUri
import com.example.geminispotifyapp.core.utils.toast

@Composable
fun LoginPage(viewModel: LoginViewModel = hiltViewModel()) {
    val isAuthenticated by viewModel.isAuthenticated.collectAsStateWithLifecycle()
    val isUserLoggedInFirebase by viewModel.isUserLoggedInFirebase.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val tag = "LoginPage"

    Log.d(tag, "Recomposing LoginPage. isAuthenticated: $isAuthenticated")

    LaunchedEffect(viewModel.navigateToUrlEvent) {
        viewModel.navigateToUrlEvent.collect { authUrl ->
            if (context is Activity) {
                try {
                    val customTabsIntent = CustomTabsIntent.Builder().build()
                    customTabsIntent.launchUrl(context, authUrl.toUri())
                } catch (e: ActivityNotFoundException) {
                    val intent = Intent(Intent.ACTION_VIEW, authUrl.toUri())
                    context.startActivity(intent)
                }
            } else {
                Log.e(tag, "Context is not an Activity, cannot launch browser.")
                toast(context, "Error launching browser. Please try again.", Toast.LENGTH_SHORT)
            }
        }
    }

    LoginContent(
        onAuthSpotifyClick = { viewModel.onLoginClicked() },
        isAuthenticated = isAuthenticated,
        onGoogleLoginClick = { viewModel.handleGoogleLogin(context) },
        onLoginWithMailClick = { email: String, password: String -> viewModel.onLoginWithMailClick(email, password) },
        onSignUpWithMailClick = { email: String, password: String -> viewModel.onSignUpWithMailClick(email, password) },
        isUserLoggedInFirebase = isUserLoggedInFirebase
    )
}

@Composable
fun LoginContent(
    onAuthSpotifyClick: () -> Unit,
    isAuthenticated: Boolean,
    modifier: Modifier = Modifier,
    onGoogleLoginClick: () -> Unit,
    onLoginWithMailClick: (email: String, password: String) -> Unit,
    onSignUpWithMailClick: (email: String, password: String) -> Unit,
    isUserLoggedInFirebase: Boolean
) {
    if (!isUserLoggedInFirebase) {
        val isNotTypingEmailAndPassword = remember { mutableStateOf(true) }
        val isSignUpMode = remember { mutableStateOf(false) }
        val email = remember { mutableStateOf("") }
        val password = remember { mutableStateOf("") }

        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isNotTypingEmailAndPassword.value) {
                Card {
                    Text(
                        text = "Auth to log in with Firebase to save your data.",
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
                Spacer(modifier = Modifier.height(48.dp))
                GoogleSignInButton(onClick = onGoogleLoginClick)
                Spacer(modifier = Modifier.height(12.dp))
                EmailSignInButton(
                    onClick = {
                        isNotTypingEmailAndPassword.value = false
                        isSignUpMode.value = false
                    }
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text("Do not have an account?")
                val SIGN_UP_TAG = "SIGN_UP"

                val annotatedSignUpText = buildAnnotatedString {
                    withStyle(style = SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline
                    )
                    ) {
                        append("Sign up")
                    }
                    val signUpEndIndex = length
                    addStringAnnotation(
                        tag = SIGN_UP_TAG,
                        annotation = "trigger_signup_action",
                        start = 0,
                        end = signUpEndIndex
                    )
                    append(" with your email here.")
                }

                ClickableText(
                    text = annotatedSignUpText,
                    modifier = Modifier
                        .padding(8.dp),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    onClick = { offset ->
                        annotatedSignUpText.getStringAnnotations(tag = SIGN_UP_TAG, start = offset, end = offset)
                            .firstOrNull()?.let { _ ->
                                isNotTypingEmailAndPassword.value = false
                                isSignUpMode.value = true
                            }
                    }
                )
            } else {
                if (isSignUpMode.value) {
                    Card {
                        Text(
                            text = "Sign up with Email",
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.primary,
                                textDecoration = TextDecoration.Underline
                            )
                        )
                    }
                    OutlinedTextField(
                        value = email.value,
                        onValueChange = { email.value = it },
                        label = { Text("Email") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(0.8f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = password.value,
                        onValueChange = { password.value = it },
                        label = { Text("密碼") },
                        visualTransformation = PasswordVisualTransformation(), // 隱藏密碼
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(0.8f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onSignUpWithMailClick(email.value, password.value) },
                        contentPadding = PaddingValues(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Sign up")
                        }
                    }
                } else {
                    Card {
                        Text(
                            text = "Log in with Email",
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.primary,
                                textDecoration = TextDecoration.Underline
                            )
                        )
                    }
                    OutlinedTextField(
                        value = email.value,
                        onValueChange = { email.value = it },
                        label = { Text("Email") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(0.8f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = password.value,
                        onValueChange = { password.value = it },
                        label = { Text("密碼") },
                        visualTransformation = PasswordVisualTransformation(), // 隱藏密碼
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(0.8f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { onLoginWithMailClick(email.value, password.value) },
                        contentPadding = PaddingValues(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Log in")
                        }
                    }
                }
            }
        }
    }
    else if (!isAuthenticated) {
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.full_logo_green_rgb),
                contentDescription = "Spotify Logo",
                modifier = Modifier.height(60.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text("Please connect to Spotify to continue")
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onAuthSpotifyClick,
                contentPadding = PaddingValues(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Connect to Spotify")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Login,
                        contentDescription = "Login with Spotify",
                        modifier = Modifier.height(20.dp)
                    )
                }
            }
        }
    } else {
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun GoogleSignInButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        border = BorderStroke(width = 1.dp, color = Color(0xFF747775)),
        color = Color(0xFFFFFFFF),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Google Icon
            Image(
                painter = painterResource(id = R.drawable.android_light_rd_na),
                contentDescription = "Google Sign In",
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // 文字設定
            Text(
                text = "Sign in with Google",
                color = Color(0xFF1F1F1F),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Default
            )
        }
    }
}

@Composable
fun EmailSignInButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        border = BorderStroke(width = 1.dp, color = Color(0xFF747775)),
        color = Color(0xFFFFFFFF),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Sign in with Email",
                color = Color(0xFF1F1F1F),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Default
            )
        }
    }
}

@Preview
@Composable
fun LoginPagePreview() {
    LoginContent(
        onAuthSpotifyClick = {},
        isAuthenticated = false,
        onGoogleLoginClick = {},
        onLoginWithMailClick = { _, _ -> },
        onSignUpWithMailClick = { _, _ -> },
        isUserLoggedInFirebase = true
    )
}
package com.example.geminispotifyapp.presentation.features.login

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.sharp.Email
import androidx.compose.material.icons.twotone.Email
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign

@Composable
fun LoginPage(viewModel: LoginViewModel = hiltViewModel()) {
    val isAuthenticated by viewModel.isAuthenticated.collectAsStateWithLifecycle()
    val isUserLoggedInFirebase by viewModel.isUserLoggedInFirebase.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val tag = "LoginPage"

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

    LaunchedEffect(isAuthenticated) {
        if (!isAuthenticated) {
            viewModel.refreshAuthState()
        }
    }

    Log.d(tag, "Recomposing LoginPage. isSpotifyAuthenticated: $isAuthenticated, isUserLoggedInFirebase: $isUserLoggedInFirebase")

    LoginContent(
        onAuthSpotifyClick = { viewModel.onLoginClicked() },
        isAuthenticated = isAuthenticated,
        onGoogleLoginClick = { viewModel.handleGoogleLogin(context) },
        onLoginWithMailClick = { email: String, password: String, context: Context -> viewModel.onLoginWithMailClick(
            email,
            password,
            context
        ) },
        onSignUpWithMailClick = { email: String, context: Context -> viewModel.onSignUpWithMailClick(email, context) },
        onAnonymousLoginClick = { viewModel.handleAnonymousLogin() },
        isUserLoggedInFirebase = isUserLoggedInFirebase
    )
}

@Composable
fun LoginContent(
    onAuthSpotifyClick: () -> Unit,
    isAuthenticated: Boolean,
    modifier: Modifier = Modifier,
    onGoogleLoginClick: () -> Unit,
    onLoginWithMailClick: (email: String, password: String, context: Context) -> Unit,
    onSignUpWithMailClick: (email: String, context: Context) -> Unit,
    onAnonymousLoginClick: () -> Unit,
    isUserLoggedInFirebase: Boolean
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when {
            !isUserLoggedInFirebase -> {
                FirebaseAuthenticationContent(
                    onGoogleLoginClick = onGoogleLoginClick,
                    onLoginWithMailClick = onLoginWithMailClick,
                    onSignUpWithMailClick = onSignUpWithMailClick,
                    onAnonymousLoginClick = onAnonymousLoginClick
                )
            }
            !isAuthenticated -> {
                SpotifyAuthenticationContent(
                    onAuthSpotifyClick = onAuthSpotifyClick
                )
            }
            else -> {
                LoadingContent()
            }
        }
    }
}

@Composable
fun FirebaseAuthenticationContent(
    onGoogleLoginClick: () -> Unit,
    onLoginWithMailClick: (email: String, password: String, context: Context) -> Unit,
    onSignUpWithMailClick: (email: String, context: Context) -> Unit,
    onAnonymousLoginClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isNotTypingEmailAndPassword = remember { mutableStateOf(true) }
    val isSignUpMode = remember { mutableStateOf(false) }
    val email = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }

    val context = LocalContext.current as Activity

    if (isNotTypingEmailAndPassword.value) {
        TitleTemplate(
            "Firebase Authentication Required",
            "Log in to save your data and personalize your experience.",
            Icons.Default.AccountBox
        )
        Spacer(modifier = Modifier.height(24.dp))
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
        Text(
            text = annotatedSignUpText,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier
                .padding(8.dp)
                .pointerInput(Unit) {
                    detectTapGestures {
                        annotatedSignUpText.getStringAnnotations(tag = SIGN_UP_TAG, start = 0, end = SIGN_UP_TAG.length)
                            .firstOrNull()?.let {
                            isNotTypingEmailAndPassword.value = false
                            isSignUpMode.value = true
                        }
                    }
                }
        )
        Spacer(modifier = Modifier.height(8.dp))


        val TRY_AS_GUEST_TAG = "TRY_AS_GUEST."
        val annotatedTryAsGuestText = buildAnnotatedString {
            withStyle(style = SpanStyle(
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline
            )
            ) {
                append("Try as guest.")
            }
            val tryAsGuestEndIndex = length
            addStringAnnotation(
                tag = TRY_AS_GUEST_TAG,
                annotation = "trigger_tryAsGuest_action",
                start = 0,
                end = tryAsGuestEndIndex
            )
        }

        Text(
            text = annotatedTryAsGuestText,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier
                .padding(8.dp)
                .pointerInput(Unit) {
                    detectTapGestures {
                        annotatedTryAsGuestText.getStringAnnotations(tag = TRY_AS_GUEST_TAG, start = 0, end = TRY_AS_GUEST_TAG.length)
                            .firstOrNull()?.let {
                                onAnonymousLoginClick()
                            }
                    }
                }
        )

    } else {
        BackHandler(onBack = { isNotTypingEmailAndPassword.value = true })
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier.fillMaxWidth(0.8f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { isNotTypingEmailAndPassword.value = true }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                if (isSignUpMode.value) {
                    TitleTemplate(
                        "Sign up with Email",
                        "Please enter your email to sign up. A link will be sent to your inbox.",
                        Icons.TwoTone.Email
                    )
                }
                else {
                    TitleTemplate(
                        "Log in with Email",
                        "Please enter your email and password to log in.",
                        Icons.Sharp.Email
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = email.value,
                onValueChange = { email.value = it },
                label = { Text("Email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(0.8f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Only show password field for login, not for email link sign up
            if (!isSignUpMode.value) {
                OutlinedTextField(
                    value = password.value,
                    onValueChange = { password.value = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(0.8f)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    if (isSignUpMode.value) {
                        onSignUpWithMailClick(email.value, context)
                    } else {
                        onLoginWithMailClick(email.value, password.value, context)
                    }
                },
                contentPadding = PaddingValues(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (isSignUpMode.value) "Sign up" else "Log in")
                }
            }
        }
    }
}

@Composable
fun SpotifyAuthenticationContent(
    onAuthSpotifyClick: () -> Unit,
    modifier: Modifier = Modifier
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

@Composable
fun LoadingContent(modifier: Modifier = Modifier) {
    CircularProgressIndicator()
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
            Icon(
                imageVector = Icons.Default.Email,
                contentDescription = "Email Sign In",
                tint = MaterialTheme.colorScheme.background,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

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

@Composable
fun TitleTemplate(title: String, description: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    textAlign = TextAlign.Start
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    textAlign = TextAlign.Start
                )
            }
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
        onLoginWithMailClick = { _, _, _ -> },
        onSignUpWithMailClick = { _, _ -> },
        onAnonymousLoginClick = {},
        isUserLoggedInFirebase = true
    )
}

@Preview
@Composable
fun FirebaseAuthenticationContentPreview() {
    FirebaseAuthenticationContent(
        onGoogleLoginClick = {},
        onLoginWithMailClick = { _, _, _ -> },
        onSignUpWithMailClick = { _, _ -> },
        onAnonymousLoginClick = {}
    )
}

@Preview
@Composable
fun SpotifyAuthenticationContentPreview() {
    SpotifyAuthenticationContent(
        onAuthSpotifyClick = {}
    )
}

@Preview
@Composable
fun LoadingContentPreview() {
    LoadingContent()
}

@Preview
@Composable
fun FirebaseEmailLoginPreview() {
    FirebaseAuthenticationContent(
        onGoogleLoginClick = {},
        onLoginWithMailClick = { _, _, _ -> },
        onSignUpWithMailClick = { _, _ -> },
        onAnonymousLoginClick = {}
    ).apply {
        val isNotTypingEmailAndPassword = remember { mutableStateOf(false) }
        val isSignUpMode = remember { mutableStateOf(false) }
        isNotTypingEmailAndPassword.value = false // Simulate being in the email input state
        isSignUpMode.value = false // Simulate login mode
    }
}

@Preview
@Composable
fun FirebaseEmailSignUpPreview() {
    FirebaseAuthenticationContent(
        onGoogleLoginClick = {},
        onLoginWithMailClick = { _, _, _ -> },
        onSignUpWithMailClick = { _, _ -> },
        onAnonymousLoginClick = {}
    ).apply {
        val isNotTypingEmailAndPassword = remember { mutableStateOf(false) }
        val isSignUpMode = remember { mutableStateOf(true) }
        isNotTypingEmailAndPassword.value = false // Simulate being in the email input state
        isSignUpMode.value = true // Simulate sign up mode
    }
}

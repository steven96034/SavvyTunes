package com.example.geminispotifyapp.features.settings

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.geminispotifyapp.data.UserProfileResponse
import com.example.geminispotifyapp.features.userdatadetail.FetchResult
import com.example.geminispotifyapp.ApiError
import androidx.core.net.toUri
import com.example.geminispotifyapp.R

@Composable
fun ProfileScreen(paddingValues: PaddingValues, viewModel: ProfileViewModel =  hiltViewModel()) {
    val userProfileState by viewModel.userProfileState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchUserProfileIfNeeded()
    }

    ProfileContent(
        paddingValues = paddingValues,
        fetchResult = userProfileState,
        onRetry = { viewModel.fetchUserProfile() },
        onOpenLinkFailed = { viewModel.onOpenLinkFailed() },
        logOut = { viewModel.logOut() }
    )
}

@Composable
private fun ProfileContent(
    paddingValues: PaddingValues,
    fetchResult: FetchResult<UserProfileResponse>,
    onRetry: () -> Unit,
    onOpenLinkFailed: () -> Unit,
    logOut: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        when (fetchResult) {
            FetchResult.Initial, FetchResult.Loading -> {
                CircularProgressIndicator()
            }
            is FetchResult.Success -> {
                UserProfileDetails(userProfile = fetchResult.data, onOpenLinkFailed = onOpenLinkFailed, logOut = logOut)
            }
            is FetchResult.Error -> {
                ErrorStateView(errorData = fetchResult.errorData, onRetry = onRetry)
            }
        }
    }
}

@Composable
private fun UserProfileDetails(userProfile: UserProfileResponse, onOpenLinkFailed: () -> Unit, logOut: () -> Unit) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        val imageUrl = userProfile.images.firstOrNull()?.url
        if (imageUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "User Profile Image",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = "Default User Profile Image",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(72.dp)
                )
            }
        }

        Text(
            text = userProfile.displayName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        ProfileInfoItem(label = "ID", value = userProfile.id)
        ProfileInfoItem(label = "Email", value = userProfile.email)
        ProfileInfoItem(label = "Country", value = userProfile.country)
        ProfileInfoItem(label = "Product Plan", value = userProfile.product)
        ProfileInfoItem(label = "Followers", value = (userProfile.followers["total"] ?: 0).toString())

        Spacer(modifier = Modifier.height(16.dp))

        userProfile.externalUrls["spotify"]?.let { spotifyProfileUrl ->
            Button(onClick = {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, spotifyProfileUrl.toUri())
                    context.startActivity(intent)
                } catch (e: Exception) {
                    onOpenLinkFailed()
                }
            },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Row {
                    Text(text = "Check in Spotify")
                    Spacer(modifier = Modifier.width(4.dp))
                    Image(
                        painter = painterResource(R.drawable.primary_logo_green_rgb),
                        contentDescription = null,
                        modifier = Modifier.height(20.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { logOut() }) {
            Text(text = "Log Out")
        }
    }
}

@Composable
private fun ProfileInfoItem(label: String, value: String?) {
    if (value.isNullOrEmpty()) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(2f)
        )
    }
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
private fun ErrorStateView(
    errorData: ApiError,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val errorMessage = when (errorData) {
            is ApiError.NetworkConnectionError -> "Network connection error. Please check your internet connection and try again."
            else -> "Load profile data failed. Please try again later."
        }
        Text(
            text = errorMessage,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}
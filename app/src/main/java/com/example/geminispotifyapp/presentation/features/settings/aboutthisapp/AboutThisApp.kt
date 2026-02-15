package com.example.geminispotifyapp.presentation.features.settings.aboutthisapp

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.geminispotifyapp.R

@Composable
fun AboutThisAppScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. App Header (Logo & Version)
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_icon),
                    contentDescription = "App Logo",
                    modifier = Modifier.size(64.dp),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Savvy Tunes",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Version 1.0.0",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }

        // 2. Tech Stack
        item {
            AboutSectionCard(title = "Core Technologies") {
                TechStackRow(
                    icon = Icons.Default.SmartToy,
                    name = "Google Gemini AI",
                    description = "Leverages generative AI to analyze your listening history and curate personalized recommendations."
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                TechStackRow(
                    icon = Icons.Default.LibraryMusic,
                    name = "Spotify API",
                    description = "Powers the extensive music catalog, metadata, and album artwork."
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                TechStackRow(
                    icon = Icons.Default.WbSunny,
                    name = "Open-Meteo API",
                    description = "Provides real-time, non-commercial weather data to match music vibes with your local atmosphere."
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                TechStackRow(
                    icon = Icons.Default.CloudQueue,
                    name = "Google Firebase",
                    description = "Handles secure user authentication and real-time cloud data synchronization."
                )
            }
        }

        // 3. Compliance (Legal & Attribution)
        item {
            AboutSectionCard(title = "Legal & Attribution") {
                Text(
                    text = "Savvy Tunes is an independent project developed for educational purposes. It is not affiliated with, endorsed, maintained, sponsored, or specifically approved by Spotify AB, Google LLC, or Open-Meteo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "• Content provided by Spotify. All artist images and album artwork are the property of their respective owners.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                val uriHandler = LocalUriHandler.current
                Row(modifier = Modifier.clickable { uriHandler.openUri("https://open-meteo.com/") }) {
                    Text(
                        text = "• ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Weather data by Open-Meteo.com",
                        style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.Underline),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = " under CC BY 4.0 license.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "• Recommendations are generated by Gemini AI models. Results may vary and are for entertainment purposes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 4. Link
        item {
            val uriHandler = LocalUriHandler.current
            AboutSectionCard(title = "Links") {
                ListItem(
                    headlineContent = { Text("Developer GitHub") },
                    leadingContent = { Icon(Icons.Default.Code, null) },
                    modifier = Modifier.clickable {
                        uriHandler.openUri("https://github.com/steven96034/GeminiSpotifyApp")
                    }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
fun AboutSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                content = content
            )
        }
    }
}

@Composable
fun TechStackRow(
    icon: ImageVector,
    name: String,
    description: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .size(24.dp)
                .padding(top = 2.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
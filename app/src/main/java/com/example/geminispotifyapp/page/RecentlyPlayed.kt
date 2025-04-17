package com.example.geminispotifyapp.page

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.geminispotifyapp.HandleBackToHome
import com.example.geminispotifyapp.R
import com.example.geminispotifyapp.data.PlayHistoryObject
import com.example.geminispotifyapp.data.SharedData.GET_ITEM_NUM
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Composable
fun RecentlyPlayedContent(recentlyPlayed: List<PlayHistoryObject>, navController: NavController) {
    val scrollState = rememberScrollState()

    HandleBackToHome(navController)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = 8.dp, horizontal = 16.dp)
    ) {
        Row {
            Column {
                Row (
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(painter = painterResource(R.drawable.primary_logo_green_rgb), contentDescription = null, modifier = Modifier.height(28.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "最近播放的歌曲",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        recentlyPlayed.forEachIndexed { index, playHistory ->
            RecentTrackItem(index + 1, playHistory)
            if (index < recentlyPlayed.size - 1) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
        }
        if (recentlyPlayed.size < GET_ITEM_NUM) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Info, contentDescription = "Info Icon")
                Spacer(modifier = Modifier.width(8.dp))
                Text("You data is not enough to show more recently played songs. (Max = $GET_ITEM_NUM)")
            }
        }

        Log.d("SpotifyDataContent", "Recently Played: $recentlyPlayed")
    }

}

@Composable
fun RecentTrackItem(index: Int, playHistory: PlayHistoryObject) {
    val track = playHistory.track

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "$index.",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(30.dp)
        )

        // 專輯封面
        val imageUrl = track.album.images.firstOrNull()?.url
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Album image",
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(2.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
        }

        // 歌曲資訊
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = track.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = track.artists.joinToString(", ") { it.name },
                style = MaterialTheme.typography.labelMedium
            )

            // 播放時間
            val timeAgo = formatTimeAgo(playHistory.playedAt)
            Text(
                text = timeAgo,
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray
            )
        }
    }
}

fun formatTimeAgo(dateString: String): String {
    try {
        // 使用ISO 8601格式解析日期字符串
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC") // 確保解析的時區為 UTC

        // 解析日期
        val lastPlayed = sdf.parse(dateString) ?: return "未知時間"

        // 獲取當前時間
        val now = java.util.Date()

        // 計算時間差（毫秒 * 1000 == 秒）
        val diff = (now.time - lastPlayed.time) / 1000

        return when {
            diff < 60 -> "剛剛"
            diff < 60 * 60 -> "${diff / (60)} 分鐘前"
            diff < 24 * 60 * 60 -> "${diff / (60 * 60)} 小時前"
            else -> "${diff / (24 * 60 * 60)} 天前"
        }
    } catch (e: Exception) {
        Log.e("TimeFormat", "解析日期失敗：$dateString", e)
        return "未知時間"
    }
}
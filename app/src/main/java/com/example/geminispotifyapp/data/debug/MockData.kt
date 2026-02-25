package com.example.geminispotifyapp.data.debug

import com.example.geminispotifyapp.data.remote.model.PlayContext
import com.example.geminispotifyapp.data.remote.model.PlayHistoryObject
import com.example.geminispotifyapp.data.remote.model.SimplifiedArtist
import com.example.geminispotifyapp.data.remote.model.SimplifiedTrack
import com.example.geminispotifyapp.data.remote.model.SpotifyAlbum
import com.example.geminispotifyapp.data.remote.model.SpotifyArtist
import com.example.geminispotifyapp.data.remote.model.SpotifyImage
import com.example.geminispotifyapp.data.remote.model.SpotifyTrack
import com.example.geminispotifyapp.data.remote.model.TrackFromCloudRecommendation
import com.example.geminispotifyapp.presentation.features.main.userdatadetail.recentlyplayed.UiPlayHistoryObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object MockData {
    val mockSpotifyTracks = List(20) { index ->
        val i = index + 1
        val mockImageUrl = "https://picsum.photos/seed/album${i + 20}/600/600"
        // "https://picsum.photos/600/600" for random picture
        val mockArtist = SimplifiedArtist(
            id = "mock_artist_id_$i",
            name = "Mock Artist $i",
            externalUrls = mapOf("spotify" to "https://open.spotify.com/artist/mock$i"),
            uri = "spotify:artist:mock$i",
            href = "https://api.spotify.com/v1/artists/mock$i"
        )

        val mockAlbum = SpotifyAlbum(
            id = "mock_album_id_$i",
            name = "Mock Album $i",
            type = "album",
            images = listOf(
                SpotifyImage(
                    url = mockImageUrl,
                    height = 600,
                    width = 600
                )
            ),
            externalUrls = mapOf("spotify" to "https://open.spotify.com/album/mock$i"),
            artists = listOf(mockArtist),
            releaseDate = "2024-01-01",
            releaseDatePrecision = "day",
            totalTracks = 10,
            uri = "spotify:album:mock$i",
            availableMarkets = listOf("TW", "US", "JP")
        )

        SpotifyTrack(
            album = mockAlbum,
            artists = listOf(mockArtist),
            availableMarkets = listOf("TW", "US", "JP"),
            discNumber = 1,
            durationMs = 180000 + (index * 10000),
            explicit = false,
            externalIds = mapOf("isrc" to "MOCKISRC$i"),
            externalUrls = mapOf("spotify" to "https://open.spotify.com/track/mock$i"),
            href = "https://api.spotify.com/v1/tracks/mock$i",
            id = "mock_track_id_$i",
            isPlayable = true,
            linkedFrom = emptyMap(),
            restrictions = emptyMap(),
            name = "Mock Track $i",
            popularity = 60 + index,
            trackNumber = 1,
            type = "track",
            uri = "spotify:track:mock$i",
            isLocal = false
        )
    }
    val mockSimplifiedTrack: List<SimplifiedTrack> = List(20) { index ->
        val i = index + 1

        val mockArtist = SimplifiedArtist(
            id = "mock_artist_id_$i",
            name = "Mock Artist $i",
            externalUrls = mapOf("spotify" to "https://open.spotify.com/artist/mock$i"),
            uri = "spotify:artist:mock$i",
            href = "https://api.spotify.com/v1/artists/mock$i"
        )
        SimplifiedTrack(
            artists = listOf(mockArtist),
            availableMarkets = listOf("TW", "US", "JP"),
            discNumber = 1,
            durationMs = 180000 + (index * 10000),
            explicit = false,
            externalUrls = mapOf("spotify" to "https://open.spotify.com/track/mock$i"),
            href = "https://api.spotify.com/v1/tracks/mock$i",
            id = "mock_track_id_$i",
            isPlayable = true,
            linkedFrom = emptyMap(),
            restrictions = emptyMap(),
            name = "Mock Track $i",
            trackNumber = 1,
            type = "track",
            uri = "spotify:track:mock$i",
            isLocal = false
        )
    }
    val mockSpotifyArtists = List(20) { index ->
        val i = index + 1
        val mockImageUrl = "https://picsum.photos/seed/artist$i/400/400"

        val mockGenres = listOf("pop", "indie rock", "chillwave", "synth-pop", "acoustic")
            .shuffled()
            .take((1..2).random())

        SpotifyArtist(
            id = "mock_artist_id_$i",
            name = "Mock Artist $i",
            popularity = 50 + (index * 2),
            type = "artist",
            externalUrls = mapOf("spotify" to "https://open.spotify.com/artist/mock$i"),
            followers = mapOf("total" to (10000 + (index * 1500))),
            genres = mockGenres,
            images = listOf(
                SpotifyImage(
                    url = mockImageUrl,
                    height = 400,
                    width = 400
                )
            ),
            href = "https://api.spotify.com/v1/artists/mock$i",
            uri = "spotify:artist:mock$i"
        )
    }
    val mockSpotifyAlbums = List(20) { index ->
        val i = index + 1
        val mockImageUrl = "https://picsum.photos/seed/album${i + 20}/600/600"

        val mockArtist = SimplifiedArtist(
            id = "mock_artist_id_$i",
            name = "Mock Artist $i",
            externalUrls = mapOf("spotify" to "https://open.spotify.com/artist/mock$i"),
            uri = "spotify:artist:mock$i",
            href = "https://api.spotify.com/v1/artists/mock$i"
        )

        SpotifyAlbum(
            id = "mock_album_id_$i",
            name = "Mock Album $i",
            type = "album",
            images = listOf(
                SpotifyImage(
                    url = mockImageUrl,
                    height = 600,
                    width = 600
                )
            ),
            externalUrls = mapOf("spotify" to "https://open.spotify.com/album/mock$i"),
            artists = listOf(mockArtist),
            releaseDate = "2024-01-01",
            releaseDatePrecision = "day",
            totalTracks = 10,
            uri = "spotify:album:mock$i",
            availableMarkets = listOf("TW", "US", "JP")
        )
    }
    val mockUiPlayHistoryObjects: List<UiPlayHistoryObject> = List(20) { index ->
        val i = index + 1

        val now = LocalDateTime.now()
        val playedAtTime = now.minusHours(i.toLong())

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss")
        val isoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")

        val formattedDateTime = playedAtTime.format(formatter)
        val originalPlayedAtIso = playedAtTime.format(isoFormatter)
        val timeAgoStr = "$i hours ago"

        val mockImageUrl = "https://picsum.photos/seed/album$i/600/600"

        val mockArtist = SimplifiedArtist(
            id = "mock_artist_id_$i",
            name = "Mock Artist $i",
            externalUrls = mapOf("spotify" to "https://open.spotify.com/artist/mock$i"),
            uri = "spotify:artist:mock$i",
            href = "https://api.spotify.com/v1/artists/mock$i"
        )

        val mockAlbum = SpotifyAlbum(
            id = "mock_album_id_$i",
            name = "Mock Album $i",
            type = "album",
            images = listOf(
                SpotifyImage(
                    url = mockImageUrl,
                    height = 600,
                    width = 600
                )
            ),
            externalUrls = mapOf("spotify" to "https://open.spotify.com/album/mock$i"),
            artists = listOf(mockArtist),
            releaseDate = "2024-01-01",
            releaseDatePrecision = "day",
            totalTracks = 10,
            uri = "spotify:album:mock$i",
            availableMarkets = listOf("TW", "US", "JP")
        )

        val mockTrack = SpotifyTrack(
            album = mockAlbum,
            artists = listOf(mockArtist),
            availableMarkets = listOf("TW", "US", "JP"),
            discNumber = 1,
            durationMs = 180000 + (index * 10000),
            explicit = false,
            externalIds = mapOf("isrc" to "MOCKISRC$i"),
            externalUrls = mapOf("spotify" to "https://open.spotify.com/track/mock$i"),
            href = "https://api.spotify.com/v1/tracks/mock$i",
            id = "mock_track_id_$i",
            isPlayable = true,
            linkedFrom = emptyMap(),
            restrictions = emptyMap(),
            name = "Mock Track $i",
            popularity = 60 + index,
            trackNumber = 1,
            type = "track",
            uri = "spotify:track:mock$i",
            isLocal = false
        )

        val mockContext = PlayContext(
            type = if (i % 2 == 0) "playlist" else "album",
            uri = "spotify:${if (i % 2 == 0) "playlist" else "album"}:mock$i",
            externalUrls = mapOf("spotify" to "https://open.spotify.com/context/mock$i")
        )

        val mockPlayHistory = PlayHistoryObject(
            track = mockTrack,
            playedAt = originalPlayedAtIso,
            context = mockContext
        )

        UiPlayHistoryObject(
            originalPlayHistory = mockPlayHistory,
            formattedPlayedAtTimeAgo = timeAgoStr,
            formattedPlayedAtDateTime = formattedDateTime
        )
    }
    val mockTrackFromCloudRecommendation: List<TrackFromCloudRecommendation> = List(20) { index ->
        val i = index + 1
        val mockImageUrl = "https://picsum.photos/seed/album${i + 20}/600/600"
        TrackFromCloudRecommendation(
            spotifyId = "mock_track_id_$i",
            name = "Mock Track $i",
            artist = "Mock Artist $i",
            imageUrl = mockImageUrl,
            uri = "spotify:track:mock$i"
        )
    }
}
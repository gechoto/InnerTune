package com.zionhuang.music.utils

import android.net.ConnectivityManager
import com.zionhuang.innertube.YouTube
import com.zionhuang.innertube.models.YouTubeClient
import com.zionhuang.innertube.models.YouTubeClient.Companion.IOS
import com.zionhuang.innertube.models.YouTubeClient.Companion.WEB_CREATOR
import com.zionhuang.innertube.models.YouTubeClient.Companion.WEB_REMIX
import com.zionhuang.innertube.models.response.PlayerResponse
import com.zionhuang.music.constants.AudioQuality
import com.zionhuang.music.db.entities.FormatEntity

object YouTubePlayerUtils {
    /**
     * The main client is used for metadata.
     * Do not use other clients for this because it can result in inconsistent metadata.
     * For example other clients can have different normalization targets (loudnessDb).
     *
     * [com.zionhuang.innertube.models.YouTubeClient.WEB_REMIX] should be preferred here because currently it is the only client which provides:
     * - the correct metadata (like loudnessDb)
     * - premium formats (not used yet)
     */
    private val MAIN_CLIENT: YouTubeClient = WEB_REMIX

    /**
     * Clients used for fallback streams in case the streams of the main client do not work.
     */
    private val STREAM_FALLBACK_CLIENTS: Array<YouTubeClient> = arrayOf(WEB_CREATOR, IOS)

    suspend fun playerResponseWithFormat(
        videoId: String,
        playlistId: String? = null,
        playedFormat: FormatEntity?,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
    ): Result<Pair<PlayerResponse, PlayerResponse.StreamingData.Format>> = runCatching {
        val mainPlayerResponse =
            YouTube.player(videoId, playlistId, client = MAIN_CLIENT).getOrThrow()

        var streamPlayerResponse: PlayerResponse? = null

        for (client in STREAM_FALLBACK_CLIENTS) {
            val playerResponse =
                YouTube.player(videoId, playlistId, client).getOrNull()
            if (playerResponse?.playabilityStatus?.status == "OK") {
                streamPlayerResponse = playerResponse
                break
            }
        }

        if (streamPlayerResponse == null) {
            throw Exception("Bad stream player response")
        }

        val format =
            findFormat(streamPlayerResponse, playedFormat, audioQuality, connectivityManager)
                ?: throw Exception("Could not find format")

        Pair(
            mainPlayerResponse.copy(
                streamingData = streamPlayerResponse.streamingData
            ),
            format
        )
    }

    suspend fun playerResponseForMetadata(
        videoId: String,
        playlistId: String? = null,
    ): Result<PlayerResponse> =
        YouTube.player(videoId, playlistId, client = MAIN_CLIENT)

    private fun findFormat(
        playerResponse: PlayerResponse,
        playedFormat: FormatEntity?,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
    ): PlayerResponse.StreamingData.Format? =
        if (playedFormat != null) {
            playerResponse.streamingData?.adaptiveFormats?.find { it.itag == playedFormat.itag }
        } else {
            playerResponse.streamingData?.adaptiveFormats
                ?.filter { it.isAudio }
                ?.maxByOrNull {
                    it.bitrate * when (audioQuality) {
                        AudioQuality.AUTO -> if (connectivityManager.isActiveNetworkMetered) -1 else 1
                        AudioQuality.HIGH -> 1
                        AudioQuality.LOW -> -1
                    } + (if (it.mimeType.startsWith("audio/webm")) 10240 else 0) // prefer opus stream
                }
        }
}
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
import okhttp3.OkHttpClient

object YTPlayerUtils {

    private val httpClient = OkHttpClient.Builder().build()

    /**
     * The main client is used for metadata and initial streams.
     * Do not use other clients for this because it can result in inconsistent metadata.
     * For example other clients can have different normalization targets (loudnessDb).
     *
     * [com.zionhuang.innertube.models.YouTubeClient.WEB_REMIX] should be preferred here because currently it is the only client which provides:
     * - the correct metadata (like loudnessDb)
     * - premium formats
     */
    private val MAIN_CLIENT: YouTubeClient = WEB_REMIX

    /**
     * Clients used for fallback streams in case the streams of the main client do not work.
     */
    private val STREAM_FALLBACK_CLIENTS: Array<YouTubeClient> = arrayOf(WEB_CREATOR, IOS)

    /**
     * Player response and format intended to use for playback.
     */
    suspend fun playerResponseForPlayback(
        videoId: String,
        playlistId: String? = null,
        playedFormat: FormatEntity?,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
    ): Result<Pair<PlayerResponse, PlayerResponse.StreamingData.Format>> = runCatching {
        val mainPlayerResponse =
            YouTube.player(videoId, playlistId, client = MAIN_CLIENT).getOrThrow()

        var format: PlayerResponse.StreamingData.Format? = findFormat(
            mainPlayerResponse,
            playedFormat,
            audioQuality,
            connectivityManager,
        )

        var streamUrl = format?.findUrl()
        if (streamUrl != null && validateStatus(streamUrl)) {
            Pair(
                mainPlayerResponse,
                format,
            )
        }

        var streamPlayerResponse: PlayerResponse? = null
        for (client in STREAM_FALLBACK_CLIENTS) {
            streamPlayerResponse =
                YouTube.player(videoId, playlistId, client).getOrNull()
            if (streamPlayerResponse?.playabilityStatus?.status == "OK") {
                format =
                    findFormat(
                        streamPlayerResponse,
                        playedFormat,
                        audioQuality,
                        connectivityManager,
                    )
                streamUrl = format?.findUrl() ?: continue
                if (validateStatus(streamUrl)) {
                    break
                }
            } else {
                streamPlayerResponse = null
                format = null
            }
        }

        if (streamPlayerResponse == null) {
            throw Exception("Bad stream player response")
        }
        if (format == null) {
            throw Exception("Could not find format")
        }

        Pair(
            mainPlayerResponse.copy(
                // This does not really do anything because if playabilityStatus is not OK
                // it will set streamPlayerResponse to null and return above
                // This should be changed to return the streamPlayerResponse
                // of the last client used even if it is not OK
                playabilityStatus = streamPlayerResponse.playabilityStatus,
                // This is currently not used and there is no reason to use it
                // the format is already returned separately
                // so streamingData does not really matter and can probably be removed
                streamingData = streamPlayerResponse.streamingData,
            ),
            format,
        )
    }

    /**
     * Simple player response intended to use for metadata only.
     * Stream URLs of this response might not work.
     */
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

    private fun validateStatus(url: String): Boolean {
        val requestBuilder = okhttp3.Request.Builder()
            .head()
            .url(url)
        val response = httpClient.newCall(requestBuilder.build()).execute()
        return response.isSuccessful
    }
}
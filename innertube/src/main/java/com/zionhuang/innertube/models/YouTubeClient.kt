package com.zionhuang.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class YouTubeClient(
    val clientName: String,
    val clientVersion: String,
    val clientId: String,
    val userAgent: String,
    val osVersion: String? = null,
    val supportsLogin: Boolean = false,
    val useSignatureTimestamp: Boolean = false,
    // val origin: String? = null,
    // val referer: String? = null,
) {
    fun toContext(locale: YouTubeLocale, visitorData: String?) = Context(
        client = Context.Client(
            clientName = clientName,
            clientVersion = clientVersion,
            osVersion = osVersion,
            gl = locale.gl,
            hl = locale.hl,
            visitorData = visitorData
        )
    )

    companion object {
        /**
         * Should be the latest Firefox ESR version.
         */
        const val USER_AGENT_WEB = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0"

        const val ORIGIN_YOUTUBE_MUSIC = "https://music.youtube.com"
        const val REFERER_YOUTUBE_MUSIC = "$ORIGIN_YOUTUBE_MUSIC/"
        const val API_URL_YOUTUBE_MUSIC = "$ORIGIN_YOUTUBE_MUSIC/youtubei/v1/"

//        private const val USER_AGENT_ANDROID = "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/65.0.3325.181 Mobile Safari/537.36"
//        val ANDROID_MUSIC = YouTubeClient(
//            clientName = "ANDROID_MUSIC",
//            clientVersion = "5.01",
//            api_key = "AIzaSyAOghZGza2MQSZkY_zfZ370N-PUdXEo8AI",
//            userAgent = USER_AGENT_ANDROID,
//        )
//        val ANDROID = YouTubeClient(
//            clientName = "ANDROID",
//            clientVersion = "17.13.3",
//            api_key = "AIzaSyA8eiZmM1FaDVjRy-df2KTyQ_vz_yYM39w",
//            userAgent = USER_AGENT_ANDROID,
//        )

        val WEB = YouTubeClient(
            clientName = "WEB",
            clientVersion = "2.20241126.01.00",
            clientId = "1",
            userAgent = USER_AGENT_WEB,
        )

        val WEB_REMIX = YouTubeClient(
            clientName = "WEB_REMIX",
            clientVersion = "1.20241127.01.00",
            clientId = "67",
            userAgent = USER_AGENT_WEB,
            supportsLogin = true,
            useSignatureTimestamp = true,
        )

        val WEB_CREATOR = YouTubeClient(
            clientName = "WEB_CREATOR",
            clientVersion = "1.20241203.01.00",
            clientId = "62",
            userAgent = USER_AGENT_WEB,
            supportsLogin = true,
            useSignatureTimestamp = true,
        )

        val TVHTML5 = YouTubeClient(
            clientName = "TVHTML5_SIMPLY_EMBEDDED_PLAYER",
            clientVersion = "2.0",
            clientId = "85",
            userAgent = "Mozilla/5.0 (PlayStation; PlayStation 4/12.00) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.4 Safari/605.1.15",
            supportsLogin = true,
            useSignatureTimestamp = true,
        )

        val IOS = YouTubeClient(
            clientName = "IOS",
            clientVersion = "19.45.4",
            clientId = "5",
            userAgent = "com.google.ios.youtube/19.45.4 (iPhone16,2; U; CPU iOS 18_1_0 like Mac OS X;)",
            osVersion = "18.1.0.22B83",
        )
    }
}

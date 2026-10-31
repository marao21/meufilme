package com.example

import com.example.model.VideoMedia
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testVideoMediaDurationFormatted() {
        val videoWithDuration = VideoMedia(
            url = "https://example.com/test.mp4",
            title = "Test Drama",
            durationSeconds = 3665L
        )
        assertEquals("01:01:05", videoWithDuration.durationFormatted)

        val shortVideo = VideoMedia(
            url = "https://example.com/test2.mp4",
            title = "Test Short",
            durationSeconds = 125L
        )
        assertEquals("02:05", shortVideo.durationFormatted)

        val liveVideo = VideoMedia(
            url = "https://example.com/live.m3u8",
            title = "Live Stream",
            isLive = true
        )
        assertEquals("AO VIVO", liveVideo.durationFormatted)
    }

    @Test
    fun testHlsFormatDetection() {
        val hlsVideo = VideoMedia(
            url = "https://example.com/stream/master.m3u8",
            title = "HLS Stream",
            format = "M3U8 (HLS)",
            isHls = true
        )
        assertTrue(hlsVideo.isHls)
        assertEquals("M3U8 (HLS)", hlsVideo.format)
    }
}

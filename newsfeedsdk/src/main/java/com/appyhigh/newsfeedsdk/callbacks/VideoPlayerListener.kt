package com.appyhigh.newsfeedsdk.callbacks

import com.google.android.exoplayer2.ui.StyledPlayerView

interface VideoPlayerListener {
    fun onVideoEnded(position: Int, duration: Long)
    fun setUpYoutubeVideo(view: StyledPlayerView, position: Int, youtubeUrl: String)
    fun releaseYoutubeVideo()
}
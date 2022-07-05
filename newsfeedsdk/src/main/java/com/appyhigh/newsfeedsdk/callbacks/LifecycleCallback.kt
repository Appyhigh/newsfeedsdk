package com.appyhigh.newsfeedsdk.callbacks

import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

interface LifecycleCallback {
    fun addObserver(youTubePlayerView: YouTubePlayerView?)
}
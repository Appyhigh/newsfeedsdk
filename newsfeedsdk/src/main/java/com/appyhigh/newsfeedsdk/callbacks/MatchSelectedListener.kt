package com.appyhigh.newsfeedsdk.callbacks

import android.view.View

interface MatchSelectedListener {
    fun onUpcomingMatchClicked(v: View, position: Int)
    fun onLiveMatchClicked(v: View, position: Int)
    fun onPastMatchClicked(v: View, position: Int)
}
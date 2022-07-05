package com.appyhigh.newsfeedsdk.callbacks

import android.view.View

interface InterestClickListener {
    fun onInterestClicked(v: View, position: Int)
}
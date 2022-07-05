package com.appyhigh.newsfeedsdk.callbacks

import android.view.View

interface InterestSelectedListener {
    fun onInterestClicked(v: View, position: Int)
}
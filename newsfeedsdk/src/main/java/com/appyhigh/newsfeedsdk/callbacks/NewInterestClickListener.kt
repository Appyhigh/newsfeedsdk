package com.appyhigh.newsfeedsdk.callbacks

import android.view.View

interface NewInterestClickListener {
    fun onInterestPinned(v: View, position: Int, isPinned: Boolean)
    fun onInterestFollowed(v: View,position: Int, isSelected: Boolean)
}
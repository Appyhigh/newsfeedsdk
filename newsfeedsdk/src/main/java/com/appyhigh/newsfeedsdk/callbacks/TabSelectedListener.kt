package com.appyhigh.newsfeedsdk.callbacks

import android.view.View

interface TabSelectedListener {
    fun onTabClicked(v: View, position: Int)
}

interface PWATabSelectedListener {
    fun onTabClicked(filename :String)
}
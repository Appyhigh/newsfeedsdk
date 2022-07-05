package com.appyhigh.newsfeedsdk.callbacks

import android.view.View

interface OnViewAttachedToWindow {
    fun onViewAttachedToWindow(v: View, position: Int)
}
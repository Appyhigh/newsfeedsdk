package com.appyhigh.newsfeedsdk.callbacks

import android.view.View

interface OnViewDetachedFromWindow {
    fun onViewDetachedFromWindow(v: View, position:Int)
}
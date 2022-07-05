package com.appyhigh.newsfeedsdk.callbacks

import android.graphics.Bitmap
import android.graphics.drawable.Drawable

interface GlideCallbackListener {
    fun onSuccess(drawable: Drawable?)
    fun onFailure()
}
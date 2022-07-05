package com.appyhigh.newsfeedsdk.callbacks

import android.view.View

interface PopularAccountClickListener {
    fun onPopularAccountClicked(v: View, position: Int)
    fun onFollowClicked(v: View, position: Int)
}
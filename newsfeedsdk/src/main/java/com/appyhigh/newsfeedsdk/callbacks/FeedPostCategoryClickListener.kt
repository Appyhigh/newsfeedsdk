package com.appyhigh.newsfeedsdk.callbacks

import android.view.View

interface FeedPostCategoryClickListener {
    fun onFeedPostCategoryClicked(v: View, position: Int)
}
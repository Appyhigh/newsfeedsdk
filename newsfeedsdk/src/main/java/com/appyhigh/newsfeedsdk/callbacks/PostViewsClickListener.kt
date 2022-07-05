package com.appyhigh.newsfeedsdk.callbacks

import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import com.appyhigh.newsfeedsdk.model.feeds.Card

interface PostViewsClickListener {
    fun onPostClicked(v: View, position: Int)
    fun onMoreOptionsClicked(v: View, position: Int)
    fun onRatingClicked(v:View, position: Int)
    fun onShareClicked(v: View, position: Int)
    fun onLikeClicked(v: View, position: Int, card: Card)
    fun onSharePost(v: View, position: Int, card: Card, isWhatsApp:Boolean)
    fun onFollowClicked(v: View, position: Int)
    fun onReportClicked(v: View, position: Int, card: Card, type: String)
    fun onPodcastClicked(v: View, position: Int, card: Card)
    fun onShowMoreClicked(v: View, card: Card, position: Int)
    fun onSideTextClicked(v: View, position: Int, card: Card)
    fun onMayLikeInterestClicked(v: View, position: Int, interest: String, isLiked: Boolean)
}
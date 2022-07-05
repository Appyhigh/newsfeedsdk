package com.appyhigh.newsfeedsdk.callbacks

import com.appyhigh.newsfeedsdk.model.feeds.Card

interface PostImpressionListener {
    fun addImpression(card: Card, totalDuration: Int?, watchedDuration: Int?)
}
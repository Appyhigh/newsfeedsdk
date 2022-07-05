package com.appyhigh.newsfeedsdk.model.explore

import com.appyhigh.newsfeedsdk.model.feeds.Card
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class ExploreResponseModel (
    @SerializedName("cards")
    @Expose
    var cards: List<Card> = ArrayList()
)
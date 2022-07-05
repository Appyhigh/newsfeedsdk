package com.appyhigh.newsfeedsdk.model

import com.appyhigh.newsfeedsdk.model.feeds.Card
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class CricketScheduleResponse(
    @SerializedName("cards")
    @Expose
    var cards: List<Card> = ArrayList()
)


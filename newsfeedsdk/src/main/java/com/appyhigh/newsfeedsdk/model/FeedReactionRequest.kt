package com.appyhigh.newsfeedsdk.model

import com.appyhigh.newsfeedsdk.Constants
import com.google.gson.annotations.SerializedName

data class FeedReactionRequest(
    @SerializedName("post_id") val feedId: String,
    @SerializedName("reaction") val reactionType: Constants.ReactionType
)

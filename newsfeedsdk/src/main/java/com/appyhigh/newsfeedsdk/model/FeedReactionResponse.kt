package com.appyhigh.newsfeedsdk.model

import com.appyhigh.newsfeedsdk.Constants
import com.google.gson.annotations.SerializedName

data class FeedReactionResponse(
    @SerializedName("reacted") val hasReacted: Boolean? = false,
    @SerializedName("reaction") val reactionType: Constants.ReactionType? = Constants.ReactionType.NONE
)
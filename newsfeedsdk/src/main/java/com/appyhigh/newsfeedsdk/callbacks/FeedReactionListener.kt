package com.appyhigh.newsfeedsdk.callbacks

import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.model.Post

interface FeedReactionListener {
    fun onReaction(item: Post?, reactionType: Constants.ReactionType)
}
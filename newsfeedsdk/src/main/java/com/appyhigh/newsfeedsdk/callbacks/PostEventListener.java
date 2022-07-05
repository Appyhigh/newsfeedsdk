package com.appyhigh.newsfeedsdk.callbacks;

import com.appyhigh.newsfeedsdk.Constants;
import com.appyhigh.newsfeedsdk.model.Post;

public interface PostEventListener {
    public void onPostReaction(int position, Boolean isAlreadyReacted, Constants.ReactionType reactionType, Post post, long currentPosition);
}

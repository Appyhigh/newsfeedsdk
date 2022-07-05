package com.appyhigh.newsfeedsdk.model.feeds

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class DefaultReactionsCount(
    @SerializedName("like_count")
    @Expose
    var likeCount: Int = 0,

    @SerializedName("love_count")
    @Expose
    var loveCount: Int = 0,

    @SerializedName("wow_count")
    @Expose
    var wowCount: Int = 0,

    @SerializedName("angry_count")
    @Expose
    var angryCount: Int = 0,

    @SerializedName("laugh_count")
    @Expose
    var laughCount: Int = 0,

    @SerializedName("sad_count")
    @Expose
    var sadCount: Int = 0
)
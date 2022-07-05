package com.appyhigh.newsfeedsdk.model.feeds

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class Reactions(
    @SerializedName("like")
    @Expose
    var like: List<Any>? = null,

    @SerializedName("love")
    @Expose
    var love: List<Any>? = null,

    @SerializedName("wow")
    @Expose
    var wow: List<Any>? = null,

    @SerializedName("angry")
    @Expose
    var angry: List<Any>? = null,

    @SerializedName("laugh")
    @Expose
    var laugh: List<Any>? = null,

    @SerializedName("sad")
    @Expose
    var sad: List<Any>? = null
)
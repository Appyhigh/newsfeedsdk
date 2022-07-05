package com.appyhigh.newsfeedsdk.model.feeds

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class Image(
    @SerializedName("quality")
    @Expose
    var quality: String? = null,

    @SerializedName("url")
    @Expose
    var url: String? = null
)
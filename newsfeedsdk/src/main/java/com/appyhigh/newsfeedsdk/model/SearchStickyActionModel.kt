package com.appyhigh.newsfeedsdk.model
import com.google.gson.annotations.SerializedName

import com.google.gson.annotations.Expose


data class SearchStickyActionModel(
    @SerializedName("widget")
    @Expose
    val widget: String? = null,
    @SerializedName("country")
    @Expose
    val country: String? = null,
    @SerializedName("language")
    @Expose
    val language: String? = null
)

data class SearchStickyWidgetModel(
    @SerializedName("icons")
    @Expose
    val icons: List<String> = ArrayList(),
)
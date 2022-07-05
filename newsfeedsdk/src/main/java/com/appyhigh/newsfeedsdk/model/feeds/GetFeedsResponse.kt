package com.appyhigh.newsfeedsdk.model.feeds

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class GetFeedsResponse(
    @SerializedName("cards")
    @Expose
    var cards: List<Card> = ArrayList(),

    @SerializedName("ad_placement")
    @Expose
    var adPlacement: List<Int> = arrayListOf(6),

    @SerializedName("publisher_name")
    @Expose
    var publisher_name: String? = "",

    @SerializedName("publisher_id")
    @Expose
    var publisher_id: String? = "",

    @SerializedName("platform")
    @Expose
    var platform: String? = "",

    @SerializedName("publisher_contact_us")
    @Expose
    var publisher_contact_us: String? = "",

    @SerializedName("publisher_website")
    @Expose
    var publisher_website: String? = "",

    @SerializedName("profile_pic")
    @Expose
    var profile_pic: String? = "",

    @SerializedName("followers")
    @Expose
    var followers: Int = 0,

    @SerializedName("languages")
    @Expose
    var languages: List<String> = ArrayList(),

    @SerializedName("language_to_select_initially")
    @Expose
    var language_to_select_initially: String? = ""
)
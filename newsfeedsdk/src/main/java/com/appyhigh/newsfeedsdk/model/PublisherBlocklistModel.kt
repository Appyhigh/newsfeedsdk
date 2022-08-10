package com.appyhigh.newsfeedsdk.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName


data class PublisherBlocklistModel(
    @SerializedName("publisher_details")
    @Expose
    val publisherDetails: List<PublisherDetail> = ArrayList()

)

data class PublisherDetail(
    @SerializedName("contact_us")
    @Expose
    val contactUs: String? = null,
    @SerializedName("country")
    @Expose
    val country: List<String> = ArrayList(),
    @SerializedName("fullname")
    @Expose
    val fullname: String? = null,
    @SerializedName("platform")
    @Expose
    val platform: String? = null,
    @SerializedName("profile_pic")
    @Expose
    val profilePic: String? = null,
    @SerializedName("publisher_id")
    @Expose
    val publisherId: String? = null,
    @SerializedName("publisher_website")
    @Expose
    val publisherWebsite: Any? = null,
    @SerializedName("username")
    @Expose
    val username: String? = null,
    var isBlocked: Boolean = true
)
package com.appyhigh.newsfeedsdk.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName


data class PrivateAdRequest(
    @SerializedName("plcmnt_id")
    @Expose
    val plcmntId: String? = null,
    @SerializedName("bundle_id")
    @Expose
    val bundleId: String? = null,
    @SerializedName("device_id")
    @Expose
    val deviceId: String? = null,
    @SerializedName("ad_size")
    @Expose
    val adSize: String? = null,
    @SerializedName("cat")
    @Expose
    val cat: String? = null,
    @SerializedName("storeurl")
    @Expose
    val storeurl: String? = null,
    @SerializedName("ver")
    @Expose
    val ver: String? = null,
    @SerializedName("ua")
    @Expose
    val ua: String? = null,
    @SerializedName("ip")
    @Expose
    val ip: String? = null
)

data class PrivateAdResponse(
    @SerializedName("creative")
    @Expose
    val creative: String? = null,
    @SerializedName("eurl")
    @Expose
    val eUrl: String? = null,
    @SerializedName("nurl")
    @Expose
    val nUrl: String? = null
)
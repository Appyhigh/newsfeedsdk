package com.appyhigh.newsfeedsdk.model

import com.google.gson.annotations.SerializedName

data class ReportRequest(
    @SerializedName("post_id") val post_id: String,
    @SerializedName("report") val report: String
)
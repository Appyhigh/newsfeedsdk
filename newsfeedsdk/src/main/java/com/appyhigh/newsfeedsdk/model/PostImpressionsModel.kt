package com.appyhigh.newsfeedsdk.model

data class ImpressionsListModel(
    val impressions_list:List<PostImpressionsModel>
)

data class PostImpressionsModel(
    var api_uri: String ="",
    val post_views: List<PostView> = ArrayList(),
    var timestamp: Long = 0
)

data class PostView(
    val country: String? =null,
    val feed_type: String? =null,
    val is_video: Boolean? =false,
    val language: String? =null,
    val interest: String? =null,
    val post_id: String? =null,
    val post_source: String? =null,
    val publisher_id: String? =null,
    val short_video: Boolean? =false,
    val source: String? =null,
    val total_video_duration: Int? =null,
    val watched_duration: Int? =null,
    val key: String
)
package com.appyhigh.newsfeedsdk.model.feeds

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class Content (
    @SerializedName("short_code")
    @Expose
    var shortCode: String? = null,

    @SerializedName("is_private")
    @Expose
    var isPrivate: Boolean? = null,

    @SerializedName("description")
    @Expose
    var description: String? = null,

    @SerializedName("comments")
    @Expose
    var comments: Int? = null,

    @SerializedName("dislikes")
    @Expose
    var dislikes: Int? = null,

    @SerializedName("media_list")
    @Expose
    var mediaList: List<String>? = null,

    @SerializedName("images")
    @Expose
    var images: List<List<ContentImage>?>? = null,

    @SerializedName("likes")
    @Expose
    var likes: Int? = null,

    @SerializedName("video_url")
    @Expose
    var videoUrl: String? = null,

    @SerializedName("caption")
    @Expose
    var caption: String? = null,

    @SerializedName("video_view_count")
    @Expose
    var videoViewCount: Int? = null,

    @SerializedName("video_duration")
    @Expose
    var videoDuration: String? = null,

    @SerializedName("duration")
    @Expose
    var duration: String? = null,

    @SerializedName("post_url")
    @Expose
    var postUrl: String? = null,

    @SerializedName("title")
    @Expose
    var title: String? = null,

    @SerializedName("author")
    @Expose
    var author: String? = null,

    @SerializedName("url")
    @Expose
    var url: String? = null,

    @SerializedName("category")
    @Expose
    var category: List<String>? = null,

    @SerializedName("retweets")
    @Expose
    var retweets: Int? = null,

    @SerializedName("hashtags")
    @Expose
    var hashtags: List<String>? = null
)

data class ContentImage(
    @SerializedName("quality")
    @Expose
    var quality: String? = null,

    @SerializedName("url")
    @Expose
    var url: String? = null
)
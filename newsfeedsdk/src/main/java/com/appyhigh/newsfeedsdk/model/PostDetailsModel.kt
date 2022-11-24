package com.appyhigh.newsfeedsdk.model

import com.google.gson.annotations.SerializedName


data class PostDetailsModel(
    @SerializedName("post_details")
    var post: Post? = null
) {
    data class Post(
        @SerializedName("is_webview")
        var isWebView: Boolean = false,

        @SerializedName("is_native")
        var isNative: Boolean = false,

        @SerializedName("publisher_id")
        var publisherId: String? = null,

        @SerializedName("post_id")
        var postId: String? = null,

        @SerializedName("post_source")
        var postSource: String? = null,

        @SerializedName("feed_type")
        var feedType: String? = null,

        @SerializedName("platform")
        var platform: String? = null,

        @SerializedName("tags")
        var tags: List<String> = ArrayList(),

        @SerializedName("published_on")
        var publishedOn: String? = null,

        @SerializedName("app_comments")
        var appComments: Int = 0,

        @SerializedName("is_video")
        var isVideo: Boolean = false,

        @SerializedName("content")
        var content: Content? = null,

        @SerializedName("reactions")
        var reaction: Reaction? = null,

        @SerializedName("reactions_count")
        var reactionCount: ReactionCount? = null,

        @SerializedName("is_reacted")
        var isReacted: String? = null,

        @SerializedName("publisher_name")
        var publisherName: String? = null,

        @SerializedName("publisher_profile_pic")
        var publisherProfilePic: String? = null,

        @SerializedName("publisher_contact_us")
        var publisherContactUs: String? = null,

        @SerializedName("language_string")
        var languageString: String? = null,

        @SerializedName("interests")
        var interests: List<String> = ArrayList(),

        @SerializedName("is_bookmarked")
        var isBookmarked: Boolean = false,

        @SerializedName("comments")
        var comments: List<FeedComment> = ArrayList(),

        @SerializedName("additional_data")
        val additional_data: AdditionalData,

        var presentUrl:String = "",

        var presentTimeStamp:Long = 0
    )

    data class Content(
        @SerializedName("short_code")
        var shortCode: String? = null,

        @SerializedName("title")
        var title: String? = null,

        @SerializedName("description")
        var description: String? = null,

        @SerializedName("media_list")
        var mediaList: List<String> = ArrayList(),

        @SerializedName("url")
        var url: String? = null,

        @SerializedName("content")
        var content: String? = null,

        @SerializedName("video_url")
        var videoUrl: String? = null,
    )

    data class Reaction(
        @SerializedName("like")
        var like: List<String> = ArrayList(),

        @SerializedName("love")
        var love: List<String> = ArrayList(),

        @SerializedName("wow")
        var wow: List<String> = ArrayList(),

        @SerializedName("angry")
        var angry: List<String> = ArrayList(),

        @SerializedName("laugh")
        var laugh: List<String> = ArrayList(),

        @SerializedName("sad")
        var sad: List<String> = ArrayList()
    )

    /**
     * Reactions received on a post
     */
    data class ReactionCount(
        @SerializedName("like_count")
        var likeCount: Int = 0,

        @SerializedName("love_count")
        var loveCount: Int = 0,

        @SerializedName("wow_count")
        var wowCount: Int = 0,

        @SerializedName("angry_count")
        var angryCount: Int = 0,

        @SerializedName("laugh_count")
        var laughCount: Int = 0,

        @SerializedName("sad_count")
        var sadCount: Int = 0
    )

    data class AdditionalData(
        val next_post: NextPost? = null,
        val related_post: NextPost? = null,
        val related_post_list: List<NextPost> = ArrayList()
    )

    data class NextPost(
        val _id: String,
        val content: NextPostContent,
        val is_video: Boolean,
        val post_id: String,
        @SerializedName("is_native")
        var isNative: Boolean = false,
        @SerializedName("published_on")
        var publishedOn: String = "",
        @SerializedName("post_source")
        var postSource: String? = null,
        @SerializedName("feed_type")
        var feedType: String? = null
    )

    data class NextPostContent(
        val author: Any,
        val caption: String = "",
        val category: List<Any>,
        val description: String,
        val images: List<List<NextPostImage>>,
        val is_private: Boolean,
        val media_list: List<String>,
        val short_code: String,
        val title: String = "",
        @SerializedName("url")
        val url: String="",
        @SerializedName("video_url")
        val video_url: String = "",
        val videos: List<Any>
    )

    data class NextPostImage(
        val quality: String,
        val url: String
    )
}
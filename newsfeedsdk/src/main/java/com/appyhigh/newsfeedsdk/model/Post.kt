package com.appyhigh.newsfeedsdk.model

import com.appyhigh.newsfeedsdk.model.FeedResponseModel.*
import com.google.gson.annotations.SerializedName
import java.util.*

data class Post(
    @SerializedName("_id")
    var id: String = "",

    @SerializedName("post_source")
    var post_source: String? = null,

    @SerializedName("feed_type")
    var feed_type: String? = null,

    @SerializedName("source")
    var source: String? = null,

    @SerializedName("is_webview")
    var isWebView: Boolean = false,

    @SerializedName("publisher_id")
    var publisherId: String? = null,

    @SerializedName("post_id")
    var postId: String? = null,

    @SerializedName("platform")
    var platform: String? = null,

    @SerializedName("tags")
    var tags: List<String> = ArrayList(),

    @SerializedName("published_on")
    var publishedOn: String? = null,

    @SerializedName("app_comments")
    var appComments: Int = 0,

    @SerializedName("createdAt")
    var createdAt: String? = null,

    @SerializedName("updatedAt")
    var updatedAt: String? = null,

    @SerializedName("is_video")
    var isVideo: Boolean = false,

    @SerializedName("content")
    var content: Content? = null,

    @SerializedName("ml_class_probs")
    var mlClassProbs: MLClassProbability? = null,

//        @SerializedName("ml_old_interests")
//        public List<MLOldInterests> mlOldInterests;
    @SerializedName("reactions")
    var reaction: Reaction? = null,

    @SerializedName("reactions_count")
    var reactionCount: ReactionCount? = null,

    @SerializedName("default_reactions_count")
    var defaultReactionCount: ReactionCount? = null,

    @SerializedName("ml_popularity_score")
    var MLPopularityScore: Double = 0.0,

    @SerializedName("ml_country")
    var MLCountry: List<String> = ArrayList(),

    @SerializedName("is_visible")
    var isVisible: Boolean = false,

    @SerializedName("ml_language")
    var MLLanguage: List<String> = ArrayList(),

    @SerializedName("ml_interests_corrected")
    var MLInterestsCorrected: Boolean = false,

    @SerializedName("short_video")
    var isShortVideo: Boolean = false,

    @SerializedName("is_reacted")
    var isReacted: String? = null,

    @SerializedName("publisher_name")
    var publisherName: String? = null,

    @SerializedName("publisher_profile_pic")
    var publisherProfilePic: String? = null,

    @SerializedName("interests")
    var interests: List<String> = ArrayList(),

    @SerializedName("is_bookmarked")
    var isBookmarked: Boolean = false,

    @SerializedName("playbackPosition")
    var playbackPosition: Long = 0,

    @SerializedName("publisher_contact_us")
    var publisher_contact_us: String = "",

    @SerializedName("is_following_publisher")
    var is_following_publisher: Boolean = false
)
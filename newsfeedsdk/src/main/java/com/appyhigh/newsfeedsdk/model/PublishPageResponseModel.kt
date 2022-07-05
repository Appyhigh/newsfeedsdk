package com.appyhigh.newsfeedsdk.model

data class PublishPageResponseModel(
    val followers: Int,
    val language_to_select_initially: String,
    val languages: List<String>,
    val platform: String,
    val posts: ArrayList<Post>,
    val profile_pic: String,
    val publisher_id: String,
    val publisher_name: String,
    val publisher_contact_us: String = "",
    val publisher_website: String = ""
)

data class PublisherPost(
    val _id: String,
    val app_comments: Int,
    val bookmarked_by: List<Any>,
    val content: PublisherContent,
    val createdAt: String,
    val default_reactions_count: PublisherDefaultReactionsCount,
    val is_video: Boolean,
    val is_visible: Boolean,
    val is_webview: Boolean,
    val language_string: String,
    val ml_class_probs: PublisherMlClassProbs,
    val ml_country: List<String>,
    val ml_interests: List<String>,
    val ml_interests_corrected: Boolean,
    val ml_language: List<String>,
    val ml_old_interests: List<Any>,
    val ml_popularity_score: Double,
    val platform: String,
    val post_id: String,
    val published_on: String,
    val publisher_id: String,
    val reactions: PublisherReactions,
    val reactions_count: PublisherReactionsCount,
    val short_video: Boolean,
    val source: String,
    val tags: List<String>,
    val updatedAt: String
)

data class PublisherContent(
    val caption: String,
    val comments: Int,
    val description: String,
    val dislikes: Int,
    val is_private: Boolean,
    val likes: Int,
    val media_list: List<String>,
    val short_code: String,
    val video_duration: String,
    val video_url: String,
    val video_view_count: Int
)

data class PublisherDefaultReactionsCount(
    val angry_count: Int,
    val laugh_count: Int,
    val like_count: Int,
    val love_count: Int,
    val sad_count: Int,
    val wow_count: Int
)

data class PublisherMlClassProbs(
    val business: Double,
    val entertainment: Double,
    val fitness: Double,
    val news: Double,
    val sports: Double,
    val technology: Double
)

data class PublisherReactions(
    val angry: List<Any>,
    val laugh: List<Any>,
    val like: List<Any>,
    val love: List<Any>,
    val sad: List<Any>,
    val wow: List<Any>
)

data class PublisherReactionsCount(
    val angry_count: Int,
    val laugh_count: Int,
    val like_count: Int,
    val love_count: Int,
    val sad_count: Int,
    val wow_count: Int
)
package com.appyhigh.newsfeedsdk.model

import com.google.gson.annotations.SerializedName
import java.util.*
import kotlin.collections.ArrayList

data class FeedResponseModel(
    @SerializedName("posts")
    var postList: List<Post> = ArrayList<Post>(),
    @SerializedName("ad_placement")
    var adPlacement: List<Int> = ArrayList<Int>()
) {
    /**
     * Model of Post
     */
    /**
     * Model of Content exist for every post
     * Stores the title, author, media files and categories
     */
    class Content {
        @SerializedName("short_code")
        var shortCode: String? = null

        @SerializedName("title")
        var title: String? = null

        @SerializedName("is_private")
        var isPrivate = false

        @SerializedName("description")
        var description: String? = null

        @SerializedName("author")
        var author: String? = null

        @SerializedName("media_list")
        var mediaList: List<String> = ArrayList()

        @SerializedName("url")
        var url: String? = null

        @SerializedName("category")
        var category: List<String> = ArrayList()

        @SerializedName("content")
        var content: String? = null

        @SerializedName("video_url")
        var videoUrl: String? = null
    }

    /**
     * Probabilities to which the post belongs to
     */
    inner class MLClassProbability {
        @SerializedName("news")
        var news = 0.0

        @SerializedName("fitness")
        var fitness = 0.0

        @SerializedName("sports")
        var sports = 0.0

        @SerializedName("entertainment")
        var entertainment = 0.0

        @SerializedName("business")
        var business = 0.0

        @SerializedName("technology")
        var technology = 0.0
    }
    //    public class MLOldInterests{
    //
    //    }
    /**
     * Reactions available on a post
     */
    class Reaction {
        @SerializedName("like")
        var like: List<String> = ArrayList()

        @SerializedName("love")
        var love: List<String> = ArrayList()

        @SerializedName("wow")
        var wow: List<String> = ArrayList()

        @SerializedName("angry")
        var angry: List<String> = ArrayList()

        @SerializedName("laugh")
        var laugh: List<String> = ArrayList()

        @SerializedName("sad")
        var sad: List<String> = ArrayList()
    }

    /**
     * Reactions received on a post
     */
    class ReactionCount {
        @SerializedName("like_count")
        var likeCount = 0

        @SerializedName("love_count")
        var loveCount = 0

        @SerializedName("wow_count")
        var wowCount = 0

        @SerializedName("angry_count")
        var angryCount = 0

        @SerializedName("laugh_count")
        var laughCount = 0

        @SerializedName("sad_count")
        var sadCount = 0
    }
}
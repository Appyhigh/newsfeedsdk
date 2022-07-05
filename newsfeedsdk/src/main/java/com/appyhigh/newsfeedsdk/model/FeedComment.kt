package com.appyhigh.newsfeedsdk.model

import com.google.gson.annotations.SerializedName

data class FeedComment(
    @SerializedName("user_id") val userId: String? = null,
    @SerializedName("comment_id") val commentId: String? = null,
    @SerializedName("comment") val commentDetails: FeedCommentDetails? = null,
    @SerializedName("timestamp") val commentedAt: String? = null,
    @SerializedName("user_profile_picture") val userImage: String? = null,
    @SerializedName("user_first_name") val firstName: String? = null,
    @SerializedName("user_last_name") val lastName: String? = null,
    @SerializedName("user_short_id") val shortId: String? = null
) {
    fun getUserName(): String {
        return if (!firstName.isNullOrEmpty()) {
            "${firstName.orEmpty()} ${lastName.orEmpty()}".trim()
        } else shortId.orEmpty()
    }
}

data class FeedCommentDetails(
    @SerializedName("comment_type") val commentType: String? = null,
    @SerializedName("comment_value") val commentValue: String? = null
)

data class MediaScheme(
    @SerializedName("mediaUrl") val mediaUrl: String? = null,
    @SerializedName("mediaType") val mediaType: String? = null
)


data class FeedCommentRequest(
    @SerializedName("comment_type") val comment_type: String,
    @SerializedName("post_id") val post_id: String,
    @SerializedName("comment_value") val comment_value: String
)

data class FeedCommentResponse(
    @SerializedName("commented") val hasCommented: Boolean? = false,
    @SerializedName("comment") val comment: FeedComment? = null
)

data class FeedCommentResponseWrapper(
    @SerializedName("response") val result: FeedCommentResponse? = null
)

data class RefreshRequest(
    @SerializedName("post_id") val post_id: String
)
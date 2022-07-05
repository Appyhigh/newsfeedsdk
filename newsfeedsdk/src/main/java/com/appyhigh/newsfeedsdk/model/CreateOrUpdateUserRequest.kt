package com.appyhigh.newsfeedsdk.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class CreateOrUpdateUserRequest(
    @SerializedName("push_token")
    @Expose
    val push_token: String,
    @SerializedName("country_code")
    @Expose
    val country_code: String? = "in",
    @SerializedName("first_name")
    @Expose
    val first_name: String?,
    @SerializedName("last_name")
    @Expose
    val last_name: String?,
    @SerializedName("email")
    @Expose
    val email: String?,
    @SerializedName("phone_number")
    val phone_number: String?,
    @SerializedName("dailling_code")
    val dialling_code: String?
)

data class CreateOrUpdateUserRequestWithUserName(
    @SerializedName("push_token")
    @Expose
    val push_token: String,
    @SerializedName("country_code")
    @Expose
    val country_code: String? = "in",
    @SerializedName("first_name")
    @Expose
    val first_name: String?,
    @SerializedName("last_name")
    @Expose
    val last_name: String?,
    @SerializedName("email")
    @Expose
    val email: String?,
    @SerializedName("phone_number")
    val phone_number: String?,
    @SerializedName("dailling_code")
    val dialling_code: String?,
    @SerializedName("username")
    @Expose
    val username: String?,
)

data class CreateOrUpdateUserRequestWithInterests(
    @SerializedName("push_token")
    @Expose
    val push_token: String,
    @SerializedName("country_code")
    @Expose
    val country_code: String? = "in",
    @SerializedName("first_name")
    @Expose
    val first_name: String?,
    @SerializedName("last_name")
    @Expose
    val last_name: String?,
    @SerializedName("email")
    @Expose
    val email: String?,
    @SerializedName("phone_number")
    val phone_number: String?,
    @SerializedName("dailling_code")
    val dialling_code: String?,
    @SerializedName("interests")
    @Expose
    val interest: String? = ""
)

data class CreateOrUpdateUserRequestWithLanguages(
    @SerializedName("push_token")
    @Expose
    val push_token: String,
    @SerializedName("country_code")
    @Expose
    val country_code: String? = "in",
    @SerializedName("first_name")
    @Expose
    val first_name: String?,
    @SerializedName("last_name")
    @Expose
    val last_name: String?,
    @SerializedName("email")
    @Expose
    val email: String?,
    @SerializedName("phone_number")
    val phone_number: String?,
    @SerializedName("dailling_code")
    val dialling_code: String?,
    @SerializedName("languages")
    @Expose
    val languages: String? = ""
)

data class CreateOrUpdateUserRequestWithBoth(
    @SerializedName("push_token")
    @Expose
    val push_token: String,
    @SerializedName("country_code")
    @Expose
    val country_code: String? = "in",
    @SerializedName("first_name")
    @Expose
    val first_name: String?,
    @SerializedName("last_name")
    @Expose
    val last_name: String?,
    @SerializedName("email")
    @Expose
    val email: String?,
    @SerializedName("phone_number")
    val phone_number: String?,
    @SerializedName("dailling_code")
    val dialling_code: String?,
    @SerializedName("interests")
    @Expose
    val interest: String? = "",
    @SerializedName("languages")
    @Expose
    val languages: String? = ""
)

data class UpdateUserPersonalizationRequest(
    @SerializedName("interests")
    @Expose
    val interest: String?=null,
    @SerializedName("language")
    @Expose
    val language: String?=null,
    @SerializedName("ordered_interests")
    @Expose
    val pinnedInterest: String? = null

)

data class UpdateUserDislikeInterests(
    @SerializedName("user_disliked_interests")
    @Expose
    val userDislikedInterests: String?=null,
)

data class UpdateLanguageRequest(
    @SerializedName("language")
    @Expose
    val language: String
)

data class UpdateUserState(
    @SerializedName("state")
    @Expose
    val state: String?
)

data class UpdateInterestsRequest(
    @SerializedName("interests")
    @Expose
    val interests: String
)

data class UpdateGEOPointsRequest(
    @SerializedName("latitude")
    @Expose
    val latitude: Double,
    @SerializedName("longitude")
    @Expose
    val longitude: Double
)

data class UpdateCryptoWatchlist(
    @SerializedName("crypto_watchlist")
    @Expose
    val crypto_watchlist: String
)


data class UpdateNotificationRequest(
    @SerializedName("cricket_notification")
    @Expose
    val cricket_notification: Boolean
)

data class FollowPublisherRequest(
    var publisher_id: String
)

data class FollowPublisherResponse(
    @SerializedName("follow-status")
    val follow_status: String
)

data class UpdateGoogleLogin(
    @SerializedName("social_login")
    @Expose
    val social_login: SocialLoginGoogle
)

data class SocialLoginGoogle(
    @SerializedName("google")
    @Expose
    val google: String
)

data class UpdateFbLogin(
    @SerializedName("social_login")
    @Expose
    val social_login: SocialLoginFb
)

data class SocialLoginFb(
    @SerializedName("facebook")
    @Expose
    val facebook: String
)

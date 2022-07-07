package com.appyhigh.newsfeedsdk.model

import android.os.Parcelable
import com.appyhigh.newsfeedsdk.Constants.LATITUDE
import com.appyhigh.newsfeedsdk.Constants.LONGITUDE
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    @SerializedName("first_name")
    @Expose
    var firstName: String? = null,

    @SerializedName("last_name")
    @Expose
    var lastName: String = "",

    @SerializedName("username")
    @Expose
    var username: String = "",

    @SerializedName("email")
    @Expose
    var email: String = "",

    @SerializedName("phone_number")
    @Expose
    var phoneNumber: String = "",

    @SerializedName("dailling_code")
    @Expose
    var dailling_code: String = "+91",

    @SerializedName("interests")
    var interests: ArrayList<String> = ArrayList(),

    @SerializedName("language")
    var languages: ArrayList<String> = ArrayList(),

    @SerializedName("user_disliked_interests")
    var userDislikeInterests: ArrayList<String> = ArrayList(),

    @SerializedName("cricket_notification")
    @Expose
    var cricket_notification: Boolean? = null,

    @SerializedName("crypto_watchlist")
    @Expose
    var crypto_watchlist: ArrayList<String>? = null,

    @SerializedName("impressions")
    @Expose
    var impressions: Impressions? = null,

    @SerializedName("ordered_interests")
    var pinnedInterests: ArrayList<String> = ArrayList(),

    @SerializedName("show_regional_field")
    var showRegionalField: Boolean = true,

    @SerializedName("state")
    var state: String? = null,

    @SerializedName("state_code")
    var stateCode: String? = null,

    @SerializedName("latitude")
    var latitude : Double? = null,

    @SerializedName("longitude")
    var longitude: Double? = null

) : Parcelable

@Parcelize
data class Impressions(
    @SerializedName("impression_time_interval_in_sec")
    @Expose
    var impression_time_interval_in_sec: Int = 60,

    @SerializedName("background_time_interval_in_min")
    @Expose
    var background_time_interval_in_min: Int = 240
) : Parcelable

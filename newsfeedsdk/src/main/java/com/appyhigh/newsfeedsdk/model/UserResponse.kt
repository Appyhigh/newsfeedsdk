package com.appyhigh.newsfeedsdk.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class UserResponse(
    @SerializedName("user") @Expose var user: User? = null
)
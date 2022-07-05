package com.appyhigh.newsfeedsdk.model

import com.google.gson.annotations.SerializedName
import java.util.*

data class InterestResponseModel(
    @SerializedName("interest_list")
    var interestList: List<Interest> = ArrayList<Interest>()
)

data class InterestStringResponseModel(
    @SerializedName("interest_list")
    var interestList: List<String> = ArrayList<String>()
)
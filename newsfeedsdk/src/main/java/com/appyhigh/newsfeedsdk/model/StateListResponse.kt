package com.appyhigh.newsfeedsdk.model


import com.google.gson.annotations.SerializedName
import com.google.gson.annotations.Expose

data class StateListResponse(
    @SerializedName("are_ads_enabled")
    @Expose
    var areAdsEnabled: Boolean = false,
    @SerializedName("cards")
    @Expose
    var cards: List<Card> = listOf()
) {
    data class Card(
        @SerializedName("card_type")
        @Expose
        var cardType: String = "",
        @SerializedName("items")
        @Expose
        var items: List<Item> = listOf()
    ) {
        data class Item(
            @SerializedName("state")
            @Expose
            var state: String = "",
            @SerializedName("state_code")
            @Expose
            var stateCode: String = ""
        )
    }
}
package com.appyhigh.newsfeedsdk.model.crypto


import com.google.gson.annotations.SerializedName
import com.google.gson.annotations.Expose

class CryptoSearchResponse : ArrayList<CryptoSearchResponse.CryptoSearchResponseItem>(){
    data class CryptoSearchResponseItem(
        @SerializedName("coin_id")
        @Expose
        var coinId: String = "",
        @SerializedName("coin_name")
        @Expose
        var coinName: String = "",
        @SerializedName("coin_symbol")
        @Expose
        var coinSymbol: String = "",
        @SerializedName("image")
        @Expose
        var image: String = ""
    )
}
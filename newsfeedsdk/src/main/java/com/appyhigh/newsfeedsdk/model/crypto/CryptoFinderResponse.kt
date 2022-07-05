package com.appyhigh.newsfeedsdk.model.crypto


import com.google.gson.annotations.SerializedName

class CryptoFinderResponse : ArrayList<CryptoFinderResponse.CryptoFinderResponseItem>(){
    data class CryptoFinderResponseItem(
        @SerializedName("base-currency-logoid")
        var baseCurrencyLogoid: String = "",
        @SerializedName("currency_code")
        var currencyCode: String = "",
        @SerializedName("currency-logoid")
        var currencyLogoid: String = "",
        @SerializedName("description")
        var description: String = "",
        @SerializedName("exchange")
        var exchange: String = "",
        @SerializedName("prefix")
        var prefix: String = "",
        @SerializedName("provider_id")
        var providerId: String = "",
        @SerializedName("symbol")
        var symbol: String = "",
        @SerializedName("type")
        var type: String = "",
        @SerializedName("typespecs")
        var typespecs: List<String> = listOf()
    )
}
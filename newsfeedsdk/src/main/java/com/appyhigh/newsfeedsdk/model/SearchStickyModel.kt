package com.appyhigh.newsfeedsdk.model
import com.google.gson.annotations.SerializedName

import com.google.gson.annotations.Expose


data class SearchStickyModel(
    @SerializedName("icons")
    @Expose
    val icons: ArrayList<String>? = arrayListOf("Messages","Email", "Alarm", "Flashlight"),
    @SerializedName("tint")
    @Expose
    var tint: String? = "#FFFFFF",
    @SerializedName("type")
    @Expose
    var type: String? = "solid",
    @SerializedName("backgroundType")
    @Expose
    var backgroundType: String? = "solid",
    @SerializedName("background")
    @Expose
    var background: String? = "default"
)


data class SearchStickyItemModel(
    val iconName:String = "",
    val icon:Int = 0,
    var isSelected:Boolean = false,
    val type: String = "",
)
package com.appyhigh.newsfeedsdk.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Thumbnail(
    @SerializedName("hdpi")
    var hdpi: String? = null,
    @SerializedName("xxxhdpi")
    var xxxhdpi: String? = null,
    @SerializedName("xxhdpi")
    var xxhdpi: String? = null,
    @SerializedName("mdpi")
    var mdpi: String? = null,
    @SerializedName("xhdpi")
    var xhdpi: String? = null
) : Parcelable
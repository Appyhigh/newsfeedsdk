package com.appyhigh.newsfeedsdk.model

import androidx.appcompat.widget.AppCompatImageView
import androidx.databinding.BindingAdapter
import com.appyhigh.newsfeedsdk.R
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class Language(
    @Expose
    @SerializedName("_id")
    var id: String,
    @Expose
    @SerializedName("language")
    var language: String = "",
    @Expose
    @SerializedName("sampleText")
    var sampleText: String = "",
    @SerializedName("isSelected")
    @Expose
    var isSelected: Boolean = false,
    @SerializedName("nativeName")
    @Expose
    var nativeName: String = ""
){
    companion object {
        @JvmStatic
        @BindingAdapter("selected")
        fun setSelectedRadio(view: AppCompatImageView, isSelected: Boolean) {
            if (isSelected) {
                view.setImageResource(R.drawable.ic_checkbox_selected)
            } else {
                view.setImageResource(R.drawable.ic_checkbox_unselected)
            }
        }
    }
}

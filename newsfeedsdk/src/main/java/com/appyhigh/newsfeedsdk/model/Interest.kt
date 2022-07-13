package com.appyhigh.newsfeedsdk.model

import android.graphics.Color
import android.graphics.Typeface
import android.os.Parcelable
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.bumptech.glide.Glide
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Interest(
    @SerializedName("interest")
    var label: String? = null,
    @SerializedName("key_id")
    var keyId: String? = null,
    @SerializedName("thumbnails")
    var thumbnails: Thumbnail? = null,
    var isSelected: Boolean = false,
    var userSelected: Boolean = false,
    @SerializedName("pwa_link")
    var pwaLink: String? = "",
    var isPinned: Boolean = false
) : Parcelable {
    companion object {
        @JvmStatic
        @BindingAdapter(value = ["interestImage"])
        fun loadImage(view: AppCompatImageView, imageUrl: String?) {
            Glide.with(view.context)
                .load(imageUrl)
                .into(view)
        }

        @JvmStatic
        @BindingAdapter(value = ["interestImage", "isSelected"], requireAll = false)
        fun loadImage(view: AppCompatImageView, imageUrl: String?, isSelected: Boolean) {
            Glide.with(view.context)
                .load(imageUrl)
                .into(view)
            if (isSelected) {
                view.setColorFilter(
                    Color.parseColor("#6CB6FF"),
                    android.graphics.PorterDuff.Mode.MULTIPLY
                )
            } else {
                view.setColorFilter(
                    Color.parseColor("#687690"),
                    android.graphics.PorterDuff.Mode.MULTIPLY
                )
            }
        }

        @JvmStatic
        @BindingAdapter("selectedText")
        fun setSelectedText(view: AppCompatTextView, selected: Boolean) {
            if (selected) {
                view.setTextColor(Color.parseColor("#6CB6FF"))
            } else {
                view.setTextColor(Color.parseColor("#687690"))
            }
        }

        @JvmStatic
        @BindingAdapter("selected")
        fun setBackgroundView(view: LinearLayout, selected: Boolean) {
            if (selected) {
                view.setBackgroundResource(R.drawable.bg_selected_preference)
            } else {
                view.setBackgroundResource(R.drawable.bg_unselected_preference)
            }
        }

        @JvmStatic
        @BindingAdapter("isSelected")
        fun setSelected(view: TextView, isSelected: Boolean) {
            if (isSelected) {
                view.setTextColor(ContextCompat.getColor(view.context, R.color.selected_tab_color))
                view.setTypeface(if (FeedSdk.font == null) null else FeedSdk.font, Typeface.BOLD)
            } else {
                view.setTextColor(
                    ContextCompat.getColor(
                        view.context,
                        R.color.un_selected_tab_color
                    )
                )
                view.setTypeface(if (FeedSdk.font == null) null else FeedSdk.font, Typeface.NORMAL)
            }
        }

        @JvmStatic
        @BindingAdapter("newInterestItem")
        fun setNewInterestItem(view: AppCompatTextView, interest: Interest) {
            try {
                val scale: Float = view.context.resources.displayMetrics.density
                val dp5 = (5 * scale + 0.5f).toInt()
                if(interest.userSelected){
                    val dp10 = (15 * scale + 0.5f).toInt()
                    view.setPadding(dp10, dp5, dp10, dp5)
                    view.setBackgroundResource(R.drawable.bg_round_blue_10)
                    view.text = view.context.getString(R.string.following)
                    view.setTextColor(Color.WHITE)
                } else{
                    val dp20 = (20 * scale + 0.5f).toInt()
                    view.setPadding(dp20, dp5, dp20, dp5)
                    view.setBackgroundResource(R.drawable.bg_round_follow_border)
                    view.text = view.context.getString(R.string.follow)
                    view.setTextColor(ContextCompat.getColor(view.context, R.color.purple_500))
                }
            } catch (ex: Exception) {
                LogDetail.LogEStack(ex)
            }
        }

        @JvmStatic
        @BindingAdapter("isPinned")
        fun setPinnedItem(view: AppCompatImageView, isPinned: Boolean) {
            try {
               if(isPinned){
                   view.setImageResource(R.drawable.ic_pin_selected)
               } else{
                   view.setImageResource(R.drawable.ic_pin_not_selected)
               }
            } catch (ex: Exception) {
                LogDetail.LogEStack(ex)
            }
        }

    }
}
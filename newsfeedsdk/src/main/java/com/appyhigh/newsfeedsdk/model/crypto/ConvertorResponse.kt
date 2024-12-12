package com.appyhigh.newsfeedsdk.model.crypto

import android.graphics.drawable.Drawable
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class ConvertorResponse(
    @SerializedName("cards")
    @Expose
    val cards: List<Card>
)
data class Card(
    @SerializedName("card_type")
    @Expose
    val card_type: String,
    @SerializedName("items")
    @Expose
    val items: List<Item>
)
data class CurrentPrice(
    @SerializedName("inr")
    @Expose
    val inr: Double,
    @SerializedName("usd")
    @Expose
    val usd: Double
)
data class Item(
    @SerializedName("coin_id")
    @Expose
    val coin_id: String,
    @SerializedName("coin_name")
    @Expose
    val coin_name: String,
    @SerializedName("coin_symbol")
    @Expose
    val coin_symbol: String,
    @SerializedName("current_price")
    @Expose
    val current_price: CurrentPrice,
    @SerializedName("image_link")
    @Expose
    val image_link: String
) {
    companion object{
        @JvmStatic
        @BindingAdapter("imageUrl")
        fun loadImage(
            view: AppCompatImageView,
            imageUrl: String?
        ) {
            if(imageUrl.isNullOrEmpty()){
                view.visibility = View.GONE
                return
            }
            Glide.with(view.context)
                .load(imageUrl)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        return true
                    }
                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable>,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        view.visibility = View.VISIBLE
                        view.setImageDrawable(resource)
                        return true
                    }
                })
                .into(view)
        }
    }
}
package com.appyhigh.newsfeedsdk.model.feeds

import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.cardview.widget.CardView
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.BindingAdapter
import coil.ImageLoader
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.model.Thumbnail
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView


data class Item(
    @SerializedName("_id")
    @Expose
    var id: String? = null,

    @SerializedName("link")
    @Expose
    var link: String? = "",

    @SerializedName("label")
    @Expose
    var label: String? = null,

    @SerializedName("on_hit")
    @Expose
    var onHit: String? = null,

    @SerializedName("source")
    @Expose
    var source: String? = null,

    @SerializedName("is_webview")
    @Expose
    var isWebview: Boolean? = null,

    @SerializedName("is_native")
    @Expose
    var isNative: Boolean = false,

    @SerializedName("publisher_id")
    @Expose
    var publisherId: String? = null,

    @SerializedName("post_id")
    @Expose
    var postId: String? = null,

    @SerializedName("platform")
    @Expose
    var platform: String? = null,

    @SerializedName("tags")
    @Expose
    var tags: List<String>? = null,

    @SerializedName("published_on")
    @Expose
    var publishedOn: String? = null,

    @SerializedName("ml_popularity_score")
    @Expose
    var mlPopularityScore: Double = 0.0,

    @SerializedName("ml_country")
    @Expose
    var mlCountry: List<String>? = null,

    @SerializedName("ml_language")
    @Expose
    var mlLanguage: List<String>? = null,

    @SerializedName("app_comments")
    @Expose
    var appComments: Int? = null,

    @SerializedName("short_video")
    @Expose
    var shortVideo: Boolean = false,

    @SerializedName("createdAt")
    @Expose
    var createdAt: String? = null,

    @SerializedName("updatedAt")
    @Expose
    var updatedAt: String? = null,

    @SerializedName("is_video")
    @Expose
    var isVideo: Boolean? = null,

    @SerializedName("language_string")
    @Expose
    var languageString: String? = null,

    @SerializedName("reactions")
    @Expose
    var reactions: Reactions? = null,

    @SerializedName("reactions_count")
    @Expose
    var reactionsCount: ReactionsCount? = null,

    @SerializedName("content")
    @Expose
    var content: Content? = null,

    @SerializedName("default_reactions_count")
    @Expose
    var defaultReactionsCount: DefaultReactionsCount? = null,

    @SerializedName("post_source")
    @Expose
    var postSource: String? = null,

    @SerializedName("feed_type")
    @Expose
    var feedType: String? = null,

    @SerializedName("is_reacted")
    @Expose
    var isReacted: String? = null,

    @SerializedName("publisher_name")
    @Expose
    var publisherName: String? = null,

    @SerializedName("publisher_profile_pic")
    @Expose
    var publisherProfilePic: String? = null,

    @SerializedName("profile_pic")
    @Expose
    var profilePic: String? = null,

    @SerializedName("fullname")
    @Expose
    var fullname: String? = null,

    @SerializedName("publisher_contact_us")
    @Expose
    var publisherContactUs: String? = null,

    @SerializedName("publisher_website")
    @Expose
    var publisherWebsite: String? = null,

    @SerializedName("interests")
    @Expose
    var interests: List<String>? = null,

    @SerializedName("is_bookmarked")
    @Expose
    var isBookmarked: Boolean? = null,

    @SerializedName("is_following_publisher")
    @Expose
    var isFollowingPublisher: Boolean? = null,

    @SerializedName("hashtags")
    @Expose
    var hashtags: List<Any>? = null,

    @SerializedName("card_type")
    @Expose
    var cardType: String? = null,

    @SerializedName("interest")
    @Expose
    var interest: String? = null,

    @SerializedName("key_id")
    @Expose
    var key_id: String? = null,

    @SerializedName("thumbnails")
    @Expose
    var thumbnails: Thumbnail? = null,

    @SerializedName("language")
    @Expose
    var language: String? = null,

    @SerializedName("country_code")
    @Expose
    var country_code: String? = null,

    @SerializedName("nativeName")
    @Expose
    var nativeName: String? = null,

    @SerializedName("sampleText")
    @Expose
    var sampleText: String? = null,
    @SerializedName("value")
    @Expose
    var value: String? = null,

    @SerializedName("selected")
    @Expose
    var selected: Boolean = false,

    @SerializedName("teama")
    @Expose
    var teama: String = "",
    @SerializedName("teamb")
    @Expose
    var teamb: String = "",
    @SerializedName("teama_Id")
    @Expose
    var teama_Id: String = "",
    @SerializedName("teamb_Id")
    @Expose
    var teamb_Id: String = "",
    @SerializedName("inn_team_1")
    @Expose
    var inn_team_1: String = "",
    @SerializedName("inn_team_2")
    @Expose
    var inn_team_2: String = "",
    @SerializedName("teama_image")
    @Expose
    var teama_image: String = "",
    @SerializedName("teamb_image")
    @Expose
    var teamb_image: String = "",
    @SerializedName("seriesname")
    @Expose
    var seriesname: String = "",
    @SerializedName("matchstatus")
    @Expose
    var matchstatus: String = "",
    @SerializedName("matchresult")
    @Expose
    var matchresult: String = "",
    @SerializedName("matchdate_ist")
    @Expose
    var matchdate_ist: String = "",
    @SerializedName("matchdate_local")
    @Expose
    var matchdate_local: String = "",
    @SerializedName("matchdate_gmt")
    @Expose
    var matchdate_gmt: String = "",
    @SerializedName("matchtime_gmt")
    @Expose
    var matchtime_gmt: String = "",
    @SerializedName("inn_score_1")
    @Expose
    var inn_score_1: String = "",
    @SerializedName("inn_score_2")
    @Expose
    var inn_score_2: String = "",
    @SerializedName("inn_score_3")
    @Expose
    var inn_score_3: String = "",
    @SerializedName("equation")
    @Expose
    var equation: String = "",
    @SerializedName("inn_score_4")
    @Expose
    var inn_score_4: String = "",
    @SerializedName("Day")
    @Expose
    var day: String = "",
    @SerializedName("Session")
    @Expose
    var session: String = "",
    @SerializedName("current_batting_team")
    @Expose
    val currentBattingTeam: String? = null,
    @SerializedName("matchfile")
    @Expose
    val matchfile: String? = null,
    @SerializedName("pwa_url")
    @Expose
    val pwaUrl: String? = null,
    @SerializedName("pwa_link")
    @Expose
    val pwaLink: String = "",
    @SerializedName("league")
    @Expose
    val league: String? = null,
    @SerializedName("team")
    @Expose
    val team: String? = null,
    @SerializedName("matches")
    @Expose
    val matches: String? = null,
    @SerializedName("team_image")
    @Expose
    val teamImage: String? = null,
    @SerializedName("win")
    @Expose
    val win: String? = null,
    @SerializedName("loss")
    @Expose
    val loss: String? = null,
    @SerializedName("position")
    @Expose
    val position: String? = null,
    @SerializedName("points")
    @Expose
    val points: String? = null,
    @SerializedName("nrr")
    @Expose
    val nrr: String? = null,
    @SerializedName("tied")
    @Expose
    val tied: String? = null,
    @SerializedName("is_qualified")
    @Expose
    val isQualified: String? = null,
    @SerializedName("are_ads_enabled")
    @Expose
    val areAdsEnabled: Boolean? = null,

    //crypto
    @SerializedName("coin_id")
    @Expose
    val coinId: String? = null,
    @SerializedName("coin_name")
    @Expose
    val coinName: String? = null,
    @SerializedName("coin_symbol")
    @Expose
    val coinSymbol: String? = null,
    @SerializedName("inr")
    @Expose
    val inr: Inr? = null,
    @SerializedName("updated_at")
    @Expose
    val updated_at: String? = null,
    @SerializedName("usd")
    @Expose
    val usd: Inr? = null,
    @SerializedName("prices")
    @Expose
    val prices: List<Double>? = ArrayList(),
    @SerializedName("timestamps")
    @Expose
    val timestamps: List<String>? = ArrayList(),
    @SerializedName("images")
    @Expose
    val images: List<String>? = ArrayList(),
    @SerializedName("current_price")
    @Expose
    val current_price: Double = 0.0,
    @SerializedName("percentage_change")
    @Expose
    val percentage_change: Double = 0.0,

    @SerializedName("24h_change")
    @Expose
    val hChange: Double = 0.0,
    @SerializedName("24h_vol")
    @Expose
    val hVol: Double = 0.0,
    @SerializedName("market_cap")
    @Expose
    val marketCap: Double = 0.0,
    @SerializedName("market_cap_rank")
    @Expose
    val marketCapRank: Int = 0,
    @SerializedName("liquidity_score")
    @Expose
    val liquidityScore: Double = 0.0,
    @SerializedName("all_time_high")
    @Expose
    val allTimeHigh: Double = 0.0,
    @SerializedName("all_time_low")
    @Expose
    val allTimeLow: Double = 0.0,
    @SerializedName("links")
    @Expose
    val links: Links? = null,
    @SerializedName("markets")
    @Expose
    val markets: List<Market>? = ArrayList<Market>(),

    //crypto alerts
    @SerializedName("alert_id")
    @Expose
    val alertId: String = "",
    @SerializedName("alert_status")
    @Expose
    val alertStatus: String = "",
    @SerializedName("lower_threshold")
    @Expose
    val lowerThreshold: Double? = null,
    @SerializedName("upper_threshold")
    @Expose
    val upperThreshold: Double? = null,
    @SerializedName("image_link")
    @Expose
    var imageLink: String? = null,

    //feed covid tracker
    @SerializedName("dropdown_items")
    @Expose
    val dropdownItems: List<String> = ArrayList(),
    @SerializedName("scrollable_tabs")
    @Expose
    val scrollableTabs: List<Item> = ArrayList(),
    @SerializedName("covid_data")
    @Expose
    val covidData: List<CovidData> = ArrayList(),
    @SerializedName("last_updated_on")
    @Expose
    val lastUpdatedOn: String = "",
    @SerializedName("covid_country_index")
    @Expose
    val covidCountryIndex: Int = 0,
    @SerializedName("covid_initial_state_index")
    @Expose
    val covidInitialStateIndex: Int = 0
) {
    /**
     * Reactions received on a post
     */
    class ReactionsCount {
        @SerializedName("like_count")
        var likeCount = 0

        @SerializedName("love_count")
        var loveCount = 0

        @SerializedName("wow_count")
        var wowCount = 0

        @SerializedName("angry_count")
        var angryCount = 0

        @SerializedName("laugh_count")
        var laughCount = 0

        @SerializedName("sad_count")
        var sadCount = 0
    }

    data class Market(
        @SerializedName("base")
        @Expose
        val base: String? = null,
        @SerializedName("last")
        @Expose
        val last: Double = 0.0,
        @SerializedName("market_name")
        @Expose
        val marketName: String? = null,
        @SerializedName("target")
        @Expose
        val target: String? = null,
        @SerializedName("trade_url")
        @Expose
        val tradeUrl: String? = null,
        @SerializedName("trust_score")
        @Expose
        val trustScore: String? = null,
        @SerializedName("volume")
        @Expose
        val volume: Double = 0.0
    )

    data class Links(
        @SerializedName("blockchain_site")
        @Expose
        val blockchainSite: List<String> = ArrayList(),
        @SerializedName("homepage")
        @Expose
        val homepage: String? = null
    )

    data class Inr(
        @SerializedName("curr_price")
        @Expose
        val currPrice: Double? = null,
        @SerializedName("24h_change")
        @Expose
        val hChange: Double = 0.0,
        @SerializedName("24h_vol")
        @Expose
        val hVol: Double? = null,
        @SerializedName("market_cap")
        @Expose
        val marketCap: Double = 0.0
    )

    data class CovidData(
        @SerializedName("state")
        @Expose
        val state: String? = null,
        @SerializedName("today")
        @Expose
        val today: DetailedCovidData? = null,
        @SerializedName("total")
        @Expose
        val total: DetailedCovidData? = null,
        @SerializedName("updatedAt")
        @Expose
        val updatedAt: String? = null
    )

    data class DetailedCovidData(
        @SerializedName("confirmed")
        @Expose
        val confirmed: Int = 0,
        @SerializedName("deceased")
        @Expose
        val deceased: Int = 0,
        @SerializedName("recovered")
        @Expose
        val recovered: Int = 0,
        @SerializedName("tested")
        @Expose
        val tested: Int = 0,
        @SerializedName("vaccinated1")
        @Expose
        val vaccinated1: Int = 0,
        @SerializedName("vaccinated2")
        @Expose
        val vaccinated2: Int = 0
    )

    companion object {

        @JvmStatic
        @BindingAdapter("tabVisibility")
        fun tabVisibility(view: View, selected: Boolean) {
            if (selected) {
                view.visibility = View.VISIBLE
            } else {
                view.visibility = View.GONE
            }
        }

        @JvmStatic
        @BindingAdapter("visible")
        fun setVisibility(view: View, isVisible: Boolean) {
            if (isVisible) {
                view.visibility = View.VISIBLE
            } else {
                view.visibility = View.GONE
            }
        }

        @JvmStatic
        @BindingAdapter("imageUrl")
        fun setImage(view: AppCompatImageView, imageUrl: String?) {
            try {
                Glide.with(view.context)
                    .apply { RequestOptions().override(200) }
                    .load(imageUrl)
                    .placeholder(R.drawable.placeholder)
                    .into(view)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        @JvmStatic
        @BindingAdapter("imageUrl")
        fun setImage(view: CircleImageView, imageUrl: String?) {
            try {
                Glide.with(view.context)
                    .apply { RequestOptions().override(200) }
                    .load(imageUrl)
                    .placeholder(R.drawable.placeholder)
                    .into(view)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        @JvmStatic
        @BindingAdapter(value = ["imageUrl", "publisherName", "parentView"], requireAll = true)
        fun setImage(
            circleImageView: CircleImageView,
            imageUrl: String?,
            publisherName: String?,
            parentView: Int
        ) {
            try {
                if(!imageUrl.isNullOrEmpty() && imageUrl.contains(".svg")){
                    val imageLoader = ImageLoader.Builder( circleImageView.context)
                        .componentRegistry { add(SvgDecoder(circleImageView.context)) }
                        .build()

                    val request = ImageRequest.Builder(circleImageView.context)
                        .crossfade(true)
                        .crossfade(500)
                        .placeholder(R.drawable.placeholder)
                        .data(imageUrl)
                        .target(
                            onSuccess = {
                                circleImageView.setImageDrawable(it)
                            },
                            onError = {
                                val parent =
                                    (circleImageView.parent as CardView).findViewById<CardView>(
                                        parentView
                                    )
                                val rlImage = parent.findViewById<RelativeLayout>(R.id.rlImage)
                                val tvImage = parent.findViewById<AppCompatTextView>(R.id.tvImage)
                                circleImageView.visibility = View.GONE
                                rlImage.visibility = View.VISIBLE
                                tvImage.text = publisherName!!.substring(0, 1).uppercase()
                            }
                        )
                        .build()

                    imageLoader.enqueue(request)
                } else{
                    Glide.with(circleImageView.context)
                        .load(imageUrl)
                        .apply(RequestOptions().override(100))
                        .placeholder(R.drawable.placeholder)
                        .listener(object : RequestListener<Drawable> {
                            override fun onLoadFailed(
                                exception: GlideException?,
                                model: Any?,
                                target: Target<Drawable>?,
                                isFirstResource: Boolean
                            ): Boolean {
                                exception?.printStackTrace()
                                imageUrl?.let {
                                    Picasso.get()
                                        .load(imageUrl)
                                        .noFade()
                                        .into(circleImageView, object : Callback {
                                            override fun onSuccess() {}
                                            override fun onError(e: java.lang.Exception?) {
                                                val parent =
                                                    (circleImageView.parent as CardView).findViewById<CardView>(
                                                        parentView
                                                    )
                                                val rlImage =
                                                    parent.findViewById<RelativeLayout>(R.id.rlImage)
                                                val tvImage =
                                                    parent.findViewById<AppCompatTextView>(R.id.tvImage)
                                                circleImageView.visibility = View.GONE
                                                rlImage.visibility = View.VISIBLE
                                                tvImage.text =
                                                    publisherName!!.substring(0, 1).uppercase()
                                            }
                                        })
                                }
                                return true
                            }

                            override fun onResourceReady(
                                resource: Drawable?,
                                model: Any?,
                                target: Target<Drawable>?,
                                dataSource: DataSource?,
                                isFirstResource: Boolean
                            ): Boolean {
                                val parent =
                                    (circleImageView.parent as CardView).findViewById<CardView>(
                                        parentView
                                    )
                                circleImageView.visibility = View.VISIBLE
                                val rlImage = parent.findViewById<RelativeLayout>(R.id.rlImage)
                                rlImage.visibility = View.GONE
                                circleImageView.setImageDrawable(resource)
                                return true
                            }

                        })
                        .into(circleImageView)
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        @JvmStatic
        @BindingAdapter(value = ["applyFont", "isBold"], requireAll = false)
        fun setFontFamily(view: TextView?, applyFont: Int, isBold: Boolean = false) {
            try{
                if(FeedSdk.font==null){
                    if(isBold){
                        view!!.setTypeface(ResourcesCompat.getFont(view.context, applyFont), Typeface.BOLD)
                    } else{
                        view!!.typeface = ResourcesCompat.getFont(view.context, applyFont)
                    }
                } else{
                    if(isBold){
                        view!!.setTypeface(FeedSdk.font, Typeface.BOLD)
                    } else{
                        view!!.typeface = FeedSdk.font
                    }
                }
            } catch (ex:java.lang.Exception){
                ex.printStackTrace()
            }
        }
    }
}
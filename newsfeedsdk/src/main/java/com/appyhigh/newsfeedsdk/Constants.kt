package com.appyhigh.newsfeedsdk

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.view.View
import android.widget.*
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.appyhigh.newsfeedsdk.activity.WebActivity
import com.appyhigh.newsfeedsdk.apicalls.ApiConfig
import com.appyhigh.newsfeedsdk.callbacks.GlideCallbackListener
import com.appyhigh.newsfeedsdk.callbacks.PWATabSelectedListener
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.model.*
import com.appyhigh.newsfeedsdk.model.feeds.Card
import com.appyhigh.newsfeedsdk.model.feeds.Item
import com.appyhigh.newsfeedsdk.utils.SpUtil
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.gson.annotations.SerializedName
import im.delight.android.webview.AdvancedWebView
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import kotlin.math.pow


object Constants {
    const val AUTHORIZATION = "Authorization"
    const val SOCKET_URL = "https://live-cricket-scores-prod.apyhi.com"
    const val OS_PLATFORM = "android"
    const val IAT = "IssuedAtTime"
    const val GUEST_USER_SDK = "GUEST_USER_SDK"
    const val SEARCH_STICKY_IAT = "SearchStickyIssuedAtTime"
    const val APP_ID = "app_id"
    const val USER_ID = "user_id"
    const val EMAIL = "email"
    const val PHONE_NUMBER = "phone_number"
    const val TAG = "tag"
    const val POST_ID = "post_id"
    const val LANG = "lang"
    const val EN = "en"
    const val JWT_TOKEN = "JWT_TOKEN"
    const val SEARCH_STICKY_JWT_TOKEN = "SEARCH_STICKY_JWT_TOKEN"
    const val IS_LOCATION_ASKED = "IS_LOCATION_ASKED"
    const val IS_GEO_POINTS_UPDATED = "IS_GEO_POINTS_UPDATED"
    const val DEVICE_MODEL = "device_model"
    const val NETWORK = "network"
    const val SHARE_MESSAGE = "SHARE_MESSAGE"
    const val PUSH_TOKEN = "PUSH_TOKEN"
    const val IS_RATING_GIVEN = "IS_RATING_GIVEN"
    const val IS_ALREADY_SHARED = "IS_ALREADY_SHARED"
    const val SESSION_NUMBER = "SESSION_NUMBER"
    const val IS_ALREADY_RATED = "IS_ALREADY_RATED"
    const val IS_FIRST_ASK = "IS_FIRST_ASK"
    const val RATING_THRESHOLD = "RATING_THRESHOLD"
    const val OPEN_COUNT = "OPEN_COUNT"
    const val NEWS_FEED_APP_ID = "news_feed_app_id"
    const val APP_NAME = "app_name"
    const val FEED_TARGET_ACTIVITY = "feed_target_activity"
    const val FEED_APP_ICON = "feed_app_icon"
    const val FEED_DEBUGGER = "feeds_debugger"
    const val COUNTRY_CODE = "country_code"
    const val LATITUDE = "lat"
    const val LONGITUDE = "long"
    const val COUNTRY = "country"
    const val ANOTHER_INTEREST = "another_interest"
    const val INTERESTS = "interests"
    const val INTEREST = "interest"
    const val POSITION = "position"
    const val FROM_APP = "from_app"
    const val SKIP = "skip"
    const val IS_VIDEO = "is_video"
    const val SHORT_VIDEO = "short_video"
    const val PAGE_NUMBER = "page_number"
    const val PAGE = "page"
    const val PUBLISHER_ID = "publisher_id"
    const val PUBLISHER_IDS = "publisher_ids"
    const val LANGUAGE = "language"
    const val FEED_TYPE = "feed_type"
    const val FEED_ID = "feed_id"
    const val IS_NATIVE = "is_native"
    const val TRUE = "true"
    const val COVID_CARD = "covid_card"
    const val PODCAST_ID = "podcast_id"
    const val PUSH_SOURCE = "push_source"
    const val FEEDSDK = "feedsdk"
    const val WHICH = "which"
    const val FROM_STICKY = "fromSticky"
    const val FROM_LIVE_MATCH = "fromLiveMatch"
    const val SDK_EXPLORE = "SDK://explore"
    const val SDK_REELS = "SDK://reels"
    const val SDK_PODCAST_DETAIL = "SDK://podcastDetail"
    const val SDK_POST_DETAIL = "SDK://feedDetail"
    const val SDK_CRYPTO_DETAIL = "SDK://cryptoCoinDetail"
    const val SDK_CRICKET_DETAIL = "SDK://cricketMatchDetail"
    const val POST_SOURCE = "post_source"
    const val BLOCKED_PUBLISHERS = "blocked_publishers"
    const val IPL_PUSH = "ipl_push"
    const val STATE = "state"
    const val STATE_CODE = "state_code"
    const val FIRST_POST_ID = "first_post"
    const val RATING = "rating"
    const val SHARE = "share"
    const val LOADER = "load_more"
    const val AD = "ad"
    const val AD_LARGE = "ad_large"
    const val PROFILE_PIC = "profile_pic"
    const val FULL_NAME = "fullname"
    const val USER_NAME = "username"
    const val IS_FOLLOWING_PUBLISHER = "is_following_publisher"
    const val PUBLISHER_CONTACT = "publisher_contact"
    const val SCREEN_TYPE = "screenType"
    const val FEED = "feed"
    const val EXPLORE = "explore"
    const val REELS = "reels"
    const val VIDEO_FEED = "videoFeed"
    const val HASHTAG = "hashtag"
    const val ALREADY_EXISTS = "already_exists"
    const val MATCH_TYPE = "match_type"
    const val FILENAME = "filename"
    const val LAUNCHTYPE = "launchType"
    const val CRICKET = "cricket"
    const val MATCHTYPE = "matchType"
    const val PWA = "pwa"
    const val MATCHES_MODE = "matchesMode"
    const val TAB = "tab"
    const val INNINGS = "innings"
    const val OVER = "over"
    const val WATCHLIST = "watchlist"
    const val CURRENCY = "currency"
    const val LOWER_THRESHOLD = "lower_threshold"
    const val UPPER_THRESHOLD = "upper_threshold"
    const val ALERT_ID = "alert_id"
    const val ORDER = "order"
    const val COIN_ID = "coin_id"
    const val LIVE_MATCHES = "live_matches"
    const val PAST_MATCHES = "past_matches"
    const val UPCOMING_MATCHES = "upcoming_matches"
    const val LIVE_MATCHES_VIEW = "live_matches_view"
    const val CRICKET_HOME_VIEW = "cricket_home_view"
    var isVideoFromSticky = false
    var videoUnitAdFromSticky = ""
    const val STICKY_NOTIFICATION = "search_sticky_notification"
    const val STICKY_BG = "sticky_bg"
    const val WEB_PLATFORMS = "search_web_platforms"
    const val IS_STICKY_NOTIFICATION_ON = "is_sticky_notification_on"
    const val IS_STICKY_SERVICE_ON = "is_sticky_service_on"
    const val PLAYER_RANKING_FRAGMENT = 0
    const val POINTS_TABLE_FRAGMENT = 1
    const val SOCKET_SERIES = "socket_series"
    const val FEED_NATIVE_AD_INTEVRAL = "feed_native_ad_interval"
    const val WEB_HISTORY = "web_history"
    const val CRYPTO_PODCASTS = "crypto_podcasts"
    const val SEARCH_FEED_SMALL = "search_feed_small"
    const val SEARCH_FEED_BIG = "search_feed_big"
    const val CRYPTO_ALERT_SELECT = "crypto_alert_select"
    const val CRYPTO_CONVERTER = "crypto_converter"
    const val COIN_ID_LIST = "coin_id_list"
    const val IS_ALREADY_JOINED = "IS_ALREADY_JOINED"
    const val TELEGRAM_CHANNEL = "telegram_channel"
    const val API_URl = "apiURL"
    const val API_METHOD = "apiMethod"
    const val API_INTERNAL = "apiInternal"
    const val PUSH_TOKEN_ = "push_token"
    const val FIRST_NAME = "first_name"
    const val LAST_NAME = "last_name"
    const val DAILLING_CODE = "dailling_code"
    const val API_DATA = "apiData"
    const val API_HEADER = "apiHeader"
    const val API_CALLING = "apiCalling"
    const val USER_DETAIL = "userDetail"
    const val DEVICE_DETAIL = "deviceDetail"
    const val POST = "POST"
    const val GET = "GET"
    const val CONFIG_MODEL = "config_model"
    const val PRIVACY_ACCEPTED = "privacy_accepted"


    var isChecked = true
    var impreesionModel: Impressions? = null
//    val colorsArray = arrayListOf(R.color.gnt_one, R.color.gnt_two, R.color.gnt_three, R.color.gnt_four, R.color.gnt_five, R.color.gnt_six,
//            R.color.gnt_one, R.color.gnt_two, R.color.gnt_three, R.color.gnt_four, R.color.gnt_five, R.color.gnt_six,
//            R.color.gnt_one, R.color.gnt_two, R.color.gnt_three, R.color.gnt_four, R.color.gnt_five, R.color.gnt_six,
//            R.color.gnt_one, R.color.gnt_two, R.color.gnt_three, R.color.gnt_four, R.color.gnt_five, R.color.gnt_six)

    enum class NetworkState(val isConnected : Boolean) {

        CONNECTED(true),

        DISCONNECTED(false),

        UNINITIALIZED(false)

    }

    fun Context.getLifecycleOwner(): LifecycleOwner {
        return try {
            this as LifecycleOwner
        } catch (exception: ClassCastException) {
            (this as ContextWrapper).baseContext as LifecycleOwner
        }
    }

    enum class ReactionType {
        @SerializedName("like")
        LIKE,

        @SerializedName("love")
        LOVE,

        @SerializedName("laugh")
        LAUGH,

        @SerializedName("wow")
        WOW,

        @SerializedName("angry")
        ANGRY,

        @SerializedName("sad")
        SAD,

        @SerializedName("none")
        NONE
    }

    enum class SocketEvent {
        @SerializedName("connect")
        CONNECT,

        @SerializedName("connect_error")
        CONNECT_ERROR,

        @SerializedName("connecting")
        CONNECTING,

        @SerializedName("reconnect")
        RECONNECT,

        @SerializedName("connect_timeout")
        CONNECT_TIMEOUT,

        @SerializedName("reconnect_attempt")
        RECONNECT_ATTEMPT,

        @SerializedName("reconnecting")
        RECONNECTING,

        @SerializedName("reconnect_error")
        RECONNECT_ERROR,

        @SerializedName("disconnect")
        DISCONNECT,

        @SerializedName("scores")
        SCORES
    }

    enum class ACTION {
        STARTFOREGROUND,
        STOPFOREGROUND
    }

    enum class CardType {
        @SerializedName("news_big_feature")
        NEWS_BIG_FEATURE,

        @SerializedName("news_small_feature")
        NEWS_SMALL_FEATURE,

        @SerializedName("news_regional")
        NEWS_REGIONAL,

        @SerializedName("news_tweet")
        NEWS_TWEET,

        @SerializedName("media_image")
        MEDIA_IMAGE,

        @SerializedName("media_video")
        MEDIA_VIDEO,

        @SerializedName("media_video_big")
        MEDIA_VIDEO_BIG,

        @SerializedName("media_grid_view")
        MEDIA_GRID_VIEW,

        @SerializedName("media_carousel")
        MEDIA_CAROUSEL,

        @SerializedName("feed_hashtags")
        FEED_HASHTAGS,

        @SerializedName("feed_publishers")
        FEED_PUBLISHERS,

        @SerializedName("feed_reels")
        FEED_REELS,

        @SerializedName("get_feeds_reels")
        GET_FEEDS_REELS,

        @SerializedName("feed_videos_horizontal")
        FEED_VIDEOS_HORIZONTAL,

        @SerializedName("get_feeds_videos")
        GET_FEEDS_VIDEOS,

        @SerializedName("feed_posts_category")
        FEED_POSTS_CATEGORY,

        @SerializedName("cricket_trending_posts")
        CRICKET_TRENDING_POSTS,

        @SerializedName("feed_posts_horizontal")
        FEED_POSTS_HORIZONTAL,

        @SerializedName("feed_icon_hashtags")
        FEED_ICON_HASHTAGS,

        @SerializedName("title_icon")
        TITLE_ICON,

        @SerializedName("title")
        TITLE,

        @SerializedName("description")
        DESCRIPTION,

        @SerializedName("daily_share_image")
        DAILY_SHARE_IMAGE,

        @SerializedName("feed_interests")
        FEED_INTERESTS,

        @SerializedName("feed_language")
        FEED_LANGUAGE,

        @SerializedName("media_podcast")
        MEDIA_PODCAST,

        @SerializedName("feed_icon_hashtags_circle")
        FEED_ICON_HASHTAGS_CIRCLE,

        @SerializedName("crypto_watchlist")
        CRYPTO_WATCHLIST,

        @SerializedName("crypto_gainers")
        CRYPTO_GAINERS,

        @SerializedName("crypto_losers")
        CRYPTO_LOSERS,

        @SerializedName("coin_links")
        COIN_LINKS,

        @SerializedName("coin_stats")
        COIN_STATS,

        @SerializedName("coin_markets")
        COIN_MARKETS,

        @SerializedName("feed_covid_tracker")
        FEED_COVID_TRACKER,

        @SerializedName("crypto_alert")
        CRYPTO_ALERT,

        @SerializedName("feed_you_make_like_interests")
        FEED_YOU_MAKE_LIKE_INTERESTS
    }

    var reels = ArrayList<Card>()
    var bigBites = ArrayList<Card>()
    var isMuted = true
    var exploreInterest: String = ""
    var exploreLanguages: String? = ""
    var isImpressionApiHit = false
    var postDetailPageNo = 0
    val exploreResponseDetails = PostImpressionsModel()
    val feedsResponseDetails = PostImpressionsModel()
    var cardsMap = HashMap<String, ArrayList<Card>>()
    var allInterestsMap = LinkedHashMap<String, Interest>()
    var allLanguagesMap = LinkedHashMap<String, Language>()
    var stateMap = LinkedHashMap<String, String>()
    var postDetailCards = ArrayList<PostDetailsModel>()
    var selectedLanguagesMap = HashMap<String, Language>()
    var showLiveMatchSmall = true
    var liveMatchResponse: CricketScheduleResponse? = null
    var searchStickyModel = SearchStickyModel()
    var stickyBackgroundSelected = 0
    var cryptoWatchListMap = HashMap<String, String>()
    var cryptoWatchList = ArrayList<Item>()
    var cricketLiveMatchURI = ""
    var cricketUpcomingMatchURI = ""
    var cricketPastMatchURI = ""
    var nativePageCount = 0
    var pwaWebViews = HashMap<String, AdvancedWebView>()
    var currentCryptoDetailCoinId = ""
    val nativeAdLifecycleCallbacks = HashMap<LinearLayout, NativeAdItem>()
    var userDetails: User? = null
    var pwaTabListeners = HashMap<String, PWATabSelectedListener>()

    fun getWidgetImage(isColored: Boolean, widget: String): Int {
        return when (widget) {
            "Camera" -> if (isColored) R.drawable.ic_sticky_color_camera else R.drawable.ic_sticky_solid_camera
            "Whatsapp" -> if (isColored) R.drawable.ic_sticky_color_whatsapp else R.drawable.ic_sticky_solid_whatsapp
            "Weather" -> if (isColored) R.drawable.ic_sticky_color_weather else R.drawable.ic_sticky_solid_weather
            "Call" -> if (isColored) R.drawable.ic_sticky_color_call else R.drawable.ic_sticky_solid_call
            "Calendar" -> if (isColored) R.drawable.ic_sticky_color_calendar else R.drawable.ic_sticky_solid_calendar
            "News" -> if (isColored) R.drawable.ic_sticky_color_feed else R.drawable.ic_sticky_solid_feed
            "Video" -> if (isColored) R.drawable.ic_sticky_color_reels else R.drawable.ic_sticky_solid_reels
            "Messages" -> if (isColored) R.drawable.ic_sticky_color_msg else R.drawable.ic_sticky_solid_msg
            "Email" -> if (isColored) R.drawable.ic_sticky_color_gmail else R.drawable.ic_sticky_solid_gmail
            "Alarm" -> if (isColored) R.drawable.ic_sticky_color_alarm else R.drawable.ic_sticky_solid_alarm
            "Flashlight" -> if (isColored) R.drawable.ic_sticky_color_flash_off else R.drawable.ic_sticky_solid_flash_off
            "FlashlightOn" -> if (isColored) R.drawable.ic_sticky_color_flash_on else R.drawable.ic_sticky_solid_flash_on
            else -> R.drawable.ic_widget
        }
    }

    fun getStickyBackground(type: String, background: String): Int {
        if (type == "color") {
            return when (background) {
                "gaming_0" -> R.drawable.bg_sticky_gaming_0
                "gaming_1" -> R.drawable.bg_sticky_gaming_1
                "gaming_2" -> R.drawable.bg_sticky_gaming_2
                "gaming_3" -> R.drawable.bg_sticky_gaming_3
                "gaming_4" -> R.drawable.bg_sticky_gaming_4
                "fashion_0" -> R.drawable.bg_sticky_fashion_0
                "fashion_1" -> R.drawable.bg_sticky_fashion_1
                "fashion_2" -> R.drawable.bg_sticky_fashion_2
                "fashion_3" -> R.drawable.bg_sticky_fashion_3
                "beauty_0" -> R.drawable.bg_sticky_beauty_0
                "beauty_1" -> R.drawable.bg_sticky_beauty_1
                "beauty_2" -> R.drawable.bg_sticky_beauty_2
                "beauty_3" -> R.drawable.bg_sticky_beauty_3
                "beauty_4" -> R.drawable.bg_sticky_beauty_4
                "education_0" -> R.drawable.bg_sticky_education_0
                "education_1" -> R.drawable.bg_sticky_education_1
                "education_2" -> R.drawable.bg_sticky_education_2
                "education_3" -> R.drawable.bg_sticky_education_3
                "education_4" -> R.drawable.bg_sticky_education_4
                "tech_0" -> R.drawable.bg_sticky_tech_0
                "tech_1" -> R.drawable.bg_sticky_tech_1
                "tech_2" -> R.drawable.bg_sticky_tech_2
                "tech_3" -> R.drawable.bg_sticky_tech_3
                "glass_0" -> R.drawable.bg_sticky_glass_0
                "glass_1" -> R.drawable.bg_sticky_glass_1
                "glass_2" -> R.drawable.bg_sticky_glass_2
                "glass_3" -> R.drawable.bg_sticky_glass_3
                "glass_4" -> R.drawable.bg_sticky_glass_4
                "miscellaneous_0" -> R.drawable.bg_sticky_misc_0
                "miscellaneous_1" -> R.drawable.bg_sticky_misc_1
                "miscellaneous_2" -> R.drawable.bg_sticky_misc_2
                "miscellaneous_3" -> R.drawable.bg_sticky_misc_3
                "miscellaneous_4" -> R.drawable.bg_sticky_misc_4
                "miscellaneous_5" -> R.drawable.bg_sticky_misc_5
                else -> R.drawable.bg_sticky_color_default
            }
        } else {
            return when (background) {
                "solid_0" -> Color.parseColor("#000000")
                "solid_1" -> Color.parseColor("#28292B")
                "solid_2" -> Color.parseColor("#D5D7DB")
                "solid_3" -> Color.parseColor("#28292B")
                "solid_4" -> Color.parseColor("#707070")
                "solid_5" -> Color.parseColor("#637691")
                "solid_6" -> Color.parseColor("#C1CCE3")
                "solid_7" -> Color.parseColor("#1A3265")
                "solid_8" -> Color.parseColor("#2F3EEF")
                "solid_9" -> Color.parseColor("#0965E0")
                "solid_10" -> Color.parseColor("#077406")
                "solid_11" -> Color.parseColor("#34A853")
                "solid_12" -> Color.parseColor("#6CE009")
                "solid_13" -> Color.parseColor("#EA4335")
                "solid_14" -> Color.parseColor("#FF5C00")
                "solid_15" -> Color.parseColor("#F1F269")
                "solid_16" -> Color.parseColor("#E009A6")
                "solid_17" -> Color.parseColor("#BC0202")
                "solid_28" -> Color.parseColor("#910666")
                "solid_19" -> Color.parseColor("#8D2AF4")
                else -> Color.parseColor(FeedSdk.searchStickyBackground)
            }
        }
    }

    fun getStickyTint(background: String): String {
        return when (background) {
            "fashion_2" -> "#000000"
            "beauty_1" -> "#000000"
            "education_0" -> "#000000"
            "education_1" -> "#000000"
            "education_2" -> "#000000"
            "solid_2" -> "#000000"
            "solid_6" -> "#000000"
            "solid_15" -> "#000000"
            else -> "#FFFFFF"
        }
    }

    fun getStringFromList(arrayList:ArrayList<String>): String?{
        var res: String = ""
        for(i in arrayList.indices){
            res += if(i==arrayList.size-1){
                arrayList[i]
            } else{
                arrayList[i]+","
            }
        }
        return if(res.isEmpty()) null else res
    }


    fun getEValueFormat(value: Double, exp: Int): String {
        if (value > 1 && value % 10 == 0.0) {
            return getEValueFormat(value / 10, exp + 1)
        } else {
            return getUnitFromValue(value, exp)
        }
    }

    fun get0EValueFormat(value: Double): String {
        val result = value.toString()
        return if (result.contains("E")) {
            try {
                val formatter: NumberFormat = DecimalFormat()
                formatter.maximumFractionDigits = 25
                formatter.format(value).toString()
            } catch (ex: java.lang.Exception) {
                LogDetail.LogEStack(ex)
                result
            }
        } else {
            result
        }
    }

    fun getConverterEValueFormat(value: Double): String {
        val result = value.toString()
        try {
            val formatter: NumberFormat = DecimalFormat()
            if (value < 1) {
                formatter.maximumFractionDigits = 25
            } else {
                formatter.maximumFractionDigits = 2
            }
            return formatter.format(value).toString()
        } catch (ex: java.lang.Exception) {
            LogDetail.LogEStack(ex)
            return result
        }
    }

    fun getUnitFromValue(value: Double, exp: Int): String {
        return when {
            exp >= 12 -> {
                BigDecimal(value * 10.0.pow((exp - 12).toDouble())).setScale(
                    2,
                    RoundingMode.HALF_EVEN
                ).toString() + "T"
            }
            exp >= 9 -> {
                BigDecimal(value * 10.0.pow((exp - 9).toDouble())).setScale(
                    2,
                    RoundingMode.HALF_EVEN
                ).toString() + "B"
            }
            exp >= 6 -> {
                BigDecimal(value * 10.0.pow((exp - 6).toDouble())).setScale(
                    2,
                    RoundingMode.HALF_EVEN
                ).toString() + "M"
            }
            else -> BigDecimal(value * 10.0.pow((exp - 3).toDouble())).setScale(
                2,
                RoundingMode.HALF_EVEN
            ).toString() + "K"
        }
    }

    fun getCryptoCoinSymbol(): String {
        return if (FeedSdk.sdkCountryCode == null || FeedSdk.sdkCountryCode!!.lowercase() == "in") "₹" else "$"
    }

    fun isDarkTheme(context: Context): Boolean {
        return context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }


    fun checkFeedApp(): Boolean {
        return when (FeedSdk.appName) {
            "MasterFeed" -> true
            "Samachari" -> true
            "Apple Today" -> true
            else -> false
        }
    }

    fun getHomeBannerAd(): String {
        return when (FeedSdk.appName) {
            "MasterFeed" -> "ca-app-pub-4310459535775382/4037134190"
            "Samachari" -> "ca-app-pub-4310459535775382/4386302763"
            "Apple Today" -> "ca-app-pub-4310459535775382/1957765763"
            else -> ""
        }
    }

    fun getInterestsString(interests: List<String>?): String? {
        var interestsString = ""
        try {
            for (i in interests!!.indices) {
                interestsString += if (i != interests.size - 1) {
                    interests[i] + ","
                } else {
                    interests[i]
                }
            }
        } catch (ex: Exception) {
            LogDetail.LogEStack(ex)
        }
        return if (interestsString.isEmpty()) {
            null
        } else {
            interestsString
        }
    }

    val languageColors = arrayListOf(
        "#B44ECD",
        "#0084FF",
        "#28B9B5",
        "#6D5CD1",
        "#EF3E42",
        "#328AD1",
        "#129353",
        "#BE8F2C"
    )

    fun setFontFamily(view: EditText?, isBold:Boolean = false) {
        try {
            if(isBold) {
                view!!.setTypeface(FeedSdk.font, Typeface.BOLD)
            }else{
                view!!.typeface = FeedSdk.font
            }
        } catch (ex: java.lang.Exception) {
            LogDetail.LogEStack(ex)
        }
    }

    fun setFontFamily(view: Button?, isBold:Boolean = false) {
        try {
            if(isBold) {
                view!!.setTypeface(FeedSdk.font, Typeface.BOLD)
            }else{
                view!!.typeface = FeedSdk.font
            }
        } catch (ex: java.lang.Exception) {
            LogDetail.LogEStack(ex)
        }
    }

    fun getLanguages(maxLanguages: List<String>): String {
        val languages = ArrayList<String>()
        var selectedLanguage = ""
        for (language in maxLanguages) {
            if (Constants.selectedLanguagesMap.containsKey(language)) {
                languages.add(language)
            }
        }
        if (languages.isEmpty()) {
            selectedLanguage = "en"
        } else {
            selectedLanguage = languages.random()
        }
        return selectedLanguage
    }

    fun loadImageFromGlide(
        context: Context,
        imageUrl: String?,
        view: ImageView?,
        callback: GlideCallbackListener,
        requestOptions: RequestOptions = RequestOptions()
    ) {
        try {
            if (imageUrl.isNullOrEmpty()) {
                view?.setImageResource(R.drawable.placeholder)
            } else {
                val theImage = GlideUrl(
                    imageUrl, LazyHeaders.Builder()
                        .addHeader("User-Agent", "5")
                        .build()
                )
                Glide.with(context)
                    .apply { requestOptions }
                    .load(theImage)
                    .error(R.drawable.placeholder)
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>?,
                            isFirstResource: Boolean
                        ): Boolean {
                            callback.onFailure()
                            return true
                        }

                        override fun onResourceReady(
                            resource: Drawable?,
                            model: Any?,
                            target: Target<Drawable>?,
                            dataSource: DataSource?,
                            isFirstResource: Boolean
                        ): Boolean {
                            callback.onSuccess(resource)
                            return true
                        }
                    })
                    .into(view!!)
            }
        } catch (ex: java.lang.Exception) {
            LogDetail.LogEStack(ex)
        }
    }


    fun setPrivacyDialog(context: Context, view: View){
        try{
            val tvPrivacy: AppCompatTextView = view.findViewById(R.id.tvPrivacy)
            val tvOk: AppCompatTextView = view.findViewById(R.id.tvOk)
            Card.setFontFamily(view.findViewById(R.id.tvTitle), true)
            Card.setFontFamily(view.findViewById(R.id.tvBody))
            Card.setFontFamily(tvPrivacy)
            Card.setFontFamily(tvOk)
            tvPrivacy.setOnClickListener {
                val link = ApiConfig().getConfigModel(context).privacyPolicyUrl
                link?.let{
                    val intent = Intent(context, WebActivity::class.java)
                    intent.putExtra("link", link)
                    intent.putExtra("title", "Privacy Policy")
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    context.startActivity(intent)
                }
            }
            tvOk.setOnClickListener {
                SpUtil.spUtilInstance!!.putBoolean(PRIVACY_ACCEPTED, true)
                for (listener in SpUtil.onRefreshListeners) {
                    listener.value.onRefreshNeeded()
                }
                FirebaseAnalytics.getInstance(context).logEvent("Privacy_Policy_Allow", null);
            }
        } catch (ex:Exception){
           LogDetail.LogEStack(ex)
        }
    }

    object Toaster {
        fun show(context: Context, text: CharSequence) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
                } else {
                    val toast =
                        android.widget.Toast.makeText(
                            context,
                            text,
                            android.widget.Toast.LENGTH_SHORT
                        )
                    toast.view?.background?.setColorFilter(
                        ContextCompat.getColor(context, android.R.color.white),
                        PorterDuff.Mode.SRC_IN
                    )
                    val textView = toast.view?.findViewById(android.R.id.message) as TextView
                    textView.setTextColor(ContextCompat.getColor(context, android.R.color.black))
                    toast.show()
                }
            } catch (ex: Exception) {
                LogDetail.LogEStack(ex)
            }
        }
    }

    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(AppCompatActivity.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }

    fun findFilename(url: String): String {
        val uri = Uri.parse(url)
        return uri.getQueryParameter("filename") ?: ""
    }

    fun ViewPager2.reduceDragSensitivity() {
        val recyclerViewField = ViewPager2::class.java.getDeclaredField("mRecyclerView")
        recyclerViewField.isAccessible = true
        val recyclerView = recyclerViewField.get(this) as RecyclerView

        val touchSlopField = RecyclerView::class.java.getDeclaredField("mTouchSlop")
        touchSlopField.isAccessible = true
        val touchSlop = touchSlopField.get(recyclerView) as Int
        touchSlopField.set(recyclerView, touchSlop * 8)       // "8" was obtained experimentally
    }

    fun TextView.setDrawableColor(@ColorInt color: Int) {
        try{
            compoundDrawables.filterNotNull().forEach {
                it.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
            }
        } catch (ex:Exception){
            LogDetail.LogEStack(ex)
        }
    }


//    enum class PostType {
//        @SerializedName("single")
//        SINGLE_MEDIA,
//        SINGLE_GRID,
//        SINGLE_CAROUSEL,
//        RECYCLER,
//        TITLE
//    }
//
//    val cardMap = hashMapOf<CardType, PostType>(
//        CardType.NEWS_BIG_FEATURE to PostType.SINGLE_MEDIA,
//        CardType.NEWS_SMALL_FEATURE to PostType.SINGLE_MEDIA,
//        CardType.NEWS_TWEET to PostType.SINGLE_MEDIA,
//        CardType.MEDIA_IMAGE to PostType.SINGLE_MEDIA,
//        CardType.MEDIA_VIDEO to PostType.SINGLE_MEDIA,
//        CardType.MEDIA_GRID_VIEW to PostType.SINGLE_GRID,
//        CardType.MEDIA_CAROUSEL to PostType.SINGLE_CAROUSEL,
//        CardType.FEED_HASHTAGS to PostType.RECYCLER,
//        CardType.FEED_PUBLISHERS to PostType.RECYCLER,
//        CardType.FEED_REELS to PostType.RECYCLER,
//        CardType.FEED_VIDEOS_HORIZONTAL to PostType.RECYCLER,
//        CardType.FEED_POSTS_CATEGORY to PostType.RECYCLER,
//        CardType.FEED_POSTS_HORIZONTAL to PostType.RECYCLER,
//        CardType.FEED_ICON_HASHTAGS to PostType.RECYCLER,
//        CardType.TITLE_ICON to PostType.TITLE,
//        CardType.DAILY_SHARE_IMAGE to PostType.SINGLE_GRID
//    )
}

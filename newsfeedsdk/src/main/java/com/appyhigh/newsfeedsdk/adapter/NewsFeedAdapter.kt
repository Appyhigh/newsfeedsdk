@file:Suppress("DEPRECATION")

package com.appyhigh.newsfeedsdk.adapter

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.Constants.AD
import com.appyhigh.newsfeedsdk.Constants.AD_LARGE
import com.appyhigh.newsfeedsdk.Constants.FEED_TYPE
import com.appyhigh.newsfeedsdk.Constants.FROM_APP
import com.appyhigh.newsfeedsdk.Constants.INTEREST
import com.appyhigh.newsfeedsdk.Constants.IS_ALREADY_RATED
import com.appyhigh.newsfeedsdk.Constants.LOADER
import com.appyhigh.newsfeedsdk.Constants.POSITION
import com.appyhigh.newsfeedsdk.Constants.POST_ID
import com.appyhigh.newsfeedsdk.Constants.POST_SOURCE
import com.appyhigh.newsfeedsdk.Constants.RATING
import com.appyhigh.newsfeedsdk.Constants.SHARE
import com.appyhigh.newsfeedsdk.Constants.TAG
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.activity.*
import com.appyhigh.newsfeedsdk.apicalls.ApiConfig
import com.appyhigh.newsfeedsdk.apicalls.ApiCreateOrUpdateUser
import com.appyhigh.newsfeedsdk.apicalls.ApiFollowPublihser
import com.appyhigh.newsfeedsdk.apicalls.ApiReactPost
import com.appyhigh.newsfeedsdk.apiclient.Endpoints
import com.appyhigh.newsfeedsdk.callbacks.*
import com.appyhigh.newsfeedsdk.customview.NewsFeedList
import com.appyhigh.newsfeedsdk.databinding.*
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.fragment.AddInterestBottomSheet
import com.appyhigh.newsfeedsdk.fragment.CryptoAlertPriceFragment
import com.appyhigh.newsfeedsdk.fragment.FeedMenuBottomSheetFragment
import com.appyhigh.newsfeedsdk.fragment.ReportIssueDialogFragment
import com.appyhigh.newsfeedsdk.model.feeds.Card
import com.appyhigh.newsfeedsdk.model.feeds.Item
import com.appyhigh.newsfeedsdk.utils.AudioTracker
import com.appyhigh.newsfeedsdk.utils.AudioTrackerListener
import com.appyhigh.newsfeedsdk.utils.Converters
import com.appyhigh.newsfeedsdk.utils.SpUtil
import com.appyhigh.newsfeedsdk.utils.SpUtil.Companion.eventsListener
import com.appyhigh.newsfeedsdk.utils.SpUtil.Companion.spUtilInstance
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.tasks.Task
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.dynamiclinks.DynamicLink
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.protobuf.Api
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.YouTubePlayerCallback
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class NewsFeedAdapter(
    private var newsFeedList: ArrayList<Card>,
    private var personalizationListener: NewsFeedList.PersonalizationListener? = null,
    var interest: String,
    private var videoPlayerListener: VideoPlayerListener? = null,
    private var postImpressionListener: PostImpressionListener? = null,
    private var requestUrl:String? = null,
    private var requestTimeStamp:Long?=null,
    private val observeYoutubePlayer: (player: YouTubePlayerView) -> Unit = {},
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>(), PostViewsClickListener,
    OnViewDetachedFromWindow, OnViewAttachedToWindow, LanguageCardClickListener,
    InterestsCardClickListener, VideoPlayerListener, MatchSelectedListener, CovidCardShareListener {
    private val NATIVE_AD = 0
    private val NEWS_FEED = 1
    private val NEWS_FEED_SMALL = 2
    private val SHARE_CARD = 3
    private val RATING_CARD = 4
    private val LOAD_MORE = 5
    private val VIDEO = 6
    private val TITLE_ICON = 7
    private val HASHTAGS = 8
    private val POPULAR_ACCOUNTS = 9
    private val FEED_POSTS_CATEGORY = 10
    private val TITLE = 11
    private val FEED_VIDEOS_HORIZONTAL = 12
    private val FEED_REELS = 13
    private val VIDEO_BIG = 14
    private val INTERESTS = 15
    private val LANGUAGES = 16
    private val VIDEO_YT = 17
    private val MEDIA_IMAGE = 18
    private val NATIVE_AD_LARGE = 25
    private val MEDIA_PODCAST = 28
    private val FEED_ICON_HASHTAGS_CIRCLE = 29
    private val CRYPTO_WATCHLIST = 30
    private val CRYPTO_GAINERS = 31
    private val DESCRIPTION = 32
    private val COIN_LINKS = 33
    private val COIN_STATS = 34
    private val COIN_MARKET = 35
    private val CRYPTO_PODCASTS = 36
    private val SEARCH_FEED_SMALL = 37
    private val SEARCH_FEED_BIG = 38
    private val FEED_COVID_TRACKER = 39
    private val CRYPTO_ALERT = 40
    private val CRICKET_TRENDING_POSTS=41
    private val TELEGRAM_CHANNEL = 42
    private val FEED_YOU_MAKE_LIKE_INTERESTS = 43
    private val NEWS_REGIONAL = 44
    private val EMPTY = 100
    private val fromPublishPage = false
    private var onSomethingClicked = false
    private var pageNo: Int = 0
    private var presentUrl:String?= requestUrl
    private var presentTimeStamp:Long? = requestTimeStamp
    private val youtubePlayerView = HashMap<Int, YouTubePlayerView>()
    private var checkPackages: HashMap<String, Boolean> = HashMap()
    private lateinit var mContext: Context

    init {
        getContentMappingPackages()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        mContext = parent.context
        when (viewType) {
            NATIVE_AD -> {
                return AdViewHolder(
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.item_ad_layout,
                        parent,
                        false
                    )
                )
            }
            NATIVE_AD_LARGE -> {
                return LargeAdViewHolder(
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.item_ad_layout_large,
                        parent,
                        false
                    )
                )
            }
            RATING_CARD -> {
                if(FeedSdk.isCryptoApp)
                    return CryptoRatingViewHolder(
                        DataBindingUtil.inflate(
                            LayoutInflater.from(parent.context),
                            R.layout.crypto_rating_card,
                            parent,
                            false
                        )
                    )
                else
                    return RatingViewHolder(
                        DataBindingUtil.inflate(
                            LayoutInflater.from(parent.context),
                            R.layout.item_rating_card,
                            parent,
                            false
                        )
                    )
            }
            SHARE_CARD -> {
                return ShareViewHolder(
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.item_share_card,
                        parent,
                        false
                    )
                )
            }
            TELEGRAM_CHANNEL  -> {
                return TelegramChannelHolder(
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.item_telegram_card,
                        parent,
                        false
                    )
                )
            }
            NEWS_FEED -> {
                return BigFeedViewHolder(
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.item_feed_big,
                        parent,
                        false
                    )
                )
            }
            NEWS_FEED_SMALL -> {
                return SmallFeedViewHolder(
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.item_news_feed_small,
                        parent,
                        false
                    )
                )
            }
            NEWS_REGIONAL -> {
            return RegionalNewsHolder(
                DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.item_regional_news,
                    parent,
                    false
                )
            )
        }
            VIDEO -> {
                return VideoViewHolder(
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.item_feed_video,
                        parent,
                        false
                    )
                )
            }
            VIDEO_YT -> {
                return VideoViewHolder(
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.item_feed_video,
                        parent,
                        false
                    )
                )
            }
            LOAD_MORE -> {
                return LoadMoreViewHolder(
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.item_load_more,
                        parent,
                        false
                    )
                )
            }
            TITLE_ICON -> {
                return TitleIconViewHolder(
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.item_title_icon,
                        parent,
                        false
                    )
                )
            }
            TITLE -> {
                return TitleViewHolder(
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.item_title,
                        parent,
                        false
                    )
                )
            }
            HASHTAGS -> {
                return HashtagsViewHolder(
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.item_feed_hashtags,
                        parent,
                        false
                    )
                )
            }
            POPULAR_ACCOUNTS -> {
                return PopularAccountsCardViewHolder(
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.item_popular_account_card,
                        parent,
                        false
                    )
                )
            }
            FEED_POSTS_CATEGORY -> {
                return FeedPostsCategoryViewHolder(
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.item_feed_posts_category_card,
                        parent,
                        false
                    )
                )
            }
            FEED_VIDEOS_HORIZONTAL -> {
                return FeedVideosHorizontalViewHolder(
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.item_feed_videos_horizontal_card,
                        parent,
                        false
                    )
                )
            }
            FEED_REELS -> {
                return FeedReelsViewHolder(
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.item_feed_reels_card,
                        parent,
                        false
                    )
                )
            }
            VIDEO_BIG -> {
                return BigVideoViewHolder(
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.item_video_big,
                        parent,
                        false
                    )
                )
            }
            INTERESTS -> {
                return InterestsViewHolder(
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.item_interests_card,
                        parent,
                        false
                    )
                )
            }
            LANGUAGES -> {
                return LanguagesViewHolder(
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.item_language_card,
                        parent,
                        false
                    )
                )
            }
            CRICKET_TRENDING_POSTS -> {
                return CricketTrendingPostsViewHolder(
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.item_trending_posts,
                        parent,
                        false
                    )
                )
            }
            MEDIA_PODCAST -> {
                return MediaPodcastViewHolder(
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.item_media_podcast,
                        parent,
                        false
                    )
                )
            }
            FEED_ICON_HASHTAGS_CIRCLE -> {
                return FeedIconHastagsCircleViewHolder(
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.item_hashtags_circle,
                        parent,
                        false
                    )
                )
            }
            CRYPTO_WATCHLIST -> {
                return CryptoWatchlistViewHolder(
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.layout_crypto_watchlist,
                        parent,
                        false
                    )
                )
            }
            CRYPTO_GAINERS -> {
                return CryptoGainersViewHolder(
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.layout_crypto_gainers,
                        parent,
                        false
                    )
                )
            }
            DESCRIPTION -> {
                return DescriptionViewHolder(
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.item_description,
                        parent,
                        false
                    )
                )
            }
            COIN_LINKS -> {
                return CoinLinksViewHolder(
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.item_coin_links,
                        parent,
                        false
                    )
                )
            }
            COIN_STATS -> {
                return CoinOverViewHolder(
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.item_coin_overview,
                        parent,
                        false
                    )
                )
            }
            COIN_MARKET -> {
                return CoinMarketViewHolder(
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.layout_crypto_gainers,
                        parent,
                        false
                    )
                )
            }
            CRYPTO_PODCASTS -> {
                return CryptoPodcastsViewHolder(
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.item_crypto_podcasts,
                        parent,
                        false
                    )
                )
            }
            SEARCH_FEED_SMALL -> {
                return SearchFeedSmallViewHolder(
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.item_search_feed_small,
                        parent,
                        false
                    )
                )
            }
            SEARCH_FEED_BIG -> {
                return SearchFeedBigViewHolder(
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.item_search_feed_big,
                        parent,
                        false
                    )
                )
            }
            FEED_COVID_TRACKER -> {
                return FeedCovidTrackerHolder(
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.item_feed_covid_tracker,
                        parent,
                        false
                    )
                )
            }
            CRYPTO_ALERT -> {
                return CryptoAlertHolder(
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.item_crypto_alert_list,
                        parent,
                        false
                    )
                )
            }
            FEED_YOU_MAKE_LIKE_INTERESTS -> {
                return MayLikeInterestHolder(
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.item_feed_like_interests,
                        parent,
                        false
                    )
                )
            }
            else -> {
                return EmptyViewHolder(
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.item_empty,
                        parent,
                        false
                    )
                )
            }
        }
    }

    override fun onBindViewHolder(mainHolder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            NATIVE_AD -> {
                val holder = mainHolder as AdViewHolder
                holder.view.card = newsFeedList[position]
                holder.view.contentUrls = getContentUrls(position)
            }
            NATIVE_AD_LARGE -> {
                val holder = mainHolder as LargeAdViewHolder
                holder.view.card = newsFeedList[position]
                holder.view.contentUrls = getContentUrls(position)
            }
            RATING_CARD -> {
                if(FeedSdk.isCryptoApp){
                    val holder = mainHolder as CryptoRatingViewHolder
                    holder.view.card = newsFeedList[position]
                    holder.view.listener = this
                    holder.view.position = holder.adapterPosition
                }
                else {
                    val holder = mainHolder as RatingViewHolder
                    holder.view.card = newsFeedList[position]
                    holder.view.listener = this
                    holder.view.position = holder.adapterPosition
                }
            }
            SHARE_CARD -> {
                val holder = mainHolder as ShareViewHolder
                holder.view.card = newsFeedList[position]
                holder.view.listener = this
                holder.view.position = holder.adapterPosition
            }
            TELEGRAM_CHANNEL -> {
                val holder = mainHolder as TelegramChannelHolder
                holder.view.joinChannel.setOnClickListener {
                    SpUtil.telegramCardListener?.onRefreshNeeded()
                }
            }
            LOAD_MORE -> {
                mainHolder as LoadMoreViewHolder
            }
            NEWS_FEED -> {
                val holder = mainHolder as BigFeedViewHolder
                holder.view.card = newsFeedList[position]
                holder.view.listener = this
                holder.view.position = holder.adapterPosition
                postImpressionListener?.addImpression(newsFeedList[position], null, null)
            }
            NEWS_FEED_SMALL -> {
                val holder = mainHolder as SmallFeedViewHolder
                holder.view.card = newsFeedList[position]
                holder.view.listener = this
                holder.view.position = holder.adapterPosition
                postImpressionListener?.addImpression(newsFeedList[position], null, null)
            }
            SEARCH_FEED_SMALL -> {
                val holder = mainHolder as SearchFeedSmallViewHolder
                holder.view.card = newsFeedList[position]
                holder.view.listener = this
                holder.view.position = holder.adapterPosition
                postImpressionListener?.addImpression(newsFeedList[position], null, null)
            }
            SEARCH_FEED_BIG -> {
                val holder = mainHolder as SearchFeedBigViewHolder
                holder.view.card = newsFeedList[position]
                holder.view.listener = this
                holder.view.position = holder.adapterPosition
                postImpressionListener?.addImpression(newsFeedList[position], null, null)
            }
            VIDEO -> {
                val holder = mainHolder as VideoViewHolder
                holder.view.card = newsFeedList[position]
                holder.view.listener = this
                holder.view.position = holder.adapterPosition
                holder.view.onAttachedListener = this
                holder.view.onDetachedListener = this
            }
            VIDEO_YT -> {
                val holder = mainHolder as VideoViewHolder
                holder.view.card = newsFeedList[position]
                holder.view.listener = this
                holder.view.position = holder.adapterPosition
                holder.view.onAttachedListener = this
                holder.view.onDetachedListener = this
            }
            TITLE_ICON -> {
                val holder = mainHolder as TitleIconViewHolder
                holder.view.position = holder.adapterPosition
                holder.view.card = newsFeedList[position]
                holder.view.listener = this
            }
            TITLE -> {
                val holder = mainHolder as TitleViewHolder
                holder.view.position = holder.adapterPosition
                holder.view.card = newsFeedList[position]
                holder.view.listener = this
            }
            HASHTAGS -> {
                val holder = mainHolder as HashtagsViewHolder
                holder.view.card = newsFeedList[position]
            }
            POPULAR_ACCOUNTS -> {
                val holder = mainHolder as PopularAccountsCardViewHolder
                holder.view.card = newsFeedList[position]
            }
            FEED_POSTS_CATEGORY -> {
                val holder = mainHolder as FeedPostsCategoryViewHolder
                holder.view.card = newsFeedList[position]
            }
            FEED_VIDEOS_HORIZONTAL -> {
                val holder = mainHolder as FeedVideosHorizontalViewHolder
                holder.view.card = newsFeedList[position]
                if(newsFeedList[position].cardType == "get_feeds_videos"){
                    postImpressionListener?.addImpression(newsFeedList[position], null, null)
                }
            }
            FEED_REELS -> {
                val holder = mainHolder as FeedReelsViewHolder
                holder.view.card = newsFeedList[position]
                if(newsFeedList[position].cardType == "get_feeds_reels"){
                    postImpressionListener?.addImpression(newsFeedList[position], null, null)
                }
            }
            VIDEO_BIG -> {
                val holder = mainHolder as BigVideoViewHolder
                holder.view.card = newsFeedList[position]
                holder.view.listener = this
                holder.view.position = position
                holder.view.onAttachedListener = this
                holder.view.onDetachedListener = this
                holder.view.feedListener = this
            }
            INTERESTS -> {
                val holder = mainHolder as InterestsViewHolder
                holder.view.card = newsFeedList[position]
                holder.view.listener = this
            }
            NEWS_REGIONAL -> {
                val holder = mainHolder as RegionalNewsHolder
                holder.view.card = newsFeedList[position]
                holder.view.listener = this
                holder.view.position = holder.adapterPosition
                postImpressionListener?.addImpression(newsFeedList[position],null,null)
            }
            LANGUAGES -> {
                val holder = mainHolder as LanguagesViewHolder
                holder.view.card = newsFeedList[position]
                holder.view.listener = this
            }
            CRICKET_TRENDING_POSTS -> {
                val holder = mainHolder as CricketTrendingPostsViewHolder
                holder.view.card = newsFeedList[position]
            }
            MEDIA_PODCAST -> {
                val holder = mainHolder as MediaPodcastViewHolder
                holder.view.card = newsFeedList[position]
                holder.view.listener = this
                holder.view.position = holder.adapterPosition
                postImpressionListener?.addImpression(newsFeedList[position], null, null)
            }
            FEED_ICON_HASHTAGS_CIRCLE -> {
                val holder = mainHolder as FeedIconHastagsCircleViewHolder
                holder.view.card = newsFeedList[position]
                holder.view.listener = this
                holder.view.position = position
            }
            CRYPTO_WATCHLIST -> {
                val holder = mainHolder as CryptoWatchlistViewHolder
                holder.view.card = newsFeedList[position]
                holder.view.allCryptoCoins.setOnClickListener {
                    val intent = Intent(holder.itemView.context, CryptoListActivity::class.java)
                    intent.putExtra(Constants.ORDER, "alphabetical")
                    intent.putExtra(INTEREST, "crypto_watchlist")
                    holder.itemView.context.startActivity(intent)
                }
                holder.view.cryptoPriceAlert.setOnClickListener {
                    val intent = Intent(holder.itemView.context, CryptoMainAlertActivity::class.java)
                    holder.itemView.context.startActivity(intent)
                }
            }
            CRYPTO_GAINERS -> {
                val holder = mainHolder as CryptoGainersViewHolder
                holder.view.card = newsFeedList[position]
            }
            DESCRIPTION -> {
                val holder = mainHolder as DescriptionViewHolder
                holder.view.card = newsFeedList[position]
            }
            COIN_LINKS -> {
                val holder = mainHolder as CoinLinksViewHolder
                holder.view.card = newsFeedList[position]
            }
            COIN_STATS -> {
                val holder = mainHolder as CoinOverViewHolder
                holder.view.card = newsFeedList[position]
            }
            COIN_MARKET -> {
                val holder = mainHolder as CoinMarketViewHolder
                holder.view.card = newsFeedList[position]
            }
            CRYPTO_PODCASTS -> {
                val holder = mainHolder as CryptoPodcastsViewHolder
                holder.view.card = newsFeedList[position]
                holder.view.listener = this
                holder.view.position = holder.adapterPosition
                postImpressionListener?.addImpression(newsFeedList[position], null, null)
            }
            FEED_COVID_TRACKER -> {
                val holder = mainHolder as FeedCovidTrackerHolder
                holder.view.card = newsFeedList[position]
                holder.view.position = holder.adapterPosition
                holder.view.listener = this
            }
            FEED_YOU_MAKE_LIKE_INTERESTS -> {
                val holder = mainHolder as MayLikeInterestHolder
                holder.view.card = newsFeedList[position]
                holder.view.position = holder.adapterPosition
                holder.view.listener = this
            }
            CRYPTO_ALERT -> {
                val holder = mainHolder as CryptoAlertHolder
                holder.view.card = newsFeedList[position]
                holder.view.position = holder.adapterPosition
                val cryptoItem = newsFeedList[position]
                holder.view.cryptoAddAlert.setOnClickListener {
                    (holder.itemView.context as CryptoMainAlertActivity)
                        .supportFragmentManager.beginTransaction()
                        .add(R.id.baseFragment, CryptoAlertPriceFragment.newInstance(cryptoItem.coinId?:"", cryptoItem.coinName?:"", cryptoItem.imageLink?:"", 0.0, null))
                        .addToBackStack(null)
                        .commit()
                }
            }
            EMPTY -> {
                mainHolder as EmptyViewHolder
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when(newsFeedList[position].cardType) {
            AD -> {
                NATIVE_AD
            }
            AD_LARGE -> {
                NATIVE_AD_LARGE
            }
            RATING -> {
                RATING_CARD
            }
            SHARE -> {
                SHARE_CARD
            }
            Constants.TELEGRAM_CHANNEL -> {
                TELEGRAM_CHANNEL
            }
            LOADER -> {
                LOAD_MORE
            }
            Constants.CardType.NEWS_BIG_FEATURE.toString()
                .toLowerCase(Locale.getDefault()) -> {
                NEWS_FEED
            }
            Constants.CardType.NEWS_SMALL_FEATURE.toString()
                .toLowerCase(Locale.getDefault()) -> {
                NEWS_FEED_SMALL
            }
            Constants.CardType.MEDIA_VIDEO.toString()
                .toLowerCase(Locale.getDefault()) -> {
                    if(newsFeedList[position].items[0].platform.toString().toLowerCase(Locale.getDefault()) != "youtube") {
                        VIDEO
                    } else{
                        VIDEO_YT
                    }
            }
            Constants.CardType.TITLE_ICON.toString()
                .toLowerCase(Locale.getDefault()) -> {
                TITLE_ICON
            }
            Constants.CardType.TITLE.toString()
                .toLowerCase(Locale.getDefault()) -> {
                TITLE
            }
            Constants.CardType.DESCRIPTION.toString()
                .toLowerCase(Locale.getDefault()) -> {
                DESCRIPTION
            }
            Constants.CardType.FEED_HASHTAGS.toString()
                .toLowerCase(Locale.getDefault()) -> {
                HASHTAGS
            }
            Constants.CardType.FEED_PUBLISHERS.toString()
                .toLowerCase(Locale.getDefault()) -> {
                POPULAR_ACCOUNTS
            }
            Constants.CardType.FEED_POSTS_CATEGORY.toString()
                .toLowerCase(Locale.getDefault()) -> {
                FEED_POSTS_CATEGORY
            }
            Constants.CardType.CRICKET_TRENDING_POSTS.toString()
                .toLowerCase(Locale.getDefault())->{
                    CRICKET_TRENDING_POSTS
                }
            Constants.CardType.FEED_VIDEOS_HORIZONTAL.toString()
                .toLowerCase(Locale.getDefault()) -> {
                FEED_VIDEOS_HORIZONTAL
            }
            Constants.CardType.GET_FEEDS_VIDEOS.toString()
                .toLowerCase(Locale.getDefault()) -> {
                FEED_VIDEOS_HORIZONTAL
            }
            Constants.CardType.FEED_REELS.toString()
                .toLowerCase(Locale.getDefault()) -> {
                FEED_REELS
            }
            Constants.CardType.GET_FEEDS_REELS.toString()
                .toLowerCase(Locale.getDefault()) -> {
                FEED_REELS
            }
            Constants.CardType.NEWS_REGIONAL.toString()
                .toLowerCase(Locale.getDefault()) -> {
                NEWS_REGIONAL
            }
            Constants.CardType.MEDIA_VIDEO_BIG.toString()
                .toLowerCase(Locale.getDefault()) -> {
                VIDEO_BIG
            }
            Constants.CardType.FEED_INTERESTS.toString()
                .toLowerCase(Locale.getDefault()) -> {
                INTERESTS
            }
            Constants.CardType.FEED_LANGUAGE.toString()
                .toLowerCase(Locale.getDefault()) -> {
                LANGUAGES
            }
            Constants.CardType.MEDIA_IMAGE.toString()
                .toLowerCase(Locale.getDefault()) -> {
                NEWS_FEED
            }
            Constants.CardType.MEDIA_PODCAST.toString()
                .toLowerCase(Locale.getDefault()) -> {
                MEDIA_PODCAST
            }
            Constants.CardType.FEED_ICON_HASHTAGS_CIRCLE.toString()
                .toLowerCase(Locale.getDefault()) -> {
                FEED_ICON_HASHTAGS_CIRCLE
            }
            Constants.CardType.CRYPTO_WATCHLIST.toString()
                .toLowerCase(Locale.getDefault()) -> {
                CRYPTO_WATCHLIST
            }
            Constants.CardType.CRYPTO_GAINERS.toString()
                .toLowerCase(Locale.getDefault()) -> {
                CRYPTO_GAINERS
            }
            Constants.CardType.CRYPTO_LOSERS.toString()
                .toLowerCase(Locale.getDefault()) -> {
                CRYPTO_GAINERS
            }
            Constants.CardType.COIN_LINKS.toString()
                .toLowerCase(Locale.getDefault()) -> {
                COIN_LINKS
            }
            Constants.CardType.COIN_STATS.toString()
                .toLowerCase(Locale.getDefault()) -> {
                COIN_STATS
            }
            Constants.CardType.COIN_MARKETS.toString()
                .toLowerCase(Locale.getDefault()) -> {
                COIN_MARKET
            }
            Constants.CRYPTO_PODCASTS -> {
                CRYPTO_PODCASTS
            }
            Constants.SEARCH_FEED_SMALL ->{
                SEARCH_FEED_SMALL
            }
            Constants.SEARCH_FEED_BIG ->{
                SEARCH_FEED_BIG
            }
            Constants.CardType.FEED_COVID_TRACKER.toString()
                .toLowerCase(Locale.getDefault()) -> {
                FEED_COVID_TRACKER
            }
            Constants.CardType.CRYPTO_ALERT.toString()
                .toLowerCase(Locale.getDefault()) -> {
                CRYPTO_ALERT
            }
            Constants.CardType.FEED_YOU_MAKE_LIKE_INTERESTS.toString()
                .toLowerCase(Locale.getDefault()) -> {
                FEED_YOU_MAKE_LIKE_INTERESTS
            }
            else -> {
                EMPTY
            }
        }
    }

    override fun getItemCount(): Int {
        return newsFeedList.size
    }


    private fun getContentMappingPackages():HashMap<String,Boolean>{
        checkPackages["messenger.video.call.chat.free"] = true
        checkPackages["messenger.chat.social.messenger.lite"] = true
        checkPackages["messenger.video.call.chat.randomchat"] = true
        checkPackages["messenger.chat.social.messenger"] = true
        checkPackages["com.messenger.messengerpro.social.chat"] = true
        checkPackages["com.Pally.Random.Video.Chat.Livetalk.Messenger"] = true
        checkPackages["com.Pally.Random.Video.Chat.Livetalk.Messenger"] = true
        return checkPackages
    }

    private fun getContentUrls(position: Int): ArrayList<String>{
        val contentUrls = ArrayList<String>()
        if(!checkPackages.containsKey(mContext.packageName)) return ArrayList()
        try{
            if(position-2>0){
                getContentPostUrl(position-2)?.let { contentUrls.add(it) }
                getContentPostUrl(position-1)?.let { contentUrls.add(it) }
            } else if(position-1>0) {
                getContentPostUrl(position-1)?.let { contentUrls.add(it) }
            }
            if(position+2>newsFeedList.size){
                getContentPostUrl(position+1)?.let { contentUrls.add(it) }
                getContentPostUrl(position+2)?.let { contentUrls.add(it) }
            } else if(position+1>0) {
                getContentPostUrl(position+1)?.let { contentUrls.add(it) }
            }
        } catch (ex:Exception){
            LogDetail.LogEStack(ex)
        }
        return contentUrls
    }

    private fun getContentPostUrl(position: Int): String?{
        try{
            val content = newsFeedList[position].items[0].content
            if(!content!!.videoUrl.isNullOrEmpty()){
                return content.videoUrl!!
            } else if(!content.postUrl.isNullOrEmpty()){
                return content.postUrl!!
            } else if (!content.url.isNullOrEmpty()){
                return content.url!!
            } else return null
        } catch (ex:Exception){
            LogDetail.LogEStack(ex)
            return null
        }
    }

    override fun onPostClicked(v: View, position: Int) {
        if (!onSomethingClicked) {
            onSomethingClicked = true
            val intent = if(newsFeedList[position].items[0].isNative!!){
                val bundle = Bundle()
                bundle.putString("NativePageOpen","Feed")
                FirebaseAnalytics.getInstance(v.context).logEvent("NativePage",bundle)
                Intent(v.context, PostNativeDetailActivity::class.java)
            } else{
                Intent(v.context, NewsFeedPageActivity::class.java)
            }
            intent.putExtra(INTEREST, interest) // send interest
            intent.putExtra(POSITION, position)
            intent.putExtra(FROM_APP, true)
            intent.putExtra(POST_SOURCE, newsFeedList[position].items[0].postSource)
            intent.putExtra(FEED_TYPE, newsFeedList[position].items[0].feedType)
            intent.putExtra(POST_ID, newsFeedList[position].items[0].postId)
            intent.putExtra(Constants.LANGUAGE, newsFeedList[position].items[0].languageString)
            intent.putExtra(Constants.SCREEN_TYPE, getScreenType())
            val reactionsCount = newsFeedList[position].items[0].reactionsCount
            reactionsCount?.let {
                intent.putExtra(
                    "reactionCount",
                    it.likeCount + it.angryCount + it.laughCount + it.wowCount + it.loveCount + it.sadCount
                )
                intent.putExtra("isReacted", newsFeedList[position].items[0].isReacted)
            }
            if (fromPublishPage) {
                intent.putExtra("fromPublishPage", true)
            } else if (v.context.toString().contains("ExploreHashtag")) {
                intent.putExtra("fromExplore", true)
                intent.putExtra(Constants.SCREEN_TYPE, Constants.EXPLORE)
            }
            try {
                if (newsFeedList[position].items[0].isVideo!!) {
                    try {
                        val player: StyledPlayerView = v.findViewById(R.id.news_item_video)
                        player.player?.playWhenReady = false
                    } catch (e: java.lang.Exception) {
                        val player: StyledPlayerView =
                            (v.parent as View).findViewById(R.id.news_item_video)
                        player.player?.playWhenReady = false
                    }
                }
            } catch (ex:Exception){}
            try {
                intent.putExtra("platform", newsFeedList[position].items[0].platform)
                if (eventsListener != null) {
                    if (newsFeedList[position].cardType!! == Constants.CardType.MEDIA_VIDEO_BIG.toString()
                            .toLowerCase(Locale.getDefault())
                    ) {
                        eventsListener!!.onVideoInteraction(
                            newsFeedList[position].items[0].postSource!!,
                            interest,
                            newsFeedList[position].items[0].postId!!,
                            newsFeedList[position].items[0].platform!!,
                            "open"
                        )
                    } else {
                        eventsListener!!.onFeedInteraction(
                            newsFeedList[position].items[0].postSource!!,
                            interest,
                            newsFeedList[position].items[0].postId!!,
                            newsFeedList[position].items[0].platform!!,
                            "open"
                        )
                    }
                }
            } catch (ex: java.lang.Exception) {
                LogDetail.LogEStack(ex)
            }
            v.context.startActivity(intent)
            Handler(Looper.getMainLooper()).postDelayed({
                onSomethingClicked = false
            }, 500)
        }
    }

    override fun onMoreOptionsClicked(v: View, position: Int) {
        val reportBottomSheet = FeedMenuBottomSheetFragment.newInstance(
            newsFeedList[position].items[0].publisherContactUs ?: "",
            newsFeedList[position].items[0].postId.toString()
        )
        if (v.context is FragmentActivity) {
            reportBottomSheet.show(
                (v.context as
                        FragmentActivity).supportFragmentManager,
                "reportBottomSheet"
            )
        } else if (((v.context as ContextWrapper).baseContext is FragmentActivity)) {
            reportBottomSheet.show(
                ((v.context as ContextWrapper).baseContext as FragmentActivity).supportFragmentManager,
                "reportBottomSheet"
            )
        }
    }


    override fun onRatingClicked(v: View, position: Int) {
        Snackbar.make(v, v.context.getString(R.string.message_thank_you), Snackbar.LENGTH_SHORT)
            .show()
        newsFeedList.removeAt(position)
        notifyItemRemoved(position)
        val context = v.context
        try {
            SpUtil.spUtilInstance?.putBoolean(IS_ALREADY_RATED, true)
            val rating = (v.parent as View).findViewById<RatingBar>(R.id.ratingBar).rating
            if(rating<4F){
                return
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val manager: ReviewManager = ReviewManagerFactory.create(context)
                val request: Task<ReviewInfo> = manager.requestReviewFlow()
                request.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val reviewInfo: ReviewInfo = task.result
                        val flow: Task<Void> =
                            manager.launchReviewFlow(context as Activity, reviewInfo)
                        flow.addOnCompleteListener { taskFinished -> }
                    } else {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(
                                    "market://details?id=" + context.getApplicationContext()
                                        .getPackageName()
                                )
                            )
                        )
                    }
                }
            } else {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + context.applicationContext.packageName))
                )
            }
        } catch (e: Exception) {
            LogDetail.LogEStack(e)
        }
    }

    override fun onLikeClicked(v: View, position: Int, card: Card) {
        val reactionCount = newsFeedList[position].items[0].reactionsCount
        try {
            if (eventsListener != null) {
                if (newsFeedList[position].cardType!! == Constants.CardType.MEDIA_VIDEO_BIG.toString()
                        .toLowerCase(Locale.getDefault())
                ) {
                    eventsListener!!.onVideoInteraction(
                        newsFeedList[position].items[0].postSource!!,
                        interest,
                        newsFeedList[position].items[0].postId!!,
                        newsFeedList[position].items[0].platform!!,
                        "like"
                    )
                } else {
                    eventsListener!!.onFeedInteraction(
                        newsFeedList[position].items[0].postSource!!,
                        interest,
                        newsFeedList[position].items[0].postId!!,
                        newsFeedList[position].items[0].platform!!,
                        "like"
                    )
                }
            }

        } catch (ex: java.lang.Exception) {
            LogDetail.LogEStack(ex)
        }
        reactionCount?.let {
            try{
                var likeCount =
                    it.likeCount + it.angryCount + it.laughCount + it.wowCount + it.loveCount + it.sadCount

                if (newsFeedList[position].items[0].isReacted == Constants.ReactionType.LIKE.toString()) {
                    likeCount = likeCount.minus(1)
                    it.likeCount -= 1
                } else {
                    likeCount = likeCount.plus(1)
                    it.likeCount += 1
                }
                if (card.cardType != Constants.CardType.MEDIA_VIDEO_BIG.toString().lowercase(Locale.getDefault())) {
                    val tvLikes: AppCompatTextView = v.findViewById(R.id.tvLikes)
                    tvLikes.text = likeCount.toString()
                }
            } catch (ex:Exception){
                LogDetail.LogEStack(ex)
            }
        }
        val ivLike: AppCompatImageView = v.findViewById(R.id.ivLike)
        val reaction: Constants.ReactionType =
            if (newsFeedList[position].items[0].isReacted == Constants.ReactionType.LIKE.toString()) {
                Constants.ReactionType.NONE
            } else {
                Constants.ReactionType.LIKE
            }
        if (card.cardType == Constants.CardType.MEDIA_VIDEO_BIG.toString().lowercase(Locale.getDefault())) {
            ivLike.setImageDrawable(
                Converters().getDisplayImageForVideos(
                    reaction.toString(),
                    v.context,
                    false
                )
            )
        } else {
            ivLike.setImageDrawable(
                Converters().getDisplayImage(
                    reaction.toString(),
                    v.context,
                    false
                )
            )
            try{
                if(reaction.toString().uppercase()== Constants.ReactionType.LIKE.toString()){
                    ivLike.setColorFilter(ContextCompat.getColor(ivLike.context, R.color.purple_500))
                } else {
                    ivLike.setColorFilter(ContextCompat.getColor(ivLike.context, R.color.feedSecondaryTintColor))
                }
            } catch (ex:Exception){
                LogDetail.LogEStack(ex)
            }
        }

        newsFeedList[position].items[0].isReacted = reaction.toString()
        card.items[0].postId?.let {
            FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let { it1 ->
                ApiReactPost().reactPostEncrypted(
                    Endpoints.REACT_POST_ENCRYPTED,
                    it1,
                    FeedSdk.userId, it, reaction)
            }
        }
    }

    override fun onFollowClicked(v: View, position: Int) {
        if (newsFeedList[position].items[0].isFollowingPublisher!!) {
            (v as AppCompatTextView).text = "Follow"
        } else {
            (v as AppCompatTextView).text = "✓Following"
        }
        newsFeedList[position].items[0].isFollowingPublisher =
            !newsFeedList[position].items[0].isFollowingPublisher!!
        newsFeedList[position].items[0].publisherId?.let {
            FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let { it1 ->
                ApiFollowPublihser().followPublisherEncrypted(
                    Endpoints.FOLLOW_PUBLISHER_ENCRYPTED,
                    it1,
                    FeedSdk.userId,
                    it
                )
            }
        }
    }

    override fun onReportClicked(v: View, position: Int, card: Card, type: String) {
        val reportIssueFragment = ReportIssueDialogFragment.newInstance(card.items[0], pageNo, type)
        if (v.context is FragmentActivity) {
            reportIssueFragment.show(
                (v.context as
                        FragmentActivity).supportFragmentManager,
                "reportIssueFragment"
            )
        } else if (((v.context as ContextWrapper).baseContext is FragmentActivity)) {
            reportIssueFragment.show(
                ((v.context as ContextWrapper).baseContext as FragmentActivity).supportFragmentManager,
                "reportIssueFragment"
            )
        }
    }

    override fun onPodcastClicked(v: View, position: Int, card: Card) {
        val intent = Intent(v.context, PodcastPlayerActivity::class.java)
        intent.putExtra(POSITION, position)
        intent.putExtra(INTEREST, interest)
        v.context.startActivity(intent)
    }

    override fun onShowMoreClicked(v: View, card: Card, position: Int) {
        val vText = v as AppCompatTextView
        if(vText.text.toString()=="Show More"){
            try{
                if(newsFeedList[position-1].items[0].id!!.lowercase().contains("artists")){
                    SpUtil.seeAllClickListener["podcast-publisher"]?.onSeeAllClicked(true)
                } else{
                    SpUtil.seeAllClickListener["podcast-category"]?.onSeeAllClicked(true)
                }
            } catch (ex:Exception){ }
            vText.text = "Show Less"
        } else{
            try{
                if(newsFeedList[position-1].items[0].id!!.lowercase().contains("artists")){
                    SpUtil.seeAllClickListener["podcast-publisher"]?.onSeeAllClicked(false)
                } else{
                    SpUtil.seeAllClickListener["podcast-category"]?.onSeeAllClicked(false)
                }
            } catch (ex:Exception){ }
            vText.text = "Show More"
        }
    }

    override fun onSideTextClicked(v: View, position: Int, card: Card) {
        if(position+1 > newsFeedList.size-1)
            return
        when (newsFeedList[position+1].cardType) {
            Constants.CardType.FEED_VIDEOS_HORIZONTAL.toString()
                .toLowerCase(Locale.getDefault()) -> {
                    addBigBites(v, newsFeedList[position+1].items, Constants.exploreResponseDetails.api_uri, Constants.exploreResponseDetails.timestamp)
                }
            Constants.CardType.GET_FEEDS_VIDEOS.toString()
                .toLowerCase(Locale.getDefault()) -> {
                    addBigBites(v, newsFeedList[position+1].items, Constants.feedsResponseDetails.api_uri, Constants.feedsResponseDetails.timestamp)
                }
            Constants.CardType.FEED_REELS.toString()
                .toLowerCase(Locale.getDefault()) -> {
                    addFeedReels(v, newsFeedList[position+1].items, Constants.exploreResponseDetails.api_uri, Constants.exploreResponseDetails.timestamp)
                }
            Constants.CardType.GET_FEEDS_REELS.toString()
                .toLowerCase(Locale.getDefault()) -> {
                    addFeedReels(v, newsFeedList[position+1].items, Constants.feedsResponseDetails.api_uri, Constants.feedsResponseDetails.timestamp)
                }
            Constants.CardType.CRYPTO_WATCHLIST.toString()
                .toLowerCase(Locale.getDefault()) -> {
                val intent = Intent(v.context, CryptoListActivity::class.java)
                intent.putExtra(Constants.ORDER, "alphabetical")
                intent.putExtra(INTEREST, "crypto_watchlist_edit")
                v.context.startActivity(intent)
            }
            Constants.CardType.CRYPTO_GAINERS.toString()
                .toLowerCase(Locale.getDefault()) -> {
                val intent = Intent(v.context, CryptoListActivity::class.java)
                intent.putExtra(Constants.ORDER, "descending")
                intent.putExtra(INTEREST, "crypto_gainers")
                v.context.startActivity(intent)
            }
            Constants.CardType.CRYPTO_LOSERS.toString()
                .toLowerCase(Locale.getDefault()) -> {
                val intent = Intent(v.context, CryptoListActivity::class.java)
                intent.putExtra(Constants.ORDER, "ascending")
                intent.putExtra(INTEREST, "crypto_losers")
                v.context.startActivity(intent)
            }
        }
    }

    private fun addBigBites(v: View, feedVideos: List<Item>, apiUri: String, timestamp: Long){
        val intent = Intent(v.context, FeedsActivity::class.java)
        intent.putExtra(TAG, "Big Bites")
        intent.putExtra(Constants.FEED_TYPE, "big_bites")
        intent.putExtra(Constants.POST_SOURCE, "big_bites")
        intent.putExtra(Constants.INTEREST, "bigBites")
        intent.putExtra("postUrl", apiUri)
        intent.putExtra("timeStamp", timestamp)
        val cardList = ArrayList<Card>()
        for (video in feedVideos) {
            val itemList = ArrayList<Item>()
            itemList.add(video)
            val card = Card(itemList)
            card.cardType = Constants.CardType.MEDIA_VIDEO.toString().lowercase(Locale.getDefault())
            cardList.add(card)
        }
        val loadMore = Card()
        loadMore.cardType = LOADER
        cardList.add(loadMore)
        Constants.bigBites = cardList
        v.context.startActivity(intent)
    }

    private fun addFeedReels(v: View, feedReels: List<Item>, apiUri: String, timestamp: Long) {
        val intent = Intent(v.context, ReelsActivity::class.java)
        intent.putExtra(POSITION, 0)
        intent.putExtra("postUrl", apiUri)
        intent.putExtra("timeStamp", timestamp)
        val cardList = ArrayList<Card>()
        for (reel in feedReels) {
            val itemList = ArrayList<Item>()
            itemList.add(reel)
            val card = Card(itemList)
            card.cardType = Constants.CardType.MEDIA_VIDEO_BIG.toString().lowercase(Locale.getDefault())
            cardList.add(card)
        }
        Constants.reels = cardList
        v.context.startActivity(intent)
    }

    override fun onShareClicked(v: View, position: Int) {
        if (!onSomethingClicked) {
            onSomethingClicked = true
            val sharingIntent = Intent(Intent.ACTION_SEND)
            sharingIntent.type = "text/plain"
            val shareBody = FeedSdk.shareBody
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "Share App")
            sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody)
            v.context.startActivity(Intent.createChooser(sharingIntent, "Share via"))
            Handler(Looper.getMainLooper()).postDelayed({
                onSomethingClicked = false
            }, 500)
        }
    }

    override fun onMayLikeInterestClicked(v: View, position:Int, interest: String, isLiked: Boolean) {
        try{
            if(!isLiked){
                newsFeedList.removeAt(position)
                notifyItemRemoved(position)
                FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let {
                    ApiCreateOrUpdateUser().updateUserDislikeInterests(it, interest)
                }
            } else {
                var interests = ""
                for(likedInterest in Constants.userDetails!!.interests){
                    interests+= "$likedInterest,"
                }
                interests+=interest
                newsFeedList.removeAt(position)
                notifyItemRemoved(position)
                FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let {
                    ApiCreateOrUpdateUser().updateUserInterests(it, interests)
                }
            }
        } catch (ex:Exception){
            LogDetail.LogEStack(ex)
        }
    }

    override fun onShareCovidData(v: View, position: Int, isWhatsApp: Boolean){
        if (!onSomethingClicked){
            onSomethingClicked = true

            val link: String = FeedSdk.mFirebaseDynamicLink + "?covid_card=" + position.toString()
            try {
                val prefix = "Corona Stats: "
                FirebaseDynamicLinks.getInstance().createDynamicLink()
                    .setLink(Uri.parse(link))
                    .setDomainUriPrefix(FeedSdk.mFirebaseDynamicLink)
                    .setAndroidParameters(
                        DynamicLink.AndroidParameters.Builder(v.context.packageName).build() )
//                    .setSocialMetaTagParameters(
//                        DynamicLink.SocialMetaTagParameters.Builder()
//                            .setTitle(FeedSdk.appName ?: "COVID_CARD")
//                            .setDescription(card.items[0].content?.title ?: "")
//                            .setImageUrl(
//                                Uri.parse(
//                                    card.items[0].content?.mediaList?.get(0)
//                                        ?: ""
//                                )
//                            ).build()
//                    )
                    .buildShortDynamicLink()
                    .addOnSuccessListener { shortDynamicLink ->
                        val shortLink: String =
                            prefix+shortDynamicLink.shortLink.toString() + v.context.getString(
                                R.string.dynamic_link_url_suffix
                            ) + " " + FeedSdk.appName + " " + v.context.getString(
                                R.string.dynamic_link_url_suffix2
                            ) + v.context.packageName
                        if (isWhatsApp) {
                            val whatsAppIntent = Intent(Intent.ACTION_SEND)
                            whatsAppIntent.type = "text/plain"
                            whatsAppIntent.setPackage("com.whatsapp")
                            whatsAppIntent.putExtra(Intent.EXTRA_TEXT, shortLink)
                            try {
                                v.context.startActivity(whatsAppIntent)
                            } catch (ex: ActivityNotFoundException) {
                                Toast.makeText(
                                    v.context,
                                    "WhatsApp is not installed!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            val intent = Intent(Intent.ACTION_SEND)
                            intent.type = "text/plain"
                            intent.putExtra(Intent.EXTRA_TEXT, shortLink)
                            v.context.startActivity(Intent.createChooser(intent, "Share via"))
                        }
                    }
                    .addOnFailureListener { e ->
                        LogDetail.LogEStack(e)
                        try {
                            if (isWhatsApp) {
                                val whatsAppIntent = Intent(Intent.ACTION_SEND)
                                whatsAppIntent.type = "text/plain"
                                whatsAppIntent.setPackage("com.whatsapp")
                                whatsAppIntent.putExtra(
                                    Intent.EXTRA_TEXT,
//                                    card.items[0].content?.url + "\n\n" +
                                            Objects.requireNonNull(
                                        spUtilInstance
                                    )!!
                                        .getString(Constants.SHARE_MESSAGE)
                                            + v.context.getString(R.string.share_link_prefix) + v.context.packageName
                                )
                                try {
                                    v.context.startActivity(whatsAppIntent)
                                } catch (ex: ActivityNotFoundException) {
                                    Toast.makeText(
                                        v.context,
                                        "Whatsapp have not been installed.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } else {
                                val intent = Intent(Intent.ACTION_SEND)
                                intent.type = "text/plain"
                                intent.putExtra(
                                    Intent.EXTRA_TEXT,
//                                    card.items[0].content?.url + "\n\n" +
                                            Objects.requireNonNull(
                                        spUtilInstance
                                    )!!
                                        .getString(Constants.SHARE_MESSAGE)
                                            + v.context.getString(R.string.share_link_prefix) + v.context.packageName
                                )
                                v.context.startActivity(Intent.createChooser(intent, "Share via"))
                            }
                        } catch (e1: Exception) {
                            Toast.makeText(
                                v.context,
                                v.context.getString(R.string.error_share_post),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            } catch (e: Exception) {
                LogDetail.LogEStack(e)
            }
            Handler(Looper.getMainLooper()).postDelayed({
                onSomethingClicked = false
            },500)
        }
    }

    override fun onSharePost(v: View, position: Int, card: Card, isWhatsApp: Boolean) {
        if (!onSomethingClicked) {
            onSomethingClicked = true
            try {
                if (eventsListener != null) {
                    if (newsFeedList[position].cardType!! == Constants.CardType.MEDIA_VIDEO_BIG.toString()
                            .toLowerCase(Locale.getDefault())
                    ) {
                        eventsListener!!.onVideoInteraction(
                            newsFeedList[position].items[0].postSource!!,
                            interest,
                            newsFeedList[position].items[0].postId!!,
                            newsFeedList[position].items[0].platform!!,
                            "share"
                        )
                    } else {
                        if (isWhatsApp) {
                            eventsListener!!.onFeedInteraction(
                                newsFeedList[position].items[0].postSource!!,
                                interest,
                                newsFeedList[position].items[0].postId!!,
                                newsFeedList[position].items[0].platform!!,
                                "whatsappShare"
                            )
                        } else {
                            eventsListener!!.onFeedInteraction(
                                newsFeedList[position].items[0].postSource!!,
                                interest,
                                newsFeedList[position].items[0].postId!!,
                                newsFeedList[position].items[0].platform!!,
                                "share"
                            )
                        }
                    }
                }
            } catch (ex: java.lang.Exception) {
                LogDetail.LogEStack(ex)
            }
            val link: String = FeedSdk.mFirebaseDynamicLink + "?feed_id=" + card.items[0].postId!!
            try {
                var prefix = ""
                if(!card.items[0].content?.title.isNullOrEmpty()){
                    prefix = HtmlCompat.fromHtml(card.items[0].content?.title?:"", HtmlCompat.FROM_HTML_MODE_LEGACY).toString()+"\n"
                } else if(!card.items[0].content?.description.isNullOrEmpty()){
                    prefix = HtmlCompat.fromHtml(card.items[0].content?.description?:"", HtmlCompat.FROM_HTML_MODE_LEGACY).toString()+"\n"
                }
                FirebaseDynamicLinks.getInstance().createDynamicLink()
                    .setLink(Uri.parse(link))
                    .setDomainUriPrefix(FeedSdk.mFirebaseDynamicLink)
                    .setAndroidParameters(
                        DynamicLink.AndroidParameters.Builder(v.context.packageName).build()
                    )
                    .setSocialMetaTagParameters(
                        DynamicLink.SocialMetaTagParameters.Builder()
                            .setTitle(FeedSdk.appName ?: "Feed")
                            .setDescription(card.items[0].content?.title ?: "")
                            .setImageUrl(
                                Uri.parse(
                                    card.items[0].content?.mediaList?.get(0)
                                        ?: ""
                                )
                            ).build()
                    )
                    .buildShortDynamicLink()
                    .addOnSuccessListener { shortDynamicLink ->
                        val shortLink: String =
                            prefix+shortDynamicLink.shortLink.toString() + v.context.getString(
                                R.string.dynamic_link_url_suffix
                            ) + " " + FeedSdk.appName + " " + v.context.getString(
                                R.string.dynamic_link_url_suffix2
                            ) + v.context.packageName
                        if (isWhatsApp) {
                            val whatsAppIntent = Intent(Intent.ACTION_SEND)
                            whatsAppIntent.type = "text/plain"
                            whatsAppIntent.setPackage("com.whatsapp")
                            whatsAppIntent.putExtra(Intent.EXTRA_TEXT, shortLink)
                            try {
                                v.context.startActivity(whatsAppIntent)
                            } catch (ex: ActivityNotFoundException) {
                                Toast.makeText(
                                    v.context,
                                    "WhatsApp is not installed!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            val intent = Intent(Intent.ACTION_SEND)
                            intent.type = "text/plain"
                            intent.putExtra(Intent.EXTRA_TEXT, shortLink)
                            v.context.startActivity(Intent.createChooser(intent, "Share via"))
                        }
                    }
                    .addOnFailureListener { e ->
                        LogDetail.LogEStack(e)
                        try {
                            if (isWhatsApp) {
                                val whatsAppIntent = Intent(Intent.ACTION_SEND)
                                whatsAppIntent.type = "text/plain"
                                whatsAppIntent.setPackage("com.whatsapp")
                                whatsAppIntent.putExtra(
                                    Intent.EXTRA_TEXT,
                                    card.items[0].content?.url + "\n\n" + Objects.requireNonNull(
                                        spUtilInstance
                                    )!!
                                        .getString(Constants.SHARE_MESSAGE)
                                            + v.context.getString(R.string.share_link_prefix) + v.context.packageName
                                )
                                try {
                                    v.context.startActivity(whatsAppIntent)
                                } catch (ex: ActivityNotFoundException) {
                                    Toast.makeText(
                                        v.context,
                                        "Whatsapp have not been installed.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } else {
                                val intent = Intent(Intent.ACTION_SEND)
                                intent.type = "text/plain"
                                intent.putExtra(
                                    Intent.EXTRA_TEXT,
                                    card.items[0].content?.url + "\n\n" + Objects.requireNonNull(
                                        spUtilInstance
                                    )!!
                                        .getString(Constants.SHARE_MESSAGE)
                                            + v.context.getString(R.string.share_link_prefix) + v.context.packageName
                                )
                                v.context.startActivity(Intent.createChooser(intent, "Share via"))
                            }
                        } catch (e1: Exception) {
                            Toast.makeText(
                                v.context,
                                v.context.getString(R.string.error_share_post),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            } catch (e: Exception) {
                LogDetail.LogEStack(e)
            }
            Handler(Looper.getMainLooper()).postDelayed({
                onSomethingClicked = false
            }, 500)
        }
    }

    fun refreshList(newsFeedListUpdated: ArrayList<Card>) {
        this.newsFeedList = newsFeedListUpdated
        notifyDataSetChanged()
    }

    fun refreshList(newsFeedListUpdated: ArrayList<Card>, from: Int) {
        this.newsFeedList = newsFeedListUpdated
        val itemCount = newsFeedList.size - from
        notifyItemRangeChanged(from, itemCount)
    }

    fun updateExploreReelsList(newsFeedListUpdated: ArrayList<Card>, pageNumber: Int) {
        val oldSize = newsFeedList.size
        newsFeedList.addAll(newsFeedListUpdated)
        val newSize = newsFeedList.size
        pageNo = pageNumber
        notifyItemRangeChanged(oldSize, newSize - oldSize)
    }

    fun getScreenType(): String {
        when (interest) {
            "hashtagActivity" -> return Constants.HASHTAG
            "reels" -> return Constants.EXPLORE
            "videofeed" -> return Constants.VIDEO_FEED
            else -> return Constants.FEED
        }
    }

    fun updateList(newsFeedListUpdated: ArrayList<Card>, selectedInterest: String, pageNumber: Int, requestUrl: String? = null, requestTimeStamp: Long? = 0) {
        var oldSize = 0
        pageNo = pageNumber
        presentUrl = requestUrl
        presentTimeStamp = requestTimeStamp
        if (selectedInterest != "videofeed") {
            newsFeedList.removeAt(newsFeedList.size - 1)
            notifyItemRemoved(newsFeedList.size - 1)
            oldSize = newsFeedList.size
            newsFeedList.addAll(newsFeedListUpdated)
            if(newsFeedListUpdated.isNotEmpty()) {
                val loadMore = Card()
                loadMore.cardType = LOADER
                newsFeedList.add(loadMore)
            }
        } else {
            oldSize = newsFeedList.size
        }
        val newSize = newsFeedList.size
        notifyItemRangeChanged(oldSize+1, newSize - oldSize)
        Constants.cardsMap[selectedInterest] = newsFeedList
    }

    @Suppress("DEPRECATION")
    override fun onViewAttachedToWindow(v: View, position: Int) {
        LogDetail.LogD("onViewAttached", "onViewAttachedToWindow")
        try{
            if (v is StyledPlayerView) {
                Handler(Looper.getMainLooper()).postDelayed({
                    AudioTracker.init(v.context, "Feeds", AudioTracker.VIDEOS, newsFeedList[position].items[0].postId, object : AudioTrackerListener{
                        override fun onSuccess() {
                            v.player?.playWhenReady = true
                        }

                        override fun onFailure() {
                            v.player?.playWhenReady = false
                        }
                    })
                }, 100)
            }
        } catch (ex:Exception){
            LogDetail.LogEStack(ex)
        }
    }

    @Suppress("DEPRECATION")
    override fun onViewDetachedFromWindow(v: View, position: Int) {
        LogDetail.LogD("onViewDetached", "onViewDetachedFromWindow")
        if (v is StyledPlayerView) {
            v.player?.playWhenReady = false
            try {
                if (newsFeedList[position].items[0].platform != "youtube") {
                    var duration = 0
                    var totalDuration = 0
                    if (v.player?.duration != null) {
                        totalDuration = (v.player!!.duration / 1000).toInt()
                    }
                    if (v.player?.currentPosition != null) {
                        duration = (v.player!!.currentPosition / 1000).toInt()
                    }
                    postImpressionListener?.addImpression(
                        newsFeedList[position],
                        totalDuration,
                        duration
                    )
                }
            } catch (ex:Exception){
                LogDetail.LogEStack(ex)
            }
        }
    }

    override fun onInterestCardClicked(v:View) {
        try{
            val bottomSheet = AddInterestBottomSheet.newInstance()
            bottomSheet.show((v.context as FragmentActivity).supportFragmentManager,"BottomSheet")
        } catch (ex:Exception){
            val intent = Intent(v.context, FeedInterestsActivity::class.java)
            v.context.startActivity(intent)
        }
    }

    override fun onLanguageCardClicked(v: View) {
        val intent = Intent(v.context, FeedLanguageActivity::class.java)
        v.context.startActivity(intent)
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is VideoViewHolder) {
            holder.view.newsItemVideo.player?.playWhenReady = false
            holder.view.newsItemVideo.player?.release()
        } else if (holder is BigVideoViewHolder) {
            holder.view.newsItemVideo.player?.playWhenReady = false
            holder.view.newsItemVideo.player?.release()
        }
    }


    override fun onLiveMatchClicked(v: View, position: Int) {
        val intent = Intent(v.context, PWAMatchScoreActivity::class.java)
        intent.putExtra("matchType", Constants.LIVE_MATCHES)
        intent.putExtra("filename", newsFeedList[position].items[0].matchfile)
        intent.putExtra("post_source", "cricket_fever")
        intent.putExtra("link", newsFeedList[position].items[0].pwaUrl)
        v.context.startActivity(intent)
    }

    override fun onUpcomingMatchClicked(v: View, position: Int) {
        val intent = Intent(v.context, PWAMatchScoreActivity::class.java)
        intent.putExtra("matchType", Constants.UPCOMING_MATCHES)
        intent.putExtra("filename", newsFeedList[position].items[0].matchfile)
        intent.putExtra("post_source", "cricket_fever")
        intent.putExtra("link", newsFeedList[position].items[0].pwaUrl)
        v.context.startActivity(intent)
    }

    override fun onPastMatchClicked(v: View, position: Int) {
        val intent = Intent(v.context, PWAMatchScoreActivity::class.java)
        intent.putExtra("matchType", Constants.PAST_MATCHES)
        intent.putExtra("filename", newsFeedList[position].items[0].matchfile)
        intent.putExtra("post_source", "cricket_fever")
        intent.putExtra("link", newsFeedList[position].items[0].pwaUrl)
        v.context.startActivity(intent)
    }

    fun pausePlayer(holder: VideoViewHolder) {
        try {
            holder.view.newsItemVideo.player?.playWhenReady = false
            var duration = 0
            var totalDuration = 0
            if (holder.view.newsItemVideo.player?.duration != null) {
                totalDuration = (holder.view.newsItemVideo.player!!.duration / 1000).toInt()
            }
            if (holder.view.newsItemVideo.player?.currentPosition != null) {
                duration = (holder.view.newsItemVideo.player!!.currentPosition / 1000).toInt()
            }
            postImpressionListener?.addImpression(
                newsFeedList[holder.view.position!!],
                totalDuration,
                duration
            )
        } catch (e: Exception) {
            LogDetail.LogEStack(e)
        }
    }

    fun releasePlayers(holders: List<BigVideoViewHolder?>){
        try {
            holders.forEach {
                if (it != null && it.view.llYoutubeView.visibility != View.VISIBLE){
                    it.view.newsItemVideo.player?.release()
                }
            }
        }catch (ex: Exception){
            LogDetail.LogEStack(ex)
        }
    }

    fun playVideo(holder: VideoViewHolder) {
        try {
            AudioTracker.init(holder.itemView.context, "Feeds", AudioTracker.VIDEOS, newsFeedList[holder.bindingAdapterPosition].items[0].postId, object : AudioTrackerListener{
                override fun onSuccess() {
                    holder.view.newsItemVideo.player?.playWhenReady = true
                }

                override fun onFailure() {
                    holder.view.newsItemVideo.player?.playWhenReady = false
                }
            })
        } catch (e: Exception) {
            LogDetail.LogEStack(e)
        }
    }

    fun pausePlayer(holder: BigVideoViewHolder) {
        try {
            LogDetail.LogD("Check", "pausePlayer " + holder.view.position)
            if (holder.view.llYoutubeView.visibility == View.VISIBLE){
                youtubePlayerView.keys.forEach {
                    youtubePlayerView[it]?.getYouTubePlayerWhenReady(object: YouTubePlayerCallback {
                        override fun onYouTubePlayer(youTubePlayer: YouTubePlayer) {
                            youTubePlayer.pause()
                        }
                    })
                }
            }else {
                holder.view.newsItemVideo.player!!.playWhenReady = false
                try {
                    var duration = 0
                    var totalDuration = 0
                    if (holder.view.newsItemVideo.player?.duration != null) {
                        totalDuration = (holder.view.newsItemVideo.player!!.duration / 1000).toInt()
                    }
                    if (holder.view.newsItemVideo.player?.currentPosition != null) {
                        duration = (holder.view.newsItemVideo.player!!.currentPosition / 1000).toInt()
                    }
                    postImpressionListener?.addImpression(
                        newsFeedList[holder.view.position!!],
                        totalDuration,
                        duration
                    )
                } catch (ex:Exception){
                    LogDetail.LogEStack(ex)
                }
            }
        } catch (e: Exception) {
            LogDetail.LogEStack(e)
        }
    }

    fun playVideo(holder: BigVideoViewHolder, position: Int) {
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                LogDetail.LogD("ISPLAYING", "position: $position == ${holder.view.position}")
                if (holder.view.llYoutubeView.visibility == View.VISIBLE){
                    youtubePlayerView.keys.forEach {
                        if (it == position){
                            youtubePlayerView[it]?.getYouTubePlayerWhenReady(object: YouTubePlayerCallback{
                                override fun onYouTubePlayer(youTubePlayer: YouTubePlayer) {
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        youTubePlayer.play()
                                    }, 300)
                                }
                            })
                        }else{
                            youtubePlayerView[it]?.getYouTubePlayerWhenReady(object: YouTubePlayerCallback{
                                override fun onYouTubePlayer(youTubePlayer: YouTubePlayer) {
                                    youTubePlayer.pause()
                                }
                            })
                        }
                    }
                }else {
                    holder.view.llYoutubeView.removeAllViewsInLayout()
                    LogDetail.LogD("Check", "playVideo " + holder.view.position)
                    AudioTracker.init(holder.itemView.context, "Reels", AudioTracker.REELS, newsFeedList[holder.bindingAdapterPosition].items[0].postId,object : AudioTrackerListener{
                        override fun onSuccess() {
                            holder.view.newsItemVideo.player?.playWhenReady = true
                        }

                        override fun onFailure() {
                            holder.view.newsItemVideo.player?.playWhenReady = false
                        }
                    })
                }
            } catch (e: Exception) {
                LogDetail.LogEStack(e)
            }
        }, 100)
    }

    override fun setUpYoutubeVideo(view: StyledPlayerView, position: Int, youtubeUrl: String){
        var youtubeVidDuration = 10000L
        val llYoutubeView =
            (view.parent as ConstraintLayout).findViewById<LinearLayout>(R.id.llYoutubeView)
        val mute = (view.parent as ConstraintLayout).findViewById<AppCompatImageView>(R.id.mute)
        try {
            llYoutubeView.removeAllViews()
        }catch (e: java.lang.Exception){
            LogDetail.LogEStack(e)
        }

        youtubePlayerView[position] = YouTubePlayerView(mContext)
        val youtubePlay =   youtubePlayerView[position]
        if (youtubePlay != null) {
            observeYoutubePlayer(youtubePlay)
        }

        val youtubeUI = youtubePlay?.getPlayerUiController()
        youtubeUI?.apply {
            showUi(false)
        }
        youtubePlay?.setOnClickListener {
            if (mute.visibility == View.GONE) {
                mute.visibility = View.VISIBLE
                if (Constants.isMuted) {
                    mute.setImageResource(R.drawable.ic_feed_mute)
                } else {
                    mute.setImageResource(R.drawable.ic_feed_unmute)
                }
                Handler(Looper.getMainLooper()).postDelayed(
                    { (view.parent as ConstraintLayout).performClick() },
                    3000
                )
            } else {
                mute.visibility = View.GONE
            }
        }
        youtubePlay?.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                youTubePlayer.cueVideo(youtubeUrl, 0f)
                (view.parent as ConstraintLayout).setOnClickListener {
                    if (mute.visibility == View.GONE) {
                        mute.visibility = View.VISIBLE
                        if (Constants.isMuted) {
                            mute.setImageResource(R.drawable.ic_feed_mute)
                        } else {
                            mute.setImageResource(R.drawable.ic_feed_unmute)
                        }
                        Handler(Looper.getMainLooper()).postDelayed(
                            { (view.parent as ConstraintLayout).performClick() },
                            3000
                        )
                    } else {
                        mute.visibility = View.GONE
                    }
                }

                mute.setOnClickListener {
                    Constants.isMuted = !Constants.isMuted
                    if (Constants.isMuted) {
                        youTubePlayer.mute()
                        mute.setImageResource(R.drawable.ic_feed_mute)
                    } else {
                        youTubePlayer.unMute()
                        mute.setImageResource(R.drawable.ic_feed_unmute)
                    }

                }
            }

            override fun onStateChange(youTubePlayer: YouTubePlayer, state: PlayerConstants.PlayerState) {

                if (state == PlayerConstants.PlayerState.ENDED) {
                    onVideoEnded(position, youtubeVidDuration)
                    youTubePlayer.seekTo(0f)
                }

                if (Constants.isMuted) {
                    youTubePlayer.mute()
                } else {
                    youTubePlayer.unMute()
                }
                super.onStateChange(youTubePlayer, state)
            }

            override fun onVideoDuration(youTubePlayer: YouTubePlayer, duration: Float) {
                super.onVideoDuration(youTubePlayer, duration)
                youtubeVidDuration = duration.toLong()
            }
        })

        llYoutubeView.addView(youtubePlay)
    }

    override fun releaseYoutubeVideo() {
//        youtubePlayerView?.release()
    }


    override fun onVideoEnded(position: Int, duration: Long) {
        videoPlayerListener?.onVideoEnded(position, duration)
        try {
            val totalDuration = (duration / 1000).toInt()
            postImpressionListener?.addImpression(
                newsFeedList[position],
                totalDuration,
                totalDuration
            )
        } catch (ex:Exception){
            LogDetail.LogEStack(ex)
        }
    }

    class BigFeedViewHolder(val view: ItemFeedBigBinding) : RecyclerView.ViewHolder(view.root)
    class SmallFeedViewHolder(val view: ItemNewsFeedSmallBinding) :
        RecyclerView.ViewHolder(view.root)

    class RatingViewHolder(val view: ItemRatingCardBinding) : RecyclerView.ViewHolder(view.root)
    class CryptoRatingViewHolder(val view: CryptoRatingCardBinding) : RecyclerView.ViewHolder(view.root)
    class ShareViewHolder(val view: ItemShareCardBinding) : RecyclerView.ViewHolder(view.root)
    class TelegramChannelHolder(val view: ItemTelegramCardBinding) : RecyclerView.ViewHolder(view.root)
    class LoadMoreViewHolder(val view: ItemLoadMoreBinding) : RecyclerView.ViewHolder(view.root)
    class AdViewHolder(val view: ItemAdLayoutBinding) : RecyclerView.ViewHolder(view.root)
    class LargeAdViewHolder(val view: ItemAdLayoutLargeBinding) : RecyclerView.ViewHolder(view.root)
    class VideoViewHolder(val view: ItemFeedVideoBinding) : RecyclerView.ViewHolder(view.root)
    class TitleIconViewHolder(val view: ItemTitleIconBinding) : RecyclerView.ViewHolder(view.root)
    class TitleViewHolder(val view: ItemTitleBinding) : RecyclerView.ViewHolder(view.root)
    class HashtagsViewHolder(val view: ItemFeedHashtagsBinding) : RecyclerView.ViewHolder(view.root)
    class PopularAccountsCardViewHolder(val view: ItemPopularAccountCardBinding) :
        RecyclerView.ViewHolder(view.root)

    class FeedPostsCategoryViewHolder(val view: ItemFeedPostsCategoryCardBinding) :
        RecyclerView.ViewHolder(view.root)

    class FeedVideosHorizontalViewHolder(val view: ItemFeedVideosHorizontalCardBinding) :
        RecyclerView.ViewHolder(view.root)

    class FeedReelsViewHolder(val view: ItemFeedReelsCardBinding) :
        RecyclerView.ViewHolder(view.root)

    class EmptyViewHolder(val view: ItemEmptyBinding) : RecyclerView.ViewHolder(view.root)
    class BigVideoViewHolder(val view: ItemVideoBigBinding) : RecyclerView.ViewHolder(view.root)
    class InterestsViewHolder(val view: ItemInterestsCardBinding) :
        RecyclerView.ViewHolder(view.root)

    class LanguagesViewHolder(val view: ItemLanguageCardBinding) :
        RecyclerView.ViewHolder(view.root)

    class CricketTrendingPostsViewHolder(val view: ItemTrendingPostsBinding) :
            RecyclerView.ViewHolder(view.root)

    class MediaPodcastViewHolder(val view: ItemMediaPodcastBinding) :
        RecyclerView.ViewHolder(view.root)

    class FeedIconHastagsCircleViewHolder(val view: ItemHashtagsCircleBinding) :
        RecyclerView.ViewHolder(view.root)

    class CryptoWatchlistViewHolder(val view: LayoutCryptoWatchlistBinding) :
        RecyclerView.ViewHolder(view.root)

    class CryptoGainersViewHolder(val view: LayoutCryptoGainersBinding) :
        RecyclerView.ViewHolder(view.root)

    class DescriptionViewHolder(val view: ItemDescriptionBinding) :
        RecyclerView.ViewHolder(view.root)

    class CoinLinksViewHolder(val view: ItemCoinLinksBinding) :
        RecyclerView.ViewHolder(view.root)

    class CoinOverViewHolder(val view: ItemCoinOverviewBinding) :
        RecyclerView.ViewHolder(view.root)

    class CoinMarketViewHolder(val view: LayoutCryptoGainersBinding) :
        RecyclerView.ViewHolder(view.root)

    class CryptoPodcastsViewHolder(val view: ItemCryptoPodcastsBinding) :
        RecyclerView.ViewHolder(view.root)

    class SearchFeedSmallViewHolder(val view: ItemSearchFeedSmallBinding) :
        RecyclerView.ViewHolder(view.root)

    class SearchFeedBigViewHolder(val view: ItemSearchFeedBigBinding) :
        RecyclerView.ViewHolder(view.root)

    class RegionalNewsHolder(val view: ItemRegionalNewsBinding):
        RecyclerView.ViewHolder(view.root)

    class FeedCovidTrackerHolder(val view: ItemFeedCovidTrackerBinding) :
        RecyclerView.ViewHolder(view.root)

    class CryptoAlertHolder(val view: ItemCryptoAlertListBinding) :
        RecyclerView.ViewHolder(view.root)

    class MayLikeInterestHolder(val view: ItemFeedLikeInterestsBinding) :
        RecyclerView.ViewHolder(view.root)
}
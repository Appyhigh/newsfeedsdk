package com.appyhigh.newsfeedsdk.customview


import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.webkit.WebSettings
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.adapter.NewsFeedAdapter
import com.appyhigh.newsfeedsdk.apicalls.ApiAppLock
import com.appyhigh.newsfeedsdk.apicalls.ApiConfig
import com.appyhigh.newsfeedsdk.apicalls.ApiGetFeeds
import com.appyhigh.newsfeedsdk.apicalls.ApiPostImpression
import com.appyhigh.newsfeedsdk.apiclient.Endpoints
import com.appyhigh.newsfeedsdk.callbacks.PostImpressionListener
import com.appyhigh.newsfeedsdk.callbacks.VideoPlayerListener
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.model.PostImpressionsModel
import com.appyhigh.newsfeedsdk.model.PostView
import com.appyhigh.newsfeedsdk.model.feeds.Card
import com.appyhigh.newsfeedsdk.model.feeds.GetFeedsResponse
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.gson.Gson


class SinglePostView : LinearLayout {
    private var pageNo = 0
    private var adIndex = 0
    private var feedType = "applock_feed"
    private var pbLoading: ProgressBar? = null
    private var newsFeedAdapter: NewsFeedAdapter? = null
    private var linearLayoutManager: LinearLayoutManager? = null
    private var rvPosts: RecyclerView? = null
    private var newsFeedList = ArrayList<Card>()
    private var presentUrl = ""
    private var presentTimeStamp: Long = 0
    var currentPosition = 0
    var postImpressions = HashMap<String, PostView>()
    private var showMultiplePosts = false
    private var language = ""
    private var interests = ""

    constructor(context: Context?) : super(context) {
        initSDK()
    }

    constructor(context: Context?, showMultiplePosts: Boolean) : super(context) {
        this.showMultiplePosts =  showMultiplePosts
        initSDK()
    }

    constructor(context: Context?, showMultiplePosts: Boolean, language: String) : super(context) {
        this.showMultiplePosts =  showMultiplePosts
        this.language = language
        initSDK()
    }

    constructor(context: Context?, showMultiplePosts: Boolean, interests: String, language: String) : super(context) {
        this.showMultiplePosts =  showMultiplePosts
        this.interests = interests
        this.language = language
        initSDK()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        try{
            val attrs = context!!.obtainStyledAttributes(attrs, R.styleable.SinglePostView, 0, 0)
            showMultiplePosts = attrs.getBoolean(R.styleable.SinglePostView_showMultiplePosts, false)
            attrs.getString(R.styleable.SinglePostView_interests)?.let { interests = it  }
            attrs.getString(R.styleable.SinglePostView_language)?.let { language = it  }
            initSDK()
            attrs.recycle()
        } catch (ex:Exception){
            initSDK()
        }
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        try{
            val attrs = context!!.obtainStyledAttributes(attrs, R.styleable.SinglePostView, defStyleAttr, 0)
            showMultiplePosts = attrs.getBoolean(R.styleable.SinglePostView_showMultiplePosts, false)
            initSDK()
            attrs.recycle()
        } catch (ex:Exception){
            initSDK()
        }
    }

    private fun initSDK() {
        if (FeedSdk.isSdkInitializationSuccessful) {
            initView()
        } else {
            FeedSdk().setListener(object : FeedSdk.OnUserInitialized {
                override fun onInitSuccess() {
                    initView()
                }
            },"singlePostView")
        }
    }

    private fun initView() {
        val view = inflate(context, R.layout.layout_app_lock_posts, this)
        pbLoading = view.findViewById(R.id.pbLoading)
        rvPosts = view.findViewById(R.id.rvPosts)
        linearLayoutManager = LinearLayoutManager(context)
        try{
            Constants.userAgent = WebSettings.getDefaultUserAgent(context)
        } catch (ex:Exception){}
        getAppLockFeeds()
    }

    private fun getAppLockFeeds() {
        adIndex = 0
        ApiAppLock().getAppLockPosts(
            showMultiplePosts,
            showOnlyReels = false,
            interests,
            language,
            object : ApiGetFeeds.GetFeedsResponseListener {
                override fun onSuccess(
                    getFeedsResponse: GetFeedsResponse,
                    url: String,
                    timeStamp: Long
                ) {
                    storeData(presentUrl, presentTimeStamp)
                    presentTimeStamp = timeStamp
                    presentUrl = url
                    adIndex += getFeedsResponse.adPlacement[0]
                    pageNo += 1
                    pbLoading?.visibility = View.GONE
//                    newsFeedList = getFeedsResponse.cards as ArrayList<Card>
                    val size = getFeedsResponse.cards.size
                    for (card in getFeedsResponse.cards) {
                        if (size>1 || card.cardType == Constants.CardType.NEWS_SMALL_FEATURE.toString()
                                .lowercase()
                        ) {
                            card.cardType = Constants.SEARCH_FEED_SMALL
                        } else {
                            card.cardType = Constants.SEARCH_FEED_BIG
                        }
                        if (card.items[0].content?.mediaList.isNullOrEmpty()) {
                            card.items[0].content!!.mediaList = arrayListOf("")
                        }
                        newsFeedList.add(card)
                    }
                    if (ApiConfig().checkShowAds(context) && false) {
                        try {
                            val adItem = Card()
                            adItem.cardType = Constants.AD
                            newsFeedList.add(adIndex, adItem)
                            LogDetail.LogD("Ad index", adIndex.toString())
                        } catch (ex: Exception) {
                            LogDetail.LogEStack(ex)
                        }
                    }
                    newsFeedAdapter =
                        NewsFeedAdapter(
                            newsFeedList,
                            object : NewsFeedList.PersonalizationListener {
                                override fun onPersonalizationClicked() {
                                }

                                override fun onRefresh() {
                                }
                            },
                            "applock_feed",
                            object : VideoPlayerListener {
                                override fun onVideoEnded(position: Int, duration: Long) {
                                    if (position + 1 < newsFeedList.size - 1) {
                                        rvPosts?.smoothScrollToPosition(position + 1)
                                    }
                                }

                                override fun setUpYoutubeVideo(
                                    view: StyledPlayerView,
                                    position: Int,
                                    youtubeUrl: String
                                ) {
                                }

                                override fun releaseYoutubeVideo() {}
                            },
                            object : PostImpressionListener {
                                override fun addImpression(
                                    card: Card,
                                    totalDuration: Int?,
                                    watchedDuration: Int?
                                ) {
                                    try {
                                        val postView = PostView(
                                            FeedSdk.sdkCountryCode ?: "in",
                                            feedType,
                                            card.items[0].isVideo,
                                            card.items[0].languageString,
                                            Constants.getInterestsString(card.items[0].interests),
                                            card.items[0].postId,
                                            card.items[0].postSource,
                                            card.items[0].publisherId,
                                            card.items[0].shortVideo,
                                            card.items[0].source,
                                            totalDuration,
                                            watchedDuration
                                        )
                                        postImpressions.put(
                                            card.items[0].postId!!,
                                            postView
                                        )
                                    } catch (ex: java.lang.Exception) {
                                        LogDetail.LogEStack(ex)
                                    }
                                }
                            })
                    rvPosts?.apply {
                        layoutManager = linearLayoutManager
                        adapter = newsFeedAdapter
                        itemAnimator = null
                    }
                    rvPosts?.isNestedScrollingEnabled = true
                    Constants.cardsMap["applock_feed"] = newsFeedList
                }
            })
    }

    fun onResume() {
        if (FeedSdk.areContentsModified[Constants.FEED] == true) {
            FeedSdk.areContentsModified[Constants.FEED] = false
            Constants.cardsMap["applock_feed"]?.let { newsFeedAdapter?.refreshList(it) }
        }
    }

    @SuppressLint("CommitPrefEdits")
    fun storeData(url: String, timeStamp: Long) {
        try {
            if (postImpressions.isEmpty()) {
                return
            }
            val postImpressionsModel =
                PostImpressionsModel(url, postImpressions.values.toList(), timeStamp)
            val gson = Gson()
            val sharedPrefs =
                context.getSharedPreferences("postImpressions", Context.MODE_PRIVATE)
            val postImpressionString = gson.toJson(postImpressionsModel)
            sharedPrefs.edit().putString(timeStamp.toString(), postImpressionString).apply()
            postImpressions = HashMap()
            ApiPostImpression().addPostImpressionsEncrypted(
                Endpoints.POST_IMPRESSIONS_ENCRYPTED,
                context
            )
        } catch (ex: java.lang.Exception) {
            LogDetail.LogEStack(ex)
        }
    }

    fun onDestroy() {
        storeData(presentUrl, presentTimeStamp)
    }

}
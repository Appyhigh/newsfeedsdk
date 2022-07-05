package com.appyhigh.newsfeedsdk.customview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import androidx.viewpager2.widget.ViewPager2
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.Constants.cardsMap
import com.appyhigh.newsfeedsdk.Constants.getLifecycleOwner
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.adapter.NewsFeedAdapter
import com.appyhigh.newsfeedsdk.apicalls.*
import com.appyhigh.newsfeedsdk.apiclient.Endpoints
import com.appyhigh.newsfeedsdk.callbacks.OnRefreshListener
import com.appyhigh.newsfeedsdk.callbacks.PostImpressionListener
import com.appyhigh.newsfeedsdk.callbacks.VideoPlayerListener
import com.appyhigh.newsfeedsdk.model.*
import com.appyhigh.newsfeedsdk.model.feeds.Card
import com.appyhigh.newsfeedsdk.model.feeds.GetFeedsResponse
import com.appyhigh.newsfeedsdk.utils.ConnectivityLiveData
import com.appyhigh.newsfeedsdk.utils.EndlessScrolling
import com.appyhigh.newsfeedsdk.utils.SpUtil
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.gson.Gson
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class VideoFeed : LinearLayout, OnRefreshListener {
    private var mUserDetails: UserResponse? = null
    private var mInterestResponseModel: InterestResponseModel? = null
    private var interestMap = HashMap<String, Interest>()
    private var interestQuery = ""
    private var pageNo = 0
    private var adIndex = 0
    private var feedType = "quick_bites"
    private var pbLoading: ProgressBar? = null
    private var noNetworkLayout: LinearLayout? = null
    private var loadLayout: LinearLayout? = null
    private var newsFeedAdapter: NewsFeedAdapter? = null
    private var linearLayoutManager: LinearLayoutManager? = null
    private var rvShortBytes: RecyclerView? = null
    private var newsFeedList = ArrayList<Card>()
    private var endlessScrolling: EndlessScrolling? = null
    private var languages = ""
    private var languagesMap = HashMap<String, Language>()
    private var mLanguageResponseModel: ArrayList<Language>? = null
    private var presentUrl = ""
    private var presentTimeStamp: Long = 0
    var currentPosition = 0
    var holders = HashMap<Int, NewsFeedAdapter.BigVideoViewHolder?>()
    var postImpressions = HashMap<String, PostView>()
    var tempR: Rect = Rect()

    constructor(context: Context?) : super(context) {
        initSDK()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        initSDK()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initSDK()
    }

    private fun initSDK() {
        if (FeedSdk.isSdkInitializationSuccessful) {
            initView()
        } else {
            FeedSdk().setListener(object : FeedSdk.OnUserInitialized {
                override fun onInitSuccess() {
                    initView()
                }
            })
        }
    }

    private fun initView() {
        val view = inflate(context, R.layout.video_feed, this)
        SpUtil.onRefreshListeners["reels"] = this
        loadLayout = view?.findViewById(R.id.loadLayout)
        pbLoading = loadLayout?.findViewById(R.id.progress_bar)
        noNetworkLayout = loadLayout?.findViewById(R.id.retry_network)
        loadLayout?.visibility = VISIBLE
        rvShortBytes = view.findViewById(R.id.rvShortBytes)
        linearLayoutManager = LinearLayoutManager(context)
        ConnectivityLiveData(context).observeForever {
            when (it) {
                Constants.NetworkState.CONNECTED -> {
                    Log.d("NETWORK", "AVAILABLE")
                    holders = HashMap<Int, NewsFeedAdapter.BigVideoViewHolder?>()
                    noNetworkLayout?.visibility = GONE
                    rvShortBytes?.visibility = VISIBLE
                    FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let {
                        ApiUserDetails().getUserResponseEncrypted(
                            Endpoints.USER_DETAILS_ENCRYPTED,
                            it,
                            object : ApiUserDetails.UserResponseListener {
                                override fun onSuccess(userDetails: UserResponse) {
                                    mUserDetails = userDetails
                                    getVideos()
                                }
                            })
                    }
                    FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let {
                        ApiGetInterests().getInterestsEncrypted(
                            Endpoints.GET_INTERESTS_ENCRYPTED,
                            it,
                            object : ApiGetInterests.InterestResponseListener {
                                override fun onSuccess(interestResponseModel: InterestResponseModel) {
                                    mInterestResponseModel = interestResponseModel
                                    getVideos()
                                }
                            })
                    }

                    ApiGetLanguages().getLanguagesEncrypted(
                        Endpoints.GET_LANGUAGES_ENCRYPTED,
                        object : ApiGetLanguages.LanguageResponseListener {
                            override fun onSuccess(languageResponseModel: List<Language>) {
                                mLanguageResponseModel =
                                    languageResponseModel as ArrayList<Language>
                                setUpLanguages()
                                getVideos()
                            }
                        }
                    )
                }
                Constants.NetworkState.DISCONNECTED -> {
                    Log.d("NETWORK", "LOST")
                    try {
                        onFocusChanged()
                        newsFeedList = ArrayList()
                        currentPosition = 0
                        rvShortBytes?.adapter = NewsFeedAdapter(newsFeedList, null, "videofeed")
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                    loadLayout?.visibility = VISIBLE
                    noNetworkLayout?.visibility = VISIBLE
                    rvShortBytes?.visibility = GONE
                }
                else -> {}
            }
        }
    }

    private fun setUpLanguages() {
        if (mUserDetails != null && mLanguageResponseModel != null) {
            var selectedLanguagesList = ArrayList<Language>()
            for (language in mLanguageResponseModel!!) {
                languagesMap[language.id] = language
            }
            if (mUserDetails?.user?.languages.isNullOrEmpty()) {
//                selectedLanguagesList = mLanguageResponseModel!!
            } else {
                for (language in languagesMap.values) {
                    if (mUserDetails?.user?.languages!!.contains(language.id)) {
                        selectedLanguagesList.add(language)
                    }
                }
            }
            FeedSdk.languagesList = selectedLanguagesList
        }
    }

    private fun getVideos() {
        var selectedInterestsList = ArrayList<Interest>()
        if (mUserDetails != null && mInterestResponseModel != null && mLanguageResponseModel != null) {
            for (interest in mInterestResponseModel?.interestList!!) {
                interestMap[interest.keyId!!] = interest
            }
            if (mUserDetails?.user?.interests.isNullOrEmpty() || (mUserDetails?.user?.interests!!.size == 1 && mUserDetails?.user?.interests!![0].isEmpty())) {
                selectedInterestsList =
                    (mInterestResponseModel?.interestList as ArrayList<Interest>?)!!
            } else {
                for (interest in interestMap.values) {
                    if (mUserDetails?.user?.interests!!.contains(interest.keyId)) {
                        selectedInterestsList.add(interest)
                    }
                }
            }
            interestQuery = ""
            for ((i, interest) in selectedInterestsList.withIndex()) {
                if (i < selectedInterestsList.size - 1) {
                    interestQuery =
                        interestQuery + interest.keyId.toString() + ","
                } else {
                    interestQuery += interest.keyId
                }
            }

            languages = ""
            for ((i, language) in FeedSdk.languagesList.withIndex()) {
                if (i < FeedSdk.languagesList.size - 1) {
                    languages =
                        languages + language.id.lowercase(Locale.getDefault()) + ","
                } else {
                    languages += language.id.lowercase(Locale.getDefault())
                }
            }
            adIndex = 0
            FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let {
                ApiGetFeeds().getVideoFeedsEncrypted(
                    Endpoints.GET_FEEDS_ENCRYPTED,
                    it,
                    FeedSdk.userId,
                    FeedSdk.sdkCountryCode ?: "in",
                    interestQuery,
                    languages,
                    pageNo,
                    feedType,
                    true,
                    true,
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
                            loadLayout?.visibility = View.GONE
                            for (card in getFeedsResponse.cards) {
                                card.cardType =
                                    Constants.CardType.MEDIA_VIDEO_BIG.toString()
                                        .lowercase(Locale.getDefault())
                                newsFeedList.add(card)
                            }
                            if (FeedSdk.showAds) {
                                try {
                                    val adItem = Card()
                                    adItem.cardType = Constants.AD_LARGE
                                    newsFeedList.add(adIndex, adItem)
                                    Log.d("Ad index", adIndex.toString())
                                } catch (ex: Exception) {
                                    ex.printStackTrace()
                                }
                            }
                            initNewsAdapter(newsFeedList)
                            rvShortBytes?.apply {
                                layoutManager = linearLayoutManager
                                adapter = newsFeedAdapter
                                itemAnimator = null
                            }
                            cardsMap["videofeed"] = newsFeedList
                            val mSnapHelper: SnapHelper = PagerSnapHelper()
                            mSnapHelper.attachToRecyclerView(rvShortBytes)
                            setEndlessScrolling()
                            rvShortBytes?.addOnScrollListener(object :
                                RecyclerView.OnScrollListener() {

                                override fun onScrolled(
                                    recyclerView: RecyclerView,
                                    dx: Int,
                                    dy: Int
                                ) {
                                    super.onScrolled(recyclerView, dx, dy)
                                    val newPos =
                                        linearLayoutManager!!.findFirstCompletelyVisibleItemPosition()
                                    if (newPos > -1 && newPos != currentPosition) {
                                        togglePlaying(currentPosition, false)
                                        togglePlaying(newPos, true)
                                    }
                                }
                            })
                        }
                    })
            }
        }
    }

    @SuppressLint("LogNotTimber")
    private fun initNewsAdapter(newsFeedList: ArrayList<Card>) {
        newsFeedAdapter =
            NewsFeedAdapter(
                newsFeedList,
                object : NewsFeedList.PersonalizationListener {
                    override fun onPersonalizationClicked() {
                    }

                    override fun onRefresh() {
                    }
                },
                "videofeed",
                object : VideoPlayerListener {
                    override fun onVideoEnded(position: Int, duration: Long) {
                        if (position + 1 < newsFeedList.size - 1) {
                            rvShortBytes?.smoothScrollToPosition(position + 1)
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
                            postImpressions.put(card.items[0].postId!!, postView)
                        } catch (ex: java.lang.Exception) {
                            ex.printStackTrace()
                        }
                    }
                }, observeYoutubePlayer = { youtube ->
                    try {
                        Log.i(
                            "TAG",
                            "initNewsAdapter: ${context.getLifecycleOwner().lifecycle.currentState}"
                        )
                        context.getLifecycleOwner().lifecycle.addObserver(youtube)
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                })

    }

    private fun setEndlessScrolling() {
        try {
            if (endlessScrolling == null) {
                endlessScrolling = object : EndlessScrolling(linearLayoutManager!!) {
                    override fun onLoadMore(currentPages: Int) {
                        getMoreFeeds()
                    }

                    override fun onHide() {}
                    override fun onShow() {}
                }
                rvShortBytes?.addOnScrollListener(endlessScrolling!!)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getMoreFeeds() {
        FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let {
            ApiGetFeeds().getVideoFeedsEncrypted(
                Endpoints.GET_FEEDS_ENCRYPTED,
                it,
                FeedSdk.userId,
                FeedSdk.sdkCountryCode ?: "in",
                interestQuery,
                languages,
                pageNo,
                feedType,
                true,
                true,
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
                        for (card in getFeedsResponse.cards) {
                            card.cardType = Constants.CardType.MEDIA_VIDEO_BIG.toString()
                                .lowercase(Locale.getDefault())
                            newsFeedList.add(card)
                        }
                        if (FeedSdk.showAds) {
                            val adItem = Card()
                            adItem.cardType = Constants.AD_LARGE
                            try {
                                if (newsFeedList.size > adIndex) {
                                    newsFeedList.add(adIndex, adItem)
                                    Log.d("Ad index", (adIndex).toString())
                                }
                                if (adIndex + getFeedsResponse.adPlacement[0] < newsFeedList.size) {
                                    adIndex += getFeedsResponse.adPlacement[0]
                                    newsFeedList.add(adIndex, adItem)
                                    Log.d("Ad index", (adIndex).toString())
                                }
                            } catch (e: java.lang.Exception) {
                                e.printStackTrace()
                            }
                        }
                        newsFeedAdapter?.updateList(
                            newsFeedList,
                            "videofeed",
                            pageNo - 1,
                            presentUrl,
                            presentTimeStamp
                        )
                    }
                })
        }
    }

    fun togglePlaying(position: Int, isPlaying: Boolean, from: String = "") {
        try {
            val holder = if (holders.containsKey(position)) {
                holders[position]
            } else {
                if (rvShortBytes?.findViewHolderForAdapterPosition(position) is NewsFeedAdapter.BigVideoViewHolder) {
                    holders[position] =
                        rvShortBytes?.findViewHolderForAdapterPosition(position) as NewsFeedAdapter.BigVideoViewHolder
                    holders[position]
                } else {
                    null
                }
            }
            if (holder != null) {
                if (isPlaying) {
                    Log.d("Check", "togglePlaying: play $position from $from")
                    newsFeedAdapter?.playVideo(holder, position)
                    currentPosition = position
                } else {
                    Log.d("Check", "togglePlaying: pause  $position from $from")
                    newsFeedAdapter?.pausePlayer(holder)
                }
            } else {
                currentPosition = position
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        val isVisible = getGlobalVisibleRect(tempR)
        Log.d("VideoFeed", "onWindowFocusChanged: $isVisible")
        if (hasWindowFocus) {
            if (FeedSdk.areContentsModified[Constants.VIDEO_FEED] == true) {
                FeedSdk.areContentsModified[Constants.VIDEO_FEED] = false
                cardsMap["videofeed"]?.let { newsFeedAdapter?.refreshList(it) }
                onResume()
            } else if (isVisible) {
                Handler(Looper.getMainLooper()).postDelayed({
                    if (getGlobalVisibleRect(tempR)) onResume()
                }, 500)
            }
        } else {
            onFocusChanged()
        }
    }

    fun onFocusChanged() {
        togglePlaying(currentPosition, false, "onFocusChanged")
    }

    fun onResume() {
        togglePlaying(currentPosition, true, "OnResume")
    }

    override fun onRefreshNeeded() {
        newsFeedList = ArrayList()
        initSDK()
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
            FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let {
                ApiPostImpression().addPostImpressionsEncrypted(
                    Endpoints.POST_IMPRESSIONS_ENCRYPTED,
                    it,
                    context
                )
            }
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
        }
    }

    fun onDestroy() {
        storeData(presentUrl, presentTimeStamp)
        try {
            val holder = holders[currentPosition]
            if (holder != null) {
                newsFeedAdapter?.pausePlayer(holder)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }


}
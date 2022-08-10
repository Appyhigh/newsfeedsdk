package com.appyhigh.newsfeedsdk.customview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.adapter.NewsFeedAdapter
import com.appyhigh.newsfeedsdk.apicalls.*
import com.appyhigh.newsfeedsdk.apiclient.Endpoints
import com.appyhigh.newsfeedsdk.callbacks.AdShownListener
import com.appyhigh.newsfeedsdk.callbacks.OnRefreshListener
import com.appyhigh.newsfeedsdk.callbacks.PostImpressionListener
import com.appyhigh.newsfeedsdk.callbacks.VideoPlayerListener
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.model.*
import com.appyhigh.newsfeedsdk.model.feeds.Card
import com.appyhigh.newsfeedsdk.model.feeds.GetFeedsResponse
import com.appyhigh.newsfeedsdk.utils.EndlessScrolling
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.gson.Gson
import java.util.*

class SearchFeedView : LinearLayout, OnRefreshListener {
    private var mUserDetails: UserResponse? = null
    private var mInterestResponseModel: InterestResponseModel? = null
    private var interestMap = HashMap<String, Interest>()
    private var interestQuery = ""
    private var pageNo = 0
    private var adIndex = 0
    private var feedType = "searchscreen_feed"
    private var pbLoading: ProgressBar? = null
    private var newsFeedAdapter: NewsFeedAdapter? = null
    private var linearLayoutManager: LinearLayoutManager? = null
    private var rvPosts: RecyclerView? = null
    private var newsFeedList = ArrayList<Card>()
    private var endlessScrolling: EndlessScrolling? = null
    private var languages = ""
    private var languagesMap = HashMap<String, Language>()
    private var mLanguageResponseModel: ArrayList<Language>? = null
    private var presentUrl = ""
    private var presentTimeStamp: Long = 0
    var currentPosition = 0
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
        val view = inflate(context, R.layout.layout_search_feed_view, this)
        Card.setFontFamily(view?.findViewById(R.id.trendingNewsTitle), true)
        pbLoading = view.findViewById(R.id.pbLoading)
        rvPosts = view.findViewById(R.id.rvPosts)
        linearLayoutManager = LinearLayoutManager(context)
        ApiUserDetails().getUserResponseEncrypted(
            Endpoints.USER_DETAILS_ENCRYPTED,
            object : ApiUserDetails.UserResponseListener {
                override fun onSuccess(userDetails: UserResponse) {
                    mUserDetails = userDetails
                    getSearchFeeds()
                }
            })
        ApiGetInterests().getInterestsEncrypted(
            Endpoints.GET_INTERESTS_ENCRYPTED,
            object : ApiGetInterests.InterestResponseListener {
                override fun onSuccess(interestResponseModel: InterestResponseModel) {
                    mInterestResponseModel = interestResponseModel
                    getSearchFeeds()
                }
            })

        ApiGetLanguages().getLanguagesEncrypted(
            Endpoints.GET_LANGUAGES_ENCRYPTED,
            object : ApiGetLanguages.LanguageResponseListener {
                override fun onSuccess(languageResponseModel: List<Language>) {
                    mLanguageResponseModel = languageResponseModel as ArrayList<Language>
                    setUpLanguages()
                    getSearchFeeds()
                }
            }
        )
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

    private fun getSearchFeeds() {
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
            ApiGetFeeds().getVideoFeedsEncrypted(
                Endpoints.GET_FEEDS_ENCRYPTED,
                FeedSdk.sdkCountryCode ?: "in",
                interestQuery,
                languages,
                pageNo,
                feedType,
                false,
                false,
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
                        for (card in getFeedsResponse.cards) {
                            if (card.cardType == Constants.CardType.NEWS_SMALL_FEATURE.toString()
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
                        if (ApiConfig().checkShowAds(context)) {
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
                                "searchscreen_feed",
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
                        Constants.cardsMap["searchscreen_feed"] = newsFeedList
                        setEndlessScrolling()
                    }
                })
        }
    }

    private fun setEndlessScrolling() {
        try {
            if (endlessScrolling == null) {
                val searchNative =
                    (parent as LinearLayout).findViewById<LinearLayout>(R.id.searchNativeAd)
                endlessScrolling =
                    object : EndlessScrolling(linearLayoutManager!!, object : AdShownListener {
                        override fun onAdShown(adType: String) {
                            if (adType == "show") {
                                searchNative?.visibility = View.VISIBLE
                            } else {
                                searchNative?.visibility = View.GONE
                            }
                        }
                    }) {
                        override fun onLoadMore(currentPages: Int) {
                            getMoreFeeds()
                        }

                        override fun onHide() {}
                        override fun onShow() {}
                    }
                rvPosts?.addOnScrollListener(endlessScrolling!!)
            }
        } catch (e: Exception) {
            LogDetail.LogEStack(e)
        }
    }


    private fun getMoreFeeds() {
        ApiGetFeeds().getVideoFeedsEncrypted(
            Endpoints.GET_FEEDS_ENCRYPTED,
            FeedSdk.sdkCountryCode ?: "in",
            interestQuery,
            languages,
            pageNo,
            feedType,
            false,
            false,
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
                        if (card.cardType == Constants.CardType.NEWS_SMALL_FEATURE.toString()
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
                    if (ApiConfig().checkShowAds(context)) {
                        val adItem = Card()
                        adItem.cardType = Constants.AD
                        try {
                            if (newsFeedList.size > adIndex) {
                                newsFeedList.add(adIndex, adItem)
                                LogDetail.LogD("Ad index", (adIndex).toString())
                            }
                            if (adIndex + getFeedsResponse.adPlacement[0] < newsFeedList.size) {
                                adIndex += getFeedsResponse.adPlacement[0]
                                newsFeedList.add(adIndex, adItem)
                                LogDetail.LogD("Ad index", (adIndex).toString())
                            }
                        } catch (e: java.lang.Exception) {
                            LogDetail.LogEStack(e)
                        }
                    }
                    newsFeedAdapter?.updateList(
                        newsFeedList,
                        "searchscreen_feed",
                        pageNo - 1,
                        presentUrl,
                        presentTimeStamp
                    )
                }
            })
    }

    override fun onRefreshNeeded() {
        newsFeedList = ArrayList()
        initSDK()
    }

    fun onResume() {
        if (FeedSdk.areContentsModified[Constants.FEED] == true) {
            FeedSdk.areContentsModified[Constants.FEED] = false
            Constants.cardsMap["searchscreen_feed"]?.let { newsFeedAdapter?.refreshList(it) }
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
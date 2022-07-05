package com.appyhigh.newsfeedsdk.customview

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.Constants.allInterestsMap
import com.appyhigh.newsfeedsdk.Constants.cardsMap
import com.appyhigh.newsfeedsdk.Constants.exploreResponseDetails
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.adapter.NewsFeedAdapter
import com.appyhigh.newsfeedsdk.apicalls.ApiExplore
import com.appyhigh.newsfeedsdk.apicalls.ApiGetInterests
import com.appyhigh.newsfeedsdk.apicalls.ApiUserDetails
import com.appyhigh.newsfeedsdk.apiclient.Endpoints
import com.appyhigh.newsfeedsdk.callbacks.OnRefreshListener
import com.appyhigh.newsfeedsdk.model.Interest
import com.appyhigh.newsfeedsdk.model.InterestResponseModel
import com.appyhigh.newsfeedsdk.model.UserResponse
import com.appyhigh.newsfeedsdk.model.explore.ExploreResponseModel
import com.appyhigh.newsfeedsdk.model.feeds.Card
import com.appyhigh.newsfeedsdk.utils.ConnectivityLiveData
import com.appyhigh.newsfeedsdk.utils.PodcastMediaPlayer
import com.appyhigh.newsfeedsdk.utils.SpUtil
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashMap

class ExploreFeed : LinearLayout,OnRefreshListener {
    private var newsFeedAdapter: NewsFeedAdapter? = null
    private var rvExplore: RecyclerView? = null
    private var interestsList = ArrayList<String>()
    private var mUserDetails: UserResponse? = null
    private var mInterestResponseModel: InterestResponseModel? = null
    private var pinnedInterestMap = HashMap<String, Interest>()
    private var pbLoading: ProgressBar? = null
    private var noNetworkLayout: LinearLayout? = null
    private var loadLayout: LinearLayout? = null
    private var languages:String? = ""
    private var newsFeedList = ArrayList<Card>()

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
        if (FeedSdk.isExploreInitializationSuccessful) {
            initView()
        } else {
            FeedSdk().setExploreListener(object : FeedSdk.OnUserInitialized {
                override fun onInitSuccess() {
                    initView()
                }
            })
        }
    }

    private fun initView() {
        val view = inflate(context, R.layout.explore_feed, this)
        setFonts(view)
        SpUtil.onRefreshListeners["explore"] = this
        PodcastMediaPlayer.setPodcastListener(view, "explore")
        rvExplore = view.findViewById(R.id.rvExplore)
        loadLayout = view?.findViewById(R.id.loadLayout)
        pbLoading = loadLayout?.findViewById(R.id.progress_bar)
        noNetworkLayout = loadLayout?.findViewById(R.id.retry_network)
        loadLayout?.visibility = VISIBLE
        ConnectivityLiveData(context).observeForever {
            when (it) {
                Constants.NetworkState.CONNECTED -> {
                    newsFeedList = ArrayList()
                    callAPI()
                }
                Constants.NetworkState.DISCONNECTED -> {
                    rvExplore?.visibility = GONE
                    loadLayout?.visibility = VISIBLE
                    noNetworkLayout?.visibility = VISIBLE
                }
                else -> {}
            }
        }
    }

    private fun callAPI(){
        noNetworkLayout?.visibility = GONE
        rvExplore?.visibility = VISIBLE
        FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let {
            ApiUserDetails().getUserResponseEncrypted(
                Endpoints.USER_DETAILS_ENCRYPTED,
                it,
                object : ApiUserDetails.UserResponseListener {
                    override fun onSuccess(userDetails: UserResponse) {
                        mUserDetails = userDetails
                        fetchExploreFeed()
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
                        fetchExploreFeed()
                    }
                })
        }
    }

    private fun fetchExploreFeed() {
        var unSelectedInterestsList = ArrayList<Interest>()
        if (mUserDetails != null && mInterestResponseModel != null) {
            for (interest in mInterestResponseModel?.interestList!!) {
                pinnedInterestMap[interest.keyId!!] = interest
                allInterestsMap[interest.keyId!!] = interest
            }
            if (mUserDetails?.user?.pinnedInterests.isNullOrEmpty()) {
                unSelectedInterestsList =
                    (mInterestResponseModel?.interestList as ArrayList<Interest>?)!!
            } else {
                for (interest in pinnedInterestMap.values) {
                    if (!mUserDetails?.user?.pinnedInterests!!.contains(interest.keyId)) {
                        unSelectedInterestsList.add(interest)
                    }
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
            if(languages == ""){
                languages = null
            }
            Constants.exploreLanguages = languages
            if(FeedSdk.appName=="CricHouse") {
                Constants.exploreInterest = "cricket"
            } else{
                Constants.exploreInterest = (if (unSelectedInterestsList.isEmpty()) {
                    mUserDetails?.user?.interests?.get(1)
                } else {
                    unSelectedInterestsList.random().keyId.toString()
                })!!
            }
            FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let {
                ApiExplore().exploreEncrypted(
                    Endpoints.EXPLORE_ENCRYPTED,
                    it,
                    FeedSdk.userId,
                    languages,
                    FeedSdk.sdkCountryCode ?: "in",
                    Constants.exploreInterest,
                    object : ApiExplore.ExploreResponseListener {
                        override fun onSuccess(
                            exploreResponseModel: ExploreResponseModel,
                            url: String,
                            timeStamp: Long
                        ) {
                            exploreResponseDetails.api_uri = url
                            exploreResponseDetails.timestamp = timeStamp
                            cardsMap["explore"] = exploreResponseModel.cards as ArrayList<Card>
                            loadLayout?.visibility = GONE
                            newsFeedList = ArrayList()
                            newsFeedList.addAll(exploreResponseModel.cards)
                            newsFeedAdapter = NewsFeedAdapter(newsFeedList, null, "explore",null, null)
                            rvExplore?.apply {
                                layoutManager = LinearLayoutManager(context)
                                adapter = newsFeedAdapter
                            }
                        }
                    })
            }
        }
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (hasWindowFocus){
            if (FeedSdk.areContentsModified[Constants.EXPLORE] == true){
                FeedSdk.areContentsModified[Constants.EXPLORE] = false
                cardsMap["explore"]?.let { newsFeedAdapter?.refreshList(it) }
            }
        }
    }

    private fun setFonts(view: View?){
        Card.setFontFamily(view?.findViewById(R.id.podcastBottomTitle))
        Card.setFontFamily(view?.findViewById(R.id.podcastBottomPublisherName))
    }

    override fun onRefreshNeeded() {
        newsFeedList = ArrayList()
        initSDK()
    }

}
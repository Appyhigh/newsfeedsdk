package com.appyhigh.newsfeedsdk.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.Constants.AD
import com.appyhigh.newsfeedsdk.Constants.IS_ALREADY_RATED
import com.appyhigh.newsfeedsdk.Constants.JWT_TOKEN
import com.appyhigh.newsfeedsdk.Constants.LOADER
import com.appyhigh.newsfeedsdk.Constants.RATING
import com.appyhigh.newsfeedsdk.Constants.SESSION_NUMBER
import com.appyhigh.newsfeedsdk.Constants.SHARE
import com.appyhigh.newsfeedsdk.Constants.cardsMap
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.adapter.NewsFeedAdapter
import com.appyhigh.newsfeedsdk.apicalls.ApiConfig
import com.appyhigh.newsfeedsdk.apicalls.ApiGetFeeds
import com.appyhigh.newsfeedsdk.apicalls.ApiPostImpression
import com.appyhigh.newsfeedsdk.apicalls.ApiUserDetails
import com.appyhigh.newsfeedsdk.apiclient.Endpoints
import com.appyhigh.newsfeedsdk.callbacks.PostImpressionListener
import com.appyhigh.newsfeedsdk.customview.NewsFeedList
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.model.*
import com.appyhigh.newsfeedsdk.model.feeds.Card
import com.appyhigh.newsfeedsdk.model.feeds.GetFeedsResponse
import com.appyhigh.newsfeedsdk.model.feeds.Item
import com.appyhigh.newsfeedsdk.utils.EndlessScrolling
import com.appyhigh.newsfeedsdk.utils.GPSTracker
import com.appyhigh.newsfeedsdk.utils.SpUtil
import com.google.gson.Gson
import io.nlopez.smartlocation.SmartLocation
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.round
import kotlin.math.roundToInt

private const val SELECTED_INTEREST = "SELECTED_INTEREST"
private const val CURRENT_POSITION = "CURRENT_POSITION"
private const val INTERESTS_LIST = "INTERESTS_LIST"
private const val SELECTED_INTERESTS_EMPTY = "SELECTED_INTERESTS_EMPTY"
private const val PERSONALIZATION_LISTENER = "PERSONALIZATION_LISTENER"
private const val USER_OBJECT = "USER_OBJECT"
private const val RC_LOCATION = 12

class PagerFragment : Fragment(), EasyPermissions.PermissionCallbacks {
    private var isAlreadyRated: Boolean = false
    private var sessionNo: Int = 0
    private var selectedInterest: String? = null
    private var currentPosition: Int = 0
    private var interestsList: ArrayList<Interest> = ArrayList()
    private var pbLoading: ProgressBar? = null
    private var pageNo = 0
    private var feedType = "category"
    private var rvPosts: RecyclerView? = null
    private var endlessScrolling: EndlessScrolling? = null
    private var linearLayoutManager: LinearLayoutManager? = null
    private var interestQuery = ""
    private var newsFeedAdapter: NewsFeedAdapter? = null
    private var adIndex = 0
    private var exploreReelCardIndex = 1
    private var exploreVideoCardIndex = 3
    private var gpsTracker: GPSTracker? = null
    var newsFeedList = ArrayList<Card>()
    var adCheckerList = ArrayList<Card>()
    var cardsFromIntent = ArrayList<Card>()
    var isSelectedInterestsEmpty = false
    private var languages = ""
    private var presentUrl = ""
    private var stateCode = ""
    private var mUser: User? = null
    private var presentTimeStamp: Long = 0
    var postImpressions = HashMap<String, PostView>()
    var intent: Intent? = null
    var dynamicLinkToCovidCard = false
    private var locationPopup: CardView? = null

    private var personalizeListener: NewsFeedList.PersonalizationListener? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            selectedInterest = it.getString(SELECTED_INTEREST)
            currentPosition = it.getInt(CURRENT_POSITION)
            interestsList = it.getParcelableArrayList(INTERESTS_LIST)!!
            isSelectedInterestsEmpty = it.getBoolean(SELECTED_INTERESTS_EMPTY)
            mUser = it.getParcelable(USER_OBJECT)
        }
        try {
            if (selectedInterest.equals("for_you")) {
                sessionNo = SpUtil.spUtilInstance?.getInt(SESSION_NUMBER, 0) ?: 0
                SpUtil.spUtilInstance?.putInt(SESSION_NUMBER, sessionNo + 1)
                sessionNo += 1
            }
            isAlreadyRated = SpUtil.spUtilInstance?.getBoolean(IS_ALREADY_RATED, false) ?: false
        } catch (e: Exception) {
            LogDetail.LogEStack(e)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_pager, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        pbLoading = view.findViewById(R.id.pbLoading)
        rvPosts = view.findViewById(R.id.rvPosts)
        locationPopup = view.findViewById(R.id.location_popup)
        pbLoading?.visibility = VISIBLE
        languages = ""
        intent = requireActivity().intent
        intent?.let { onIntent(it) }
        gpsTracker = GPSTracker(requireContext())
        for ((i, language) in FeedSdk.languagesList.withIndex()) {
            if (i < FeedSdk.languagesList.size - 1) {
                languages = languages + language.id.lowercase(Locale.getDefault()) + ","
            } else {
                languages += language.id.lowercase(Locale.getDefault())
            }
        }
        interestQuery = ""
        if (selectedInterest.equals("for_you")) {
            feedType = "own_interests"
            val tempList = ArrayList<Interest>()
            for (newInterest in interestsList) {
                if (newInterest.keyId != "for_you" || newInterest.keyId != "podcasts")
                    tempList.add(newInterest)
            }
            for ((i, interest) in tempList.withIndex()) {
                if (i < tempList.size - 1) {
                    interestQuery =
                        interestQuery + interest.keyId.toString() + ","
                } else {
                    interestQuery += interest.keyId
                }
            }
            getForYouFeed()
        } else if (selectedInterest.equals("near_you")) {
            getRegionalFeed()
        } else {
            interestQuery = selectedInterest.toString()
            getForYouFeed()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            gpsTracker?.showSettingsAlert()
        }
    }

    @AfterPermissionGranted(RC_LOCATION)
    private fun getRegionalFeed() {
        var longitude = 27.9853685
        var latitude = 76.7256248
        SpUtil.spUtilInstance?.getString(LOCATION_DEF)?.let {
            try {
                stateCode = Constants.stateMap[it]!!
            } catch (ex: Exception) {
                LogDetail.LogEStack(ex)
            }
        }

        mUser?.stateCode?.let { stateCode = mUser!!.stateCode!! }
        mUser?.latitude?.let { mUser!!.latitude!! }
        mUser?.longitude?.let { longitude = mUser!!.longitude!! }

        val perms = listOf<String>(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (stateCode.isNotEmpty()) {
            locationPopup?.visibility = GONE
        } else if (EasyPermissions.hasPermissions(requireContext(), *perms.toTypedArray())) {
            locationPopup?.visibility = GONE
            SmartLocation.with(context).location()
                .oneFix()
                .start {
                    longitude = it.longitude
                    latitude = it.latitude
                }
        } else {
            locationPopup?.visibility = VISIBLE
            locationPopup?.setOnClickListener {
                EasyPermissions.requestPermissions(
                    this, "Allow permission to location to access local news",
                    RC_LOCATION, *perms.toTypedArray()
                )
            }
            getForYouFeed()
            return
        }
        pageNo = 0
        adIndex = 0

        FeedSdk.spUtil?.getString(JWT_TOKEN)?.let {
            ApiGetFeeds().getRegionalFeedsEncrypted(
                Endpoints.GET_REGIONAL_FEEDS_ENCRYPTED,
                it,
                round(latitude * 100000) / 100000.toDouble(),
                round(longitude * 100000) / 100000.toDouble(),
                stateCode,
                pageNo,
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
                        pbLoading?.visibility = GONE
                        if (cardsFromIntent.size > 0) {
                            newsFeedList.addAll(cardsFromIntent)
                            newsFeedList.addAll(getFeedsResponse.cards as ArrayList<Card>)
                        } else {
                            newsFeedList = getFeedsResponse.cards as ArrayList<Card>
                        }
                        val adItem = Card()
                        val loadMore = Card()
                        loadMore.cardType = LOADER
                        adItem.cardType = AD
                        if (ApiConfig().checkShowAds(requireContext()) && FeedSdk.showFeedAdAtFirst && newsFeedList.size > 0 && newsFeedList[0].cardType != Constants.AD) {
                            newsFeedList.add(0, adItem)
                        }
                        try {
                            if (cardsFromIntent.size == 0 && ApiConfig().checkShowAds(requireContext())) {
                                newsFeedList.add(adIndex, adItem)
                            }
                        } catch (ex: Exception) {
                            LogDetail.LogEStack(ex)
                        }
                        newsFeedList.add(loadMore)
                        LogDetail.LogD("Ad index", adIndex.toString())
                        linearLayoutManager = LinearLayoutManager(requireActivity())
                        cardsMap[selectedInterest.toString()] = newsFeedList
                        newsFeedAdapter = NewsFeedAdapter(
                            newsFeedList,
                            object : NewsFeedList.PersonalizationListener {
                                override fun onPersonalizationClicked() {
                                    personalizeListener?.onPersonalizationClicked()
                                }

                                override fun onRefresh() {

                                }
                            }, selectedInterest.toString(), null,
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
                                        postImpressions[card.items[0].postId!!] = postView
                                        storeImpressions(url, timeStamp)
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

                        newsFeedList.forEachIndexed { index, card ->
                            if (card.cardType == "feed_covid_tracker") {
                                if (dynamicLinkToCovidCard) {
                                    rvPosts?.scrollToPosition(index)
                                }

                                return@forEachIndexed
                            }
                        }

                        adCheckerList.addAll(newsFeedList)
                        setEndlessScrolling()
                    }

                }
            )
        }
    }

    private fun getForYouFeed() {
        pageNo = 0
        adIndex = 0
        var hasFirstPostId = false
        if (SpUtil.pushIntent != null) {
            if (SpUtil.pushIntent?.getStringExtra("post_id") != "" && SpUtil.pushIntent?.getStringExtra(
                    "post_source"
                ) != ""
                && SpUtil.pushIntent?.getStringExtra("interests") == selectedInterest
            ) {
                hasFirstPostId = true
            } else if (SpUtil.pushIntent!!.hasExtra("page")
                && SpUtil.pushIntent!!.getStringExtra("page") == "SDK://feed"
                && SpUtil.pushIntent!!.hasExtra("post_id")
            ) {
                hasFirstPostId = true
            }
        }

        FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let {
            ApiGetFeeds().getFeedsEncrypted(
                Endpoints.GET_FEEDS_ENCRYPTED,
                it,
                FeedSdk.userId,
                FeedSdk.sdkCountryCode ?: "in",
                interestQuery,
                languages,
                pageNo,
                feedType,
                hasFirstPostId,
                object : ApiGetFeeds.GetFeedsResponseListener {
                    override fun onSuccess(
                        getFeedsResponse: GetFeedsResponse,
                        url: String,
                        timeStamp: Long
                    ) {
                        if (isAdded) {
                            requireActivity().runOnUiThread {
                                storeData(presentUrl, presentTimeStamp)
                                presentTimeStamp = timeStamp
                                presentUrl = url
                                adIndex += getFeedsResponse.adPlacement[0]
                                pageNo += 1
                                pbLoading?.visibility = GONE
                                if (cardsFromIntent.size > 0) {
                                    newsFeedList.addAll(cardsFromIntent)
                                    newsFeedList.addAll(getFeedsResponse.cards as ArrayList<Card>)
                                } else {
                                    newsFeedList = getFeedsResponse.cards as ArrayList<Card>
                                }
                                val adItem = Card()
                                val loadMore = Card()
                                loadMore.cardType = LOADER
                                adItem.cardType = AD
                                if (selectedInterest.equals("for_you") && Constants.checkFeedApp()) {
                                    val items = java.util.ArrayList<Item>()
                                    items.add(Item(id = getHomeNativeAd()))
                                    adItem.items = items
                                }
                                if (ApiConfig().checkShowAds(requireContext()) && FeedSdk.showFeedAdAtFirst && newsFeedList.size > 0 && newsFeedList[0].cardType != Constants.AD) {
                                    newsFeedList.add(0, adItem)
                                }
                                if (selectedInterest.equals("for_you")) {
                                    try {
                                        val ratingPost = Card()
                                        val sharePost = Card()
                                        ratingPost.cardType = RATING
                                        sharePost.cardType = SHARE
                                        if (!isAlreadyRated && sessionNo % 3 == 0 && sessionNo % 6 != 0) {
                                            newsFeedList.add(7, ratingPost)
                                        } else if (sessionNo % 6 == 0) {
                                            newsFeedList.add(7, sharePost)
                                        }
                                    } catch (ex: Exception) {
                                        LogDetail.LogEStack(ex)
                                    }
                                }
                                try {
                                    if (cardsFromIntent.size == 0 && ApiConfig().checkShowAds(requireContext())) {
                                        newsFeedList.add(adIndex, adItem)
                                    }
                                } catch (ex: Exception) {
                                    LogDetail.LogEStack(ex)
                                }
                                newsFeedList.add(loadMore)
                                LogDetail.LogD("Ad index", adIndex.toString())
                                linearLayoutManager = LinearLayoutManager(requireContext())
                                cardsMap[selectedInterest.toString()] = newsFeedList
                                newsFeedAdapter = NewsFeedAdapter(
                                    newsFeedList,
                                    object : NewsFeedList.PersonalizationListener {
                                        override fun onPersonalizationClicked() {
                                            personalizeListener?.onPersonalizationClicked()
                                        }

                                        override fun onRefresh() {

                                        }
                                    }, selectedInterest.toString(), null,
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
                                                postImpressions[card.items[0].postId!!] = postView
                                                storeImpressions(url, timeStamp)
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

                                newsFeedList.forEachIndexed { index, card ->
                                    if (card.cardType == "feed_covid_tracker") {
                                        if (dynamicLinkToCovidCard) {
                                            rvPosts?.scrollToPosition(index)
                                        }

                                        return@forEachIndexed
                                    }
                                }

                                adCheckerList.addAll(newsFeedList)
                                setEndlessScrolling()
                            }
                        }
                    }
                })
        }
    }

    private fun onIntent(intent: Intent) {
        try {
            if (intent.hasExtra("covid_card")) {
                dynamicLinkToCovidCard = true
            }
        } catch (ex: Exception) {
            LogDetail.LogEStack(ex)
        }
    }

    fun resetEndlessScrolling() {
        endlessScrolling = null
    }

    private fun setEndlessScrolling() {
        try {
            if (endlessScrolling == null) {
                endlessScrolling = object : EndlessScrolling(linearLayoutManager!!) {
                    override fun onLoadMore(currentPages: Int) {
                        if (feedType == "own_interests")
                            getMoreFeeds()
                        else {
                            getMoreRegionalFeeds()
                        }
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

    private fun getMoreRegionalFeeds() {
        try {
            if (FeedSdk.parentNudgeView != null && FeedSdk.parentNudgeView!!.visibility == View.VISIBLE) {
                FeedSdk.parentNudgeView!!.visibility = View.GONE
            }
        } catch (ex: Exception) {
            LogDetail.LogEStack(ex)
        }
        FeedSdk.spUtil?.getString(JWT_TOKEN)?.let {
            ApiGetFeeds().getRegionalFeedsEncrypted(
                Endpoints.GET_REGIONAL_FEEDS_ENCRYPTED,
                it,
                gpsTracker?.latitude,
                gpsTracker?.longitude,
                stateCode,
                pageNo,
                object : ApiGetFeeds.GetFeedsResponseListener {
                    override fun onSuccess(
                        getFeedsResponse: GetFeedsResponse,
                        url: String,
                        timeStamp: Long
                    ) {
                        storeData(presentUrl, presentTimeStamp)
                        presentTimeStamp = timeStamp
                        presentUrl = url
                        Constants.feedsResponseDetails.api_uri = presentUrl
                        Constants.feedsResponseDetails.timestamp = timeStamp
                        adIndex += getFeedsResponse.adPlacement[0]
                        var newsFeedList = getFeedsResponse.cards as java.util.ArrayList<Card>
                        adCheckerList.addAll(newsFeedList)
                        try {
                            if (ApiConfig().checkShowAds(requireContext())) {
                                val adItem = Card()
                                adItem.cardType = AD
                                try {
                                    if (adCheckerList.size > adIndex) {
                                        newsFeedList.add(adIndex - pageNo * 10, adItem)
                                        adCheckerList.add(adIndex, adItem)
                                        LogDetail.LogD("Ad index", (adIndex - pageNo * 10).toString())
                                    }
                                    if (adIndex + getFeedsResponse.adPlacement[0] < adCheckerList.size) {
                                        adIndex += getFeedsResponse.adPlacement[0]
                                        newsFeedList.add(adIndex - pageNo * 10, adItem)
                                        adCheckerList.add(adIndex, adItem)
                                        LogDetail.LogD("Ad index", (adIndex - pageNo * 10).toString())
                                    }
                                } catch (e: java.lang.Exception) {
                                    LogDetail.LogEStack(e)
                                }
                            }
                        } catch (ex: Exception) {
                            LogDetail.LogEStack(ex)
                        }
                        newsFeedAdapter?.updateList(
                            newsFeedList,
                            selectedInterest.toString(),
                            pageNo,
                            presentUrl,
                            presentTimeStamp
                        )
                        pageNo += 1
                    }

                }
            )
        }
    }

    private fun getMoreFeeds() {
        try {
            if (FeedSdk.parentNudgeView != null && FeedSdk.parentNudgeView!!.visibility == View.VISIBLE) {
                FeedSdk.parentNudgeView!!.visibility = View.GONE
            }
        } catch (ex: Exception) {
            LogDetail.LogEStack(ex)
        }
        FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let {
            ApiGetFeeds().getFeedsEncrypted(
                Endpoints.GET_FEEDS_ENCRYPTED,
                it,
                FeedSdk.userId,
                FeedSdk.sdkCountryCode ?: "in",
                interestQuery,
                languages,
                pageNo,
                feedType,
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
                        Constants.feedsResponseDetails.api_uri = presentUrl
                        Constants.feedsResponseDetails.timestamp = timeStamp
                        adIndex += getFeedsResponse.adPlacement[0]
                        var newsFeedList = getFeedsResponse.cards as java.util.ArrayList<Card>
                        adCheckerList.addAll(newsFeedList)
                        try {
                            if (ApiConfig().checkShowAds(requireContext())) {
                                val adItem = Card()
                                adItem.cardType = AD
                                if (selectedInterest.equals("for_you") && Constants.checkFeedApp()) {
                                    val items = java.util.ArrayList<Item>()
                                    items.add(Item(id = getHomeNativeAd()))
                                    adItem.items = items
                                }
                                try {
                                    if (adCheckerList.size > adIndex) {
                                        newsFeedList.add(adIndex - pageNo * 10, adItem)
                                        adCheckerList.add(adIndex, adItem)
                                        LogDetail.LogD("Ad index", (adIndex - pageNo * 10).toString())
                                    }
                                    if (adIndex + getFeedsResponse.adPlacement[0] < adCheckerList.size) {
                                        adIndex += getFeedsResponse.adPlacement[0]
                                        newsFeedList.add(adIndex - pageNo * 10, adItem)
                                        adCheckerList.add(adIndex, adItem)
                                        LogDetail.LogD("Ad index", (adIndex - pageNo * 10).toString())
                                    }
                                } catch (e: java.lang.Exception) {
                                    LogDetail.LogEStack(e)
                                }
                            }
                        } catch (ex: Exception) {
                            LogDetail.LogEStack(ex)
                        }
                        newsFeedAdapter?.updateList(
                            newsFeedList,
                            selectedInterest.toString(),
                            pageNo,
                            presentUrl,
                            presentTimeStamp
                        )
                        pageNo += 1
                    }
                })
        }
    }

    fun stopVideoPlayback() {
        try {
            val startPos = linearLayoutManager?.findFirstVisibleItemPosition()
            val endPos = linearLayoutManager?.findLastVisibleItemPosition()

            for (pos in startPos!!..endPos!!) {
                val holder = rvPosts?.findViewHolderForAdapterPosition(pos)
                if (holder is NewsFeedAdapter.VideoViewHolder) {
                    newsFeedAdapter?.pausePlayer(holder)
                }
            }
        } catch (e: Exception) {

        }
    }

    override fun onStop() {
        super.onStop()
        try {
            val startPos = linearLayoutManager?.findFirstVisibleItemPosition()
            val endPos = linearLayoutManager?.findLastVisibleItemPosition()

            for (pos in startPos!!..endPos!!) {
                val holder = rvPosts?.findViewHolderForAdapterPosition(pos)
                if (holder is NewsFeedAdapter.VideoViewHolder) {
                    newsFeedAdapter?.pausePlayer(holder)
                }
            }
        } catch (e: Exception) {

        }
    }

    override fun onResume() {
        super.onResume()
        if (FeedSdk.areContentsModified[Constants.FEED] == true) {
            FeedSdk.areContentsModified[Constants.FEED] = false
            cardsMap[selectedInterest.toString()]?.let { newsFeedAdapter?.refreshList(it) }
        }
        if (FeedSdk.isRefreshNeeded) {
            FeedSdk.isRefreshNeeded = false
            personalizeListener?.onRefresh()
        }
        try {
            val perms = listOf<String>(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            if (EasyPermissions.hasPermissions(
                    requireContext(),
                    *perms.toTypedArray()
                ) && locationPopup?.visibility == View.VISIBLE
            ) {
                newsFeedList = ArrayList()
                getRegionalFeed()
            }
        } catch (ex: Exception) {
            LogDetail.LogEStack(ex)
        }
    }

    fun startVideoPlayback() {
        try {
            val startPos = linearLayoutManager?.findFirstVisibleItemPosition()
            val endPos = linearLayoutManager?.findLastVisibleItemPosition()

            for (pos in startPos!!..endPos!!) {
                val holder = rvPosts?.findViewHolderForAdapterPosition(pos)
                if (holder is NewsFeedAdapter.VideoViewHolder) {
                    newsFeedAdapter?.playVideo(holder)
                    break
                }
            }
        } catch (e: Exception) {

        }
    }

    fun storeImpressions(url: String, timeStamp: Long) {
        try {
            if (postImpressions.isEmpty()) {
                return
            }
            val postImpressionsModel =
                PostImpressionsModel(url, postImpressions.values.toList(), timeStamp)
            val gson = Gson()
            val sharedPrefs =
                requireContext().getSharedPreferences("postImpressions", Context.MODE_PRIVATE)
            val postImpressionString = gson.toJson(postImpressionsModel)
            sharedPrefs.edit().putString(timeStamp.toString(), postImpressionString).apply()
        } catch (ex: java.lang.Exception) {
            LogDetail.LogEStack(ex)
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
                requireContext().getSharedPreferences("postImpressions", Context.MODE_PRIVATE)
            val postImpressionString = gson.toJson(postImpressionsModel)
            sharedPrefs.edit().putString(timeStamp.toString(), postImpressionString).apply()
            postImpressions = HashMap()
            FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let {
                ApiPostImpression().addPostImpressionsEncrypted(
                    Endpoints.POST_IMPRESSIONS_ENCRYPTED,
                    it,
                    requireContext()
                )
            }
        } catch (ex: java.lang.Exception) {
            LogDetail.LogEStack(ex)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        storeData(presentUrl, presentTimeStamp)
    }

    private fun getHomeNativeAd(): String {
        return when (FeedSdk.appName) {
            "MasterFeed" -> "ca-app-pub-4310459535775382/1779702172"
            "Samachari" -> "ca-app-pub-4310459535775382/5826758399"
            "Apple Today" -> "ca-app-pub-4310459535775382/4924740269"
            else -> ""
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(
            selectedInterest: String,
            currentPosition: Int,
            interestsList: ArrayList<Interest>,
            isSelectedInterestsEmpty: Boolean,
            personalizationListener: NewsFeedList.PersonalizationListener,
            intentCards: ArrayList<Card>? = null,
            user: User? = null
        ) =
            PagerFragment().apply {
                arguments = Bundle().apply {
                    putString(SELECTED_INTEREST, selectedInterest)
                    putInt(CURRENT_POSITION, currentPosition)
                    putParcelableArrayList(INTERESTS_LIST, interestsList)
                    putBoolean(SELECTED_INTERESTS_EMPTY, isSelectedInterestsEmpty)
                    putParcelable(USER_OBJECT, user)
                }
                personalizeListener = personalizationListener
                if (intentCards != null) {
                    cardsFromIntent = intentCards
                }
            }

    }
}

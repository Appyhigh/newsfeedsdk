package com.appyhigh.newsfeedsdk.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.Constants.AD
import com.appyhigh.newsfeedsdk.Constants.IS_ALREADY_RATED
import com.appyhigh.newsfeedsdk.Constants.LOADER
import com.appyhigh.newsfeedsdk.Constants.LOCATION_POPUP_TIMESTAMP
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
import com.appyhigh.newsfeedsdk.apiclient.Endpoints
import com.appyhigh.newsfeedsdk.callbacks.PostImpressionListener
import com.appyhigh.newsfeedsdk.customview.NewsFeedList
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.model.Interest
import com.appyhigh.newsfeedsdk.model.PostImpressionsModel
import com.appyhigh.newsfeedsdk.model.PostView
import com.appyhigh.newsfeedsdk.model.User
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
    private var longitude: Double = 0.0
    private var latitude: Double = 0.0
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
    var intent: Intent? = null
    var dynamicLinkToCovidCard = false
    private var locationPopup: CardView? = null
    private var noPosts: TextView? = null
    private var TAG = "PagerFragment"
    private var personalizeListener: NewsFeedList.PersonalizationListener? = null
    val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LogDetail.LogD(TAG, "onCreate")
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
        LogDetail.LogD(TAG, "onCreateView")
        return inflater.inflate(R.layout.fragment_pager, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        LogDetail.LogD(TAG, "onViewCreated")
        pbLoading = view.findViewById(R.id.pbLoading)
        rvPosts = view.findViewById(R.id.rvPosts)
        noPosts = view.findViewById(R.id.noPosts)
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
            setInterestString()
            getForYouFeed()
        } else if (selectedInterest.equals("near_you")) {
            getRegionalFeed()
        } else {
            interestQuery = selectedInterest.toString()
            getForYouFeed()
        }
    }

    private fun setInterestString(){
        feedType = "own_interests"
        val tempList = ArrayList<Interest>()
        for (newInterest in interestsList) {
            if (newInterest.keyId != "for_you" && newInterest.keyId != "near_you" && newInterest.keyId != "podcasts")
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
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {}

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            gpsTracker?.showSettingsAlert()
        }
    }

    @AfterPermissionGranted(RC_LOCATION)
    private fun getRegionalFeed() {
        SpUtil.spUtilInstance?.getString(LOCATION_DEF)?.let {
            try {
                stateCode = Constants.stateMap[it]!!
            } catch (ex: Exception) {
                LogDetail.LogEStack(ex)
            }
        }
        newsFeedList = ArrayList<Card>()
        endlessScrolling = null
        mUser?.stateCode?.let { stateCode = mUser!!.stateCode!! }
        mUser?.latitude?.let { latitude = mUser!!.latitude!! }
        mUser?.longitude?.let { longitude = mUser!!.longitude!! }
        if(gpsTracker?.latitude!=null && gpsTracker?.latitude!! >0){
            latitude = gpsTracker?.latitude!!
        }
        if(gpsTracker?.longitude!=null && gpsTracker?.longitude!! >0){
            longitude = gpsTracker?.longitude!!
        }
        val perms = listOf<String>(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (stateCode.isNotEmpty()) {
            locationPopup?.visibility = GONE
            if (EasyPermissions.hasPermissions(requireContext(), *perms.toTypedArray())) {
                SmartLocation.with(context).location()
                    .oneFix()
                    .start {
                        longitude = it.longitude
                        latitude = it.latitude
                    }
            }
            else {
                val prevTimeStamp = SpUtil.spUtilInstance!!.getLong(LOCATION_POPUP_TIMESTAMP, 0)
                val presentTimeStamp = System.currentTimeMillis()
                val dayMilliSeconds = 24*60*60*1000
                if(presentTimeStamp - prevTimeStamp > dayMilliSeconds){
                    locationPopup?.visibility = VISIBLE
                    locationPopup?.setOnClickListener {
                        EasyPermissions.requestPermissions(
                            this, "Allow permission to location to access local news",
                            RC_LOCATION, *perms.toTypedArray()
                        )
                    }
                    SpUtil.spUtilInstance!!.putLong(LOCATION_POPUP_TIMESTAMP, presentTimeStamp)
                }
            }
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
            setInterestString()
            getForYouFeed()
            return
        }
        pageNo = 0
        adIndex = 0
        val sharedPreferences: SharedPreferences = requireContext().getSharedPreferences("postImpressions", Context.MODE_PRIVATE)
        val postPreferences: SharedPreferences = requireContext().getSharedPreferences("postIdsDb", Context.MODE_PRIVATE)
        ApiGetFeeds().getRegionalFeedsEncrypted(
            Endpoints.GET_REGIONAL_FEEDS_ENCRYPTED,
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
                    Handler(Looper.getMainLooper()).post {
                        LogDetail.LogD(TAG, "apigetfeeds started")
                        storeData()
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
                        try{
                            if(newsFeedList.isEmpty()){
                                pbLoading?.visibility = View.GONE
                                noPosts?.visibility = View.VISIBLE
                                return@post
                            } else{
                                noPosts?.visibility = View.GONE
                            }
                        } catch (ex:Exception){}
                        val adItem = Card()
                        val loadMore = Card()
                        loadMore.cardType = LOADER
                        adItem.cardType = AD
                        if (activity != null && isAdded) {
                            if (ApiConfig().checkShowAds(requireContext()) && FeedSdk.showFeedAdAtFirst && newsFeedList.size > 0 && newsFeedList[0].cardType != Constants.AD) {
                                newsFeedList.add(0, adItem)
                            }
                            try {
                                if (cardsFromIntent.size == 0 && ApiConfig().checkShowAds(requireContext()) && newsFeedList.size>5) {
                                    newsFeedList.add(adIndex, adItem)
                                }
                            } catch (ex: Exception) {
                                LogDetail.LogEStack(ex)
                            }
                        }
                        newsFeedList.add(loadMore)
                        LogDetail.LogD(TAG, adIndex.toString())
                        if (activity != null && isAdded) {
                            LogDetail.LogD(TAG, "apigetfeeds adapter setup")
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
                                                card.items[0].feedType?:feedType,
                                                card.items[0].isVideo,
                                                card.items[0].languageString,
                                                Constants.getInterestsString(card.items[0].interests),
                                                card.items[0].postId,
                                                card.items[0].postSource,
                                                card.items[0].publisherId,
                                                card.items[0].shortVideo,
                                                card.items[0].source,
                                                totalDuration,
                                                watchedDuration,
                                                card.items[0].postId+"PagerFragment"+selectedInterest.toString()
                                            )
                                            ApiPostImpression().storeImpression(sharedPreferences, postPreferences, presentUrl, presentTimeStamp, postView)
                                        } catch (ex: java.lang.Exception) {
                                            LogDetail.LogEStack(ex)
                                        }
                                    }
                                })
                            LogDetail.LogD(TAG, "apigetfeeds set recycler")
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
                        } else {
                            LogDetail.LogD("apiFetFeeds", "adapter not attached")
                        }

                        adCheckerList.addAll(newsFeedList)
                        setEndlessScrolling()
                    }
                }
            }
        )
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
        val sharedPreferences: SharedPreferences = requireContext().getSharedPreferences("postImpressions", Context.MODE_PRIVATE)
        val postPreferences: SharedPreferences = requireContext().getSharedPreferences("postIdsDb", Context.MODE_PRIVATE)
        ApiGetFeeds().getFeedsEncrypted(
            Endpoints.GET_FEEDS_ENCRYPTED,
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
                            storeData()
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

                            if(newsFeedList.isEmpty()){
                                pbLoading?.visibility = View.GONE
                                noPosts?.visibility = View.VISIBLE
                                return@runOnUiThread
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
                                if (cardsFromIntent.size == 0 && ApiConfig().checkShowAds(requireContext()) && newsFeedList.size>5) {
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
                                                card.items[0].feedType?:feedType,
                                                card.items[0].isVideo,
                                                card.items[0].languageString,
                                                Constants.getInterestsString(card.items[0].interests),
                                                card.items[0].postId,
                                                card.items[0].postSource,
                                                card.items[0].publisherId,
                                                card.items[0].shortVideo,
                                                card.items[0].source,
                                                totalDuration,
                                                watchedDuration,
                                                card.items[0].postId+"PagerFragment"+selectedInterest.toString()
                                            )
                                            ApiPostImpression().storeImpression(sharedPreferences, postPreferences, presentUrl, presentTimeStamp, postView)
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
                        if (selectedInterest.equals("near_you"))
                            getMoreRegionalFeeds()
                        else {
                            getMoreFeeds()
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
        ApiGetFeeds().getRegionalFeedsEncrypted(
            Endpoints.GET_REGIONAL_FEEDS_ENCRYPTED,
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
                    storeData()
                    presentTimeStamp = timeStamp
                    presentUrl = url
                    Constants.feedsResponseDetails.api_uri = presentUrl
                    Constants.feedsResponseDetails.timestamp = timeStamp
                    adIndex += getFeedsResponse.adPlacement[0]
                    var newsFeedList = getFeedsResponse.cards as java.util.ArrayList<Card>
                    if(newsFeedList.isEmpty()){
                        Toast.makeText(requireContext(), "No posts found!", Toast.LENGTH_SHORT).show()
                    }
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

    private fun getMoreFeeds() {
        try {
            if (FeedSdk.parentNudgeView != null && FeedSdk.parentNudgeView!!.visibility == View.VISIBLE) {
                FeedSdk.parentNudgeView!!.visibility = View.GONE
            }
        } catch (ex: Exception) {
            LogDetail.LogEStack(ex)
        }
        ApiGetFeeds().getFeedsEncrypted(
            Endpoints.GET_FEEDS_ENCRYPTED,
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
                    storeData()
                    presentTimeStamp = timeStamp
                    presentUrl = url
                    Constants.feedsResponseDetails.api_uri = presentUrl
                    Constants.feedsResponseDetails.timestamp = timeStamp
                    adIndex += getFeedsResponse.adPlacement[0]
                    var newsFeedList = getFeedsResponse.cards as java.util.ArrayList<Card>
                    if(newsFeedList.isEmpty()){
                        Toast.makeText(requireContext(), "No posts found!", Toast.LENGTH_SHORT).show()
                    }
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

    @SuppressLint("CommitPrefEdits")
    fun storeData() {
        try {
            ApiPostImpression().addPostImpressionsEncrypted(
                Endpoints.POST_IMPRESSIONS_ENCRYPTED,
                requireContext()
            )
        } catch (ex: java.lang.Exception) {
            LogDetail.LogEStack(ex)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        storeData()
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

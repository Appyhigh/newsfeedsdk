package com.appyhigh.newsfeedsdk.customview

import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.adapter.InterestAdapter
import com.appyhigh.newsfeedsdk.adapter.NewsFeedSliderAdapter
import com.appyhigh.newsfeedsdk.apicalls.ApiGetInterests
import com.appyhigh.newsfeedsdk.apicalls.ApiGetLanguages
import com.appyhigh.newsfeedsdk.apicalls.ApiUserDetails
import com.appyhigh.newsfeedsdk.apiclient.Endpoints
import com.appyhigh.newsfeedsdk.callbacks.InterestSelectedListener
import com.appyhigh.newsfeedsdk.callbacks.OnRefreshListener
import com.appyhigh.newsfeedsdk.callbacks.PersonalizeCallListener
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.fragment.*
import com.appyhigh.newsfeedsdk.model.Interest
import com.appyhigh.newsfeedsdk.model.InterestResponseModel
import com.appyhigh.newsfeedsdk.model.Language
import com.appyhigh.newsfeedsdk.model.UserResponse
import com.appyhigh.newsfeedsdk.model.feeds.Card
import com.appyhigh.newsfeedsdk.utils.ConnectivityLiveData
import com.appyhigh.newsfeedsdk.utils.PodcastMediaPlayer
import com.appyhigh.newsfeedsdk.utils.SpUtil
import java.util.*

class NewsFeedScrollView : LinearLayout, PersonalizeCallListener, OnRefreshListener {
    private var rvInterests: RecyclerView? = null
    private var mUserDetails: UserResponse? = null
    private var mInterestResponseModel: InterestResponseModel? = null
    private var interestAdapter: InterestAdapter? = null
    private var pbLoading: ProgressBar? = null
    private var noNetworkLayout: LinearLayout? = null
    private var loadLayout: LinearLayout? = null
    private var interestMap = LinkedHashMap<String, Interest>()
    private var languagesMap = HashMap<String, Language>()
    private var vpFeed: ViewPager2? = null
    private var fragmentList: ArrayList<Fragment> = ArrayList()
    private var pinnedInterestList = ArrayList<Interest>()
    private var isRefreshNeeded = false
    private var mLanguageResponseModel: ArrayList<Language>? = null
    private var selectedIndex = 0
    private var ivAdd: AppCompatImageView? = null
    private var ivMap: AppCompatImageView? = null
    private var newInterestList = ArrayList<Interest>()

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
            LogDetail.LogD("FeedSdk", "if isSdkInitializationSuccessful")
            startInitView()
        } else {
            LogDetail.LogD("FeedSdk", "setup listener")
            FeedSdk().setListener(object : FeedSdk.OnUserInitialized {
                override fun onInitSuccess() {
                    LogDetail.LogD("FeedSdk", "else onInitSuccess")
                    startInitView()
                }
            }, "scroll view")
        }
    }

    private fun startInitView(){
        SpUtil.onRefreshListeners["news"] = this
        val view = inflate(context, R.layout.layout_news_feed_scroll_view, this)
        val llPrivacy = view.findViewById<LinearLayout>(R.id.llPrivacy)
        if(!SpUtil.spUtilInstance!!.getBoolean(Constants.PRIVACY_ACCEPTED, false)){
            llPrivacy.gravity = Gravity.TOP
            Constants.setPrivacyDialog(context, view)
        } else{
            llPrivacy.visibility = View.GONE
            initView(view)
        }
    }


    private fun initView(view: View) {
        Card.setFontFamily(view.findViewById(R.id.podcastBottomTitle) as TextView)
        Card.setFontFamily(view.findViewById(R.id.podcastBottomPublisherName) as TextView)
        mUserDetails = null
        mInterestResponseModel = null
        mLanguageResponseModel = null
        PodcastMediaPlayer.setPodcastListener(view, "newsFeedList")
        rvInterests = view.findViewById(R.id.rvInterests)
        vpFeed = view.findViewById(R.id.vpFeed)
        loadLayout = view.findViewById(R.id.loadLayout)
        pbLoading = loadLayout?.findViewById(R.id.progress_bar)
        noNetworkLayout = loadLayout?.findViewById(R.id.retry_network)
        loadLayout?.visibility = VISIBLE
        ivAdd = findViewById(R.id.ivAdd)
        ivMap = findViewById(R.id.iv_map)
        if (FeedSdk.personalizationListener != null) {
            ivAdd?.visibility = GONE
        }
//        ivAdd?.setOnClickListener {
//            isRefreshNeeded = true
//            context.startActivity(Intent(context, AddInterestsActivity::class.java))
//        }
        ivMap?.setOnClickListener {
            val bottomSheet = ChangeLocationBottomSheet()
            bottomSheet.show(getFragmentManager(context)!!, "ChangeLocationBottomSheet")
        }
        SpUtil.personalizeCallListener = this
        ivAdd?.setOnClickListener {
            val personaliseMenuBottomSheet = PersonaliseMenuBottomSheet()
            personaliseMenuBottomSheet.show(getFragmentManager(context)!!, "personaliseMenuBottomSheet")
        }
        ConnectivityLiveData(context).observeForever {
            when (it) {
                Constants.NetworkState.CONNECTED -> {
                    mUserDetails = null
                    mInterestResponseModel = null
                    mLanguageResponseModel = null
                    pinnedInterestList = ArrayList()
                    newInterestList = ArrayList()
                    LogDetail.LogD("NETWORK", "AVAILABLE")
                    noNetworkLayout?.visibility = GONE
                    rvInterests?.visibility = VISIBLE
                    ApiGetLanguages().getLanguagesEncrypted(
                        Endpoints.GET_LANGUAGES_ENCRYPTED,
                        object : ApiGetLanguages.LanguageResponseListener {
                            override fun onSuccess(languageResponseModel: List<Language>) {
                                mLanguageResponseModel =
                                    languageResponseModel as ArrayList<Language>
                                setUpLanguages()
                                setUpInterestAdapter()
                            }
                        }
                    )
                    ApiUserDetails().getUserResponseEncrypted(
                        Endpoints.USER_DETAILS_ENCRYPTED,
                        object : ApiUserDetails.UserResponseListener {
                            override fun onSuccess(userDetails: UserResponse) {
                                mUserDetails = userDetails
                                setUpLanguages()
                                setUpInterestAdapter()
                            }
                        })
                    ApiGetInterests().getInterestsEncrypted(
                        Endpoints.GET_INTERESTS_ENCRYPTED,
                        object : ApiGetInterests.InterestResponseListener {
                            override fun onSuccess(interestResponseModel: InterestResponseModel) {
                                mInterestResponseModel = interestResponseModel
                                setUpInterestAdapter()
                            }
                        })
                }
                Constants.NetworkState.DISCONNECTED -> {
                    LogDetail.LogD("NETWORK", "LOST")
                    loadLayout?.visibility = VISIBLE
                    noNetworkLayout?.visibility = VISIBLE
                    rvInterests?.visibility = GONE
                }
                else -> {}
            }
        }
    }

    @SuppressLint("LogNotTimber")
    private fun getFragmentManager(context: Context?): FragmentManager? {
        return when (context) {
            is FragmentActivity -> context.supportFragmentManager
            is ContextWrapper -> getFragmentManager(context.baseContext)
            else -> {
                LogDetail.LogD("TAG", "getFragmentManager: " + context.toString())
                null
            }
        }
    }

    private fun setUpLanguages() {
        if (mUserDetails != null && mLanguageResponseModel != null) {
            var selectedLanguagesList = ArrayList<Language>()
            for (language in mLanguageResponseModel!!) {
                languagesMap[language.id] = language
                Constants.allLanguagesMap[language.id.lowercase(Locale.getDefault())] = language
            }
            Constants.selectedLanguagesMap = HashMap()
            if (mUserDetails?.user?.languages.isNullOrEmpty()) {
//                selectedLanguagesList = mLanguageResponseModel!!
            } else {
                for (language in languagesMap.values) {
                    if (mUserDetails?.user?.languages!!.contains(language.id.lowercase(Locale.getDefault()))) {
                        selectedLanguagesList.add(language)
                        Constants.selectedLanguagesMap[language.id.lowercase(Locale.getDefault())] =
                            language
                    }
                }
            }
            FeedSdk.languagesList = selectedLanguagesList
        }
    }

    private fun setUpInterestAdapter() {
        if (mUserDetails == null || mLanguageResponseModel == null) {
            return
        }
        if (FeedSdk.hideFilters) {
            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams.setMargins(0, 0, 0, 0)
            rvInterests?.layoutParams = layoutParams
        } else {
            ivAdd?.visibility = View.VISIBLE
        }
        var selectedInterestsList = ArrayList<Interest>()
        var isSelectedInterestsEmpty = false
        var pos = 0
        if (mUserDetails != null && mInterestResponseModel != null) {
            fetchCryptoWatchList(mUserDetails)
            for (interest in mInterestResponseModel?.interestList!!) {
                interestMap[interest.keyId!!] = interest
            }

            if (mUserDetails?.user?.interests.isNullOrEmpty() || (mUserDetails?.user?.interests!!.size == 1 && mUserDetails?.user?.interests!![0].isEmpty())) {
                isSelectedInterestsEmpty = true
                selectedInterestsList =
                    (mInterestResponseModel?.interestList as ArrayList<Interest>?)!!
            } else {
                mUserDetails?.user?.pinnedInterests?.let { list ->
                    list.forEach {
                        if (it.isNotEmpty()) {
                            interestMap[it]!!.isPinned = true
                            pinnedInterestList.add(interestMap[it]!!)
                        }
                    }
                }
                for (interest in mUserDetails?.user?.interests!!) {
                    if (interestMap.containsKey(interest)) {
                        selectedInterestsList.add(interestMap[interest]!!)
                    }
                }
                FeedSdk.interestsList = selectedInterestsList
            }
        }
        getInterestsOrder(selectedInterestsList, isSelectedInterestsEmpty)
    }

    private fun getInterestsOrder(
        selectedInterestsList: ArrayList<Interest>,
        isSelectedInterestsEmpty: Boolean
    ) {
        var interests = "for_you"
        var pos = 0
        if (selectedInterestsList.isEmpty() && Constants.allInterestsMap.values.isNotEmpty()) {
            selectedInterestsList.addAll(Constants.allInterestsMap.values.toList() as ArrayList<Interest>)
        }
        if(Constants.userDetails?.showRegionalField == true || mUserDetails?.user?.showRegionalField == true) {
            interests += ",near_you"
        }
        for (i in 0 until pinnedInterestList.size) {
            interests += "," + pinnedInterestList[i].keyId
        }
        LogDetail.LogD("NewsFeedScrollList", "getInterestsOrder: api called $interests")
        ApiGetInterests().getInterestsAppWiseEncrypted(
            Endpoints.GET_INTERESTS_APPWISE_ENCRYPTED,
            interests,
            object : ApiGetInterests.InterestOrderResponseListener {
                override fun onSuccess(interestList: ArrayList<String>) {
                    Handler(Looper.getMainLooper()).post {
                       checkAndAddTabs(interestList)
                        if (mUserDetails != null && mInterestResponseModel != null) {
                            try {
                                if (SpUtil.pushIntent != null && !SpUtil.pushIntent!!.getBooleanExtra(
                                        "isForYou",
                                        false
                                    )
                                    && SpUtil.pushIntent!!.hasExtra("short_video") && SpUtil.pushIntent!!.getStringExtra(
                                        "short_video"
                                    ) == "false"
                                ) {
                                    pos = isInterestFound(
                                        SpUtil.pushIntent!!.getStringExtra("interests")!!,
                                        newInterestList
                                    )
                                    if (pos == -1) {
                                        if (Constants.allInterestsMap.containsKey(
                                                SpUtil.pushIntent!!.getStringExtra(
                                                    "interests"
                                                )
                                            )
                                        ) {
                                            Constants.allInterestsMap[SpUtil.pushIntent!!.getStringExtra(
                                                "interests"
                                            )]?.let {
                                                newInterestList.add(it)
                                            }
                                        }
                                        pos = newInterestList.size - 1
                                    }
                                } else if (SpUtil.pushIntent != null && SpUtil.pushIntent!!.hasExtra(
                                        "page"
                                    ) && (
                                            SpUtil.pushIntent!!.getStringExtra("page")!!
                                                .contains("SDK://feed")
                                                    || SpUtil.pushIntent!!.getStringExtra("page")!!
                                                .contains("SDK://podcastHome")
                                                    || SpUtil.pushIntent!!.getStringExtra("page")!!
                                                .contains("SDK://cryptoHome")
                                                    || SpUtil.pushIntent!!.getStringExtra("page")!!
                                                .contains("SDK://cricketHome")
                                            )
                                ) {
                                    pos = isInterestFound(
                                        SpUtil.pushIntent!!.getStringExtra("category")!!,
                                        newInterestList
                                    )
                                    if (pos == -1) {
                                        if (Constants.allInterestsMap.containsKey(
                                                SpUtil.pushIntent!!.getStringExtra(
                                                    "category"
                                                )
                                            )
                                        ) {
                                            Constants.allInterestsMap[SpUtil.pushIntent!!.getStringExtra(
                                                "category"
                                            )]?.let {
                                                newInterestList.add(it)
                                            }
                                        }
                                        pos = newInterestList.size - 1
                                    }
                                }
                            } catch (ex: Exception) {
                                LogDetail.LogEStack(ex)
                            }
                            loadLayout?.visibility = GONE
                            val distinctList = newInterestList.distinct().toList()
                            interestAdapter =
                                InterestAdapter(ArrayList(distinctList), onInterestSelected)
                            rvInterests?.apply {
                                layoutManager =
                                    LinearLayoutManager(
                                        this.context,
                                        LinearLayoutManager.HORIZONTAL,
                                        false
                                    )
                                adapter = interestAdapter
                                itemAnimator = null
                            }
                        }
                        setUpPagerAdapter(
                            newInterestList,
                            if (FeedSdk.interestsList.isNullOrEmpty()) {
                                mInterestResponseModel?.interestList
                            } else {
                                FeedSdk.interestsList
                            },
                            isSelectedInterestsEmpty
                        )
                        vpFeed?.registerOnPageChangeCallback(object :
                            ViewPager2.OnPageChangeCallback() {
                            override fun onPageSelected(position: Int) {
                                super.onPageSelected(position)
                                selectedIndex = position
                                try {
                                    if (newInterestList.size > 0) {
                                        for ((index, interest) in newInterestList.withIndex()) {
                                            if (interest.isSelected) {
                                                newInterestList[index].isSelected = false
                                                interestAdapter?.updateItem(
                                                    newInterestList[index],
                                                    index
                                                )
                                                interestAdapter?.notifyItemChanged(index)
                                            }
                                        }
                                        newInterestList[position].isSelected = true
                                        interestAdapter?.updateItem(
                                            newInterestList[position],
                                            position
                                        )
                                        interestAdapter?.notifyItemChanged(position)
                                        rvInterests?.scrollToPosition(position)
                                    }
                                } catch (e: Exception) {
                                    LogDetail.LogEStack(e)
                                }
                                try {
                                    if (SpUtil.eventsListener != null) {
                                        SpUtil.eventsListener!!.onFeedCategoryClick(newInterestList[position].label!!)
                                    }
                                } catch (ex: java.lang.Exception) {
                                    LogDetail.LogEStack(ex)
                                }
                            }
                        })
                        try {
                            if (SpUtil.pushIntent != null && SpUtil.pushIntent!!.hasExtra("short_video") && SpUtil.pushIntent!!.getStringExtra(
                                    "short_video"
                                ) == "false"
                            ) {
                                pos = isInterestFound(
                                    SpUtil.pushIntent!!.getStringExtra("interests")!!,
                                    newInterestList
                                )
                                vpFeed?.offscreenPageLimit = pos + 1
                                vpFeed?.currentItem = pos
                                Handler(Looper.getMainLooper()).postDelayed({
                                    rvInterests?.scrollToPosition(pos)
                                }, 1000)
                            } else if (SpUtil.pushIntent != null && SpUtil.pushIntent!!.hasExtra("page") && (
                                        SpUtil.pushIntent!!.getStringExtra("page")!!
                                            .contains("SDK://feed")
                                                || SpUtil.pushIntent!!.getStringExtra("page")!!
                                            .contains("SDK://podcastHome")
                                                || SpUtil.pushIntent!!.getStringExtra("page")!!
                                            .contains("SDK://cryptoHome")
                                                || SpUtil.pushIntent!!.getStringExtra("page")!!
                                            .contains("SDK://cricketHome")
                                        )
                            ) {
                                pos = isInterestFound(
                                    SpUtil.pushIntent!!.getStringExtra("category")!!,
                                    newInterestList
                                )
                                vpFeed?.offscreenPageLimit = pos + 1
                                vpFeed?.currentItem = pos
                                Handler(Looper.getMainLooper()).postDelayed({
                                    rvInterests?.scrollToPosition(pos)
                                }, 1000)
                            }
                        } catch (ex: Exception) {
                            LogDetail.LogEStack(ex)
                        }
                    }

                }
            })
    }


    private var onInterestSelected = object : InterestSelectedListener {
        override fun onInterestClicked(v: View, position: Int) {
            selectedIndex = position
            vpFeed?.setCurrentItem(position, false)
        }
    }

    private fun setUpPagerAdapter(
        selectedInterestsList: ArrayList<Interest>,
        interestList: List<Interest>?,
        isSelectedInterestsEmpty: Boolean
    ) {
        fragmentList = ArrayList()
        for ((i, interest) in selectedInterestsList.withIndex()) {
            when (interest.keyId) {
                "cricket" -> {
                    val cricketLink = try {
                        Constants.allInterestsMap["cricket"]!!.pwaLink ?: ""
                    } catch (e: Exception) {
                        "cricket"
                    }
                    fragmentList.add(
                        CricketHomePWAFragment.newInstance(
                            cricketLink, "cricket"
                        )
                    )
                }
                "podcasts" -> fragmentList.add(PodcastsFragment.newInstance())
                "crypto" -> fragmentList.add(CryptoFragment.newInstance())
                else -> {
                    if(!interest.pwaLink.isNullOrEmpty()){
                        interest.keyId?.let {
                            fragmentList.add(
                                PWAFragment.newInstance(
                                    interest.pwaLink!!, it
                                )
                            )
                        }
                    } else{
                        fragmentList.add(
                            PagerFragment.newInstance(
                                interest.keyId.toString(), i,
                                if (interestList.isNullOrEmpty()) selectedInterestsList else (interestList as ArrayList<Interest>),
                                isSelectedInterestsEmpty,
                                object : NewsFeedList.PersonalizationListener {
                                    override fun onPersonalizationClicked() {
                                        ivAdd?.performClick()
                                    }

                                    override fun onRefresh() {
                                        startInitView()
                                    }
                                },
                                null,
                                mUserDetails?.user
                            )
                        )
                    }
                }
            }
        }
        Handler(Looper.getMainLooper()).post {
            try {
                vpFeed?.adapter = NewsFeedSliderAdapter(
                    if ((context as ContextWrapper).baseContext is FragmentActivity)
                        (context as ContextWrapper).baseContext as FragmentActivity
                    else context as FragmentActivity, fragmentList
                )
                vpFeed?.isUserInputEnabled = false
                val recyclerViewField = ViewPager2::class.java.getDeclaredField("mRecyclerView")
                recyclerViewField.isAccessible = true
                val recyclerView = recyclerViewField.get(vpFeed) as RecyclerView
                val touchSlopField = RecyclerView::class.java.getDeclaredField("mTouchSlop")
                touchSlopField.isAccessible = true
                val touchSlop = touchSlopField.get(recyclerView) as Int
                touchSlopField.set(recyclerView, touchSlop * 6) //6 is empirical value
            } catch (ex: java.lang.Exception) {
                LogDetail.LogEStack(ex)
            }
        }
    }

    private fun checkAndAddTabs(interestList:ArrayList<String>){
        newInterestList = ArrayList<Interest>()
        for(interest in interestList){
            try{
                if(interest == "for_you"){
                    newInterestList.add(
                        Interest("For You", "for_you", null, false)
                    )
                } else if(interest == "near_you"){
                    newInterestList.add(Interest("Near You", "near_you", null, false))
                } else if(interest =="podcast"){
                    newInterestList.add(Interest("Podcasts", "podcasts", null, false))
                }
            else {
                    if(interestMap.containsKey(interest)){
                        newInterestList.add(interestMap[interest]!!)
                    } else{
                        for(search in pinnedInterestList){
                            if(search.keyId == interest){
                                newInterestList.add(search)
                                break
                            }
                        }
                    }
                }
            } catch (ex:Exception){
                LogDetail.LogEStack(ex)
            }
        }
    }

    fun refreshFeeds() {
        initSDK()
    }

    fun stopVideoPlayback() {
        try {
            for (fragment in fragmentList) {
                if (fragment is PagerFragment)
                    fragment.stopVideoPlayback()
            }
        } catch (e: java.lang.Exception) {
            LogDetail.LogEStack(e)
        }
    }

    fun startVideoPlayback() {
        try {
            for ((index, fragment) in fragmentList.withIndex()) {
                if (index == selectedIndex) {
                    (fragment as PagerFragment).startVideoPlayback()
                    break
                }
            }
        } catch (e: java.lang.Exception) {
            LogDetail.LogEStack(e)
        }
    }

    override fun onGEOPointsUpdate() {
        try {
            initSDK()
        } catch (ex: java.lang.Exception) {
            LogDetail.LogEStack(ex)
        }
    }

    fun isInterestFound(keyId: String, interestList: List<Interest>): Int {
        for ((i, interest) in interestList.withIndex()) {
            if (interest.keyId == keyId) {
                return i
            }
        }
        return -1
    }

    private fun fetchCryptoWatchList(userDetails: UserResponse?) {
        try {
            val watchList = userDetails?.user?.crypto_watchlist
            if (watchList != null) {
                for (crypto in watchList) {
                    if (crypto.isNotEmpty()) {
                        Constants.cryptoWatchListMap[crypto] = crypto
                    }
                }
            }
        } catch (ex: Exception) {
            LogDetail.LogEStack(ex)
        }
    }

    override fun onRefreshNeeded() {
        refreshFeeds()
    }
}

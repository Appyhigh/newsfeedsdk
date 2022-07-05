package com.appyhigh.newsfeedsdk.customview

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.adapter.NewsFeedAdapter
import com.appyhigh.newsfeedsdk.apicalls.ApiCrypto
import com.appyhigh.newsfeedsdk.apicalls.ApiUserDetails
import com.appyhigh.newsfeedsdk.apiclient.Endpoints
import com.appyhigh.newsfeedsdk.callbacks.OnRefreshListener
import com.appyhigh.newsfeedsdk.callbacks.PersonalizeCallListener
import com.appyhigh.newsfeedsdk.model.UserResponse
import com.appyhigh.newsfeedsdk.model.feeds.Card
import com.appyhigh.newsfeedsdk.utils.SpUtil
import java.util.*

class CryptoHomeView : LinearLayout, PersonalizeCallListener, OnRefreshListener {

    private var TAG="CryptoHomeView"

    private var mUserDetails: UserResponse? = null
    private var pbLoading: ProgressBar? = null
    private var rvPosts: RecyclerView? = null
    private var newsFeedAdapter: NewsFeedAdapter? = null
    private var interest = "crypto_home"

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
            Log.d(TAG, "if isSdkInitializationSuccessful")
            initView()
        } else {
            FeedSdk().setListener(object : FeedSdk.OnUserInitialized {
                override fun onInitSuccess() {
                    Log.d(TAG, "else onInitSuccess")
                    initView()
                }
            })
        }
    }

    private fun initView() {
        val view = inflate(context, R.layout.layout_crypto_home, this)
        pbLoading = view.findViewById(R.id.pbLoading)
        rvPosts = view.findViewById(R.id.rvPosts)
        pbLoading?.visibility = View.VISIBLE
        rvPosts?.visibility = View.GONE
        mUserDetails = null
        FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let {
            ApiUserDetails().getUserResponseEncrypted(
                Endpoints.USER_DETAILS_ENCRYPTED,
                it,
                object : ApiUserDetails.UserResponseListener {
                    override fun onSuccess(userDetails: UserResponse) {
                        mUserDetails = userDetails
                        setUpHome()
                    }
                })
        }
        val refresh = view.findViewById<SwipeRefreshLayout>(R.id.pullToRefresh)
        refresh.setOnRefreshListener {
            setUpHome(object :OnRefreshListener{
                override fun onRefreshNeeded() {
                    refresh.isRefreshing = false
                }
            })
        }
    }

    private fun setUpHome(listener: OnRefreshListener?=null){
        if (mUserDetails != null){
            fetchCryptoWatchList(mUserDetails)
            FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let {
                ApiCrypto().getCryptoHomeEncrypted(
                    Endpoints.GET_CRYPTO_HOME_ENCRYPTED,
                    it,
                    0,
                    null, object : ApiCrypto.CryptoResponseListener {
                        override fun onSuccess(
                            cryptoResponse: ApiCrypto.CryptoResponse,
                            url: String,
                            timeStamp: Long
                        ) {
                            val cryptoList = cryptoResponse.cards as ArrayList<Card>
                            if(!SpUtil.spUtilInstance!!.getBoolean(Constants.IS_ALREADY_JOINED, false)) {
                                val telegramPost = Card()
                                telegramPost.cardType = Constants.TELEGRAM_CHANNEL
                                cryptoList.add(telegramPost)
                            }
                            Constants.cardsMap[interest] = cryptoList
                            if(!SpUtil.spUtilInstance!!.getBoolean(Constants.IS_ALREADY_RATED, false)) {
                                val ratingPost = Card()
                                ratingPost.cardType = Constants.RATING
                                cryptoList.add(ratingPost)
                            }
                            newsFeedAdapter = NewsFeedAdapter(
                                cryptoList,
                                null, interest)
                            rvPosts?.apply {
                                adapter = newsFeedAdapter
                                itemAnimator = null
                            }
                            pbLoading?.visibility = View.GONE
                            listener?.onRefreshNeeded()
                            rvPosts?.visibility = View.VISIBLE
                        }
                    })
            }
        }
    }

    private fun fetchCryptoWatchList(userDetails: UserResponse?){
        try{
            val watchList = userDetails?.user?.crypto_watchlist
            if(watchList!=null){
                for(crypto in watchList){
                    if(crypto.isNotEmpty()){
                        Constants.cryptoWatchListMap[crypto] = crypto
                    }
                }
            }
        } catch (ex:Exception){
            ex.printStackTrace()
        }
    }

    override fun onRefreshNeeded() {

    }

    override fun onGEOPointsUpdate() {

    }
}
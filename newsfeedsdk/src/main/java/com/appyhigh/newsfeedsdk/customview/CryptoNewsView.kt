package com.appyhigh.newsfeedsdk.customview

import android.content.Context
import android.content.ContextWrapper
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleObserver
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.adapter.NewsFeedSliderAdapter
import com.appyhigh.newsfeedsdk.adapter.TabsAdapter
import com.appyhigh.newsfeedsdk.apicalls.ApiCrypto
import com.appyhigh.newsfeedsdk.apiclient.Endpoints
import com.appyhigh.newsfeedsdk.callbacks.TabSelectedListener
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.fragment.CryptoLearnCommonFragment
import com.appyhigh.newsfeedsdk.model.feeds.Card
import com.appyhigh.newsfeedsdk.model.feeds.Item

class CryptoNewsView : LinearLayout, LifecycleObserver {

    private var TAG="CryptoNewsView"
    var fragmentList = ArrayList<Fragment>()

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
            LogDetail.LogD(TAG, "if isSdkInitializationSuccessful")
            initView()
        } else {
            FeedSdk().setListener(object : FeedSdk.OnUserInitialized {
                override fun onInitSuccess() {
                    LogDetail.LogD(TAG, "else onInitSuccess")
                    initView()
                }
            })
            initView()
        }
    }

    private fun initView() {
        val view = inflate(context, R.layout.layout_crypto_news, this)
        val pbLoading = view.findViewById<ProgressBar>(R.id.pbLoading)
        val vpFeed:ViewPager2 = view.findViewById(R.id.vpFeed)
        val rvTabs:RecyclerView = view.findViewById(R.id.rvTabs)
        fragmentList = ArrayList()
        FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let {
            ApiCrypto().getCryptoHomeEncrypted(
                Endpoints.GET_CRYPTO_HOME_ENCRYPTED,
                it,
                1,
                null, object : ApiCrypto.CryptoResponseListener {
                    override fun onSuccess(
                        cryptoResponse: ApiCrypto.CryptoResponse,
                        url: String,
                        timeStamp: Long
                    ) {
                        val cryptoList = cryptoResponse.cards as ArrayList<Card>
                        val tabsList = ArrayList<Item>()
                        tabsList.add(0, Item(id="General", selected = true))
                        if(cryptoList.size>1 && cryptoList[1].cardType=="feed_hashtags"){
                            tabsList.addAll(cryptoList[1].items)
                            cryptoList.removeAt(0)
                            cryptoList.removeAt(0)
                        }
                        val tabsAdapter = TabsAdapter(tabsList, object:TabSelectedListener{
                            override fun onTabClicked(v: View, position: Int) {
                                vpFeed.setCurrentItem(position, false)
                            }
                        }, "crypto_learn")
                        rvTabs.apply {
                            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                            adapter = tabsAdapter
                            for(tab in tabsList){
                                if(tab.id == "General"){
                                    fragmentList.add(CryptoLearnCommonFragment.newInstance("crypto", url, timeStamp))
                                } else{
                                    fragmentList.add(CryptoLearnCommonFragment.newInstance(tab.id!!, url, timeStamp))
                                }
                            }
                        }
                        vpFeed.isUserInputEnabled = true
                        vpFeed.adapter =
                            NewsFeedSliderAdapter(if ((context as ContextWrapper).baseContext is FragmentActivity)
                                (context as ContextWrapper).baseContext as FragmentActivity
                            else context as FragmentActivity, fragmentList)
                        vpFeed.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                            override fun onPageSelected(position: Int) {
                                super.onPageSelected(position)
                                tabsAdapter.onTabCanged(position)
                            }
                        })
                        pbLoading.visibility = View.GONE
                        val loadMore = Card()
                        loadMore.cardType = Constants.LOADER
                        cryptoList.add(loadMore)
                        Constants.cardsMap["crypto"] = cryptoList
                    }
                }
            )
        }
    }
}
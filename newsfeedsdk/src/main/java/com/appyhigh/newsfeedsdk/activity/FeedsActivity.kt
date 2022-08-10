package com.appyhigh.newsfeedsdk.activity

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.Constants.FEED_TYPE
import com.appyhigh.newsfeedsdk.Constants.INTEREST
import com.appyhigh.newsfeedsdk.Constants.POST_SOURCE
import com.appyhigh.newsfeedsdk.Constants.TAG
import com.appyhigh.newsfeedsdk.Constants.bigBites
import com.appyhigh.newsfeedsdk.Constants.cardsMap
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.adapter.NewsFeedAdapter
import com.appyhigh.newsfeedsdk.apicalls.ApiConfig
import com.appyhigh.newsfeedsdk.apicalls.ApiGetFeeds
import com.appyhigh.newsfeedsdk.apicalls.ApiGetPostsByTag
import com.appyhigh.newsfeedsdk.apicalls.ApiPostImpression
import com.appyhigh.newsfeedsdk.apiclient.Endpoints
import com.appyhigh.newsfeedsdk.callbacks.PostImpressionListener
import com.appyhigh.newsfeedsdk.databinding.ActivityFeedsBinding
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.model.PostImpressionsModel
import com.appyhigh.newsfeedsdk.model.PostView
import com.appyhigh.newsfeedsdk.model.feeds.Card
import com.appyhigh.newsfeedsdk.model.feeds.GetFeedsResponse
import com.appyhigh.newsfeedsdk.utils.EndlessScrolling
import com.appyhigh.newsfeedsdk.utils.showAdaptiveBanner
import com.google.gson.Gson
import java.util.*

class FeedsActivity : AppCompatActivity() {
    private var postSource = "unknown"
    private var feedType = "unknown"
    private var interest = "unknown"
    private var tag = ""
    private var presentUrl = ""
    private var presentTimeStamp: Long = 0
    var languages: String? = null
    var pageNo = 0
    var postImpressions = HashMap<String, PostView>()
    private var endlessScrolling: EndlessScrolling? = null
    private var postImpressionListener: PostImpressionListener? = null

    private var binding: ActivityFeedsBinding? = null
    private var mLayoutManager: LinearLayoutManager? = null
    private var newsFeedAdapter: NewsFeedAdapter? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeedsBinding.inflate(layoutInflater)
        val view = binding?.root
        setContentView(view)
        Card.setFontFamily(binding?.title, true)
        Card.setFontFamily(binding?.noPosts, true)
        if(ApiConfig().checkShowAds(this)) {
            showAdaptiveBanner(this, Constants.getHomeBannerAd(), binding!!.bannerAd)
        }
        binding?.backBtn?.setOnClickListener { onBackPressed() }
        try {
            postSource = intent.getStringExtra(POST_SOURCE) ?: "unknown"
            feedType = intent.getStringExtra(FEED_TYPE) ?: "unknown"
            tag = intent.getStringExtra(TAG) ?: "unknown"
            interest = intent.getStringExtra(INTEREST) ?: "unknown"
        } catch (ex: Exception) {
            LogDetail.LogEStack(ex)
        }
        binding!!.title.text = tag
        postImpressionListener = object : PostImpressionListener {
            override fun addImpression(card: Card, totalDuration: Int?, watchedDuration: Int?) {
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
                } catch (ex: java.lang.Exception) {
                    LogDetail.LogEStack(ex)
                }
            }
        }

        if (interest == "hashtagActivity") {
            getHashtagPosts()
        } else {
            presentUrl = intent.getStringExtra("postUrl") ?: ""
            presentTimeStamp = intent.getLongExtra("timeStamp", 0)
            binding?.progressLayout?.visibility = View.GONE
            cardsMap[interest] = bigBites
            mLayoutManager = LinearLayoutManager(this@FeedsActivity)
            newsFeedAdapter = NewsFeedAdapter(
                bigBites,
                null,
                interest,
                null,
                postImpressionListener
            )
            binding?.recyclerView?.apply {
                layoutManager = mLayoutManager
                adapter = newsFeedAdapter
            }
            if (bigBites.isEmpty()) {
                binding!!.recyclerView.visibility = View.GONE
                binding!!.noPosts.visibility = View.VISIBLE
            } else {
                binding!!.recyclerView.visibility = View.VISIBLE
                binding!!.noPosts.visibility = View.GONE
            }
            setEndlessScrolling()
        }

    }

    private fun getBigBites() {
        if (pageNo == 0) {
            languages = ""
            for ((i, language) in FeedSdk.languagesList.withIndex()) {
                if (i < FeedSdk.languagesList.size - 1) {
                    languages =
                        languages + language.id.lowercase(Locale.getDefault()) + ","
                } else {
                    languages += language.id.lowercase(Locale.getDefault())
                }
            }
            if (languages == "") {
                languages = null
            }
        }
        ApiGetFeeds().getVideoFeedsEncrypted(
            Endpoints.GET_FEEDS_ENCRYPTED,
            FeedSdk.sdkCountryCode ?: "in",
            Constants.exploreInterest,
            languages,
            pageNo,
            feedType,
            true,
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
                    pageNo += 1
                    val cards = ArrayList<Card>()
                    cards.addAll(getFeedsResponse.cards as ArrayList<Card>)
                    newsFeedAdapter?.updateList(
                        cards,
                        interest,
                        pageNo - 1,
                        presentUrl,
                        presentTimeStamp
                    )
                }
            })
    }


    private fun getHashtagPosts() {
        ApiGetPostsByTag().getPostsByTagEncrypted(
            Endpoints.GET_POSTS_BY_TAG_ENCRYPTED,
            tag,
            postSource,
            feedType,
            object : ApiGetPostsByTag.PostsByTagResponseListener {
                override fun onSuccess(
                    getFeedsResponse: GetFeedsResponse,
                    url: String,
                    timeStamp: Long
                ) {
                    storeData(presentUrl, presentTimeStamp)
                    presentTimeStamp = timeStamp
                    presentUrl = url
                    binding?.progressLayout?.visibility = View.GONE
                    mLayoutManager = LinearLayoutManager(this@FeedsActivity)
                    newsFeedAdapter = NewsFeedAdapter(
                        getFeedsResponse.cards as ArrayList<Card>,
                        null,
                        interest,
                        null,
                        postImpressionListener
                    )
                    binding?.recyclerView?.apply {
                        layoutManager = mLayoutManager
                        adapter = newsFeedAdapter
                    }
                    cardsMap[interest] = getFeedsResponse.cards as ArrayList<Card>
                    if (getFeedsResponse.cards.isEmpty()) {
                        binding!!.recyclerView.visibility = View.GONE
                        binding!!.noPosts.visibility = View.VISIBLE
                    } else {
                        binding!!.recyclerView.visibility = View.VISIBLE
                        binding!!.noPosts.visibility = View.GONE
                    }
                }
            })
    }

    override fun onStop() {
        super.onStop()
        try {
            val startPos = mLayoutManager?.findFirstVisibleItemPosition()
            val endPos = mLayoutManager?.findLastVisibleItemPosition()

            for (pos in startPos!!..endPos!!) {
                val holder = binding?.recyclerView?.findViewHolderForAdapterPosition(pos)
                if (holder is NewsFeedAdapter.VideoViewHolder) {
                    newsFeedAdapter?.pausePlayer(holder)
                }
            }
        } catch (e: Exception) {
            LogDetail.LogEStack(e)
        }
    }

    override fun onResume() {
        super.onResume()
        if (FeedSdk.areContentsModified[Constants.HASHTAG] == true) {
            FeedSdk.areContentsModified[Constants.HASHTAG] = false
            cardsMap[interest]?.let { newsFeedAdapter?.refreshList(it) }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
        storeData(presentUrl, presentTimeStamp)
    }

    private fun setEndlessScrolling() {
        try {
            if (endlessScrolling == null) {
                endlessScrolling = object : EndlessScrolling(mLayoutManager!!) {
                    override fun onLoadMore(currentPages: Int) {
                        getBigBites()
                    }

                    override fun onHide() {}
                    override fun onShow() {}
                }
                binding?.recyclerView?.addOnScrollListener(endlessScrolling!!)
            }
        } catch (e: Exception) {
            LogDetail.LogEStack(e)
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
            val sharedPrefs = getSharedPreferences("postImpressions", Context.MODE_PRIVATE)
            val postImpressionString = gson.toJson(postImpressionsModel)
            sharedPrefs.edit().putString(timeStamp.toString(), postImpressionString).apply()
            postImpressions = HashMap()
            ApiPostImpression().addPostImpressionsEncrypted(
                Endpoints.POST_IMPRESSIONS_ENCRYPTED,
                this
            )
        } catch (ex: java.lang.Exception) {
            LogDetail.LogEStack(ex)
        }
    }
}
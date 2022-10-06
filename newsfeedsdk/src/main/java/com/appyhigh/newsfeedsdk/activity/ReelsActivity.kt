package com.appyhigh.newsfeedsdk.activity

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.Constants.POSITION
import com.appyhigh.newsfeedsdk.Constants.cardsMap
import com.appyhigh.newsfeedsdk.Constants.reels
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.adapter.NewsFeedAdapter
import com.appyhigh.newsfeedsdk.apicalls.ApiConfig
import com.appyhigh.newsfeedsdk.apicalls.ApiGetFeeds
import com.appyhigh.newsfeedsdk.apicalls.ApiPostImpression
import com.appyhigh.newsfeedsdk.apiclient.Endpoints
import com.appyhigh.newsfeedsdk.callbacks.PostImpressionListener
import com.appyhigh.newsfeedsdk.callbacks.VideoPlayerListener
import com.appyhigh.newsfeedsdk.customview.NewsFeedList
import com.appyhigh.newsfeedsdk.databinding.ActivityReelsBinding
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.model.PostImpressionsModel
import com.appyhigh.newsfeedsdk.model.PostView
import com.appyhigh.newsfeedsdk.model.feeds.Card
import com.appyhigh.newsfeedsdk.model.feeds.GetFeedsResponse
import com.appyhigh.newsfeedsdk.utils.EndlessScrolling
import com.appyhigh.newsfeedsdk.utils.SpUtil
import com.appyhigh.newsfeedsdk.utils.showAdaptiveBanner
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.gson.Gson
import java.util.*

class ReelsActivity : AppCompatActivity() {
    var mAdapter: NewsFeedAdapter? = null
    private var binding: ActivityReelsBinding? = null
    private var linearLayoutManager:LinearLayoutManager? = null
    private var endlessScrolling: EndlessScrolling? = null
    private var pageNo = 0
    private var feedType = "quick_bites"
    private var presentUrl = ""
    private var presentTimeStamp:Long = 0
    var postImpressions = HashMap<String, PostView>()
    var currentPosition = 0
    var holders = HashMap<Int,RecyclerView.ViewHolder>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReelsBinding.inflate(layoutInflater)
        val view = binding?.root
        setContentView(view)
        if(ApiConfig().checkShowAds(this) && Constants.checkFeedApp()){
            showAdaptiveBanner(this, Constants.getHomeBannerAd(), binding!!.bannerAd)
        }
        presentUrl = intent.getStringExtra("postUrl")?:""
        presentTimeStamp = intent.getLongExtra("timeStamp", 0)
        mAdapter = NewsFeedAdapter(reels, object : NewsFeedList.PersonalizationListener {
            override fun onPersonalizationClicked() {
            }

            override fun onRefresh() {
            }
        }, "reels", object :VideoPlayerListener{
            override fun onVideoEnded(position: Int, duration: Long) {
                if (position + 1 < reels.size - 1) {
                    binding?.rvReels?.smoothScrollToPosition(position + 1)
                }
            }

            override fun setUpYoutubeVideo(view: StyledPlayerView, position: Int, youtubeUrl: String) {}

            override fun releaseYoutubeVideo() {}
        },
            object : PostImpressionListener {
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
                        postImpressions.put(card.items[0].postId!!,postView)
                    } catch (ex:java.lang.Exception){
                        LogDetail.LogEStack(ex)
                    }
                }
            })
        linearLayoutManager = LinearLayoutManager(this@ReelsActivity)
        binding?.rvReels?.apply {
            cardsMap["reels"] = reels
            layoutManager = linearLayoutManager
            adapter = mAdapter
        }
        currentPosition = intent.getIntExtra(POSITION, 0)
        binding?.rvReels?.scrollToPosition(intent.getIntExtra(POSITION, 0))
        val mSnapHelper: SnapHelper = PagerSnapHelper()
        mSnapHelper.attachToRecyclerView(binding?.rvReels)
        binding?.rvReels?.addOnScrollListener(object : RecyclerView.OnScrollListener(){

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val newPos = linearLayoutManager!!.findFirstCompletelyVisibleItemPosition()
                if(newPos>-1 && newPos!=currentPosition){
                    togglePlaying(currentPosition, false)
                    togglePlaying(newPos, true)
                }
            }
        })

        Handler(Looper.getMainLooper()).postDelayed({
            togglePlaying(currentPosition, true)
        },1000)
        setEndlessScrolling()
    }

    fun togglePlaying(position:Int, isPlaying:Boolean){
        try{
            val holder = if(holders.containsKey(position)){
                 holders[position]
            } else{
                if(SpUtil.useReelsV2){
                    holders[position] = binding?.rvReels?.findViewHolderForAdapterPosition(position) as NewsFeedAdapter.Big2VideoViewHolder
                } else {
                    holders[position] = binding?.rvReels?.findViewHolderForAdapterPosition(position) as NewsFeedAdapter.BigVideoViewHolder
                }
                holders[position]
            }
            if(holder is NewsFeedAdapter.BigVideoViewHolder){
                if(isPlaying){
                    mAdapter?.playVideo(holder!!, position)
                    currentPosition = position
                } else{
                    mAdapter?.pausePlayer(holder!!)
                }
            } else if(holder is NewsFeedAdapter.Big2VideoViewHolder) {
                if(isPlaying){
                    mAdapter?.playVideo(holder!!, position)
                    currentPosition = position
                } else{
                    mAdapter?.pausePlayer(holder!!)
                }
            }

        } catch (ex:Exception){
            LogDetail.LogEStack(ex)
        }
    }

    override fun onResume() {
        super.onResume()
        if (FeedSdk.areContentsModified[Constants.EXPLORE] == true){
            FeedSdk.areContentsModified[Constants.EXPLORE] = false
            cardsMap["reels"]?.let { mAdapter?.refreshList(it) }
        }
        togglePlaying(currentPosition, true)
    }

    override fun onPause() {
        super.onPause()
       togglePlaying(currentPosition, false)
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
        storeData(presentUrl, presentTimeStamp)
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
                binding?.rvReels?.addOnScrollListener(endlessScrolling!!)
            }
        } catch (e: Exception) {
            LogDetail.LogEStack(e)
        }
    }

    private fun getMoreFeeds() {
        var lang:String? = ""
        if(Constants.exploreLanguages != ""){
            lang = Constants.exploreLanguages
        } else{
            lang = null
        }
        pageNo = 0
        ApiGetFeeds().getVideoFeedsEncrypted(
            Endpoints.GET_FEEDS_ENCRYPTED,
            FeedSdk.sdkCountryCode ?: "in",
            Constants.exploreInterest,
            lang,
            pageNo,
            feedType,
            true,
            true,
            object : ApiGetFeeds.GetFeedsResponseListener {
                override fun onSuccess(getFeedsResponse: GetFeedsResponse, url: String, timeStamp: Long) {
                    storeData(presentUrl, presentTimeStamp)
                    presentTimeStamp = timeStamp
                    presentUrl = url
                    val cards = ArrayList<Card>()
                    for (card in getFeedsResponse.cards) {
                        card.cardType = Constants.CardType.MEDIA_VIDEO_BIG.toString().lowercase(
                            Locale.getDefault())
                        cards.add(card)
                    }
                    cardsMap["reels"]?.addAll(cards)
                    mAdapter?.updateExploreReelsList(cards, pageNo)
                    if(getFeedsResponse.cards.isNotEmpty()) {
                        pageNo += 1
                    }
                }
            })
    }

    @SuppressLint("CommitPrefEdits")
    fun storeData(url: String, timeStamp: Long){
        try {
            if(postImpressions.isEmpty()){
                return
            }
            val postImpressionsModel = PostImpressionsModel(url, postImpressions.values.toList(), timeStamp)
            val gson = Gson()
            val sharedPrefs = getSharedPreferences("postImpressions", Context.MODE_PRIVATE)
            val postImpressionString = gson.toJson(postImpressionsModel)
            sharedPrefs.edit().putString(timeStamp.toString(), postImpressionString).apply()
            postImpressions = HashMap()
            ApiPostImpression().addPostImpressionsEncrypted(
                Endpoints.POST_IMPRESSIONS_ENCRYPTED,
                this
            )
        } catch (ex:java.lang.Exception){
            LogDetail.LogEStack(ex)
        }
    }
}
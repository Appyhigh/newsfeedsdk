package com.appyhigh.newsfeedsdk.activity

import android.annotation.SuppressLint
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.adapter.NewsFeedAdapter
import com.appyhigh.newsfeedsdk.apicalls.*
import com.appyhigh.newsfeedsdk.apiclient.Endpoints
import com.appyhigh.newsfeedsdk.callbacks.PostImpressionListener
import com.appyhigh.newsfeedsdk.databinding.ActivityPodcastsCategoryBinding
import com.appyhigh.newsfeedsdk.model.PostImpressionsModel
import com.appyhigh.newsfeedsdk.model.PostView
import com.appyhigh.newsfeedsdk.model.feeds.Card
import com.appyhigh.newsfeedsdk.utils.EndlessScrolling
import com.appyhigh.newsfeedsdk.utils.PodcastMediaPlayer
import com.appyhigh.newsfeedsdk.utils.SpUtil
import com.appyhigh.newsfeedsdk.utils.showAdaptiveBanner
import com.google.gson.Gson
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class PodcastsCategoryActivity : AppCompatActivity() {

    private var binding: ActivityPodcastsCategoryBinding? = null
    var pageNo=0
    var postImpressions = HashMap<String, PostView>()
    var groupType = "podcast-category"
    private var endlessScrolling: EndlessScrolling? = null
    private var linearLayoutManager: GridLayoutManager? = null
    //    private var linearLayoutManager: LinearLayoutManager? = null
    private var newsFeedAdapter: NewsFeedAdapter? = null
    private var presentUrl = ""
    private var presentTimeStamp:Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPodcastsCategoryBinding.inflate(layoutInflater)
        val view = binding?.root
        setContentView(view)
        setFonts(view)
        if(ApiConfig().checkShowAds() && Constants.checkFeedApp()){
            showAdaptiveBanner(this, Constants.getHomeBannerAd(), binding!!.bannerAd)
        }
        PodcastMediaPlayer.setPodcastListener(view!!, "podcastCategory")
        binding!!.pbLoading.visibility = View.VISIBLE
        binding!!.rvPosts.visibility = View.GONE
        binding!!.backBtn.setOnClickListener { finish() }
        groupType = intent.getStringExtra("groupType")!!
        val listener = object : PodcastResponseListener {
            override fun onSuccess(
                podcastResponse: PodcastResponse,
                url: String,
                timeStamp: Long
            ) {
                storeData(presentUrl, presentTimeStamp)
                presentUrl = url
                presentTimeStamp = timeStamp
                binding!!.pbLoading.visibility = View.GONE
                binding!!.rvPosts.visibility = View.VISIBLE
                val podcastList = podcastResponse.cards as ArrayList<Card>
                if(podcastList.size==1 && podcastList[0].cardType == "title_icon"){
                    binding!!.noPosts.visibility = View.VISIBLE
                } else{
                    binding!!.noPosts.visibility = View.GONE
                }
                val loadMore = Card()
                loadMore.cardType = Constants.LOADER
                podcastList.add(loadMore)
                Constants.cardsMap[groupType] = podcastList
                newsFeedAdapter = NewsFeedAdapter(
                    podcastList,
                    null, groupType, null,
                    object : PostImpressionListener {
                        override fun addImpression(card: Card, totalDuration: Int?, watchedDuration: Int?) {
                            try {
                                val postView = PostView(
                                    FeedSdk.sdkCountryCode ?: "in",
                                    groupType,
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
                            } catch (ex: Exception){
                                ex.printStackTrace()
                            }
                        }
                    })
                linearLayoutManager = GridLayoutManager(this@PodcastsCategoryActivity, 2)
                linearLayoutManager?.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int {
                        return try {
                            if (Constants.cardsMap[groupType]!![position].cardType != Constants.CardType.MEDIA_PODCAST.toString().lowercase()) {
                                2
                            } else 1
                        } catch (ex:java.lang.Exception){
                            1
                        }
                    }
                }
//                linearLayoutManager = LinearLayoutManager(this@PodcastsCategoryActivity)
                binding!!.rvPosts.apply {
                    layoutManager = linearLayoutManager
                    adapter = newsFeedAdapter
                    itemAnimator = null
                }
                pageNo+=1
                setEndlessScrolling()
            }
        }
        if(groupType == "podcast-category") {
            binding!!.headerTitle.text = intent.getStringExtra(Constants.INTEREST)!!
            FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let {
                ApiPodcast().getPodcastCategoryEncrypted(
                    Endpoints.PODCAST_CATEGORY_ENCRYPTED,
                    it,
                    intent.getStringExtra(Constants.INTEREST)!!,
                    getLanguageQuery(),
                    pageNo,
                    listener
                )
            }
        } else{
            binding!!.headerTitle.text = intent.getStringExtra("publisher_name")!!
            FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let {
                ApiPodcast().getPodcastPublisherEncrypted(
                    Endpoints.PODCAST_PUBLISHER_ENCRYPTED,
                    it,
                    intent.getStringExtra(Constants.PUBLISHER_ID)!!,
                    getLanguageQuery(),
                    pageNo,
                    listener
                )
            }
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
                        getMorePodcasts()
                    }

                    override fun onHide() {}
                    override fun onShow() {}
                }
                binding?.rvPosts?.addOnScrollListener(endlessScrolling!!)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getMorePodcasts(){
        val listener = object : PodcastResponseListener {
            override fun onSuccess(
                podcastResponse: PodcastResponse,
                url: String,
                timeStamp: Long
            ) {
                storeData(presentUrl, presentTimeStamp)
                presentUrl = url
                presentTimeStamp = timeStamp
                val podcastList = podcastResponse.cards as ArrayList<Card>
                newsFeedAdapter?.updateList(podcastList, groupType, pageNo, presentUrl, presentTimeStamp)
                pageNo+=1
            }

        }
        if(groupType == "podcast-category") {
            FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let {
                ApiPodcast().getPodcastCategoryEncrypted(
                    Endpoints.PODCAST_CATEGORY_ENCRYPTED,
                    it,
                    intent.getStringExtra(Constants.INTEREST)!!,
                    getLanguageQuery(),
                    pageNo,
                    listener
                )
            }
        } else{
            FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let {
                ApiPodcast().getPodcastPublisherEncrypted(
                    Endpoints.PODCAST_PUBLISHER_ENCRYPTED,
                    it,
                    intent.getStringExtra(Constants.PUBLISHER_ID)!!,
                    getLanguageQuery(),
                    pageNo,
                    listener
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        storeData(presentUrl, presentTimeStamp)
    }

    private fun getLanguageQuery(): String {
        var languageQuery = ""
        if (FeedSdk.languagesList.size > 0) {
            languageQuery = ""
            for (i in FeedSdk.languagesList.indices) {
                languageQuery = if (i < FeedSdk.languagesList.size - 1) {
                    languageQuery + FeedSdk.languagesList[i].id.lowercase(Locale.getDefault()) + ","
                } else {
                    languageQuery + FeedSdk.languagesList[i].id.lowercase(Locale.getDefault())
                }
            }
            languageQuery = "$languageQuery"
        }
        return languageQuery
    }

    @SuppressLint("CommitPrefEdits")
    fun storeData(url: String, timeStamp: Long){
        try {
            if(postImpressions.isEmpty() || url.isEmpty()){
                return
            }
            val postImpressionsModel = PostImpressionsModel(url, postImpressions.values.toList(), timeStamp)
            val gson = Gson()
            val sharedPrefs = getSharedPreferences("postImpressions", Context.MODE_PRIVATE)
            val postImpressionString = gson.toJson(postImpressionsModel)
            sharedPrefs.edit().putString(timeStamp.toString(), postImpressionString).apply()
            postImpressions = HashMap()
            FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let {
                ApiPostImpression().addPostImpressionsEncrypted(
                    Endpoints.POST_IMPRESSIONS_ENCRYPTED,
                    it,
                    this
                )
            }
        } catch (ex:java.lang.Exception){
            ex.printStackTrace()
        }
    }

    private fun setFonts(view: View?){
        Card.setFontFamily(binding?.headerTitle, true)
        Card.setFontFamily(binding?.noPosts)
        Card.setFontFamily(view?.findViewById(R.id.podcastBottomTitle))
        Card.setFontFamily(view?.findViewById(R.id.podcastBottomPublisherName))
    }

}
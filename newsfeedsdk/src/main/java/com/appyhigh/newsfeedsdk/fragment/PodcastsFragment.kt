package com.appyhigh.newsfeedsdk.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.adapter.NewsFeedAdapter
import com.appyhigh.newsfeedsdk.apicalls.*
import com.appyhigh.newsfeedsdk.apiclient.Endpoints
import com.appyhigh.newsfeedsdk.callbacks.PostImpressionListener
import com.appyhigh.newsfeedsdk.databinding.FragmentPodcastsBinding
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.model.PostImpressionsModel
import com.appyhigh.newsfeedsdk.model.PostView
import com.appyhigh.newsfeedsdk.model.feeds.Card
import com.appyhigh.newsfeedsdk.utils.EndlessScrolling
import com.google.gson.Gson
import java.util.*

class PodcastsFragment : Fragment() {

    lateinit var binding: FragmentPodcastsBinding
    var pageNo = 0
    private var adIndex = 0
    var adCheckerList = ArrayList<Card>()
    var postImpressions = HashMap<String, PostView>()
    private var presentUrl = ""
    private var presentTimeStamp:Long = 0
    private var endlessScrolling: EndlessScrolling? = null
    private var linearLayoutManager: GridLayoutManager? = null
//    private var linearLayoutManager: LinearLayoutManager? = null
    private var newsFeedAdapter: NewsFeedAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPodcastsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.pbLoading.visibility = View.VISIBLE
        binding.rvPosts.visibility = View.GONE
        ApiPodcast().getPodcastHomeEncrypted(
            Endpoints.PODCAST_HOME_ENCRYPTED,
            getLanguageQuery(),
            pageNo,
            object : PodcastResponseListener{
                override fun onSuccess(
                    podcastResponse: PodcastResponse,
                    url: String,
                    timeStamp: Long
                ) {
                    storeData(presentUrl, presentTimeStamp)
                    presentUrl = url
                    presentTimeStamp = timeStamp
                    adIndex += podcastResponse.adPlacement[0]
                    binding.pbLoading.visibility = View.GONE
                    binding.rvPosts.visibility = View.VISIBLE
                    val podcastList = podcastResponse.cards as ArrayList<Card>
                    val adItem = Card()
                    adItem.cardType = Constants.AD
                    if(ApiConfig().checkShowAds(requireContext()) && FeedSdk.showFeedAdAtFirst && podcastList.size>0 && podcastList[0].cardType!=Constants.AD){
                        podcastList.add(0, adItem)
                        podcastList.add(adIndex, adItem)
                    }
                    val loadMore = Card()
                    loadMore.cardType = Constants.LOADER
                    podcastList.add(loadMore)
                    Constants.cardsMap["podcasts"] = podcastList
                    newsFeedAdapter = NewsFeedAdapter(
                        podcastList,
                        null, "podcasts", null,
                        object : PostImpressionListener {
                            override fun addImpression(card: Card, totalDuration: Int?, watchedDuration: Int?) {
                                try {
                                    val postView = PostView(
                                        FeedSdk.sdkCountryCode ?: "in",
                                        card.items[0].feedType?:"category",
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
                                } catch (ex: Exception){
                                    LogDetail.LogEStack(ex)
                                }
                            }
                        }, presentUrl, presentTimeStamp)
                    linearLayoutManager = GridLayoutManager(requireActivity(), 2)
                    linearLayoutManager?.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                        override fun getSpanSize(position: Int): Int {
                            return try {
                                if (Constants.cardsMap["podcasts"]!![position].cardType != Constants.CardType.MEDIA_PODCAST.toString().lowercase()) {
                                    2
                                } else 1
                            } catch (ex:java.lang.Exception){
                                1
                            }
                        }
                    }
                    //                    linearLayoutManager = LinearLayoutManager(requireActivity())
                    binding.rvPosts.apply {
                        layoutManager = linearLayoutManager
                        adapter = newsFeedAdapter
                        itemAnimator = null
                    }
                    pageNo+=1
                    adCheckerList.addAll(podcastList)
                    setEndlessScrolling()
                }

            }
        )
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
                binding.rvPosts.addOnScrollListener(endlessScrolling!!)
            }
        } catch (e: Exception) {
            LogDetail.LogEStack(e)
        }
    }

    fun getMorePodcasts(){
        ApiPodcast().getPodcastHomeEncrypted(
            Endpoints.PODCAST_HOME_ENCRYPTED,
            getLanguageQuery(),
            pageNo,
            object : PodcastResponseListener{
                override fun onSuccess(
                    podcastResponse: PodcastResponse,
                    url: String,
                    timeStamp: Long
                ) {
                    storeData(presentUrl, presentTimeStamp)
                    presentUrl = url
                    presentTimeStamp = timeStamp
                    adIndex += podcastResponse.adPlacement[0]
                    val podcastList = podcastResponse.cards as ArrayList<Card>
                    adCheckerList.addAll(podcastList)
                    if(ApiConfig().checkShowAds(requireContext())) {
                        val adItem = Card()
                        adItem.cardType = Constants.AD
                        try {
                            if((adIndex - pageNo * 10)%2!=0){
                                adIndex+=1
                            }
                            var adPlacement = podcastResponse.adPlacement[0]
                            if (adCheckerList.size > adIndex) {
                                podcastList.add(adIndex - pageNo * 10, adItem)
                                adPlacement+=1
                                LogDetail.LogD("Podcast ad index", (adIndex - pageNo * 10).toString())
                            }
                            if((adIndex + podcastResponse.adPlacement[0] - pageNo * 10)%2!=0){
                                adIndex+=1
                            }
                            if (adIndex + adPlacement < adCheckerList.size) {
                                adIndex += adPlacement
                                podcastList.add(adIndex - pageNo * 10, adItem)
                                LogDetail.LogD("Podcast ad index", (adIndex - pageNo * 10).toString())
                            }
                        } catch (e: java.lang.Exception) {
                            LogDetail.LogEStack(e)
                        }
                    }
                    newsFeedAdapter?.updateList(podcastList, "podcasts", pageNo, presentUrl, presentTimeStamp)
                    pageNo+=1
                }
            })
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
            val sharedPrefs = requireContext().getSharedPreferences("postImpressions", Context.MODE_PRIVATE)
            val postImpressionString = gson.toJson(postImpressionsModel)
            sharedPrefs.edit().putString(timeStamp.toString(), postImpressionString).apply()
            postImpressions = HashMap()
            ApiPostImpression().addPostImpressionsEncrypted(
                Endpoints.POST_IMPRESSIONS_ENCRYPTED,
                requireContext()
            )
        } catch (ex:java.lang.Exception){
            LogDetail.LogEStack(ex)
        }
    }

    companion object {
        fun newInstance(): PodcastsFragment {
            return PodcastsFragment()
        }
    }
}
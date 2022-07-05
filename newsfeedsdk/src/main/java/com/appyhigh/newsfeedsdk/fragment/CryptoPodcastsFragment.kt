package com.appyhigh.newsfeedsdk.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.adapter.NewsFeedAdapter
import com.appyhigh.newsfeedsdk.apicalls.ApiPodcast
import com.appyhigh.newsfeedsdk.apicalls.ApiPostImpression
import com.appyhigh.newsfeedsdk.apicalls.PodcastResponse
import com.appyhigh.newsfeedsdk.apicalls.PodcastResponseListener
import com.appyhigh.newsfeedsdk.apiclient.Endpoints
import com.appyhigh.newsfeedsdk.callbacks.PostImpressionListener
import com.appyhigh.newsfeedsdk.databinding.FragmentCryptoPodcastsBinding
import com.appyhigh.newsfeedsdk.databinding.FragmentPodcastsBinding
import com.appyhigh.newsfeedsdk.model.PostImpressionsModel
import com.appyhigh.newsfeedsdk.model.PostView
import com.appyhigh.newsfeedsdk.model.feeds.Card
import com.appyhigh.newsfeedsdk.utils.EndlessScrolling
import com.appyhigh.newsfeedsdk.utils.SpUtil
import com.google.gson.Gson
import java.util.ArrayList

/**
 * A simple [Fragment] subclass.
 * Use the [CryptoPodcastsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CryptoPodcastsFragment : Fragment() {

    lateinit var binding: FragmentCryptoPodcastsBinding
    var pageNo = 0
    var postImpressions = HashMap<String, PostView>()
    private var presentUrl = ""
    private var presentTimeStamp:Long = 0
    private var endlessScrolling: EndlessScrolling? = null
    private var linearLayoutManager: GridLayoutManager? = null
    private var newsFeedAdapter: NewsFeedAdapter? = null



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCryptoPodcastsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Card.setFontFamily(binding.title)
        binding.pbLoading.visibility = View.VISIBLE
        binding.rvPosts.visibility = View.GONE
        pageNo = 0
        FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let {
            ApiPodcast().getPodcastCategoryEncrypted(
                Endpoints.PODCAST_CATEGORY_ENCRYPTED,
                it,
                "News",
                getLanguageQuery(),
                pageNo,
                object : PodcastResponseListener {
                    override fun onSuccess(
                        podcastResponse: PodcastResponse,
                        url: String,
                        timeStamp: Long
                    ) {
                        storeData(presentUrl, presentTimeStamp)
                        presentUrl = url
                        presentTimeStamp = timeStamp
                        binding.pbLoading.visibility = View.GONE
                        binding.rvPosts.visibility = View.VISIBLE
                        val podcastList = podcastResponse.cards as ArrayList<Card>
                        podcastList.removeAt(0)
                        for(card in podcastResponse.cards){
                            if(card.cardType == Constants.CardType.MEDIA_PODCAST.toString().lowercase()){
                                card.cardType = Constants.CRYPTO_PODCASTS
                            }
                        }
                        val loadMore = Card()
                        loadMore.cardType = Constants.LOADER
                        podcastList.add(loadMore)
                        Constants.cardsMap["crypto_podcasts"] = podcastList
                        newsFeedAdapter = NewsFeedAdapter(
                            podcastList,
                            null, "crypto_podcasts", null,
                            object : PostImpressionListener {
                                override fun addImpression(card: Card, totalDuration: Int?, watchedDuration: Int?) {
                                    try {
                                        val postView = PostView(
                                            FeedSdk.sdkCountryCode ?: "in",
                                            "category",
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
                                        ex.printStackTrace()
                                    }
                                }
                            }, presentUrl, presentTimeStamp)
                        linearLayoutManager = GridLayoutManager(requireActivity(), 2)
                        linearLayoutManager?.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                            override fun getSpanSize(position: Int): Int {
                                return try {
                                    if (Constants.cardsMap["crypto_podcasts"]!![position].cardType == Constants.LOADER) {
                                        2
                                    } else 1
                                } catch (ex:java.lang.Exception){
                                    1
                                }
                            }
                        }
                        binding.rvPosts.apply {
                            layoutManager = linearLayoutManager
                            adapter = newsFeedAdapter
                            itemAnimator = null
                        }
                        pageNo+=1
                        setEndlessScrolling()
                    }

                }
            )
        }
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
            e.printStackTrace()
        }
    }

    fun getMorePodcasts(){
        FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let {
            ApiPodcast().getPodcastHomeEncrypted(
                Endpoints.PODCAST_HOME_ENCRYPTED,
                it,
                getLanguageQuery(),
                pageNo,
                object : PodcastResponseListener {
                    override fun onSuccess(
                        podcastResponse: PodcastResponse,
                        url: String,
                        timeStamp: Long
                    ) {
                        storeData(presentUrl, presentTimeStamp)
                        presentUrl = url
                        presentTimeStamp = timeStamp
                        val podcastList = podcastResponse.cards as ArrayList<Card>
                        for(card in podcastResponse.cards){
                            if(card.cardType == Constants.CardType.MEDIA_PODCAST.toString().lowercase()){
                                card.cardType = Constants.CRYPTO_PODCASTS
                            }
                        }
                        newsFeedAdapter?.updateList(podcastList, "podcasts", pageNo, presentUrl, presentTimeStamp)
                        pageNo+=1
                    }
                })
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
                    languageQuery + FeedSdk.languagesList[i].id.toLowerCase() + ","
                } else {
                    languageQuery + FeedSdk.languagesList[i].id.toLowerCase()
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
            FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let {
                ApiPostImpression().addPostImpressionsEncrypted(
                    Endpoints.POST_IMPRESSIONS_ENCRYPTED,
                    it,
                    requireContext()
                )
            }
        } catch (ex:java.lang.Exception){
            ex.printStackTrace()
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment CryptoPodcastsFragment.
         */
        @JvmStatic
        fun newInstance() = CryptoPodcastsFragment()
    }
}
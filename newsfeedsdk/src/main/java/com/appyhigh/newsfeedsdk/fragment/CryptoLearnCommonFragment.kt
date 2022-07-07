package com.appyhigh.newsfeedsdk.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.adapter.NewsFeedAdapter
import com.appyhigh.newsfeedsdk.apicalls.ApiConfig
import com.appyhigh.newsfeedsdk.apicalls.ApiCrypto
import com.appyhigh.newsfeedsdk.apicalls.ApiGetPostsByTag
import com.appyhigh.newsfeedsdk.apicalls.ApiPostImpression
import com.appyhigh.newsfeedsdk.apiclient.Endpoints
import com.appyhigh.newsfeedsdk.callbacks.PostImpressionListener
import com.appyhigh.newsfeedsdk.databinding.FragmentCryptoLearnCommonBinding
import com.appyhigh.newsfeedsdk.model.PostImpressionsModel
import com.appyhigh.newsfeedsdk.model.PostView
import com.appyhigh.newsfeedsdk.model.feeds.Card
import com.appyhigh.newsfeedsdk.model.feeds.GetFeedsResponse
import com.appyhigh.newsfeedsdk.model.feeds.Item
import com.appyhigh.newsfeedsdk.utils.EndlessScrolling
import com.appyhigh.newsfeedsdk.utils.SpUtil
import com.google.gson.Gson
import java.util.ArrayList
import java.util.HashMap


/**
 * A simple [Fragment] subclass.
 * Use the [CryptoLearnCommonFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CryptoLearnCommonFragment : Fragment() {

    private var binding: FragmentCryptoLearnCommonBinding?=null
    var pageNo = 1
    var postImpressions = HashMap<String, PostView>()
    private var presentUrl = ""
    private var presentTimeStamp:Long = 0
    private var endlessScrolling: EndlessScrolling? = null
    private var linearLayoutManager: LinearLayoutManager? = null
    private var newsFeedAdapter: NewsFeedAdapter? = null
    private var interest = "crypto"
    private var postSource = "crypto_trends"
    private var feedType = "crypto_trends"
    private var postImpressionListener: PostImpressionListener?=null
    var newsFeedList = ArrayList<Card>()
    private var adIndex = 0
    var adCheckerList = ArrayList<Card>()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentCryptoLearnCommonBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding!!.pbLoading.visibility = View.VISIBLE
        binding!!.rvLearnPosts.visibility = View.GONE
        postImpressionListener = object : PostImpressionListener {
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
        }
        if(Constants.cardsMap.containsKey(interest)){
            binding?.pbLoading?.visibility = View.GONE
            if(Constants.cardsMap[interest]!!.isEmpty()){
                binding?.noPosts?.visibility = View.VISIBLE
            } else{
                binding!!.rvLearnPosts.visibility = View.VISIBLE
                linearLayoutManager = LinearLayoutManager(context)
                newsFeedList = Constants.cardsMap[interest]!!
                adIndex += 6
                val adItem = Card()
                adItem.cardType = Constants.AD
                try{
                    if(ApiConfig().checkShowAds() && newsFeedList.size>0 && newsFeedList[0].cardType!=Constants.AD){
                        newsFeedList.add(0, adItem)
                    }
                    if(ApiConfig().checkShowAds() && newsFeedList.size>6) {
                        newsFeedList.add(adIndex, adItem)
                    }
                } catch (ex:Exception){
                    ex.printStackTrace()
                }
                newsFeedAdapter = NewsFeedAdapter(newsFeedList, null, interest, null, postImpressionListener)
                binding?.rvLearnPosts?.apply {
                    layoutManager = linearLayoutManager
                    adapter = newsFeedAdapter
                }
                adCheckerList.addAll(newsFeedList)
                pageNo+=1
                setEndlessScrolling()
            }
        } else {
            getHashtagPosts()
        }
    }

    private fun getHashtagPosts(){
        FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let {
            ApiGetPostsByTag().getPostsByTagEncrypted(
                Endpoints.GET_POSTS_BY_TAG_ENCRYPTED,
                it,
                FeedSdk.userId,
                interest,
                postSource,
                feedType,
                object : ApiGetPostsByTag.PostsByTagResponseListener {
                    override fun onSuccess(getFeedsResponse: GetFeedsResponse, url: String, timeStamp: Long) {
                        storeData(presentUrl, presentTimeStamp)
                        presentTimeStamp = timeStamp
                        presentUrl = url
                        binding?.pbLoading?.visibility = View.GONE
                        newsFeedList = getFeedsResponse.cards as ArrayList<Card>
                        adIndex += 6
                        val adItem = Card()
                        adItem.cardType = Constants.AD
                        try{
                            if(ApiConfig().checkShowAds() && newsFeedList.size>0 && newsFeedList[0].cardType!=Constants.AD){
                                newsFeedList.add(0, adItem)
                            }
                            if(ApiConfig().checkShowAds() && newsFeedList.size>6) {
                                newsFeedList.add(adIndex, adItem)
                            }
                        } catch (ex:Exception){
                            ex.printStackTrace()
                        }
                        if(newsFeedList.isEmpty()){
                            binding!!.noPosts.visibility = View.VISIBLE
                        } else {
                            binding!!.rvLearnPosts.visibility = View.VISIBLE
                        }
                        linearLayoutManager = LinearLayoutManager(context)
                        newsFeedAdapter = NewsFeedAdapter(newsFeedList, null, interest, null, postImpressionListener)
                        binding?.rvLearnPosts?.apply {
                            layoutManager = linearLayoutManager
                            adapter = newsFeedAdapter
                        }
                        Constants.cardsMap[interest] = getFeedsResponse.cards as ArrayList<Card>
                    }
                })
        }
    }

    private fun setEndlessScrolling() {
        try {
            if (endlessScrolling == null) {
                endlessScrolling = object : EndlessScrolling(linearLayoutManager!!) {
                    override fun onLoadMore(currentPages: Int) {
                        getMoreCryptoPosts()
                    }

                    override fun onHide() {}
                    override fun onShow() {}
                }
                binding!!.rvLearnPosts.addOnScrollListener(endlessScrolling!!)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun getMoreCryptoPosts(){
        FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let {
            ApiCrypto().getCryptoHomeEncrypted(
                Endpoints.GET_CRYPTO_HOME_ENCRYPTED,
                it,
                pageNo,
                null, object : ApiCrypto.CryptoResponseListener {
                    override fun onSuccess(
                        cryptoResponse: ApiCrypto.CryptoResponse,
                        url: String,
                        timeStamp: Long
                    ) {
                        storeData(presentUrl, presentTimeStamp)
                        presentUrl = url
                        presentTimeStamp = timeStamp
                        val newsFeedList = cryptoResponse.cards as ArrayList<Card>
                        adIndex += 6
                        adCheckerList.addAll(newsFeedList)
                        if(ApiConfig().checkShowAds()) {
                            val adItem = Card()
                            adItem.cardType = Constants.AD
                            try {
                                if (adCheckerList.size > adIndex) {
                                    newsFeedList.add(adIndex - ((pageNo - 1) * 10), adItem)
                                    adCheckerList.add(adIndex, adItem)
                                    Log.d("Ad index", (adIndex - ((pageNo - 1) * 10)).toString())
                                }
                                if (adIndex + 6 < adCheckerList.size) {
                                    adIndex += 6
                                    newsFeedList.add(adIndex - ((pageNo - 1) * 10), adItem)
                                    adCheckerList.add(adIndex, adItem)
                                    Log.d("Ad index", (adIndex - ((pageNo - 1) * 10)).toString())
                                }
                            } catch (e: java.lang.Exception) {
                                e.printStackTrace()
                            }
                        }
                        newsFeedAdapter?.updateList(newsFeedList, interest, pageNo, presentUrl, presentTimeStamp)
                        pageNo+=1
                    }
                })
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        storeData(presentUrl, presentTimeStamp)
    }


    override fun onResume(){
        super.onResume()
        if (FeedSdk.areContentsModified[Constants.FEED] == true) {
            FeedSdk.areContentsModified[Constants.FEED] = false
            Constants.cardsMap[interest]?.let { newsFeedAdapter?.refreshList(it) }
        }
    }

    override fun onPause() {
        super.onPause()
        stopVideoPlayback()
    }

    fun stopVideoPlayback() {
        try {
            val startPos = linearLayoutManager?.findFirstVisibleItemPosition()
            val endPos = linearLayoutManager?.findLastVisibleItemPosition()

            for (pos in startPos!!..endPos!!) {
                val holder = binding!!.rvLearnPosts.findViewHolderForAdapterPosition(pos)
                if (holder is NewsFeedAdapter.VideoViewHolder) {
                    newsFeedAdapter?.pausePlayer(holder)
                }
            }
        } catch (e: Exception) {

        }
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
         * @return A new instance of fragment CryptoLearnCommonFragment.
         */
        @JvmStatic
        fun newInstance(interest: String, url:String, timeStamp: Long) : CryptoLearnCommonFragment{
            val cryptoLearnCommonFragment = CryptoLearnCommonFragment()
            cryptoLearnCommonFragment.interest = interest
            cryptoLearnCommonFragment.presentUrl = url
            cryptoLearnCommonFragment.presentTimeStamp = timeStamp
            return cryptoLearnCommonFragment
        }
    }
}
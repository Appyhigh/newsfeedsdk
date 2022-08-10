package com.appyhigh.newsfeedsdk.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.adapter.NewsFeedAdapter
import com.appyhigh.newsfeedsdk.apicalls.ApiCrypto
import com.appyhigh.newsfeedsdk.apicalls.ApiPostImpression
import com.appyhigh.newsfeedsdk.apiclient.Endpoints
import com.appyhigh.newsfeedsdk.callbacks.PostImpressionListener
import com.appyhigh.newsfeedsdk.databinding.FragmentCryptoBinding
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.model.PostImpressionsModel
import com.appyhigh.newsfeedsdk.model.PostView
import com.appyhigh.newsfeedsdk.model.feeds.Card
import com.appyhigh.newsfeedsdk.utils.EndlessScrolling
import com.google.gson.Gson

class CryptoFragment : Fragment() {

    lateinit var binding: FragmentCryptoBinding
    var pageNo = 0
    var postImpressions = HashMap<String, PostView>()
    private var presentUrl = ""
    private var presentTimeStamp:Long = 0
    private var endlessScrolling: EndlessScrolling? = null
    private var linearLayoutManager: LinearLayoutManager? = null
    private var newsFeedAdapter: NewsFeedAdapter? = null
    private var interest = "crypto"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCryptoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.pbLoading.visibility = View.VISIBLE
        binding.rvPosts.visibility = View.GONE
        pageNo = 0
        endlessScrolling = null
        ApiCrypto().getCryptoHomeEncrypted(
            Endpoints.GET_CRYPTO_HOME_ENCRYPTED,
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
                    val cryptoList = cryptoResponse.cards as ArrayList<Card>
                    val loadMore = Card()
                    loadMore.cardType = Constants.LOADER
                    cryptoList.add(loadMore)
                    Constants.cardsMap[interest] = cryptoList
                    newsFeedAdapter = NewsFeedAdapter(
                        cryptoResponse.cards as ArrayList<Card>,
                        null, interest, null,
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
                                    LogDetail.LogEStack(ex)
                                }
                            }
                        }, presentUrl, presentTimeStamp)
                    linearLayoutManager = LinearLayoutManager(requireActivity())
                    binding.rvPosts.apply {
                        layoutManager = linearLayoutManager
                        adapter = newsFeedAdapter
                        itemAnimator = null
                    }
                    binding.pbLoading.visibility = View.GONE
                    binding.rvPosts.visibility = View.VISIBLE
                    pageNo+=1
                    setEndlessScrolling()
                }
            }
        )
    }

    override fun onResume() {
        super.onResume()
        if (FeedSdk.areContentsModified[Constants.FEED] == true) {
            FeedSdk.areContentsModified[Constants.FEED] = false
            Constants.cardsMap[interest]?.let { newsFeedAdapter?.refreshList(it) }
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
                binding.rvPosts.addOnScrollListener(endlessScrolling!!)
            }
        } catch (e: Exception) {
            LogDetail.LogEStack(e)
        }
    }

    fun getMoreCryptoPosts(){
        ApiCrypto().getCryptoHomeEncrypted(
            Endpoints.GET_CRYPTO_HOME_ENCRYPTED,
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
                    val cryptoList = cryptoResponse.cards as ArrayList<Card>
                    newsFeedAdapter?.updateList(cryptoList, interest, pageNo, presentUrl, presentTimeStamp)
                    pageNo+=1
                }
            })
    }

    override fun onDestroy() {
        super.onDestroy()
        storeData(presentUrl, presentTimeStamp)
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
        fun newInstance(): CryptoFragment {
            return CryptoFragment()
        }
    }
}
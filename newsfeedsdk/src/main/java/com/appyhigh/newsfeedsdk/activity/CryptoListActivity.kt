package com.appyhigh.newsfeedsdk.activity

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.adapter.CryptoDetailsAdapter
import com.appyhigh.newsfeedsdk.adapter.CryptoSearchAdapter
import com.appyhigh.newsfeedsdk.adapter.CryptoSearchItem
import com.appyhigh.newsfeedsdk.apicalls.ApiCreateOrUpdateUser
import com.appyhigh.newsfeedsdk.apicalls.ApiCrypto
import com.appyhigh.newsfeedsdk.apiclient.Endpoints
import com.appyhigh.newsfeedsdk.callbacks.OnRefreshListener
import com.appyhigh.newsfeedsdk.databinding.ActivityCryptoDetailsBinding
import com.appyhigh.newsfeedsdk.model.PostView
import com.appyhigh.newsfeedsdk.model.crypto.CryptoSearchResponse
import com.appyhigh.newsfeedsdk.model.feeds.Card
import com.appyhigh.newsfeedsdk.model.feeds.Item
import com.appyhigh.newsfeedsdk.utils.EndlessScrolling
import com.appyhigh.newsfeedsdk.utils.PodcastMediaPlayer
import com.appyhigh.newsfeedsdk.utils.SpUtil
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class CryptoListActivity : AppCompatActivity(), ApiCrypto.CryptoSearchListener {

    private var binding: ActivityCryptoDetailsBinding? = null
    var pageNo=0
    var postImpressions = HashMap<String, PostView>()
    var orderType:String?=null
    private var endlessScrolling: EndlessScrolling? = null
    private var linearLayoutManager: LinearLayoutManager? = null
    private var cryptoAdapter: CryptoDetailsAdapter? = null
    private var presentUrl = ""
    private var presentTimeStamp:Long = 0
    private var interest = ""
    private var watchlist:String?=null
    private var searchAdapter = CryptoSearchAdapter(ArrayList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCryptoDetailsBinding.inflate(layoutInflater)
        val view = binding?.root
        setContentView(view)
        setFonts(view)
        PodcastMediaPlayer.setPodcastListener(view!!, "cryptoDetails")
        binding!!.pbLoading.visibility = View.VISIBLE
        binding!!.rvPosts.visibility = View.GONE
        binding!!.backBtn.setOnClickListener { finish() }
        if(intent.hasExtra(Constants.ORDER)) {
            orderType = intent.getStringExtra(Constants.ORDER)!!
        }
        interest = intent.getStringExtra(Constants.INTEREST)!!
        binding!!.headerTitle.text = when(interest){
            "crypto_gainers" -> "Top Gainers"
            "crypto_losers" -> "Top Losers"
            "crypto_watchlist_edit" -> "Edit Watchlist"
            else -> "All Coins"
        }
        when(interest){
            "crypto_gainers" -> SpUtil.cryptoEventsListener?.onCryptoHomeCTAClicked("Top Gainers", "See All")
            "crypto_losers" -> SpUtil.cryptoEventsListener?.onCryptoHomeCTAClicked("Top Losers", "See All")
            "crypto_watchlist_edit" -> SpUtil.cryptoEventsListener?.onCryptoHomeCTAClicked("Watchlist", "Edit")
            else -> SpUtil.cryptoEventsListener?.onCryptoHomeCTAClicked("Watchlist", "All Coins")
        }
        if(interest == "crypto_watchlist_edit"){
            val watchListString = ApiCreateOrUpdateUser().getWatchlistString()
            watchlist = if(watchListString!="") watchListString else null
        }
        binding!!.rvSearchPosts.adapter = searchAdapter
        fetchData()
        binding!!.pullToRefresh.setOnRefreshListener {
            fetchData(object :OnRefreshListener{
                override fun onRefreshNeeded() {
                    binding!!.pullToRefresh.isRefreshing = false
                }
            })
        }
        binding!!.search.doOnTextChanged { text, start, before, count ->
            if(text.isNullOrEmpty()){
                binding!!.rvSearchPosts.visibility = View.GONE
                searchAdapter.updateList(ArrayList())
            } else{
                binding!!.rvSearchPosts.visibility = View.VISIBLE
                FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let {
                    ApiCrypto().searchCryptoCoinsEncrypted(
                        Endpoints.CRYPTO_SEARCH_ENCRYPTED,
                        it,
                        text.toString(),this)
                }
            }
        }
    }

    private fun setFonts(view: View?){
        Card.setFontFamily(binding?.headerTitle, true)
        Card.setFontFamily(binding?.search)
        Card.setFontFamily(view?.findViewById(R.id.podcastBottomTitle))
        Card.setFontFamily(view?.findViewById(R.id.podcastBottomPublisherName))
    }

    private fun fetchData(listener: OnRefreshListener?=null){
        FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let {
            ApiCrypto().getCryptoDetailsEncrypted(
                Endpoints.GET_CRYPTO_DETAILS_ENCRYPTED,
                it,
                if(listener==null) pageNo else 0,
                watchlist,
                orderType, object : ApiCrypto.CryptoDetailsResponseListener {
                    override fun onSuccess(
                        cryptoResponse: ApiCrypto.CryptoDetailsResponse,
                        url: String,
                        timeStamp: Long
                    ) {
                        presentUrl = url
                        presentTimeStamp = timeStamp
                        val cryptoCard = cryptoResponse.cards
                        val newCryptoCardItems = ArrayList<Item>()
                        val loadMore = Item(key_id = Constants.LOADER)
                        if (cryptoCard != null) {
                            newCryptoCardItems.addAll(cryptoCard.items)
                        }
                        newCryptoCardItems.add(loadMore)
                        cryptoAdapter = CryptoDetailsAdapter(newCryptoCardItems, isEditable = true, cryptoCard?.cardType?:"", true, interest)
                        linearLayoutManager = LinearLayoutManager(this@CryptoListActivity)
                        binding!!.rvPosts.apply {
                            layoutManager = linearLayoutManager
                            adapter = cryptoAdapter
                            itemAnimator = null
                        }
                        binding!!.pbLoading.visibility = View.GONE
                        listener?.onRefreshNeeded()
                        binding!!.rvPosts.visibility = View.VISIBLE
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
                        getMoreCryptoPosts()
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

    fun getMoreCryptoPosts(){
        if(interest == "crypto_watchlist_edit"){
            val watchListString = ApiCreateOrUpdateUser().getWatchlistString()
            watchlist = if(watchListString!="") watchListString else null
        }
        FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let {
            ApiCrypto().getCryptoDetailsEncrypted(
                Endpoints.GET_CRYPTO_DETAILS_ENCRYPTED,
                it,
                pageNo,
                null,
                orderType, object : ApiCrypto.CryptoDetailsResponseListener {
                    override fun onSuccess(
                        cryptoResponse: ApiCrypto.CryptoDetailsResponse,
                        url: String,
                        timeStamp: Long
                    ) {
                        presentUrl = url
                        presentTimeStamp = timeStamp
                        val cryptoCard = cryptoResponse.cards
                        val newCryptoCardItems = ArrayList<Item>()
                        val loadMore = Item(key_id = Constants.LOADER)
                        if(cryptoCard!=null && cryptoCard.items.isNotEmpty()) {
                            newCryptoCardItems.addAll(cryptoCard.items)
                            newCryptoCardItems.add(loadMore)
                            cryptoAdapter?.updateList(newCryptoCardItems)
                            pageNo += 1
                        }
                    }
                })
        }
    }

    override fun onSuccess(cryptoResponse: CryptoSearchResponse) {
        val data = cryptoResponse.map {
            CryptoSearchItem(it.coinId,it.coinName,it.coinSymbol)
        }
        searchAdapter.updateList(data as ArrayList<CryptoSearchItem>)
    }
}
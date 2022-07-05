package com.appyhigh.newsfeedsdk.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.adapter.NewsFeedAdapter
import com.appyhigh.newsfeedsdk.apicalls.ApiCrypto
import com.appyhigh.newsfeedsdk.apiclient.Endpoints
import com.appyhigh.newsfeedsdk.databinding.FragmentCryptoCoinBinding
import com.appyhigh.newsfeedsdk.model.feeds.Card

class CryptoCoinFragment : Fragment() {

    lateinit var binding: FragmentCryptoCoinBinding
    private var interest:String="unknown"
    private var coinId:String=""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCryptoCoinBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if(Constants.cardsMap.containsKey(interest)){
            val adapter = NewsFeedAdapter(Constants.cardsMap[interest]!!, null, interest)
            binding.rvPosts.adapter = adapter
            binding.pbLoading.visibility = View.GONE
            binding.rvPosts.visibility = View.VISIBLE
        } else{
            FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let {
                ApiCrypto().getCryptoCoinDetailsEncrypted(
                    Endpoints.GET_CRYPTO_COIN_DETAILS_ENCRYPTED,
                    it,
                    coinId,
                    interest,
                    null,
                    null, null, object : ApiCrypto.CryptoResponseListener {
                        override fun onSuccess(
                            cryptoResponse: ApiCrypto.CryptoResponse,
                            url: String,
                            timeStamp: Long
                        ) {
                            val cards = cryptoResponse.cards as ArrayList<Card>
                            Constants.cardsMap[interest] = cards
                            val adapter = NewsFeedAdapter(cards, null, interest)
                            binding.rvPosts.adapter = adapter
                            binding.pbLoading.visibility = View.GONE
                            binding.rvPosts.visibility = View.VISIBLE
                        }
                    }
                )
            }
        }
    }

    companion object {
        fun newInstance(coin_id:String, tabInterest: String) = CryptoCoinFragment().apply {
            interest = tabInterest
            coinId = coin_id
        }
    }
}
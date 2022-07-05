package com.appyhigh.newsfeedsdk.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.adapter.NewsFeedAdapter
import com.appyhigh.newsfeedsdk.apicalls.ApiCrypto
import com.appyhigh.newsfeedsdk.apiclient.Endpoints
import com.appyhigh.newsfeedsdk.callbacks.OnRefreshListener
import com.appyhigh.newsfeedsdk.databinding.FragmentCryptoAlertListBinding
import com.appyhigh.newsfeedsdk.model.feeds.Card
import com.appyhigh.newsfeedsdk.utils.SpUtil
import java.util.*

/**
 * A simple [Fragment] subclass.
 * Use the [CryptoAlertListFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CryptoAlertListFragment : Fragment(), OnRefreshListener {

    lateinit var binding: FragmentCryptoAlertListBinding
    var newsFeedAdapter:NewsFeedAdapter?=null
    var keepLag = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = FragmentCryptoAlertListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Card.setFontFamily(binding?.priceAlertsTitle)
        Card.setFontFamily(binding?.addPriceAlertTitle)
        SpUtil.alertRefreshListener = this
        if(keepLag){
            Handler(Looper.getMainLooper()).postDelayed({
                FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let {
                    ApiCrypto().getCryptoAlertViewEncrypted(
                        Endpoints.CRYPTO_ALERT_VIEW_ENCRYPTED,
                        it,
                        object : ApiCrypto.CryptoResponseListener{
                            override fun onSuccess(cryptoResponse: ApiCrypto.CryptoResponse, url: String, timeStamp: Long) {
                                if(cryptoResponse.cards.isEmpty()){
                                    binding.pbLoading.visibility = View.GONE
                                    binding.cryptoPriceAlert.visibility = View.GONE
                                    binding.noPriceAlerts.visibility = View.VISIBLE
                                } else {
                                    binding.pbLoading.visibility = View.GONE
                                    binding.rvPosts.visibility = View.VISIBLE
                                    binding.cryptoPriceAlert.visibility = View.VISIBLE
                                    newsFeedAdapter = NewsFeedAdapter(cryptoResponse.cards as ArrayList<Card>, null, "crypto_alert")
                                    binding.rvPosts.apply {
                                        adapter = newsFeedAdapter
                                        layoutManager = LinearLayoutManager(requireContext())
                                    }
                                }
                            }
                        })
                }
            },1000)
        } else{
            FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let {
                ApiCrypto().getCryptoAlertViewEncrypted(
                    Endpoints.CRYPTO_ALERT_VIEW_ENCRYPTED,
                    it,
                    object : ApiCrypto.CryptoResponseListener{
                        override fun onSuccess(cryptoResponse: ApiCrypto.CryptoResponse, url: String, timeStamp: Long) {
                            if(cryptoResponse.cards.isEmpty()){
                                binding.pbLoading.visibility = View.GONE
                                binding.cryptoPriceAlert.visibility = View.GONE
                                binding.noPriceAlerts.visibility = View.VISIBLE
                            } else {
                                binding.pbLoading.visibility = View.GONE
                                binding.rvPosts.visibility = View.VISIBLE
                                binding.cryptoPriceAlert.visibility = View.VISIBLE
                                newsFeedAdapter = NewsFeedAdapter(cryptoResponse.cards as ArrayList<Card>, null, "crypto_alert")
                                binding.rvPosts.apply {
                                    adapter = newsFeedAdapter
                                    layoutManager = LinearLayoutManager(requireContext())
                                }
                            }
                        }
                    })
            }
        }
        binding.cryptoPriceAlert.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.baseFragment, CryptoAlertSelectFragment.newInstance())
                .addToBackStack(null)
                .commit()
        }
        binding.noPriceCryptoPriceAlert.setOnClickListener { binding.cryptoPriceAlert.performClick() }
        binding.backBtn.setOnClickListener {
            SpUtil.backPressListener?.onFragmentBackPressed()
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment CryptoAlertListFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(keepLag: Boolean = false) : CryptoAlertListFragment{
             val cryptoAlertListFragment =  CryptoAlertListFragment()
             cryptoAlertListFragment.keepLag = keepLag
             return cryptoAlertListFragment
        }
    }

    override fun onRefreshNeeded() {
        binding.pbLoading.visibility = View.VISIBLE
        binding.rvPosts.visibility = View.GONE
        binding.noPriceAlerts.visibility = View.GONE
        FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let {
            ApiCrypto().getCryptoAlertViewEncrypted(
                Endpoints.CRYPTO_ALERT_VIEW_ENCRYPTED,
                it,
                object : ApiCrypto.CryptoResponseListener{
                override fun onSuccess(cryptoResponse: ApiCrypto.CryptoResponse, url: String, timeStamp: Long) {
                    if(cryptoResponse.cards.isEmpty()){
                        binding.pbLoading.visibility = View.GONE
                        binding.cryptoPriceAlert.visibility = View.GONE
                        binding.noPriceAlerts.visibility = View.VISIBLE
                    } else {
                        binding.pbLoading.visibility = View.GONE
                        binding.rvPosts.visibility = View.VISIBLE
                        binding.cryptoPriceAlert.visibility = View.VISIBLE
                        newsFeedAdapter?.refreshList(cryptoResponse.cards as ArrayList<Card>)
                    }
                }
            })
        }
    }
}
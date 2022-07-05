package com.appyhigh.newsfeedsdk.fragment

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.adapter.CryptoDetailsAdapter
import com.appyhigh.newsfeedsdk.apicalls.ApiCrypto
import com.appyhigh.newsfeedsdk.apiclient.Endpoints
import com.appyhigh.newsfeedsdk.callbacks.OnFragmentClickListener
import com.appyhigh.newsfeedsdk.databinding.FragmentCryptoAlertSelectBinding
import com.appyhigh.newsfeedsdk.model.crypto.CryptoSearchResponse
import com.appyhigh.newsfeedsdk.model.feeds.Card
import com.appyhigh.newsfeedsdk.model.feeds.Item
import com.appyhigh.newsfeedsdk.utils.EndlessScrolling
import com.appyhigh.newsfeedsdk.utils.SpUtil


/**
 * A simple [Fragment] subclass.
 * Use the [CryptoAlertSelectFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CryptoAlertSelectFragment : Fragment(), ApiCrypto.CryptoSearchListener {

    lateinit var binding: FragmentCryptoAlertSelectBinding
    var pageNo=0
    private var endlessScrolling: EndlessScrolling? = null
    private var linearLayoutManager: LinearLayoutManager? = null
    private var cryptoAdapter: CryptoDetailsAdapter? = null
    private var showSearch = false
    private var cryptoAlertItems = ArrayList<Item>()
    private var type: String? = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = FragmentCryptoAlertSelectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Card.setFontFamily(binding.headerText)
        Card.setFontFamily(binding.searchText)
        binding.backBtn.setOnClickListener {
            hideKeyboard()
            SpUtil.backPressListener?.onFragmentBackPressed()
        }
        binding.searchIcon.setOnClickListener {
            if(showSearch){
                showSearch = false
                hideKeyboard()
                cryptoAdapter?.updateWatchList(cryptoAlertItems)
                binding.searchText.visibility = View.GONE
                binding.headerText.visibility = View.VISIBLE
                binding.searchIcon.setImageResource(R.drawable.ic_crypto_alert_search)
            } else{
                showSearch = true
                binding.headerText.visibility = View.GONE
                binding.searchText.visibility = View.VISIBLE
                binding.searchIcon.setImageResource(R.drawable.ic_close)
                try{
                    binding.searchText.requestFocus()
                    val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
                } catch (ex:java.lang.Exception){
                    ex.printStackTrace()
                }
            }
        }
        binding.searchText.doOnTextChanged { text, start, before, count ->
            if(text.isNullOrEmpty()){
                cryptoAdapter?.updateWatchList(cryptoAlertItems)
            } else{
                FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let {
                    ApiCrypto().searchCryptoCoinsEncrypted(
                        Endpoints.CRYPTO_SEARCH_ENCRYPTED,
                        it,
                        text.toString(),this)
                }
            }
        }
        fetchData()
    }

    private fun fetchData(){
        FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let {
            ApiCrypto().getCryptoDetailsEncrypted(
                Endpoints.GET_CRYPTO_DETAILS_ENCRYPTED,
                it,
                0, null, null, object : ApiCrypto.CryptoDetailsResponseListener {
                override fun onSuccess(cryptoResponse: ApiCrypto.CryptoDetailsResponse, url: String, timeStamp: Long) {
                    val cryptoCard = cryptoResponse.cards
                    val newCryptoCardItems = ArrayList<Item>()
                    val loadMore = Item(key_id = Constants.LOADER)
                    if (cryptoCard != null) {
                        newCryptoCardItems.addAll(cryptoCard.items)
                        cryptoAlertItems.addAll(cryptoCard.items)
                    }
                    newCryptoCardItems.add(loadMore)
                    val cardType = if(type==Constants.CRYPTO_CONVERTER) Constants.CRYPTO_CONVERTER else Constants.CRYPTO_ALERT_SELECT
                    cryptoAdapter = CryptoDetailsAdapter(newCryptoCardItems, false, cardType,
                        false, "", object : OnFragmentClickListener {
                            override fun onFragmentClicked() {
                                hideKeyboard()
                            }

                            override fun onCryptoConvertorClicked(coinId: String) {
                                val output = Intent()
                                output.putExtra("CoinId", coinId)
                                val activity = requireContext() as Activity
                                activity.setResult(RESULT_OK, output)
                                activity.finish()
                            }
                        })
                    linearLayoutManager = LinearLayoutManager(requireContext())
                    binding.rvPosts.apply {
                        layoutManager = linearLayoutManager
                        adapter = cryptoAdapter
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
    }

    private fun setEndlessScrolling() {
        try {
            if (endlessScrolling == null) {
                endlessScrolling = object : EndlessScrolling(linearLayoutManager!!) {
                    override fun onLoadMore(currentPages: Int) {
                        if(!showSearch) {
                            getMoreCryptoPosts()
                        }
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

    fun getMoreCryptoPosts(){
        FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let {
            ApiCrypto().getCryptoDetailsEncrypted(
                Endpoints.GET_CRYPTO_DETAILS_ENCRYPTED,
                it,
                pageNo, null, null, object : ApiCrypto.CryptoDetailsResponseListener {
                override fun onSuccess(cryptoResponse: ApiCrypto.CryptoDetailsResponse, url: String, timeStamp: Long) {
                    val cryptoCard = cryptoResponse.cards
                    val newCryptoCardItems = ArrayList<Item>()
                    val loadMore = Item(key_id = Constants.LOADER)
                    if(cryptoCard!=null && cryptoCard.items.isNotEmpty()) {
                        newCryptoCardItems.addAll(cryptoCard.items)
                        cryptoAlertItems.addAll(cryptoCard.items)
                        newCryptoCardItems.add(loadMore)
                        cryptoAdapter?.updateList(newCryptoCardItems)
                        pageNo += 1
                    }
                }
            })
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        hideKeyboard()
    }

    private fun hideKeyboard(){
        try {
            val imm = requireContext().getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.searchText.windowToken, 0)
        } catch (ex:Exception){
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
         * @return A new instance of fragment CryptoAlertSelectFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(type:String?=""): CryptoAlertSelectFragment{
            val cryptoAlertSelectFragment = CryptoAlertSelectFragment()
            cryptoAlertSelectFragment.type = type
            return cryptoAlertSelectFragment
        }
    }

    override fun onSuccess(cryptoResponse: CryptoSearchResponse) {
        val data = cryptoResponse.map {
            Item(
                coinId = it.coinId,
                coinName = it.coinName,
                coinSymbol = it.coinSymbol,
                imageLink = it.image
            )
        }
        cryptoAdapter?.updateWatchList(data as ArrayList<Item>)
        cryptoAlertItems.addAll(data)
    }
}
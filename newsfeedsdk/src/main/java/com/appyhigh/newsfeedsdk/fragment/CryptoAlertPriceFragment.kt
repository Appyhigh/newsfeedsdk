package com.appyhigh.newsfeedsdk.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.apicalls.ApiCrypto
import com.appyhigh.newsfeedsdk.apiclient.Endpoints
import com.appyhigh.newsfeedsdk.callbacks.NumberKeyboardListener
import com.appyhigh.newsfeedsdk.databinding.FragmentCryptoAlertPriceBinding
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.model.feeds.Card
import com.appyhigh.newsfeedsdk.model.feeds.Item
import com.appyhigh.newsfeedsdk.utils.SpUtil
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * A simple [Fragment] subclass.
 * Use the [CryptoAlertPriceFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CryptoAlertPriceFragment : Fragment() {
    private var coinName: String = ""
    private var coinId: String = ""
    private var coinIcon: String = ""
    private var targetPrice: Double = 0.0
    private var currPrice: Double? = null

    lateinit var binding: FragmentCryptoAlertPriceBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentCryptoAlertPriceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Card.setFontFamily(binding.title)
        Card.setFontFamily(binding.coinName)
        Card.setFontFamily(binding.enterTargetPrice)
        Card.setFontFamily(binding.editPrice)
        Card.setFontFamily(binding.currPriceTitle)
        Card.setFontFamily(binding.currPrice, true)
        Card.setFontFamily(binding.setPriceTitle)
        binding.backBtn.setOnClickListener {
            SpUtil.backPressListener?.onFragmentBackPressed()
        }
        binding.coinName.text = coinName
        Item.setImage(binding.coinSymbol, coinIcon)
        if(currPrice!=null){
            binding.currPrice.text = Constants.getCryptoCoinSymbol()+ BigDecimal(currPrice!!).setScale(2, RoundingMode.HALF_EVEN)
        } else{
            FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let {
                ApiCrypto().getCryptoCoinDetailsEncrypted(
                    Endpoints.GET_CRYPTO_COIN_DETAILS_ENCRYPTED,
                    it,
                    coinId, null, null, null, null, object : ApiCrypto.CryptoResponseListener{
                        override fun onSuccess(cryptoResponse: ApiCrypto.CryptoResponse, url: String, timeStamp: Long) {
                            try{
                                currPrice = cryptoResponse.cards[0].items[0].current_price
                                binding.currPrice.text = Constants.getCryptoCoinSymbol()+ BigDecimal(currPrice!!).setScale(2, RoundingMode.HALF_EVEN)
                            } catch (ex:Exception){
                                LogDetail.LogEStack(ex)
                            }
                        }
                    })
            }
        }
        binding.cryptoPriceAlert.setOnClickListener {
            try{
                if(binding.editPrice.text.isEmpty()){
                    return@setOnClickListener
                }
            } catch (ex: Exception){
                LogDetail.LogEStack(ex)
            }
            binding.cryptoPriceAlert.visibility = View.GONE
            binding.priceSaving.visibility = View.VISIBLE
            var upperThreshold:Double? = null
            var lowerThreshold:Double? = null
            try{
                if(binding.editPrice.text.toString().toDouble()>currPrice!!){
                    upperThreshold = binding.editPrice.text.toString().toDouble()
                } else{
                    lowerThreshold = binding.editPrice.text.toString().toDouble()
                }
            } catch (ex:Exception){
                LogDetail.LogEStack(ex)
                binding.cryptoPriceAlert.visibility = View.VISIBLE
                binding.priceSaving.visibility = View.GONE
                return@setOnClickListener
            }
            FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let { it1 ->
                ApiCrypto().addCryptoAlertEncrypted(
                    Endpoints.CRYPTO_ALERT_ADD_ENCRYPTED,
                    it1,
                    coinId, upperThreshold, lowerThreshold, object : ApiCrypto.CryptoAlertResponseListener{
                        override fun onSuccess() {
                            val parentManager = parentFragmentManager
                            parentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                            parentManager.beginTransaction()
                                .add(R.id.baseFragment, CryptoAlertListFragment.newInstance())
                                .disallowAddToBackStack()
                                .commit()
                        }
                    })
            }
        }
        binding.numPad.setListener(object : NumberKeyboardListener{
            override fun onResult(value: String, cursorAt: Int) {
                try{
                    binding.editPrice.text = value
                    if(value.toDouble()>currPrice!!){
                        binding.thresholdIcon.setImageResource(R.drawable.ic_crypto_alert_upper_threshold)
                    } else{
                        binding.thresholdIcon.setImageResource(R.drawable.ic_crypto_alert_lower_threshold)
                    }
                } catch (ex:Exception){
                    LogDetail.LogEStack(ex)
                }
            }
        })
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment CryptoAlertPriceFragment.
         */
        @JvmStatic
        fun newInstance(coinId: String, coinName: String, coinIcon: String, targetPrice: Double, currPrice: Double?) : CryptoAlertPriceFragment {
            val cryptoAlertPriceFragment = CryptoAlertPriceFragment()
            cryptoAlertPriceFragment.coinName = coinName
            cryptoAlertPriceFragment.coinId = coinId
            cryptoAlertPriceFragment.coinIcon = coinIcon
            cryptoAlertPriceFragment.targetPrice = targetPrice
            cryptoAlertPriceFragment.currPrice = currPrice
            return cryptoAlertPriceFragment
        }
    }
}
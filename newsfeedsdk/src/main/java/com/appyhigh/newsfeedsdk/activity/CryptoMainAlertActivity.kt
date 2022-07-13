package com.appyhigh.newsfeedsdk.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.callbacks.BackPressListener
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.fragment.CryptoAlertListFragment
import com.appyhigh.newsfeedsdk.fragment.CryptoAlertSelectFragment
import com.appyhigh.newsfeedsdk.utils.SpUtil

class CryptoMainAlertActivity : AppCompatActivity(), BackPressListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crypto_main_alert)
        SpUtil.backPressListener = this
        try{
            if(intent.hasExtra("isConverter")){
                supportFragmentManager.beginTransaction()
                    .add(R.id.baseFragment, CryptoAlertSelectFragment.newInstance(Constants.CRYPTO_CONVERTER), "cryptoConverter")
                    .disallowAddToBackStack()
                    .commit()
            } else{
                supportFragmentManager.beginTransaction()
                    .add(R.id.baseFragment, CryptoAlertListFragment.newInstance(), "cryptoAlert")
                    .disallowAddToBackStack()
                    .commit()
            }
        } catch (ex: Exception){
            LogDetail.LogEStack(ex)
        }
    }

    override fun onFragmentBackPressed() {
        if(supportFragmentManager.backStackEntryCount>0){
            onBackPressed()
        } else{
            finish()
        }
    }
}
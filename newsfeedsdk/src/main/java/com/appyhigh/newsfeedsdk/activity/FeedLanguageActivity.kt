package com.appyhigh.newsfeedsdk.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.GridLayoutManager
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.adapter.FeedLanguagesAdapter
import com.appyhigh.newsfeedsdk.apicalls.ApiUpdateUserPersonalization
import com.appyhigh.newsfeedsdk.apiclient.Endpoints
import com.appyhigh.newsfeedsdk.databinding.ActivityFeedLanguageBinding
import com.appyhigh.newsfeedsdk.fragment.AddInterestBottomSheet
import com.appyhigh.newsfeedsdk.model.Language
import com.appyhigh.newsfeedsdk.model.feeds.Card
import com.appyhigh.newsfeedsdk.utils.SpUtil
import java.util.*
import kotlin.collections.ArrayList

class FeedLanguageActivity : AppCompatActivity() {

    private var binding: ActivityFeedLanguageBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeedLanguageBinding.inflate(layoutInflater)
        val view = binding?.root
        setContentView(view)
        Card.setFontFamily(binding?.tvTitle, true)
        Card.setFontFamily(binding?.tvSubTitle)
        Card.setFontFamily(binding?.btnProceed)
        binding!!.pbLoading.visibility = View.GONE
        val oldList = FeedSdk.languagesList
        val languageMap = Constants.allLanguagesMap

        for (item in oldList) {
            if (languageMap[item.id.lowercase(Locale.getDefault())] != null) {
                languageMap[item.id.lowercase(Locale.getDefault())]?.isSelected = true
            }
        }
        val finalList = ArrayList<Language>(languageMap.values)
        val languageAdapter = FeedLanguagesAdapter(finalList)
        binding!!.rvLanguages.apply {
            layoutManager = GridLayoutManager(this@FeedLanguageActivity, 2)
            adapter = languageAdapter
        }
        binding!!.btnProceed.setOnClickListener {

            val list = languageAdapter?.getItems()?.filter { it.isSelected }

            val languageList = ArrayList<com.appyhigh.newsfeedsdk.model.Language>()
            if (list != null) {
                for (item in list) {
                    languageList.add(
                        com.appyhigh.newsfeedsdk.model.Language(
                            item.id,
                            item.language,
                            item.sampleText,
                            item.isSelected,
                            item.nativeName
                        )
                    )
                }
            }
            if (languageList.isEmpty()) {
                Constants.Toaster.show(this, getString(R.string.choose_atleast_one_language))
            } else {
                binding!!.btnProceed.visibility = View.GONE
                binding!!.pbLoader.visibility = View.VISIBLE
                FeedSdk.languagesList = languageList
                FeedSdk.userId?.let { it1 ->
                    FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let { it2 ->
                        ApiUpdateUserPersonalization().updateUserPersonalizationEncrypted(
                            Endpoints.UPDATE_USER_ENCRYPTED,
                            it1,
                            FeedSdk.interestsList,
                            FeedSdk.languagesList,
                            object : ApiUpdateUserPersonalization.UpdatePersonalizationListener {
                                override fun onSuccess() {
                                    for (listener in SpUtil.onRefreshListeners) {
                                        listener.value.onRefreshNeeded()
                                    }
                                    callPersonaliseEvent()
                                    if(FeedSdk.interestsList.isEmpty()){
                                        try{
                                            val bottomSheet = AddInterestBottomSheet.newInstance()
                                            bottomSheet.show((FeedSdk.mContext as FragmentActivity).supportFragmentManager,"BottomSheet")
                                            finish()
                                        } catch (ex:Exception){
                                            val intent = Intent(this@FeedLanguageActivity, FeedInterestsActivity::class.java)
                                            startActivity(intent)
                                            setResult(RESULT_OK)
                                            finish()
                                        }
                                    } else{
                                        Handler(Looper.getMainLooper()).postDelayed({
                                            setResult(RESULT_OK)
                                            finish()
                                        },1000)
                                    }
                                }

                                override fun onFailure() {

                                }
                            }
                        )
                    }
                }
            }
        }
        binding!!.ivBack.setOnClickListener {
            onBackPressed()
        }
    }

    private fun callPersonaliseEvent(){
        var interests =""
        var languages=""
        for(interest in FeedSdk.interestsList){
            interests += interest.keyId+","
        }
        if(interests.endsWith(",")){
            interests.dropLast(1)
        }
        for(lang in FeedSdk.languagesList){
            languages+=lang.language+","
        }
        if(languages.endsWith(",")){
            languages.dropLast(1)
        }
        SpUtil.eventsListener?.onPersonalizePopup(true, interests, languages)
    }
}
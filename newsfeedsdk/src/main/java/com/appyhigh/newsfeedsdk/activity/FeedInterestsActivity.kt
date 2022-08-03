package com.appyhigh.newsfeedsdk.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.GridLayoutManager
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.adapter.FeedInterestsAdapter
import com.appyhigh.newsfeedsdk.apicalls.ApiUpdateUserPersonalization
import com.appyhigh.newsfeedsdk.apiclient.Endpoints
import com.appyhigh.newsfeedsdk.databinding.ActivityFeedInterestsBinding
import com.appyhigh.newsfeedsdk.model.Interest
import com.appyhigh.newsfeedsdk.model.Thumbnail
import com.appyhigh.newsfeedsdk.model.feeds.Card
import com.appyhigh.newsfeedsdk.utils.SpUtil

class FeedInterestsActivity : AppCompatActivity() {

    private var binding: ActivityFeedInterestsBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeedInterestsBinding.inflate(layoutInflater)
        val view = binding?.root
        setContentView(view)
        binding!!.pbLoading.visibility = View.GONE
        Card.setFontFamily(binding?.tvTitle, true)
        Card.setFontFamily(binding?.tvSubTitle, true)
        Card.setFontFamily(binding?.btnProceed)
        val oldList = ArrayList<Interest>(FeedSdk.interestsList)
        val interestMap = LinkedHashMap<String, Interest>(Constants.allInterestsMap)
        for (item in oldList) {
            if (interestMap[item.keyId] != null) {
                interestMap[item.keyId]?.userSelected = true
            }
        }
        val finalList = ArrayList<Interest>(interestMap.values)
        val interestAdapter = FeedInterestsAdapter(finalList)
        binding!!.rvInterests.apply {
            layoutManager = GridLayoutManager(this@FeedInterestsActivity, 2)
            adapter = interestAdapter
            itemAnimator = null
        }
        binding!!.ivBack.setOnClickListener {
            onBackPressed()
        }
        val pbLoader = findViewById<ProgressBar>(R.id.pbLoader)
        val btnProceed = findViewById<AppCompatButton>(R.id.btnProceed)
        btnProceed.setOnClickListener {
            val list = interestAdapter?.getItems()
                ?.filter { it.userSelected }
            val interestList = ArrayList<com.appyhigh.newsfeedsdk.model.Interest>()
            if (list != null) {
                for (item in list) {
                    interestList.add(
                        com.appyhigh.newsfeedsdk.model.Interest(
                            item.label,
                            item.keyId,
                            Thumbnail(
                                item.thumbnails?.hdpi,
                                item.thumbnails?.xxxhdpi,
                                item.thumbnails?.xxhdpi,
                                item.thumbnails?.mdpi,
                                item.thumbnails?.xhdpi
                            )
                        )
                    )
                }
            }
            FeedSdk.interestsList = interestList

            if (interestList.size < 3) {
                Constants.Toaster.show(this, getString(R.string.choose_atleast_three_interests))
            } else {
                btnProceed.visibility = View.GONE
                pbLoader.visibility = View.VISIBLE
                ApiUpdateUserPersonalization().updateUserPersonalizationEncrypted(
                    Endpoints.UPDATE_USER_ENCRYPTED,
                    FeedSdk.interestsList,
                    FeedSdk.languagesList,
                    object : ApiUpdateUserPersonalization.UpdatePersonalizationListener {
                        override fun onSuccess() {
                            for (listener in SpUtil.onRefreshListeners) {
                                listener.value.onRefreshNeeded()
                            }
                            callPersonaliseEvent()
                            if(FeedSdk.languagesList.isEmpty()){
                                val intent = Intent(this@FeedInterestsActivity, FeedLanguageActivity::class.java)
                                startActivity(intent)
                                setResult(RESULT_OK)
                                finish()
                            } else {
                                Handler(Looper.getMainLooper()).postDelayed({
                                    setResult(RESULT_OK)
                                    finish()
                                }, 1000)
                            }
                        }

                        override fun onFailure() {

                        }
                    })
                }

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
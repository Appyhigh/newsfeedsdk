package com.appyhigh.newsfeedsdk.activity

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.adapter.NewsFeedSliderAdapter
import com.appyhigh.newsfeedsdk.adapter.TabsAdapter
import com.appyhigh.newsfeedsdk.apicalls.ApiCricketSchedule
import com.appyhigh.newsfeedsdk.apiclient.Endpoints
import com.appyhigh.newsfeedsdk.callbacks.TabSelectedListener
import com.appyhigh.newsfeedsdk.customview.NewsFeedList
import com.appyhigh.newsfeedsdk.databinding.ActivityPwaCricketTabsBinding
import com.appyhigh.newsfeedsdk.fragment.CricketPWAFragment
import com.appyhigh.newsfeedsdk.fragment.PagerFragment
import com.appyhigh.newsfeedsdk.model.CricketScheduleResponse
import com.appyhigh.newsfeedsdk.model.feeds.Card
import com.appyhigh.newsfeedsdk.model.feeds.Item
import java.text.FieldPosition

class PWACricketTabsActivity : AppCompatActivity() {

    lateinit var binding: ActivityPwaCricketTabsBinding
    var fragmentList = ArrayList<Fragment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPwaCricketTabsBinding.inflate(layoutInflater)
        val view = binding?.root
        setContentView(view)
        var tabsAdapter: TabsAdapter? = null
        binding.backBtn.setOnClickListener { finish() }
        val interest = intent.getStringExtra(Constants.INTEREST) ?: ""
        val url = intent.getStringExtra("link") ?: ""
        var selectTab = 0
        var newsTab = -1
        FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let {
            ApiCricketSchedule().getCricketTabsEncrypted(
                Endpoints.GET_CRICKET_TABS_ENCRYPTED,
                it,
                object : ApiCricketSchedule.CricketScheduleResponseListener {
                    override fun onSuccess(cricketScheduleResponse: CricketScheduleResponse) {
                        binding.pbLoading.visibility = View.GONE
                        var cards = cricketScheduleResponse.cards[0].items as ArrayList<Item>
                        fragmentList = ArrayList<Fragment>()
                        var count = 0
                        for (tab in cards) {
                            if (tab.key_id == interest || checkMatch(tab.pwaLink, url)) {
                                selectTab = count
                            }
                            if (tab.key_id == "cricket_news") {
                                newsTab = count
                                fragmentList.add(
                                    PagerFragment.newInstance(
                                        Constants.allInterestsMap["cricket"]!!.keyId.toString(), 0,
                                        ArrayList(),
                                        false,
                                        object : NewsFeedList.PersonalizationListener {
                                            override fun onPersonalizationClicked() {}

                                            override fun onRefresh() {}
                                        },
                                    )
                                )
                            } else {
                                fragmentList.add(
                                    CricketPWAFragment.newInstance(
                                        tab.pwaLink,
                                        tab.key_id!!
                                    )
                                )
                            }
                            count += 1
                        }
                        binding.rvCricketTabs.apply {
                            layoutManager = LinearLayoutManager(
                                context,
                                LinearLayoutManager.HORIZONTAL,
                                false
                            )
                            tabsAdapter = TabsAdapter(cards, object : TabSelectedListener {
                                override fun onTabClicked(v: View, position: Int) {
                                    binding.vpCricketFeed.setCurrentItem(position, false)
                                }
                            }, "cricket", selectTab)
                            adapter = tabsAdapter
                        }
                        binding.vpCricketFeed.isUserInputEnabled = true
                        binding.vpCricketFeed.offscreenPageLimit = 3
                        binding.vpCricketFeed.adapter =
                            NewsFeedSliderAdapter(this@PWACricketTabsActivity, fragmentList)
                        binding.vpCricketFeed.setCurrentItem(selectTab, false)
                        moveTab(tabsAdapter, selectTab)
                        notifyListener(cards[selectTab])
                        binding.vpCricketFeed.registerOnPageChangeCallback(object :
                            ViewPager2.OnPageChangeCallback() {
                            override fun onPageSelected(position: Int) {
                                super.onPageSelected(position)
                                try {
                                    notifyListener(cards[position])
                                    if (fragmentList[position] is CricketPWAFragment) {
                                        (fragmentList[newsTab] as PagerFragment).stopVideoPlayback()
                                    }
                                } catch (ex: Exception) {
                                    ex.printStackTrace()
                                }
                                moveTab(tabsAdapter, position)
                            }
                        })
                    }

                    override fun onFailure(error: Throwable) {}
                })
        }
    }

    private fun moveTab(tabsAdapter: TabsAdapter?, position: Int) {
        try {
            val isUp = tabsAdapter!!.getCurrentPosition() <= position
            if (isUp) {
                if (tabsAdapter?.itemCount!! > position + 1) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        binding.rvCricketTabs.scrollToPosition(position + 1)
                    }, 200)
                } else {
                    Handler(Looper.getMainLooper()).postDelayed({
                        binding.rvCricketTabs.scrollToPosition(position)
                    }, 200)
                }
            } else {
                if (position - 1 > -1) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        binding.rvCricketTabs.scrollToPosition(position - 1)
                    }, 200)
                } else {
                    Handler(Looper.getMainLooper()).postDelayed({
                        binding.rvCricketTabs.scrollToPosition(position)
                    }, 200)
                }
            }
            tabsAdapter?.onTabCanged(position)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun notifyListener(card: Item) {
        if (card.pwaLink.contains("filename")) {
            for (tab in Constants.pwaTabListeners.values) {
                tab.onTabClicked(Constants.findFilename(card.pwaLink))
            }
        }
    }

    private fun checkMatch(pwaLink: String, url: String): Boolean {
        return if (url.contains("filename")) {
            val pwaUri = Uri.parse(pwaLink)
            val uri = Uri.parse(url)
            pwaUri.getQueryParameter("filename") == uri.getQueryParameter("filename")
        } else false
    }

    override fun onBackPressed() {
        try {
            val currentPos = binding.vpCricketFeed.currentItem
            if (fragmentList[currentPos] is PagerFragment) {
                super.onBackPressed()
            } else {
                (fragmentList[currentPos] as CricketPWAFragment).onBackPressed()
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}
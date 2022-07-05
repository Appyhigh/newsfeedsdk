package com.appyhigh.newsfeedsdk.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import java.util.*

class NewsFeedSliderAdapter(
    fragmentActivity: FragmentActivity,
    private var fragmentList: ArrayList<Fragment>
) :
    FragmentStateAdapter(fragmentActivity) {
    override fun createFragment(position: Int): Fragment {
        return fragmentList[position]
    }

    override fun getItemCount(): Int {
        return fragmentList.size
    }
}

package com.appyhigh.newsfeedsdk.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class PersonalizeAdapter(activity: FragmentActivity, private val itemsCount: Int,
                         private var fragmentList:ArrayList<Fragment>) :
    FragmentStateAdapter(activity) {


    override fun getItemCount(): Int {
        return itemsCount
    }

    override fun createFragment(position: Int): Fragment {
        return fragmentList.get(position)
    }
}
package com.appyhigh.newsfeedsdk.fragment

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.adapter.PersonalizeAdapter
import com.appyhigh.newsfeedsdk.apicalls.ApiGetInterests
import com.appyhigh.newsfeedsdk.apicalls.ApiUserDetails
import com.appyhigh.newsfeedsdk.callbacks.PersonalizeCallback
import com.appyhigh.newsfeedsdk.model.Interest
import com.appyhigh.newsfeedsdk.model.InterestResponseModel
import com.appyhigh.newsfeedsdk.model.Language
import com.appyhigh.newsfeedsdk.model.UserResponse
import com.appyhigh.newsfeedsdk.model.feeds.Card
import com.appyhigh.newsfeedsdk.utils.SpUtil
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class PersonaliseBottomSheetFragment : BottomSheetDialogFragment() {

    companion object {
        fun newInstance(personalizeCallback: PersonalizeCallback?): PersonaliseBottomSheetFragment {
            val personaliseBottomSheetFragment = PersonaliseBottomSheetFragment()
            personaliseBottomSheetFragment.personalizeCallback = personalizeCallback
            return personaliseBottomSheetFragment
        }
    }

    private var selectedInterests = ArrayList<Interest>()
    private var selectedLanguages = ArrayList<Language>()
    private var pinnedInterests = ArrayList<Interest>()
    var personalizeCallback: PersonalizeCallback? = null
    var infoGiven = false

    override fun getTheme(): Int {
        return R.style.AppBottomSheetDialogTheme
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(
            R.layout.bottom_sheet_personalise, container,
            false
        )
        Card.setFontFamily(view?.findViewById(R.id.title), true)
        try {
            val vpPersonalize: ViewPager2 = view.findViewById(R.id.vpPersonalize)
            val fragmentList = ArrayList<Fragment>()
            val personalizeAdapter = PersonalizeAdapter(requireActivity(), 2, fragmentList)
            fragmentList.add(
                PersonalizeFragment.newInstance(
                    0,
                    personalizeCallback!!,
                    object : SavePersonalization {
                        override fun onSave(pos: Int) {
                            checkSelection(fragmentList, vpPersonalize)
                        }
                    })
            )

            fragmentList.add(
                PersonalizeFragment.newInstance(
                    1,
                    personalizeCallback!!,
                    object : SavePersonalization {
                        override fun onSave(pos: Int) {
                            checkSelection(fragmentList, vpPersonalize)
                        }
                    })
            )
            vpPersonalize.adapter = personalizeAdapter
            val tabLayout: TabLayout = view.findViewById(R.id.tab_layout)
            TabLayoutMediator(
                tabLayout, vpPersonalize
            ) { tab: TabLayout.Tab, position: Int ->
                if (position == 0) {
                    tab.text = "Select Interest"
                } else {
                    tab.text = "Select Language"
                }
            }.attach()
            vpPersonalize.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                }

                override fun onPageScrolled(
                    position: Int,
                    positionOffset: Float,
                    positionOffsetPixels: Int
                ) {
                    super.onPageScrolled(position, positionOffset, positionOffsetPixels)

                }
            })

        } catch (e: Exception) {
            dismiss()
        }
        return view
    }

    private fun checkSelection(fragmentList: ArrayList<Fragment>, vpPersonalize: ViewPager2) {
        selectedInterests = (fragmentList[0] as PersonalizeFragment).saveInterests()!!
        pinnedInterests = (fragmentList[0] as PersonalizeFragment).savePinnedInterests()!!
        selectedLanguages = (fragmentList[1] as PersonalizeFragment).saveLanguages()
        if (selectedInterests.isNotEmpty() && selectedLanguages.isNotEmpty()) {
            infoGiven = true
            dismiss()
            pinnedInterests.forEach {
                if(!selectedInterests.contains(it)){
                    selectedInterests.add(it)
                }
                selectedInterests[selectedInterests.indexOf(it)].isPinned = true
            }
            personalizeCallback?.onPersonalize(selectedInterests, selectedLanguages)
            var interests =""
            var languages=""
            for(interest in selectedInterests){
                interests += interest.keyId+","
            }
            for(lang in FeedSdk.languagesList){
                languages+=lang.language+","
            }
            SpUtil.eventsListener?.onPersonalizePopup(true, interests, languages)
        } else if (selectedInterests.isNotEmpty() && selectedLanguages.isEmpty()) {
            vpPersonalize.currentItem = 1
        } else {
            vpPersonalize.currentItem = 0
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(requireContext(), theme).apply {
            behavior.peekHeight = requireActivity().getScreenHeight()
        }
    }

    interface SavePersonalization {
        fun onSave(pos: Int)
    }

    fun Activity.getScreenHeight(): Int {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.heightPixels
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if(!infoGiven)
            SpUtil.eventsListener?.onPersonalizePopup(false, "NA", "NA")
    }
}
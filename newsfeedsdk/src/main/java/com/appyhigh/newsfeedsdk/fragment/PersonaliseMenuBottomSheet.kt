package com.appyhigh.newsfeedsdk.fragment

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.activity.FeedLanguageActivity
import com.appyhigh.newsfeedsdk.activity.PublisherBlockActivity
import com.appyhigh.newsfeedsdk.databinding.FragmentPersonaliseMenuBinding
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.model.feeds.Card
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PersonaliseMenuBottomSheet: BottomSheetDialogFragment() {

    lateinit var binding: FragmentPersonaliseMenuBinding

    override fun getTheme(): Int {
        return R.style.AppBottomSheetDialogTheme
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPersonaliseMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Card.setFontFamily(binding.interestTitle)
        Card.setFontFamily(binding.feedLanguageTitle)
        Card.setFontFamily(binding.blockPublisherTitle)
        binding.llInterest.setOnClickListener {
            try{
                val bottomSheet = AddInterestBottomSheet.newInstance()
                val fm = getFragmentManager(requireContext())
                bottomSheet.show(fm!!,"AddInterestBottomSheet")
            } catch (ex:Exception){
                LogDetail.LogEStack(ex)
            }
            dismiss()
        }
        binding.llFeedLanguage.setOnClickListener {
            val intent = Intent(context, FeedLanguageActivity::class.java)
            requireContext().startActivity(intent)
            dismiss()
        }
        binding.llBlockPublisher.setOnClickListener {
            val intent = Intent(context, PublisherBlockActivity::class.java)
            requireContext().startActivity(intent)
            dismiss()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(requireContext(), theme).apply {
            behavior.peekHeight = requireActivity().getScreenHeight()
        }
    }

    private fun getFragmentManager(context: Context?): FragmentManager? {
        return when (context) {
            is FragmentActivity -> context.supportFragmentManager
            is ContextThemeWrapper -> getFragmentManager(context.baseContext)
            else -> null
        }
    }


    private fun Activity.getScreenHeight(): Int {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            val displayMetrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            displayMetrics.heightPixels
        } else{
            val outMetrics = windowManager.currentWindowMetrics
            val bounds = outMetrics.bounds
            bounds.height()
        }
    }
}
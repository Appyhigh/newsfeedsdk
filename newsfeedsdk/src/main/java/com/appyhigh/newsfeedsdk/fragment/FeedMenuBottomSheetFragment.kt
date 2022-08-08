package com.appyhigh.newsfeedsdk.fragment

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.DisplayMetrics
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.LinearLayout
import android.widget.Toast
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.activity.ContactPublisherActivity
import com.appyhigh.newsfeedsdk.apicalls.ApiCreateOrUpdateUser
import com.appyhigh.newsfeedsdk.model.feeds.Card
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.net.URL
import java.util.regex.Pattern

class FeedMenuBottomSheetFragment :
    BottomSheetDialogFragment() {

    private var contactUs: String = ""
    private var postId: String = ""
    private var publisherId: String = ""

    companion object {
        fun newInstance(contactUs: String, publisherId: String, postId: String): FeedMenuBottomSheetFragment {
            val feedMenuBottomSheetFragment = FeedMenuBottomSheetFragment()
            feedMenuBottomSheetFragment.contactUs = contactUs
            feedMenuBottomSheetFragment.publisherId = publisherId
            feedMenuBottomSheetFragment.postId = postId
            return feedMenuBottomSheetFragment
        }
    }


    override fun getTheme(): Int {
        return R.style.AppBottomSheetDialogTheme
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(
            R.layout.bottom_sheet_feed_menu, container,
            false
        )
        Card.setFontFamily(view?.findViewById(R.id.contactPublisherTitle))
        Card.setFontFamily(view?.findViewById(R.id.blockPublisherTitle))
        Card.setFontFamily(view?.findViewById(R.id.reportPostTitle))
        // get the views and attach the listener
        val llContactPublisher = view.findViewById<LinearLayout>(R.id.llContactPublisher)
        val llBlockPublisher = view.findViewById<LinearLayout>(R.id.llBlockPublisher)
        val llReportPost = view.findViewById<LinearLayout>(R.id.llReportPost)

        if (postId == "") {
            llReportPost.visibility = View.GONE
        } else {
            llReportPost.visibility = View.VISIBLE
        }

        llReportPost.setOnClickListener {
            val reportBottomSheet = ReportBottomSheetFragment.newInstance(postId, object : ReportPost {
                override fun onReportPost() {
                    dismiss()
                }
            })
            reportBottomSheet.show(
                requireActivity().supportFragmentManager,
                "repostBotttomsheet"
            )
        }

        llBlockPublisher.setOnClickListener {
            ApiCreateOrUpdateUser().updateBlockPublisher(publisherId, "block")
            Toast.makeText(requireContext(), "We won't show posts from this publisher again", Toast.LENGTH_SHORT).show()
            dialog?.dismiss()
        }

        // Verify Url before giving the option to click
        if (checkURL(contactUs)){
            llContactPublisher.visibility = View.VISIBLE
        }else{
            llContactPublisher.visibility = View.GONE
        }
        llContactPublisher.setOnClickListener {
            dismiss()
            if (contactUs.isNotEmpty()) {
                val intent = Intent(requireActivity(), ContactPublisherActivity::class.java)
                intent.putExtra("url", contactUs)
                startActivity(intent)
            }
        }

        return view
    }

    private fun checkURL(input: CharSequence): Boolean {
        if (TextUtils.isEmpty(input)) {
            return false
        }
        val URL_PATTERN: Pattern = Patterns.WEB_URL
        var isURL: Boolean = URL_PATTERN.matcher(input).matches()
        if (!isURL) {
            val urlString = input.toString() + ""
            if (URLUtil.isNetworkUrl(urlString)) {
                try {
                    URL(urlString)
                    isURL = true
                } catch (e: Exception) {
                }
            }
        }
        return isURL
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(requireContext(), theme).apply {
            behavior.peekHeight = requireActivity().getScreenHeight()
        }
    }


    private fun Activity.getScreenHeight(): Int {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.heightPixels
    }

    interface ReportPost {
        fun onReportPost()
    }
}
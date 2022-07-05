package com.appyhigh.newsfeedsdk.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.adapter.FeedReportIssueAdapter
import com.appyhigh.newsfeedsdk.adapter.FeedReportIssueListener
import com.appyhigh.newsfeedsdk.adapter.FeedReportIssueModel
import com.appyhigh.newsfeedsdk.apicalls.ApiReportIssueListener
import com.appyhigh.newsfeedsdk.apicalls.ApiReportIssues
import com.appyhigh.newsfeedsdk.apicalls.ReportIssueModel
import com.appyhigh.newsfeedsdk.apiclient.Endpoints
import com.appyhigh.newsfeedsdk.databinding.LayoutReportIssueDialogBinding
import com.appyhigh.newsfeedsdk.model.feeds.Item

class ReportIssueDialogFragment: DialogFragment() {

    var postId = ""
    var pageNo = 0
    var item : Item?=null
    var type: String=""
    lateinit var binding: LayoutReportIssueDialogBinding
    var issueSelected:String?=null
    var additionalComments:String?=null

    companion object {
        fun newInstance(
            item: Item,
            pageNo: Int,
            type: String
        ): ReportIssueDialogFragment {
            val reportIssueDialogFragment = ReportIssueDialogFragment()
            reportIssueDialogFragment.item = item
            reportIssueDialogFragment.pageNo = pageNo
            reportIssueDialogFragment.type = type
            reportIssueDialogFragment.postId = item.postId!!
            return reportIssueDialogFragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = LayoutReportIssueDialogBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onResume() {
        // Get existing layout params for the window
        val params: ViewGroup.LayoutParams = dialog!!.window!!.attributes
        params.width = WindowManager.LayoutParams.MATCH_PARENT
        params.height = WindowManager.LayoutParams.WRAP_CONTENT
        dialog!!.window!!.attributes = params as WindowManager.LayoutParams
        // Call super onResume after sizing
        super.onResume()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val reportIssueList = ArrayList<FeedReportIssueModel>()
        if(type=="podcasts"){
            reportIssueList.add(FeedReportIssueModel("Not relevant", false))
            reportIssueList.add(FeedReportIssueModel("Not suitable/ NFSW", false))
            reportIssueList.add(FeedReportIssueModel("Feature image/ title/ logo missing", false))
            reportIssueList.add(FeedReportIssueModel("Podcast category mismatch", false))
            reportIssueList.add(FeedReportIssueModel("Podcast not playing", false))
            reportIssueList.add(FeedReportIssueModel("Podcast Wrong language mapping", false))
        } else {
            reportIssueList.add(FeedReportIssueModel("Not relevant", false))
            reportIssueList.add(FeedReportIssueModel("Not suitable/ NFSW", false))
            reportIssueList.add(FeedReportIssueModel("Web page unoptimised / too many ads", false))
            reportIssueList.add(FeedReportIssueModel("Wrong category mapping", false))
            reportIssueList.add(FeedReportIssueModel("Feature image/ title/ logo missing", false))
            reportIssueList.add(FeedReportIssueModel("Video does not play", false))
            reportIssueList.add(FeedReportIssueModel("Post link does not open", false))
            reportIssueList.add(FeedReportIssueModel("Very old post", false))
            reportIssueList.add(FeedReportIssueModel("Wrong language post in feed", false))
            reportIssueList.add(FeedReportIssueModel("Consecutive Posts", false))
        }
        binding.rvReports.adapter = FeedReportIssueAdapter(reportIssueList, object : FeedReportIssueListener{
            override fun onIssueClicked(issue: String?) {
                issueSelected = issue
            }

        })
        binding.close.setOnClickListener { dismiss() }
        binding.submit.setOnClickListener {
            additionalComments = binding.additional.text.toString()
            if(additionalComments!!.isBlank()){
                additionalComments = null
            }
            if(issueSelected==null && additionalComments==null){
                Toast.makeText(this.requireContext(), "Please select any one issue or fill the Other!", Toast.LENGTH_SHORT).show()
            } else{
                if(issueSelected==null){
                    issueSelected = "Others"
                }
                var interestsString = ""
                for(i in item!!.interests!!.indices){
                    interestsString += if(i==0) {
                        item!!.interests!![i]
                    } else{
                        ","+item!!.interests!![i]
                    }
                }
                FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let { it1 ->
                    ApiReportIssues().reportIssuesEncrypted(
                        Endpoints.REPORT_ISSUES_ENCRYPTED,
                        it1,
                        ReportIssueModel(postId, interestsString, item!!.feedType, pageNo, item!!.languageString, item!!.postSource, issueSelected, additionalComments),
                        object : ApiReportIssueListener {
                            override fun onSuccess() {
                                dismiss()
                            }

                        })
                }
            }
        }
    }


}
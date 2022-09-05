package com.appyhigh.newsfeedsdk.fragment

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.apicalls.ApiConfig
import com.appyhigh.newsfeedsdk.apicalls.ApiReportPost
import com.appyhigh.newsfeedsdk.apiclient.Endpoints
import com.appyhigh.newsfeedsdk.model.feeds.Card
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar

class ReportBottomSheetFragment : BottomSheetDialogFragment() {
    var reportString = ""
    var postId = ""
    var radioButton: RadioButton?=null
    private var reportPost: FeedMenuBottomSheetFragment.ReportPost? = null

    companion object {
        fun newInstance(
            postId: String,
            reportPost: FeedMenuBottomSheetFragment.ReportPost
        ): ReportBottomSheetFragment {
            val reportBottomSheetFragment = ReportBottomSheetFragment()
            reportBottomSheetFragment.postId = postId
            reportBottomSheetFragment.reportPost = reportPost
            return reportBottomSheetFragment
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
            R.layout.bottom_sheet_report, container,
            false
        )
        setFonts(view)
        val btnSave = view.findViewById<AppCompatButton>(R.id.btnSave)
        val goBack = view.findViewById<AppCompatTextView>(R.id.tvGoBack)
        val rgReport = view.findViewById<RadioGroup>(R.id.rgReport)
        val pbSaving = view.findViewById<ProgressBar>(R.id.pbSaving)
        setValues(rgReport)
        goBack.setOnClickListener {
            dismiss()
        }
        btnSave?.setOnClickListener {
            if (reportString.isNotEmpty()) {
                ApiReportPost().reportPostEncrypted(
                    Endpoints.REPORT_POST_ENCRYPTED,
                    postId,
                    reportString,
                    object : ApiReportPost.ReportPostResponseListener {
                        override fun onSuccess() {
                            dismiss()
                            reportPost?.onReportPost()
                            Snackbar.make(btnSave, "Post Reported!", Snackbar.LENGTH_SHORT).show()
                        }

                        override fun onFailure() {
                            dismiss()
                            reportPost?.onReportPost()
                            Snackbar.make(
                                btnSave,
                                "Something went wrong,Please Try again later!",
                                Snackbar.LENGTH_SHORT
                            ).show()
                        }
                    })
            } else {
                Toast.makeText(
                    requireActivity(),
                    "Please Choose one of the above options!",
                    Toast.LENGTH_SHORT
                ).show()
            }

        }
        return view
    }

    private fun setValues(rgReport: RadioGroup){
        val reports = ApiConfig().getConfigModel(requireContext()).reportPostsText
        for(i in reports.indices){
            val itemView = LayoutInflater.from(requireContext()).inflate(R.layout.item_report_bottom_sheet, null)
            val rbValue = itemView.findViewById<RadioButton>(R.id.rbValue)
            rbValue.id = View.generateViewId()
            rbValue.text = reports[i]
            rbValue.setOnCheckedChangeListener { buttonView, isChecked ->
                if(isChecked) {
                    reportString = reports[i]
                    radioButton?.isChecked = false
                    radioButton = buttonView as RadioButton?
                }
            }
            Constants.setFontFamily(rbValue)
            if(i==reports.size-1){
                itemView.findViewById<View>(R.id.vLine).visibility = View.GONE
            }
            rgReport.addView(itemView)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(requireContext(), theme).apply {
            behavior.peekHeight = requireActivity().getScreenHeight()
        }
    }


    fun Activity.getScreenHeight(): Int {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.heightPixels
    }

    private fun setFonts(view: View?){
        Card.setFontFamily(view?.findViewById(R.id.btnSave) as Button, true)
        Card.setFontFamily(view?.findViewById(R.id.tvGoBack) as TextView)
    }
}
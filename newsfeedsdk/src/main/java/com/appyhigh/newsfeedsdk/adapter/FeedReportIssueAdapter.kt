package com.appyhigh.newsfeedsdk.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.callbacks.LanguageClickListener
import com.appyhigh.newsfeedsdk.databinding.ItemFeedReportIssueBinding

class FeedReportIssueAdapter(
    var reportIssueList: ArrayList<FeedReportIssueModel>,
    var listener: FeedReportIssueListener) : RecyclerView.Adapter<FeedReportIssueAdapter.FeedReportIssueViewHolder>(){

    inner class FeedReportIssueViewHolder(val view: ItemFeedReportIssueBinding) :
        RecyclerView.ViewHolder(view.root)

    var selectedIssuePosition = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedReportIssueViewHolder {
        return FeedReportIssueViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.item_feed_report_issue,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: FeedReportIssueViewHolder, position: Int) {
        val reportIssue = reportIssueList[position]
        holder.view.title.text = reportIssue.title
        if(reportIssue.isSelected){
            selectedIssuePosition = position
            holder.view.icon.setImageResource(R.drawable.ic_report_issue_selected)
        } else{
            holder.view.icon.setImageResource(R.drawable.ic_report_issue_not_selected)
        }
        holder.itemView.setOnClickListener {
            if(!reportIssue.isSelected){
                reportIssueList[selectedIssuePosition].isSelected = false
                notifyItemChanged(selectedIssuePosition)
                selectedIssuePosition = position
                reportIssue.isSelected = true
                holder.view.icon.setImageResource(R.drawable.ic_report_issue_selected)
                listener.onIssueClicked(reportIssue.title)
            } else{
                reportIssue.isSelected = false
                holder.view.icon.setImageResource(R.drawable.ic_report_issue_not_selected)
                listener.onIssueClicked(null)
            }
        }
    }

    override fun getItemCount(): Int {
        return reportIssueList.size
    }
}

interface FeedReportIssueListener{
    fun onIssueClicked(issue: String?)
}

data class FeedReportIssueModel(
    var title:String = "",
    var isSelected: Boolean = false
)
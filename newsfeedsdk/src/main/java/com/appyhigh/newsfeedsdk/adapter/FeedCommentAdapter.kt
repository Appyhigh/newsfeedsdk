package com.appyhigh.newsfeedsdk.adapter

import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.model.FeedComment
import com.appyhigh.newsfeedsdk.model.feeds.Card
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class FeedCommentAdapter(private val comments: ArrayList<FeedComment>, private var cardType: String) :
    RecyclerView.Adapter<FeedCommentAdapter.CommentViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        if(cardType=="native"){
            return CommentViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_native_comment, parent, false)
            )
        } else {
            return CommentViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[holder.absoluteAdapterPosition]
        Card.setFontFamily(holder.tvUserName)
        Card.setFontFamily(holder.tv_comment_text)
        Card.setFontFamily(holder.tv_comment_time)
        holder.tvUserName.text = comment.getUserName()
        holder.tv_comment_text.text = comment.commentDetails?.commentValue
        holder.tv_comment_time.text = comment.commentedAt?.getTimeInAgoFormat()
        Glide.with(holder.itemView.context)
            .load(comment.userImage)
            .into(holder.iv_user)
    }

    override fun getItemCount(): Int {
        return comments.size
    }

    class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        val tv_comment_text: TextView = itemView.findViewById(R.id.tv_comment_text)
        val tv_comment_time: TextView = itemView.findViewById(R.id.tv_comment_time)
        val iv_user: ImageView = itemView.findViewById(R.id.iv_user)
    }
}

fun String.getTimeInAgoFormat(): String {
    val DATE_TIME_SERVER = "yyyy-MM-dd HH:mm:ss"
    val DATE_TIME_1 = "dd-MMM-yyy hh:mm a"

    val sdfT = DATE_TIME_SERVER.getCurrentTimeZoneFormat()
    val cal = Calendar.getInstance()
    val tz = cal.timeZone

    /* date formatter in local timezone */
    val sdf = DATE_TIME_1.getCurrentTimeZoneFormat()
    sdf.timeZone = tz

    /* print your timestamp and double check it's the date you expect */
    try {
        if (this.isNotEmpty()) cal.time = sdfT.parse(this)
    } catch (e: Exception) {
        // e.printStackTrace();
    }

    val timestamp = cal.timeInMillis
    val localTime = sdf.format(Date(timestamp))
    Log.d("Time: ", localTime)

    Log.d("Server time: ", timestamp.toString() + "")

    /* log the device timezone */
    Log.d("Time zone: ", tz.displayName)

    /* log the system time */
    Log.d("System time: ", System.currentTimeMillis().toString() + "")

    val relTime = DateUtils.getRelativeTimeSpanString(
        timestamp,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS
    )

    return relTime.toString()
}

//String is the format we want the output to be in
private fun String.getCurrentTimeZoneFormat(): SimpleDateFormat {
    val sdf = SimpleDateFormat(this, Locale.getDefault())

    val currentDate = Date()
    val tz = Calendar.getInstance().timeZone

    // String name1 = tz.getDisplayName(tz.inDaylightTime(currentDate), TimeZone.SHORT);
    val name =
        TimeZone.getDefault().getDisplayName(tz.inDaylightTime(currentDate), TimeZone.SHORT)
    sdf.timeZone = TimeZone.getTimeZone("\"" + name + "\"")
    // Log.d("current time zone", sdf.getTimeZone().getDisplayName() + "::" + TimeZone.getDefault().getDisplayName() + ": " + TimeZone.getDefault().getID());

    return sdf
}

package com.appyhigh.newsfeedsdk.adapter

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.callbacks.GlideCallbackListener
import com.appyhigh.newsfeedsdk.callbacks.OnRelatedPostClickListener
import com.appyhigh.newsfeedsdk.databinding.ItemRelatedNativePostBinding
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.model.PostDetailsModel
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class FeedNextPostAdapter(private var posts:ArrayList<PostDetailsModel.NextPost>, private var onRelatedPostClickListener: OnRelatedPostClickListener):
    RecyclerView.Adapter<FeedNextPostAdapter.NextPostViewHolder>() {

    class NextPostViewHolder(val view: ItemRelatedNativePostBinding) : RecyclerView.ViewHolder(view.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NextPostViewHolder {
        return NextPostViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.item_related_native_post,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: NextPostViewHolder, position: Int) {
        try{
            val post = posts[position]
            if(!post.content.title.isNullOrEmpty()){
                holder.view.description.text = HtmlCompat.fromHtml(post.content.title, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()

            } else if(!post.content.caption.isNullOrEmpty()){
                holder.view.description.text = HtmlCompat.fromHtml(post.content.caption, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
            } else{
                var newDescription = HtmlCompat.fromHtml( HtmlCompat.fromHtml(post.content.description, HtmlCompat.FROM_HTML_MODE_LEGACY).toString(), HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
                newDescription = newDescription.replace("\n\n","\n").replace((65532).toChar(), (32).toChar())
                holder.view.description.text = newDescription
            }
            Constants.loadImageFromGlide(holder.itemView.context, post.content.media_list[0], holder.view.image, object : GlideCallbackListener {
                override fun onSuccess(drawable: Drawable?) {
                    try{
                        holder.view.image.setImageDrawable(drawable)
                    } catch (ex:Exception){
                        LogDetail.LogEStack(ex)
                    }
                }

                override fun onFailure() {
                    Picasso.get()
                        .load(post.content.media_list[0])
                        .noFade()
                        .into(holder.view.image, object : Callback {
                            override fun onSuccess() {}
                            override fun onError(e: java.lang.Exception?) {
                                holder.view.image.visibility = View.GONE
                            }
                        })
                }
            })
            val url = if(post.content.url.isNullOrEmpty()) post.content.video_url else post.content.url
            holder.view.share.setOnClickListener {
                onRelatedPostClickListener.onSharePost(post.post_id, post.content.caption, post.content.media_list[0], false, url)
            }
            holder.view.whatsappShare.setOnClickListener {
                onRelatedPostClickListener.onSharePost(post.post_id, post.content.caption, post.content.media_list[0], true, url)
            }
            holder.itemView.setOnClickListener {
                onRelatedPostClickListener.onPostClick(post)
            }
            holder.view.publisherPostedTime.text = getTime(post.publishedOn)
        } catch (ex:Exception){
            LogDetail.LogEStack(ex)
        }
    }

    override fun getItemCount(): Int {
        return posts.size
    }

    @Throws(ParseException::class)
    private fun getTime(dateTime: String): String {
        @SuppressLint("SimpleDateFormat") val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val publishedDate = dateFormat.parse(dateTime)
        val now = Date()
        val secondsInMilli: Long = 1000
        val minutesInMilli = secondsInMilli * 60
        val hoursInMilli = minutesInMilli * 60
        val daysInMilli = hoursInMilli * 24
        var difference = now.time - publishedDate.time
        val elapsedDays = difference / daysInMilli
        difference %= daysInMilli
        val elapsedHours = difference / hoursInMilli
        difference %= hoursInMilli
        val elapsedMinutes = difference / minutesInMilli
        difference %= minutesInMilli
        val elapsedSeconds = difference / secondsInMilli
        return when {
            elapsedDays != 0L -> {
                "$elapsedDays days ago"
            }
            elapsedHours != 0L -> {
                "$elapsedHours hours ago"
            }
            elapsedMinutes != 0L -> {
                "$elapsedMinutes minutes ago"
            }
            else -> {
                "$elapsedSeconds seconds ago"
            }
        }
    }
}
package com.appyhigh.newsfeedsdk.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.apicalls.ApiSearchSticky
import com.appyhigh.newsfeedsdk.model.SearchStickyWidgetModel

class WebPlatformsGridAdapter(
    var context: Context, var webPlatformsModel: SearchStickyWidgetModel, var webPlatformListener: WebPlatformListener
    ) : BaseAdapter() {

    var TAG = "WebPlatformsAdapter"

    override fun getCount(): Int {
        return webPlatformsModel.icons.size
    }

    override fun getItem(position: Int): Any {
        return webPlatformsModel.icons[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    @SuppressLint("ViewHolder")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.item_web_platform, null)
        val webPlatform = webPlatformsModel.icons[position]
        val title: TextView = view.findViewById(R.id.title)
        val icon: ImageView = view.findViewById(R.id.icon)
        title.text = webPlatform
        icon.setImageResource(getWebPlatformIcon(webPlatform))
        view.setOnClickListener {
            webPlatformListener.onWebPlatformClicked(webPlatform)
            ApiSearchSticky().userActionSearch(webPlatform)
        }
        return view
    }


    fun getWebPlatformIcon(webPlatform: String): Int{
        return when(webPlatform){
            "Facebook" -> R.drawable.ic_fb_logo
            "Youtube" -> R.drawable.ic_youtube_logo
            "Gmail" -> R.drawable.ic_gmail_logo
            "Twitter" -> R.drawable.ic_twitter_logo
            "Instagram" -> R.drawable.ic_instagram
            "LinkedIn" -> R.drawable.ic_linked_in_logo
            "Snapchat" -> R.drawable.ic_snapchat_logo
            "Reddit" -> R.drawable.ic_reddit_logo
            else -> R.drawable.ic_google
        }
    }
}

interface WebPlatformListener{
   fun onWebPlatformClicked(webPlatform: String)
}
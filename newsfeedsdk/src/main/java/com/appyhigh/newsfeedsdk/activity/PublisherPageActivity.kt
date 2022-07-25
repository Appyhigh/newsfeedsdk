package com.appyhigh.newsfeedsdk.activity

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import coil.ImageLoader
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.Constants.FULL_NAME
import com.appyhigh.newsfeedsdk.Constants.IS_FOLLOWING_PUBLISHER
import com.appyhigh.newsfeedsdk.Constants.POSITION
import com.appyhigh.newsfeedsdk.Constants.PROFILE_PIC
import com.appyhigh.newsfeedsdk.Constants.PUBLISHER_CONTACT
import com.appyhigh.newsfeedsdk.Constants.PUBLISHER_ID
import com.appyhigh.newsfeedsdk.Constants.cardsMap
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.adapter.NewsFeedAdapter
import com.appyhigh.newsfeedsdk.apicalls.ApiConfig
import com.appyhigh.newsfeedsdk.apicalls.ApiFollowPublihser
import com.appyhigh.newsfeedsdk.apicalls.ApiGetPublisherPosts
import com.appyhigh.newsfeedsdk.apicalls.ApiPostImpression
import com.appyhigh.newsfeedsdk.apiclient.Endpoints
import com.appyhigh.newsfeedsdk.callbacks.GlideCallbackListener
import com.appyhigh.newsfeedsdk.callbacks.PostImpressionListener
import com.appyhigh.newsfeedsdk.databinding.ActivityPublisherPageBinding
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.fragment.FeedMenuBottomSheetFragment
import com.appyhigh.newsfeedsdk.model.PostImpressionsModel
import com.appyhigh.newsfeedsdk.model.PostView
import com.appyhigh.newsfeedsdk.model.feeds.Card
import com.appyhigh.newsfeedsdk.model.feeds.GetFeedsResponse
import com.appyhigh.newsfeedsdk.utils.showAdaptiveBanner
import com.google.gson.Gson
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import java.util.*
import kotlin.math.abs

class PublisherPageActivity : AppCompatActivity() {
    private var binding: ActivityPublisherPageBinding? = null
    private var fullName = ""
    private var profilePic = ""
    private var publisherId = ""
    private var position = 0
    private var isFollowingPublisher = false
    private var pageNo = 0
    private var publisherContactUs = ""
    private var layoutManager: LinearLayoutManager? = null
    private var newsFeedAdapter: NewsFeedAdapter? = null
    private var feedsResponseModel: GetFeedsResponse? = null
    private var presentUrl = ""
    private var presentTimeStamp:Long = 0
    var postImpressions = HashMap<String, PostView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPublisherPageBinding.inflate(layoutInflater)
        val view = binding?.root
        setContentView(view)
        setFonts(view)
        if(ApiConfig().checkShowAds(this) && Constants.checkFeedApp()){
            showAdaptiveBanner(this, Constants.getHomeBannerAd(), binding!!.bannerAd)
        }
        if (!intent.hasExtra(PUBLISHER_ID)) {
            finish()
        }

        fullName = intent.getStringExtra(FULL_NAME).toString()
        profilePic = intent.getStringExtra(PROFILE_PIC).toString()
        publisherId = intent.getStringExtra(PUBLISHER_ID).toString()
        isFollowingPublisher = intent.getBooleanExtra(IS_FOLLOWING_PUBLISHER, false)
        if(intent.hasExtra(PUBLISHER_CONTACT) && intent.getStringExtra(PUBLISHER_CONTACT)!=null){
            publisherContactUs = intent.getStringExtra(PUBLISHER_CONTACT)!!
        }
        position = intent.getIntExtra(POSITION, -1)

        binding?.profileOnlyLayout!!.visibility = View.VISIBLE
        binding?.profileLayout!!.visibility = View.GONE
        binding?.profileOnlyLayoutProgressBar!!.visibility = View.GONE
        binding?.profileLayoutProgressBar!!.visibility = View.GONE
        binding?.noPosts!!.visibility = View.GONE

        FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let {
            ApiGetPublisherPosts().getPublisherPostsEncrypted(
                Endpoints.GET_PUBLISHER_POSTS_ENCRYPTED,
                it,
                FeedSdk.userId,
                pageNo,
                publisherId,
                object : ApiGetPublisherPosts.PublisherPostsResponseListener {
                    override fun onSuccess(feedsResponse: GetFeedsResponse, url: String, timeStamp: Long) {
                        storeData(presentUrl, presentTimeStamp)
                        presentTimeStamp = timeStamp
                        presentUrl = url
                        feedsResponseModel = feedsResponse

                        if (feedsResponseModel?.cards!!.isEmpty()) {
                            binding?.publishLayout!!.visibility = View.VISIBLE
                            binding?.noPosts!!.visibility = View.VISIBLE
                            binding?.publishProgress!!.visibility = View.GONE
                        } else {
                            layoutManager = LinearLayoutManager(this@PublisherPageActivity)
                            binding?.publishPageRecycler!!.layoutManager = layoutManager
                            newsFeedAdapter =
                                NewsFeedAdapter(
                                    feedsResponseModel?.cards as ArrayList<Card>,
                                    null,
                                    "publishPage",
                                    null,
                                    object : PostImpressionListener {
                                        override fun addImpression(card: Card, totalDuration: Int?, watchedDuration: Int?) {
                                            try {
                                                val postView = PostView(
                                                    FeedSdk.sdkCountryCode ?: "in",
                                                    "explore_publisher",
                                                    card.items[0].isVideo,
                                                    card.items[0].languageString,
                                                    Constants.getInterestsString(card.items[0].interests),
                                                    card.items[0].postId,
                                                    card.items[0].postSource,
                                                    card.items[0].publisherId,
                                                    card.items[0].shortVideo,
                                                    card.items[0].source,
                                                    totalDuration,
                                                    watchedDuration
                                                )
                                                postImpressions.put(card.items[0].postId!!,postView)
                                            } catch (ex:java.lang.Exception){
                                                LogDetail.LogEStack(ex)
                                            }
                                        }
                                    })
                            binding?.publishPageRecycler!!.adapter = newsFeedAdapter
                            binding?.publishProgress!!.visibility = View.GONE
                            binding?.followers!!.text =
                                reformatFollowers((feedsResponseModel?.followers!!))
                            binding?.profileFollowers!!.text =
                                (feedsResponseModel?.followers!!).toString()
                            binding?.publishProgress!!.visibility = View.GONE
                            binding?.publishLayout!!.visibility = View.VISIBLE
                            cardsMap["publishPage"] = feedsResponse.cards as ArrayList<Card>
                        }
                    }
                }
            )
        }

        binding?.backBtn?.setOnClickListener {
            onBackPressed()
        }
        binding?.profileBackBtn?.setOnClickListener {
            onBackPressed()
        }

        binding?.moreBtn?.setOnClickListener {
            val reportBottomSheet = FeedMenuBottomSheetFragment.newInstance(publisherContactUs, "")
            reportBottomSheet.show(supportFragmentManager, "reportBottomSheet")
        }

        binding?.profileMoreBtn?.setOnClickListener {
            val reportBottomSheet = FeedMenuBottomSheetFragment.newInstance(publisherContactUs, "")
            reportBottomSheet.show(supportFragmentManager, "reportBottomSheet")
        }

        if (isFollowingPublisher) {
//            CricketFeedFragment.isFollowed = true
//            CricketFeedFragment.position = position
            changeStyle(true)
        } else {
//            CricketFeedFragment.isFollowed = false
//            CricketFeedFragment.position = position
            changeStyle(false)
        }

        binding?.followBtn?.setOnClickListener {
            try {
                if (intent.hasExtra(Constants.SCREEN_TYPE)) {
                    FeedSdk.areContentsModified[intent.getStringExtra(Constants.SCREEN_TYPE)!!] = true
                }
            } catch (ex:Exception){
                LogDetail.LogEStack(ex)
            }
//            binding?.followBtn!!.visibility = View.GONE
//            binding?.profileFollowBtn!!.visibility = View.GONE
//            binding?.profileLayoutProgressBar!!.visibility = View.VISIBLE
//            binding?.profileOnlyLayoutProgressBar!!.visibility = View.VISIBLE
            if (binding?.followBtn!!.text == "Follow") {
                changeStyle(true)
            } else {
                changeStyle(false)
            }
            try{
                for ((index, item) in cardsMap["explore"]!!.withIndex()) {
                    if (item.cardType == Constants.CardType.FEED_PUBLISHERS.toString().lowercase(Locale.getDefault())) {
                        for ((publisherIndex, publisher) in item.items.withIndex()) {
                            if (publisher.publisherId == publisherId) {
                                publisher.isFollowingPublisher = !publisher.isFollowingPublisher!!
                                cardsMap["explore"]!![index].items[publisherIndex].isFollowingPublisher =
                                    publisher.isFollowingPublisher
                                break
                            }
                            break
                        }
                    }
                }
            } catch (ex:Exception){
                LogDetail.LogEStack(ex)
            }
            FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let { it1 ->
                ApiFollowPublihser().followPublisherEncrypted(
                    Endpoints.FOLLOW_PUBLISHER_ENCRYPTED,
                    it1,
                    FeedSdk.userId, publisherId)
            }
        }

        binding?.publishProgress!!.visibility = View.VISIBLE
        binding?.publishLayout!!.visibility = View.GONE

        if(profilePic.contains(".svg")){
            val imageLoader = ImageLoader.Builder(this)
                .componentRegistry { add(SvgDecoder(this@PublisherPageActivity)) }
                .build()

            val request = ImageRequest.Builder(this)
                .crossfade(true)
                .crossfade(500)
                .placeholder(R.drawable.placeholder)
                .data(imageLoader)
                .target(
                    onSuccess = {
                        binding?.publishProfilePic!!.setImageDrawable(it)
                    },
                    onError = {
                        try {
                            binding?.publisherImage?.text = fullName.substring(0, 1).uppercase()
                        } catch (ex: java.lang.Exception) {
                            binding?.publisherImage!!.text = "N"
                        }
                        binding?.publisherImage!!.visibility = View.VISIBLE
                        binding?.publishProfilePic!!.visibility = View.GONE
                    }
                )
                .build()

            val request2 = ImageRequest.Builder(this)
                .crossfade(true)
                .crossfade(500)
                .placeholder(R.drawable.placeholder)
                .data(imageLoader)
                .target(
                    onSuccess = {
                        binding?.profilePic!!.setImageDrawable(it)
                    },
                    onError = {
                        try {
                            binding?.profilePublisherImage!!.text =
                                fullName.substring(0, 1).uppercase()
                        } catch (ex: java.lang.Exception) {
                            binding?.profilePublisherImage!!.text = "N"
                        }
                        binding?.profilePublisherImage!!.visibility = View.VISIBLE
                        binding?.publishProfilePic!!.visibility = View.GONE
                    }
                )
                .build()

            imageLoader.enqueue(request)
            imageLoader.enqueue(request2)
        } else{
            Constants.loadImageFromGlide(this, profilePic, binding?.publishProfilePic, object : GlideCallbackListener {
                override fun onSuccess(drawable: Drawable?) {
                    try{
                        binding?.publishProfilePic?.setImageDrawable(drawable)
                    } catch (ex:Exception){
                        LogDetail.LogEStack(ex)
                    }
                }

                override fun onFailure() {
                    Picasso.get()
                        .load(profilePic)
                        .noFade()
                        .into(binding?.publishProfilePic!!, object : Callback {
                            override fun onSuccess() {}
                            override fun onError(e: java.lang.Exception?) {
                                try {
                                    binding?.publisherImage?.text = fullName.substring(0, 1).uppercase()
                                } catch (ex: java.lang.Exception) {
                                    binding?.publisherImage!!.text = "N"
                                }
                                binding?.publisherImage!!.visibility = View.VISIBLE
                                binding?.publishProfilePic!!.visibility = View.GONE
                            }
                        })
                }

            })
            Constants.loadImageFromGlide(this, profilePic, binding?.profilePic, object : GlideCallbackListener{
                override fun onSuccess(drawable: Drawable?) {
                    try{
                        binding?.profilePic?.setImageDrawable(drawable)
                    } catch (ex:Exception){
                        LogDetail.LogEStack(ex)
                    }
                }

                override fun onFailure() {
                    Picasso.get()
                        .load(profilePic)
                        .noFade()
                        .into(binding?.profilePic!!, object : Callback {
                            override fun onSuccess() {}
                            override fun onError(e: java.lang.Exception?) {
                                try {
                                    binding?.profilePublisherImage!!.text =
                                        fullName.substring(0, 1).uppercase()
                                } catch (ex: java.lang.Exception) {
                                    binding?.profilePublisherImage!!.text = "N"
                                }

                                binding?.profilePublisherImage!!.visibility = View.VISIBLE
                                binding?.publishProfilePic!!.visibility = View.GONE
                            }
                        })
                }

            })
        }

        binding?.publishProfileName!!.text = fullName
        binding?.profileName!!.text = fullName


    }

    private fun reformatFollowers(number: Int): String {
        var numberString = ""
        numberString = when {
            abs(number / 1000000) >= 1 -> {
                (number / 1000000).toString().toString() + "M"
            }
            abs(number / 1000) > 1 -> {
                (number / 1000).toString().toString() + "K"
            }
            else -> {
                number.toString()
            }
        }
        return numberString
    }

    override fun onResume() {
        super.onResume()
        cardsMap["publishPage"]?.let { newsFeedAdapter?.refreshList(it) }
    }

    override fun onStop() {
        super.onStop()
        try {
            val startPos = layoutManager?.findFirstVisibleItemPosition()
            val endPos = layoutManager?.findLastVisibleItemPosition()

            for (pos in startPos!!..endPos!!) {
                val holder = binding?.publishPageRecycler?.findViewHolderForAdapterPosition(pos)
                if (holder is NewsFeedAdapter.VideoViewHolder) {
                    newsFeedAdapter?.pausePlayer(holder)
                }
            }
        } catch (e: Exception) {
            LogDetail.LogEStack(e)
        }
    }

    fun changeStyle(isFollowed: Boolean){
        try {
            if (isFollowed) {
                binding?.followBtn!!.text = "✓Following"
                binding?.followBtn!!.setBackgroundResource(R.drawable.bg_white_publish_rounded)
                binding?.followBtn!!.setTextColor(Color.parseColor("#0A1A3F"))
                binding?.profileFollowBtn!!.text = "✓Following"
                binding?.profileFollowBtn!!.setBackgroundResource(R.drawable.bg_white_publish_rounded)
                binding?.profileFollowBtn!!.setTextColor(Color.parseColor("#0A1A3F"))
            } else {
                binding?.followBtn!!.text = "Follow"
                binding?.followBtn!!.setBackgroundResource(R.drawable.bg_publish_page_dark)
                binding?.followBtn!!.setTextColor(Color.WHITE)
                binding?.profileFollowBtn!!.text = "Follow"
                binding?.profileFollowBtn!!.setBackgroundResource(R.drawable.bg_publish_page_dark)
                binding?.profileFollowBtn!!.setTextColor(Color.WHITE)
            }
        } catch (ex:Exception){
            LogDetail.LogEStack(ex)
        }
    }

    @SuppressLint("CommitPrefEdits")
    fun storeData(url: String, timeStamp: Long){
        try {
            if(postImpressions.isEmpty()){
                return
            }
            val postImpressionsModel = PostImpressionsModel(url, postImpressions.values.toList(), timeStamp)
            val gson = Gson()
            val sharedPrefs = getSharedPreferences("postImpressions", Context.MODE_PRIVATE)
            val postImpressionString = gson.toJson(postImpressionsModel)
            sharedPrefs.edit().putString(timeStamp.toString(), postImpressionString).apply()
            postImpressions = HashMap()
            FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let {
                ApiPostImpression().addPostImpressionsEncrypted(
                    Endpoints.POST_IMPRESSIONS_ENCRYPTED,
                    it,
                    this
                )
            }
        } catch (ex:java.lang.Exception){
            LogDetail.LogEStack(ex)
        }
    }

    private fun setFonts(view: View?){
        Card.setFontFamily(binding?.publisherImage, true)
        Card.setFontFamily(binding?.publishProfileName, true)
        Card.setFontFamily(binding?.followers, true)
        Card.setFontFamily(binding?.tvFollowersTitle, true)
        Card.setFontFamily(binding?.followBtn)
        Card.setFontFamily(binding?.profilePublisherImage, true)
        Card.setFontFamily(binding?.profileName, true)
        Card.setFontFamily(binding?.profileFollowers, true)
        Card.setFontFamily(binding?.profileFollowersTitle, true)
        Card.setFontFamily(binding?.profileFollowBtn)
        Card.setFontFamily(binding?.noPosts, true)
    }

    override fun onDestroy() {
        super.onDestroy()
        storeData(presentUrl, presentTimeStamp)
    }

}
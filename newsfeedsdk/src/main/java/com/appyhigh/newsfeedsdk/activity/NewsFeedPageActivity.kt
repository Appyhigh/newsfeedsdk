package com.appyhigh.newsfeedsdk.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import coil.ImageLoader
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.Constants.ALREADY_EXISTS
import com.appyhigh.newsfeedsdk.Constants.FEED_ID
import com.appyhigh.newsfeedsdk.Constants.FEED_TYPE
import com.appyhigh.newsfeedsdk.Constants.INTEREST
import com.appyhigh.newsfeedsdk.Constants.LANGUAGE
import com.appyhigh.newsfeedsdk.Constants.POSITION
import com.appyhigh.newsfeedsdk.Constants.POST_ID
import com.appyhigh.newsfeedsdk.Constants.POST_SOURCE
import com.appyhigh.newsfeedsdk.Constants.ReactionType
import com.appyhigh.newsfeedsdk.Constants.cardsMap
import com.appyhigh.newsfeedsdk.Constants.postDetailCards
import com.appyhigh.newsfeedsdk.Constants.setDrawableColor
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.apicalls.*
import com.appyhigh.newsfeedsdk.apiclient.Endpoints
import com.appyhigh.newsfeedsdk.callbacks.FeedReactionListener
import com.appyhigh.newsfeedsdk.callbacks.GlideCallbackListener
import com.appyhigh.newsfeedsdk.databinding.ActivityNewsFeedPageBinding
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.fragment.FeedMenuBottomSheetFragment
import com.appyhigh.newsfeedsdk.fragment.NonNativeCommentBottomSheet
import com.appyhigh.newsfeedsdk.model.*
import com.appyhigh.newsfeedsdk.model.feeds.Card
import com.appyhigh.newsfeedsdk.utils.*
import com.appyhigh.newsfeedsdk.utils.SpUtil.Companion.eventsListener
import com.appyhigh.newsfeedsdk.utils.SpUtil.Companion.spUtilInstance
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.StyledPlayerView.SHOW_BUFFERING_WHEN_PLAYING
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.util.Util
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.dynamiclinks.DynamicLink.AndroidParameters
import com.google.firebase.dynamiclinks.DynamicLink.SocialMetaTagParameters
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.firebase.dynamiclinks.ShortDynamicLink
import com.google.gson.Gson
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.YouTubePlayerCallback
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.YouTubePlayerFullScreenListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.utils.loadOrCueVideo
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*


class NewsFeedPageActivity : AppCompatActivity() {
    private var simpleExoPlayer: ExoPlayer? = null
    private var videoUrl: String? = null
    private var playWhenReady = true
    private var currentWindow = 0
    private var playbackPosition: Long = 0
    private var isVideo = false
    private var imageUrl = ""
    private var isYoutube = false
    private var youtubeVideoId: String? = null
    private var youtubePlayer: YouTubePlayer? = null
    private var postId: String? = null
    private var mFullScreenDialog: Dialog? = null
    private var showFullScreen = false
    private var nonNativeCommentBottomSheet: NonNativeCommentBottomSheet? = null
    private val TAG = NewsFeedPageActivity::class.java.name
    private var comments: ArrayList<FeedComment> = ArrayList<FeedComment>()
    private var reacted = ""
    var likes = 0
    var position = 0
    var commentsCount = 0
    private var post_source = "unknown"
    private var feed_type = "unknown"
    private var binding: ActivityNewsFeedPageBinding? = null
    private var layout_video_error: View? = null
    private var exo_fullscreen_button: View? = null
    private var exo_fullscreen_icon: ImageView? = null
    private var details_close_button: View? = null
    private var interest = ""
    var presentPostDetailsModel: PostDetailsModel? = null
    var nextCardPost: PostDetailsModel.NextPost? = null
    var nextCardAlreadyExists: Boolean = false
    var adsModel = ApiConfig().getConfigModel(this)
    var mInterstitialAd: InterstitialAd? = null
    var totalDuration = 0
    var duration = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewsFeedPageBinding.inflate(layoutInflater)
        val view = binding?.root
        setContentView(view)
        setFonts(view)
        PodcastMediaPlayer.setPodcastListener(view!!, "feedDetailPage")
        interest = intent.getStringExtra(INTEREST) ?: "unknown"
        position = intent.getIntExtra(POSITION, 0)
        layout_video_error = findViewById(R.id.layout_video_error)
        exo_fullscreen_button = findViewById(R.id.exo_fullscreen_button)
        details_close_button = findViewById(R.id.details_close_button)
        exo_fullscreen_icon = findViewById(R.id.exo_fullscreen_icon)
        layout_video_error?.visibility = View.GONE
        exo_fullscreen_button?.visibility = View.GONE
        details_close_button?.visibility = View.GONE
        Constants.postDetailPageNo = 0
        if (position == 0) {
            Constants.nativePageCount = 1
        }
        showAds()
        if (intent.hasExtra(INTEREST)) {
            postDetailCards = ArrayList()
        }
        if (intent.hasExtra(ALREADY_EXISTS) && position < postDetailCards.size) {
            postId = intent.getStringExtra(POST_ID)
            handleResults(postDetailCards[position], true)
        } else if (intent != null) {
            onIntent(intent)
        }
        binding?.ivBack?.setOnClickListener { onBackPressed() }
        if (postDetailCards.isNotEmpty() && position > 0) {
            binding?.prevCard?.visibility = View.VISIBLE
        }
        binding?.newsItemMoreOption?.setOnClickListener {
            try{
                val reportBottomSheet = FeedMenuBottomSheetFragment.newInstance(
                    presentPostDetailsModel?.post?.publisherContactUs?:"",
                    presentPostDetailsModel?.post?.publisherId?:"",
                    postId!!
                )
                reportBottomSheet.show(supportFragmentManager, "reportBottomSheet")
            } catch (ex:Exception){
                LogDetail.LogEStack(ex)
            }
        }
        binding?.prevCard?.setOnClickListener {
            if (postDetailCards.size > position) {
                val intent = if (Constants.postDetailCards[position - 1].post!!.isNative!!) {
                    val bundle = Bundle()
                    bundle.putString("NativePageOpen", "Feed")
                    FirebaseAnalytics.getInstance(this).logEvent("NativePage", bundle)
                    Intent(this, PostNativeDetailActivity::class.java)
                } else {
                    Intent(this, NewsFeedPageActivity::class.java)
                }
                intent.putExtra(POSITION, position - 1)
                intent.putExtra(ALREADY_EXISTS, true)
                intent.putExtra(POST_ID, postId)
                intent.putExtra(LANGUAGE, intent.getStringExtra(LANGUAGE) ?: "en")
                intent.putExtra("from_app", true)
                startActivity(intent)
                finish()
            }
        }
        binding?.newsItemStats?.setOnClickListener { binding?.tvComments?.performClick() }
        binding?.tvComments?.setOnClickListener {
            try {
                if (eventsListener != null) {
                    eventsListener!!.onFeedInteraction(
                        post_source,
                        interest,
                        postId!!,
                        intent.getStringExtra("platform").toString(),
                        "comment"
                    )
                }
            } catch (ex: java.lang.Exception) {
                LogDetail.LogEStack(ex)
            }
            nonNativeCommentBottomSheet = NonNativeCommentBottomSheet(
                comments,
                object : BlogDetailsFragmentListener {
                    override fun onClose() {}
                    override fun onDismissComment() {}
                    override fun onPostComment(comment: String?) {
                        comment?.let {
                            postComment(it)
                        }
                    }
                })
            nonNativeCommentBottomSheet?.show(supportFragmentManager, TAG)
        }
        binding?.tvLikes?.setOnClickListener {
            try {
                FeedSdk.areContentsModified[intent.getStringExtra(Constants.SCREEN_TYPE)!!] = true
            } catch (ex: Exception) {
                FeedSdk.areContentsModified[Constants.FEED] = true
                LogDetail.LogEStack(ex)
            }
            try {
                if (eventsListener != null) {
                    eventsListener!!.onFeedInteraction(
                        post_source,
                        interest,
                        postId!!,
                        intent.getStringExtra("platform").toString(),
                        "like"
                    )
                }
            } catch (ex: java.lang.Exception) {
                LogDetail.LogEStack(ex)
            }
            if (!reacted.equals("none", ignoreCase = true) && !reacted.equals("false", ignoreCase = true)) {
                reacted = "none"
                try {
                    if (cardsMap[interest] != null) {
                        val card = cardsMap[interest]!![position]
                        var likeCount = card.items[0].reactionsCount?.likeCount!!
                        likeCount -= 1
                        card.items[0].reactionsCount?.likeCount = likeCount
                        card.items[0].isReacted = Constants.ReactionType.NONE.toString()
                        cardsMap[interest]!![position] = card
                    }
                    val pos = if (postDetailCards.size == 1) 0 else position
                    var likeCount = postDetailCards[pos].post?.reactionCount?.likeCount!!
                    likeCount -= 1
                    postDetailCards[pos].post?.reactionCount?.likeCount = likeCount
                    postDetailCards[pos].post?.isReacted = ReactionType.NONE.toString()
                } catch (e: Exception) {
                    LogDetail.LogEStack(e)
                }
                likes -= 1
                binding?.newsItemStats?.setText("$likes Likes & $commentsCount Comments")
            } else {
                reacted = "like"
                try {
                    if (cardsMap[interest] != null) {
                        val card = cardsMap[interest]!![position]
                        var likeCount = card.items[0].reactionsCount?.likeCount!!
                        likeCount += 1
                        card.items[0].reactionsCount?.likeCount = likeCount
                        card.items[0].isReacted = Constants.ReactionType.LIKE.toString()
                        cardsMap[interest]!![position] = card
                    }
                    val pos = if (postDetailCards.size == 1) 0 else position
                    var likeCount = postDetailCards[pos].post?.reactionCount?.likeCount!!
                    likeCount += 1
                    postDetailCards[pos].post?.reactionCount?.likeCount = likeCount
                    postDetailCards[pos].post?.isReacted = ReactionType.LIKE.toString()
                } catch (e: Exception) {
                    LogDetail.LogEStack(e)
                }
                likes += 1
                binding?.newsItemStats?.text = "$likes Likes  & $commentsCount Comments"
            }
            binding?.tvLikes?.setCompoundDrawablesWithIntrinsicBounds(
                Converters().getDisplayImage(
                    reacted,
                    this@NewsFeedPageActivity,
                    false
                ), null, null, null
            )
            if(reacted=="none"){
                binding?.tvLikes?.setDrawableColor(ContextCompat.getColor(this, R.color.feedSecondaryTintColor))
            } else{
                binding?.tvLikes?.setDrawableColor(ContextCompat.getColor(this, R.color.purple_500))
            }
            postReaction()
        }
        binding?.tvLikes?.setOnLongClickListener {
            binding?.tvLikes?.showReactionsPopUpWindow(
                null,
                object : FeedReactionListener {
                    override fun onReaction(item: Post?, reactionType: ReactionType) {
                        if (reacted.isEmpty() || reacted == "none") likes += 1
                        reacted = reactionType.toString().lowercase(Locale.getDefault())
                        postReaction()
                        binding?.tvLikes?.setCompoundDrawablesWithIntrinsicBounds(
                            Converters().getDisplayImage(
                                reacted,
                                this@NewsFeedPageActivity,
                                false
                            ), null, null, null
                        )
                    }
                },
                false,
                binding?.tvLikes!!
            )
            false
        }
    }

    private fun showAds() {
        try {
            ApiConfig().requestAd(this, "post_detail_footer_banner", object : ConfigAdRequestListener{
                override fun onPrivateAdSuccess(webView: WebView) {
                    binding!!.bannerAd.removeAllViews()
                    binding!!.bannerAd.addView(webView)
                }

                override fun onAdmobAdSuccess(adId: String) {
                    showAdaptiveBanner(this@NewsFeedPageActivity, adId, binding!!.bannerAd)
                }

                override fun onAdHide() {
                    binding!!.bannerAd.visibility = View.GONE
                }
            }, true)

            if (ApiConfig().checkShowAds(this) && adsModel.postDetailInterstitial.showAdmob) {
                if (Constants.nativePageCount > adsModel.showInterstitialAfterPosts) {
                    loadInterstitialAd(
                        this,
                        adsModel.postDetailInterstitial.admobId,
                        object : InterstitialAdUtilLoadCallback {
                            override fun onAdFailedToLoad(
                                adError: LoadAdError,
                                ad: InterstitialAd?
                            ) {
                                mInterstitialAd = null
                            }

                            override fun onAdLoaded(ad: InterstitialAd?) {
                                mInterstitialAd = ad
                            }

                            override fun onAdDismissedFullScreenContent() {
                                Constants.nativePageCount = 0
                                mInterstitialAd = null
                                nextCardPost?.let { getNextCard(it, nextCardAlreadyExists) }
                            }

                            override fun onAdFailedToShowFullScreenContent(adError: AdError?) {}

                            override fun onAdShowedFullScreenContent() {}
                        })
                }
            }
        } catch (ex: Exception) {
            LogDetail.LogEStack(ex)
        }
    }


    fun postReaction() {
        var reactionType = ReactionType.LIKE
        when (reacted.lowercase(Locale.getDefault())) {
            "like" -> {
                reactionType = ReactionType.LIKE
            }
            "wow" -> {
                reactionType = ReactionType.WOW
            }
            "love" -> {
                reactionType = ReactionType.LOVE
            }
            "angry" -> {
                reactionType = ReactionType.ANGRY
            }
            "laugh" -> {
                reactionType = ReactionType.LAUGH
            }
            "sad" -> {
                reactionType = ReactionType.SAD
            }
            "none" -> {
                reactionType = ReactionType.NONE
            }
        }
        ApiReactPost().reactPostEncrypted(
            Endpoints.REACT_POST_ENCRYPTED,
            postId!!,
            presentPostDetailsModel?.post?.postSource, presentPostDetailsModel?.post?.feedType,
            reactionType
        )
    }

    private fun onIntent(intent: Intent) {
        try {
            if (intent.hasExtra("from_app")) {
                postId = intent.getStringExtra(POST_ID)
                if (intent.hasExtra(POST_SOURCE)) post_source =
                    intent.getStringExtra(POST_SOURCE) ?: "unknown"
                if (intent.hasExtra(FEED_TYPE)) feed_type =
                    intent.getStringExtra(FEED_TYPE) ?: "unknown"
                exo_fullscreen_button!!.visibility = View.VISIBLE
                details_close_button!!.visibility = View.VISIBLE
                getData(postId!!)
            } else {
                getDynamicLink(this, intent)
            }
        } catch (ex: Exception) {
            LogDetail.LogEStack(ex)
        }
    }

    private fun getDynamicLink(context: Context, intent: Intent) {
        FirebaseDynamicLinks.getInstance().getDynamicLink(intent)
            .addOnSuccessListener(OnSuccessListener { pendingDynamicLinkData ->
                val deepLink: Uri?
                if (pendingDynamicLinkData != null) {
                    deepLink = pendingDynamicLinkData.link
                    LogDetail.LogD("Firebase", deepLink.toString())
                    val intent = getIntent()
                    val data = intent.data ?: return@OnSuccessListener
                    if (data.toString().lowercase(Locale.getDefault()).contains(FEED_ID)) {
                        if (data.getQueryParameter(FEED_ID) != null) {
                            postId = data.getQueryParameter(FEED_ID)
                            getData(postId!!)
                        }
                    } else {
                        Toast.makeText(
                            context,
                            getString(R.string.error_some_issue_occurred),
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                }
            })
            .addOnFailureListener { e -> LogDetail.LogEStack(e) }
    }

    private fun postComment(comment: String) {
        ApiCommentPost().postCommentEncrypted(
            Endpoints.COMMENT_POST_ENCRYPTED,
            postId!!,
            presentPostDetailsModel?.post?.postSource, presentPostDetailsModel?.post?.feedType,
            "text",
            comment,
            object : ApiCommentPost.PostCommentResponse {
                override fun onSuccess(feedCommentResponseWrapper: FeedCommentResponseWrapper) {
                    handlePostResults(feedCommentResponseWrapper)
                }
            })
    }

    private fun getData(postId: String) {
        ApiGetPostDetails().getPostDetailsEncrypted(
            Endpoints.GET_POSTS_DETAILS_ENCRYPTED,
            postId,
            post_source,
            feed_type,
            object : ApiGetPostDetails.PostDetailsResponse {
                override fun onSuccess(
                    postDetailsModel: PostDetailsModel,
                    url: String,
                    timeStamp: Long
                ) {
                    postDetailsModel.post?.presentUrl = url
                    postDetailsModel.post?.presentTimeStamp = timeStamp
                    handleResults(postDetailsModel, false)
                }

                override fun onFailure() {
                    try{
                        Handler(Looper.getMainLooper()).post {
                            try{
                                Toast.makeText(
                                    this@NewsFeedPageActivity,
                                    getString(R.string.error_some_issue_occurred),
                                    Toast.LENGTH_SHORT
                                ).show()
                            } catch (ex:Exception){ LogDetail.LogEStack(ex) }
                        }
                    } catch (ex:Exception){}
                    finish()
                }
            })
    }

    private fun handlePostResults(feedCommentResponse: FeedCommentResponseWrapper) {
        Handler(Looper.getMainLooper()).post {
            try {
                commentsCount += 1
                try {
                    FeedSdk.areContentsModified[intent.getStringExtra(Constants.SCREEN_TYPE)!!] =
                        true
                } catch (ex: Exception) {
                    FeedSdk.areContentsModified[Constants.FEED] = true
                    LogDetail.LogEStack(ex)
                }
                if (cardsMap[interest] != null) {
                    val card = cardsMap[interest]!![position]
                    var commentsCount = card.items[0].appComments!!
                    commentsCount += 1
                    card.items[0].appComments = commentsCount
                    cardsMap[interest]!![position] = card
                }
                val pos = if (postDetailCards.size == 1) 0 else position
                var commentsCount = postDetailCards[pos].post?.appComments!!
                commentsCount += 1
                postDetailCards[pos].post?.appComments = commentsCount
                binding?.newsItemStats!!.text = "$likes Likes  & $commentsCount Comments"
            } catch (e: Exception) {
                LogDetail.LogEStack(e)
            }
            feedCommentResponse.result?.comment?.let {
                nonNativeCommentBottomSheet?.updateComments(
                    it
                )
            }
        }
    }

    private fun handleResults(postDetailsModel: PostDetailsModel, isAlreadyExists: Boolean) {
        presentPostDetailsModel = postDetailsModel
        post_source = postDetailsModel.post?.postSource?:"unknown"
        feed_type = postDetailsModel.post?.feedType?:"unknown"
        if (!isAlreadyExists) {
            postDetailCards.add(postDetailsModel)
        } else {
            exo_fullscreen_button!!.visibility = View.VISIBLE
            details_close_button!!.visibility = View.VISIBLE
        }
        comments = postDetailsModel.post?.comments as ArrayList<FeedComment>
        if (intent.hasExtra("isReacted")) {
            reacted = intent.getStringExtra("isReacted")!!.toString()
        } else {
            reacted = postDetailsModel.post?.isReacted.toString()
        }
        val logoUrl: String = postDetailsModel.post?.publisherProfilePic.toString()
        val title: String = postDetailsModel.post?.content?.description.toString()
        val publisherName: String = postDetailsModel.post?.publisherName.toString()
        var publishedOn: String = postDetailsModel.post?.publishedOn.toString()
        val url: String = postDetailsModel.post?.content?.url.toString()
        if (intent.hasExtra("reactionCount")) {
            likes = intent.getIntExtra("reactionCount", 0)
        } else {
            likes = postDetailsModel.post?.reactionCount?.likeCount!!
            likes += postDetailsModel.post?.reactionCount?.angryCount!!
            likes += postDetailsModel.post?.reactionCount?.laughCount!!
            likes += postDetailsModel.post?.reactionCount?.loveCount!!
            likes += postDetailsModel.post?.reactionCount?.sadCount!!
            likes += postDetailsModel.post?.reactionCount?.wowCount!!
        }
        commentsCount = postDetailsModel.post?.appComments!!
        val category: String = postDetailsModel.post?.platform.toString().uppercase()
        val isWebView: Boolean = postDetailsModel.post?.isWebView!!
        binding?.newsItemStats!!.text = "$likes Likes  & $commentsCount Comments"
        isVideo = postDetailsModel.post?.isVideo ?: false
        isYoutube = postDetailsModel.post?.platform.equals("youtube")
        if (isYoutube) {
            youtubeVideoId = postDetailsModel.post?.content?.shortCode!!
        }
        if (isVideo) {
            videoUrl = postDetailsModel.post?.content?.mediaList!![0]
            if (postDetailsModel.post?.content?.mediaList?.size!! > 1) {
                imageUrl = postDetailsModel.post?.content?.mediaList!![1]
            }
            playbackPosition = intent.getLongExtra("playback_position", 0)
        } else {
            if (postDetailsModel.post?.content?.mediaList?.size!! > 0) {
                imageUrl = postDetailsModel.post?.content?.mediaList!![0]
            }
        }
        binding?.tvLikes?.setCompoundDrawablesWithIntrinsicBounds(
            Converters().getDisplayImage(reacted, this, false),
            null,
            null,
            null
        )
        try{
            if(reacted.toString().uppercase()== Constants.ReactionType.LIKE.toString()){
                binding?.tvLikes?.setDrawableColor(ContextCompat.getColor(this, R.color.purple_500))
            } else {
                binding?.tvLikes?.setDrawableColor(ContextCompat.getColor(this, R.color.feedSecondaryTintColor))
            }
        } catch (ex:Exception){
            LogDetail.LogEStack(ex)
        }
        try {
            publishedOn = getTime(publishedOn)
            LogDetail.LogD("PublishedOn", publishedOn)
        } catch (e: ParseException) {
            LogDetail.LogD("PublishedOn", e.message.toString())
            LogDetail.LogEStack(e)
        }
        if (Converters().getDisplayImageForPlatForm(
                postDetailsModel.post?.platform!!.lowercase(
                    Locale.getDefault()
                ), this
            ) != null
        ) {
            binding?.ivPublisherSource?.setImageDrawable(
                Converters().getDisplayImageForPlatForm(
                    postDetailsModel.post?.platform!!.lowercase(
                        Locale.getDefault()
                    ), this
                )
            )
            binding?.ivPublisherSource?.visibility = View.VISIBLE
        } else {
            binding?.ivPublisherSource?.visibility = View.INVISIBLE
        }
        details_close_button!!.setOnClickListener { onBackPressed() }
        if (isWebView) {
            binding?.newsPageWebView!!.visibility = View.VISIBLE
            binding?.newsPageScrollView!!.visibility = View.GONE
            binding?.newsPageWebView!!.loadUrl(url)
            binding?.newsPageWebView!!.settings.javaScriptEnabled = true
            binding?.newsPageWebView!!.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                    view.loadUrl(url)
                    return false
                }
            }
            binding?.tvTime?.text = publishedOn
            binding?.tvPublisher?.text = publisherName
            if (logoUrl.contains(".svg")) {
                val imageLoader = ImageLoader.Builder(this@NewsFeedPageActivity)
                    .componentRegistry { add(SvgDecoder(this@NewsFeedPageActivity)) }
                    .build()

                val request = ImageRequest.Builder(this@NewsFeedPageActivity)
                    .crossfade(true)
                    .crossfade(500)
                    .placeholder(R.drawable.placeholder)
                    .data(logoUrl)
                    .target(
                        onSuccess = {
                            binding?.ivPublisherImage!!.setImageDrawable(it)
                        },
                        onError = {
                            try {
                                binding?.tvPublisherImage?.text =
                                    publisherName.toString().substring(0, 1).uppercase()
                            } catch (ex: Exception) {
                                binding?.tvPublisherImage?.text = "N"
                            }
                            binding?.tvPublisherImage?.visibility = View.VISIBLE
                            binding?.ivPublisherImage?.visibility = View.GONE
                        }
                    )
                    .build()

                imageLoader.enqueue(request)
            } else {
                Constants.loadImageFromGlide(
                    this,
                    logoUrl,
                    binding?.ivPublisherImage,
                    object : GlideCallbackListener {
                        override fun onSuccess(drawable: Drawable?) {
                            try {
                                binding!!.ivPublisherImage.setImageDrawable(drawable)
                            } catch (ex: Exception) {
                                LogDetail.LogEStack(ex)
                            }
                        }

                        override fun onFailure() {
                            if (logoUrl.isEmpty()) {
                                try {
                                    binding?.tvPublisherImage?.text =
                                        publisherName.toString().substring(0, 1).uppercase()
                                } catch (ex: Exception) {
                                    binding?.tvPublisherImage?.text = "N"
                                }
                                binding?.tvPublisherImage?.visibility = View.VISIBLE
                                binding?.ivPublisherImage?.visibility = View.GONE
                            } else {
                                Picasso.get()
                                    .load(logoUrl)
                                    .noFade()
                                    .into(binding!!.ivPublisherImage, object : Callback {
                                        @SuppressLint("LogNotTimber")
                                        override fun onSuccess() {
                                            LogDetail.LogD("TAG", "onSuccess: Picasso " + logoUrl)
                                        }

                                        override fun onError(e: java.lang.Exception?) {
                                            try {
                                                binding?.tvPublisherImage?.text =
                                                    publisherName.toString().substring(0, 1)
                                                        .uppercase()
                                            } catch (ex: Exception) {
                                                binding?.tvPublisherImage?.text = "N"
                                            }
                                            binding?.tvPublisherImage?.visibility = View.VISIBLE
                                            binding?.ivPublisherImage?.visibility = View.GONE
                                        }
                                    })
                            }
                        }
                    })
            }
        } else {
            binding?.newsPageWebView!!.visibility = View.GONE
            binding?.newsPageScrollView!!.visibility = View.VISIBLE
            binding?.tvTime?.text = publishedOn
            binding?.tvPublisher?.text = publisherName
            binding?.newsPageCategory!!.text = category
            binding?.newsPageTitle!!.text = HtmlCompat.fromHtml(title, HtmlCompat.FROM_HTML_MODE_LEGACY)
            if (binding?.newsPageTitle!!.length() > 150) makeTextViewResizable(
                binding?.newsPageTitle,
                3,
                "See More",
                true
            )
            binding?.newsItemStats!!.text = "$likes Likes & $commentsCount Comments"
            Constants.loadImageFromGlide(
                this,
                logoUrl,
                binding?.ivPublisherImage,
                object : GlideCallbackListener {
                    override fun onSuccess(drawable: Drawable?) {
                        try {
                            binding!!.ivPublisherImage.setImageDrawable(drawable)
                        } catch (ex: Exception) {
                            LogDetail.LogEStack(ex)
                        }
                    }

                    override fun onFailure() {
                        Picasso.get()
                            .load(logoUrl)
                            .noFade()
                            .into(binding!!.ivPublisherImage, object : Callback {
                                override fun onSuccess() {
                                    LogDetail.LogD("TAG", "onSuccess: Picasso " + logoUrl)
                                }

                                override fun onError(e: java.lang.Exception?) {
                                    try {
                                        binding?.tvPublisherImage?.text =
                                            publisherName.toString().substring(0, 1)
                                                .uppercase()
                                    } catch (ex: Exception) {
                                        binding?.tvPublisherImage?.text = "N"
                                    }
                                    binding?.tvPublisherImage?.visibility = View.VISIBLE
                                    binding?.ivPublisherImage?.visibility = View.GONE
                                }
                            })
                    }

                })
            if (isYoutube) {
                binding?.videoView!!.visibility = View.GONE
                binding?.newsPageImage!!.visibility = View.GONE
                binding?.newsPageVideoYoutube!!.visibility = View.VISIBLE
                lifecycle.addObserver(binding?.newsPageVideoYoutube!!)
                binding?.newsPageVideoYoutube!!.getYouTubePlayerWhenReady(object : YouTubePlayerCallback{
                    override fun onYouTubePlayer(youTubePlayer: YouTubePlayer) {
                        youtubePlayer = youTubePlayer
                        youTubePlayer.loadVideo(youtubeVideoId!!, playbackPosition.toFloat())
                    }
                })
                binding?.newsPageVideoYoutube!!.addFullScreenListener(object :
                    YouTubePlayerFullScreenListener {
                    override fun onYouTubePlayerEnterFullScreen() {
                        binding!!.newsPageVideoYoutube.enterFullScreen()
                        openFullscreenDialog(true)
                    }

                    override fun onYouTubePlayerExitFullScreen() {
                        binding!!.newsPageVideoYoutube.exitFullScreen()
                        closeFullscreenDialog(true)
                    }
                })
                binding?.newsPageVideoYoutube!!.addYouTubePlayerListener(object :
                    AbstractYouTubePlayerListener() {
                    override fun onReady(youTubePlayer: YouTubePlayer) {
                        youtubePlayer = youTubePlayer
                        youTubePlayer.loadOrCueVideo(lifecycle, youtubeVideoId!!, playbackPosition.toFloat())
                    }

                    override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                        super.onCurrentSecond(youTubePlayer, second)
                        duration = second.toInt()
                    }

                    override fun onVideoDuration(youTubePlayer: YouTubePlayer, duration: Float) {
                        super.onVideoDuration(youTubePlayer, duration)
                        totalDuration = duration.toInt()
                    }
                })
                initFullscreenDialog(true)
            } else if (isVideo) {
                binding?.videoView!!.visibility = View.VISIBLE
                if (exo_fullscreen_button!!.visibility == View.VISIBLE) {
                    initFullscreenDialog()
                    exo_fullscreen_button!!.setOnClickListener {
                        if (!showFullScreen) {
                            openFullscreenDialog()
                        } else {
                            closeFullscreenDialog()
                        }
                    }
                }
                binding?.newsPageImage!!.visibility = View.GONE
                binding?.newsPageVideoYoutube!!.visibility = View.GONE
                if (intent.hasExtra("hasErrorPlaying")) {
                    binding?.videoView!!.visibility = View.GONE
                    layout_video_error!!.visibility = View.VISIBLE
                } else {
                    binding?.videoView!!.setShowBuffering(SHOW_BUFFERING_WHEN_PLAYING)
                    initializePlayer()
                }
            } else {
                binding?.newsPageImage!!.visibility = View.VISIBLE
                binding?.videoView!!.visibility = View.GONE
                binding?.newsPageVideoYoutube!!.visibility = View.GONE
                if (imageUrl.isEmpty()) {
                    binding?.newsPageImage!!.visibility = View.GONE
                }
                Constants.loadImageFromGlide(
                    this,
                    imageUrl,
                    binding?.newsPageImage,
                    object : GlideCallbackListener {
                        override fun onSuccess(drawable: Drawable?) {
                            try {
                                binding?.newsPageImage?.setImageDrawable(drawable)
                            } catch (ex: Exception) {
                                LogDetail.LogEStack(ex)
                            }
                        }

                        override fun onFailure() {}

                    })
            }
        }
        binding?.tvShare?.setOnClickListener { v ->
            sharePost(
                v.context,
                postId,
                title,
                imageUrl,
                false,
                url
            )

        }
        binding?.tvWhatsappShare?.setOnClickListener { v ->
            sharePost(
                v.context,
                postId,
                title,
                imageUrl,
                true,
                url
            )
        }
        binding?.progressBar?.visibility = View.GONE
        if (intent.hasExtra("isComment") && intent.getBooleanExtra("isComment", false)) {
            binding?.tvComments?.performClick()
        }
        try {
            if (postDetailsModel.post?.additional_data?.next_post != null) {
                val nextPost = postDetailsModel.post!!.additional_data.next_post
                binding?.nextCard?.visibility = View.VISIBLE
                binding?.nextCard?.setOnClickListener {
                    getNextCard(nextPost!!, isAlreadyExists)
                }
                val relatedPost = postDetailsModel.post!!.additional_data.related_post
                val url = try {
                    relatedPost!!.content.images[0][0].url
                } catch (ex: Exception) {
                    relatedPost!!.content.media_list[relatedPost.content.media_list.size - 1]
                }
                if (relatedPost!!.is_video) {
                    binding?.relatedVideo?.visibility = View.VISIBLE
                    Constants.loadImageFromGlide(
                        this,
                        url,
                        binding?.relatedVideoImage,
                        object : GlideCallbackListener {
                            override fun onSuccess(drawable: Drawable?) {
                                try {
                                    binding?.relatedVideoImage?.setImageDrawable(drawable)
                                } catch (ex: Exception) {
                                    LogDetail.LogEStack(ex)
                                }
                            }

                            override fun onFailure() {}

                        })
                    binding?.relatedVideo?.setOnClickListener {
                        getNextCard(relatedPost, isAlreadyExists)
                    }
                } else {
                    binding?.relatedPost?.visibility = View.VISIBLE
                    binding?.relatedPostDescription?.text = relatedPost.content.title
                    Constants.loadImageFromGlide(
                        this,
                        url,
                        binding?.relatedPostImage,
                        object : GlideCallbackListener {
                            override fun onSuccess(drawable: Drawable?) {
                                try {
                                    binding?.relatedPostImage?.setImageDrawable(drawable)
                                } catch (ex: Exception) {
                                    LogDetail.LogEStack(ex)
                                }
                            }

                            override fun onFailure() {}

                        })
                    binding?.relatedPost?.setOnClickListener {
                        getNextCard(relatedPost, isAlreadyExists)
                    }
                }
            }
        } catch (ex: Exception) {
            LogDetail.LogEStack(ex)
        }
        if(!isVideo) {
            storeData()
        }
    }

    private fun getNextCard(nextPost: PostDetailsModel.NextPost, isAlreadyExists: Boolean) {
        if (mInterstitialAd != null) {
            nextCardAlreadyExists = isAlreadyExists
            nextCardPost = nextPost
            mInterstitialAd?.show(this)
        } else {
            Constants.nativePageCount = Constants.nativePageCount + 1
            val intent = if (nextPost.isNative) {
                val bundle = Bundle()
                bundle.putString("NativePageOpen", "Feed")
                FirebaseAnalytics.getInstance(this).logEvent("NativePage", bundle)
                Intent(this, PostNativeDetailActivity::class.java)
            } else {
                Intent(this, NewsFeedPageActivity::class.java)
            }
            if (isAlreadyExists && postDetailCards.size > position + 1) {
                intent.putExtra(ALREADY_EXISTS, true)
                intent.putExtra(POSITION, position + 1)
            } else {
                intent.putExtra(POSITION, postDetailCards.size)
            }
            intent.putExtra(LANGUAGE, intent.getStringExtra(LANGUAGE) ?: "en")
            intent.putExtra(POST_ID, postId)
            intent.putExtra(POST_SOURCE , nextPost.postSource)
            intent.putExtra(FEED_TYPE ,nextPost.feedType)
            intent.putExtra("from_app", true)
            startActivity(intent)
            finish()
        }
    }

    private fun handleError(throwable: Throwable) {
        LogDetail.LogEStack(throwable)
        binding?.progressBar?.visibility = View.GONE
        Toast.makeText(this, "Some issue occured, please try after sometime", Toast.LENGTH_SHORT)
            .show()
        finish()
    }

    private fun initializePlayer() {
        if (simpleExoPlayer == null) {
            simpleExoPlayer = ExoPlayer.Builder(this).build()
            binding?.videoView!!.player = simpleExoPlayer
            val uri = Uri.parse(videoUrl)
            val mediaSource = buildMediaSource(uri)
            simpleExoPlayer!!.seekTo(currentWindow, playbackPosition)
            simpleExoPlayer!!.setMediaSource(mediaSource, false)
            simpleExoPlayer!!.prepare()
            simpleExoPlayer!!.playWhenReady = false
            binding!!.videoView.useController = false
            Card.setFontFamily(binding!!.duration, false)
            val updateSeekBar: Runnable = object : Runnable {
                @SuppressLint("SetTextI18n")
                override fun run() {
                    try {
                        val totalDuration = simpleExoPlayer!!.duration
                        val currentDuration = simpleExoPlayer!!.currentPosition
                        val progress = ((currentDuration * 1.0) / totalDuration) * 100
                        binding!!.seekbar.progress = progress.toInt()
                        if (totalDuration <= 0) {
                            binding!!.duration.text = ""
                        } else {
                            binding!!.duration.text =
                                PodcastMediaPlayer.convertTime(currentDuration) + "/" + PodcastMediaPlayer.convertTime(
                                    totalDuration
                                )
                        }
                        if (binding!!.playController.visibility == View.VISIBLE)
                            Handler(Looper.getMainLooper()).postDelayed(this, 1000)
                    } catch (ex: java.lang.Exception) {
                    }
                }
            }
            binding!!.seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if(fromUser){
                        val currentPosition = (progress *  simpleExoPlayer!!.duration) / 100
                        simpleExoPlayer!!.seekTo(currentPosition)
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
            binding!!.videoFrame.setOnClickListener {
                try{
                    if(binding!!.playController.visibility == View.GONE){
                        binding!!.playController.visibility = View.VISIBLE
                        if (simpleExoPlayer!!.isPlaying) {
                            binding!!.playVideo.setImageResource(R.drawable.ic_podcast_pause_white)
                        } else {
                            binding!!.playVideo.setImageResource(R.drawable.ic_podcast_play_white)
                        }
                        Handler(Looper.getMainLooper()).post(updateSeekBar)
                        Handler(Looper.getMainLooper()).postDelayed({
                            try{
                                binding!!.playController.visibility = View.GONE
                            } catch (ex:Exception){
                                LogDetail.LogEStack(ex)
                            }},4000)
                    } else{
                        binding!!.playController.visibility = View.GONE
                    }
                } catch (ex:Exception){
                    LogDetail.LogEStack(ex)
                }
            }
            binding!!.forwardVideo.setOnClickListener { simpleExoPlayer!!.seekTo(simpleExoPlayer!!.currentPosition + (10 * 1000)) }
            binding!!.backwardVideo.setOnClickListener { simpleExoPlayer!!.seekTo(simpleExoPlayer!!.currentPosition - (10 * 1000)) }
            binding!!.playVideo.setOnClickListener {
                if (simpleExoPlayer!!.isPlaying) {
                    binding!!.playVideo.setImageResource(R.drawable.ic_podcast_play_white)
                    simpleExoPlayer!!.playWhenReady = false
                } else {
                    AudioTracker.init(this, "FeedDetails", AudioTracker.VIDEOS, postId, object : AudioTrackerListener{
                        override fun onSuccess() {
                            binding!!.playVideo.setImageResource(R.drawable.ic_podcast_pause_white)
                            if(simpleExoPlayer!!.duration <= simpleExoPlayer!!.currentPosition){
                                simpleExoPlayer!!.seekTo(0)
                            }
                            simpleExoPlayer?.playWhenReady = true
                        }
                        override fun onFailure() {
                            try {
                                binding!!.playVideo.setImageResource(R.drawable.ic_podcast_play_white)
                                simpleExoPlayer?.playWhenReady = false
                            } catch (ex:Exception){ LogDetail.LogEStack(ex) }
                        }
                    })
                }
            }
            binding!!.playController.setOnClickListener {  }
            Handler(Looper.getMainLooper()).post(updateSeekBar)
            AudioTracker.init(this, "FeedDetails", AudioTracker.VIDEOS, postId, object : AudioTrackerListener{
                override fun onSuccess() { simpleExoPlayer?.playWhenReady = true }
                override fun onFailure() {
                    try {
                        simpleExoPlayer?.playWhenReady = false
                    } catch (ex:Exception){ LogDetail.LogEStack(ex) }
                }
            })
        }
    }

    private fun buildMediaSource(uri: Uri): MediaSource {
        val dataSourceFactory: com.google.android.exoplayer2.upstream.DataSource.Factory = DefaultDataSource.Factory(this)
        return ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(uri))
    }

    public override fun onStart() {
        super.onStart()
        if (Util.SDK_INT >= 24 && isVideo && !isYoutube) {
            initializePlayer()
        }
    }

    public override fun onResume() {
        super.onResume()
        if (isVideo && !isYoutube) {
            hideSystemUi()
            if (Util.SDK_INT < 24 || simpleExoPlayer == null) {
                initializePlayer()
            }
        }
    }

    public override fun onPause() {
        super.onPause()
        if (Util.SDK_INT < 24 && isVideo && !isYoutube) {
            releasePlayer()
        }
    }

    public override fun onStop() {
        super.onStop()
        if (Util.SDK_INT >= 24 && isVideo && !isYoutube) {
            releasePlayer()
        }
    }

    private fun releasePlayer() {
        if (simpleExoPlayer != null) {
            playWhenReady = simpleExoPlayer!!.playWhenReady
            playbackPosition = simpleExoPlayer!!.currentPosition
            currentWindow = simpleExoPlayer!!.currentMediaItemIndex
            simpleExoPlayer!!.release()
            simpleExoPlayer = null
        }
    }

    private fun hideSystemUi() {
        binding?.videoView!!.systemUiVisibility = (View.SYSTEM_UI_FLAG_LOW_PROFILE
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
    }

    fun sharePost(
        context: Context,
        id: String?,
        title: String?,
        imageUrl: String?,
        isWhatsApp: Boolean,
        postUrl: String
    ) {
        if (isWhatsApp) {
            try {
                if (eventsListener != null) {
                    eventsListener!!.onFeedInteraction(
                        post_source,
                        interest,
                        postId!!,
                        intent.getStringExtra("platform").toString(),
                        "whatsappShare"
                    )
                }
            } catch (ex: java.lang.Exception) {
                LogDetail.LogEStack(ex)
            }
        } else {
            try {
                if (eventsListener != null) {
                    eventsListener!!.onFeedInteraction(
                        post_source,
                        interest,
                        postId!!,
                        intent.getStringExtra("platform").toString(),
                        "share"
                    )
                }
            } catch (ex: java.lang.Exception) {
                LogDetail.LogEStack(ex)
            }
        }
        val link = FeedSdk.mFirebaseDynamicLink + "?feed_id=" + id
        if (imageUrl == null) {
            try {
                var prefix = ""
                val card = cardsMap[interest]!![position]
                if(!card.items[0].content?.title.isNullOrEmpty()){
                    prefix = HtmlCompat.fromHtml(card.items[0].content?.title?:"", HtmlCompat.FROM_HTML_MODE_LEGACY).toString()+"\n"
                } else if(!card.items[0].content?.description.isNullOrEmpty()){
                    prefix = HtmlCompat.fromHtml(card.items[0].content?.description?:"", HtmlCompat.FROM_HTML_MODE_LEGACY).toString()+"\n"
                }
                FirebaseDynamicLinks.getInstance().createDynamicLink()
                    .setLink(Uri.parse(link))
                    .setDomainUriPrefix(FeedSdk.mFirebaseDynamicLink)
                    .setAndroidParameters(AndroidParameters.Builder(context.packageName).build())
                    .setSocialMetaTagParameters(
                        SocialMetaTagParameters.Builder()
                            .setTitle(FeedSdk.appName ?: "Feed").setDescription(
                                title!!
                            ).build()
                    )
                    .buildShortDynamicLink()
                    .addOnSuccessListener { shortDynamicLink ->
                        val shortLink: String =
                            prefix + shortDynamicLink.shortLink.toString() + context.getString(R.string.dynamic_link_url_suffix) + " " + FeedSdk.appName + " " + context.getString(
                                R.string.dynamic_link_url_suffix2
                            ) + packageName
                        if (isWhatsApp) {
                            val whatsAppIntent = Intent(Intent.ACTION_SEND)
                            whatsAppIntent.type = "text/plain"
                            whatsAppIntent.setPackage("com.whatsapp")
                            whatsAppIntent.putExtra(Intent.EXTRA_TEXT, shortLink)
                            try {
                                context.startActivity(whatsAppIntent)
                            } catch (ex: ActivityNotFoundException) {
                                Toast.makeText(
                                    context,
                                    "Whatsapp have not been installed.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            val intent = Intent(Intent.ACTION_SEND)
                            intent.type = "text/plain"
                            intent.putExtra(Intent.EXTRA_TEXT, shortLink)
                            context.startActivity(Intent.createChooser(intent, "Share via"))
                        }
                    }
                    .addOnFailureListener { e ->
                        LogDetail.LogEStack(e)
                        try {
                            if (isWhatsApp) {
                                val whatsAppIntent = Intent(Intent.ACTION_SEND)
                                whatsAppIntent.type = "text/plain"
                                whatsAppIntent.setPackage("com.whatsapp")
                                whatsAppIntent.putExtra(
                                    Intent.EXTRA_TEXT,
                                    """
                                        $postUrl
                                        
                                        
                                        """.trimIndent() + Objects.requireNonNull(
                                        spUtilInstance
                                    )!!
                                        .getString(Constants.SHARE_MESSAGE)
                                            + context.getString(R.string.share_link_prefix) + context.packageName
                                )
                                try {
                                    context.startActivity(whatsAppIntent)
                                } catch (ex: ActivityNotFoundException) {
                                    Toast.makeText(
                                        context,
                                        "Whatsapp have not been installed.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } else {
                                val intent = Intent(Intent.ACTION_SEND)
                                intent.type = "text/plain"
                                intent.putExtra(
                                    Intent.EXTRA_TEXT,
                                    """
                                        $postUrl
                                        
                                        
                                        """.trimIndent() + Objects.requireNonNull(
                                        spUtilInstance
                                    )!!
                                        .getString(Constants.SHARE_MESSAGE)
                                            + context.getString(R.string.share_link_prefix) + context.packageName
                                )
                                context.startActivity(Intent.createChooser(intent, "Share via"))
                            }
                        } catch (e1: Exception) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.error_share_post),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            } catch (e: Exception) {
                LogDetail.LogEStack(e)
            }
        } else {
            try {
                FirebaseDynamicLinks.getInstance().createDynamicLink()
                    .setLink(Uri.parse(link))
                    .setDomainUriPrefix(FeedSdk.mFirebaseDynamicLink)
                    .setAndroidParameters(AndroidParameters.Builder(context.packageName).build())
                    .setSocialMetaTagParameters(
                        SocialMetaTagParameters.Builder()
                            .setTitle(FeedSdk.appName ?: "Feed").setDescription(
                                title!!
                            ).setImageUrl(Uri.parse(imageUrl)).build()
                    )
                    .buildShortDynamicLink()
                    .addOnSuccessListener { shortDynamicLink: ShortDynamicLink ->
                        val shortLink: String =
                            shortDynamicLink.shortLink.toString() + context.getString(R.string.dynamic_link_url_suffix) + " " + FeedSdk.appName + " " + context.getString(
                                R.string.dynamic_link_url_suffix2
                            ) + packageName
                        if (isWhatsApp) {
                            val whatsAppIntent = Intent(Intent.ACTION_SEND)
                            whatsAppIntent.type = "text/plain"
                            whatsAppIntent.setPackage("com.whatsapp")
                            whatsAppIntent.putExtra(Intent.EXTRA_TEXT, shortLink)
                            try {
                                context.startActivity(whatsAppIntent)
                            } catch (ex: ActivityNotFoundException) {
                                Toast.makeText(
                                    context,
                                    "Whatsapp have not been installed.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            val intent = Intent(Intent.ACTION_SEND)
                            intent.type = "text/plain"
                            intent.putExtra(Intent.EXTRA_TEXT, shortLink)
                            context.startActivity(Intent.createChooser(intent, "Share via"))
                        }
                    }
                    .addOnFailureListener { e: Exception ->
                        LogDetail.LogEStack(e)
                        try {
                            if (isWhatsApp) {
                                val whatsAppIntent = Intent(Intent.ACTION_SEND)
                                whatsAppIntent.type = "text/plain"
                                whatsAppIntent.setPackage("com.whatsapp")
                                whatsAppIntent.putExtra(
                                    Intent.EXTRA_TEXT,
                                    """
                                        $postUrl
                                        
                                        
                                        """.trimIndent() + Objects.requireNonNull(
                                        spUtilInstance
                                    )!!
                                        .getString(Constants.SHARE_MESSAGE)
                                            + context.getString(R.string.share_link_prefix) + context.packageName
                                )
                                try {
                                    context.startActivity(whatsAppIntent)
                                } catch (ex: ActivityNotFoundException) {
                                    Toast.makeText(
                                        context,
                                        "Whatsapp have not been installed.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } else {
                                val intent = Intent(Intent.ACTION_SEND)
                                intent.type = "text/plain"
                                intent.putExtra(
                                    Intent.EXTRA_TEXT,
                                    """
                                        $postUrl
                                        
                                        
                                        """.trimIndent() + Objects.requireNonNull(
                                        spUtilInstance
                                    )!!
                                        .getString(Constants.SHARE_MESSAGE)
                                            + context.getString(R.string.share_link_prefix) + context.packageName
                                )
                                context.startActivity(Intent.createChooser(intent, "Share via"))
                            }
                        } catch (e1: Exception) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.error_share_post),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            } catch (e: Exception) {
                LogDetail.LogEStack(e)
            }
        }
    }

    override fun onBackPressed() {
        try{
            if (isTaskRoot) {
                val activity = Class.forName(FeedSdk.feedTargetActivity) as Class<out Activity?>?
                startActivity(Intent(this, activity).putExtra("fromSticky", "true"))
                finish()
            } else{
                super.onBackPressed()
            }
        } catch (ex:Exception){
            super.onBackPressed()
        }
    }

    /**
     * @param dateTime TimeStamp of Post
     * @return Difference between the current time and published time of post
     * @throws ParseException if given time is not in format (yyyy-MM-dd HH:mm:ss)
     */
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

    private fun initFullscreenDialog(isYoutube: Boolean = false) {
        mFullScreenDialog =
            object : Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen) {
                override fun onBackPressed() {
                    if (showFullScreen) {
                        if(isYoutube) binding?.newsPageVideoYoutube?.exitFullScreen()
                        closeFullscreenDialog(isYoutube)
                    }
                    super.onBackPressed()
                }
            }
    }

    private fun openFullscreenDialog(isYoutube: Boolean = false) {
        if(isYoutube){
            (binding?.newsPageVideoYoutube!!.parent as ViewGroup).removeView(binding?.newsPageVideoYoutube!!)
        } else {
            binding?.closeButtonIcon!!.visibility = View.GONE
            (binding?.videoFrame!!.parent as ViewGroup).removeView(binding?.videoFrame!!)
            exo_fullscreen_icon!!.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.ic_fullscreen_skrink
                )
            )
            binding?.videoView?.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
        }
        changeOrientationToLandscape(true)
        mFullScreenDialog!!.addContentView(
            if(isYoutube) binding?.newsPageVideoYoutube!! else binding?.videoFrame!!,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        if(isYoutube) {
            try{
                youtubePlayer?.play()
            } catch (ex:Exception){}
        }
        showFullScreen = true
        mFullScreenDialog!!.show()
    }

    private fun closeFullscreenDialog(isYoutube: Boolean = false) {
        changeOrientationToLandscape(false)
        if(isYoutube){
            (binding?.newsPageVideoYoutube!!.parent as ViewGroup).removeView(binding?.newsPageVideoYoutube!!)
            (findViewById<View>(R.id.main_frame) as FrameLayout).addView(binding?.newsPageVideoYoutube!!)
        } else{
            binding?.detailsCloseButton!!.visibility = View.VISIBLE
            (binding?.videoFrame!!.parent as ViewGroup).removeView(binding?.videoFrame!!)
            (findViewById<View>(R.id.main_frame) as FrameLayout).addView(binding?.videoFrame!!)
            exo_fullscreen_icon!!.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.ic_fullscreen_expand
                )
            )
            binding?.videoView?.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
        }
        showFullScreen = false
        mFullScreenDialog!!.dismiss()
    }

    /**
     * Changes the Orientation
     * @param shouldLandscape
     */
    private fun changeOrientationToLandscape(shouldLandscape: Boolean) {
        requestedOrientation = if (shouldLandscape) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    interface BlogDetailsFragmentListener {
        fun onClose()
        fun onDismissComment()
        fun onPostComment(comment: String?)
    }

    override fun onDestroy() {
        super.onDestroy()
        storeData()
        binding = null
    }

    companion object {
        fun makeTextViewResizable(
            tv: TextView?,
            maxLine: Int,
            expandText: String,
            viewMore: Boolean
        ) {
            if (tv!!.tag == null) {
                tv.tag = tv.text
            }
            val vto = tv.viewTreeObserver
            vto.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    val obs = tv.viewTreeObserver
                    obs.removeGlobalOnLayoutListener(this)
                    if (maxLine == 0) {
                        val lineEndIndex = tv.layout.getLineEnd(0)
                        val text = tv.text.subSequence(0, lineEndIndex - expandText.length + 1)
                            .toString() + " " + expandText
                        tv.text = text
                        tv.movementMethod = LinkMovementMethod.getInstance()
                        tv.setText(
                            addClickablePartTextViewResizable(
                                HtmlCompat.fromHtml(tv.text.toString(), HtmlCompat.FROM_HTML_MODE_LEGACY), tv, maxLine, expandText,
                                viewMore
                            ), TextView.BufferType.SPANNABLE
                        )
                    } else if (maxLine > 0 && tv.lineCount >= maxLine) {
                        val lineEndIndex = tv.layout.getLineEnd(maxLine - 1)
                        val text = tv.text.subSequence(0, lineEndIndex - expandText.length + 1)
                            .toString() + " " + expandText
                        tv.text = text
                        tv.movementMethod = LinkMovementMethod.getInstance()
                        tv.setText(
                            addClickablePartTextViewResizable(
                                HtmlCompat.fromHtml(tv.text.toString(), HtmlCompat.FROM_HTML_MODE_LEGACY), tv, maxLine, expandText,
                                viewMore
                            ), TextView.BufferType.SPANNABLE
                        )
                    } else {
                        val lineEndIndex = tv.layout.getLineEnd(tv.layout.lineCount - 1)
                        val text =
                            tv.text.subSequence(0, lineEndIndex).toString() + " " + expandText
                        tv.text = text
                        tv.movementMethod = LinkMovementMethod.getInstance()
                        tv.setText(
                            addClickablePartTextViewResizable(
                                HtmlCompat.fromHtml(tv.text.toString(), HtmlCompat.FROM_HTML_MODE_LEGACY), tv, lineEndIndex, expandText,
                                viewMore
                            ), TextView.BufferType.SPANNABLE
                        )
                    }
                }
            })
        }

        private fun addClickablePartTextViewResizable(
            strSpanned: Spanned, tv: TextView?,
            maxLine: Int, spanableText: String, viewMore: Boolean
        ): SpannableStringBuilder {
            val str = strSpanned.toString()
            val ssb = SpannableStringBuilder(strSpanned)
            if (str.contains(spanableText)) {
                ssb.setSpan(object : MySpannable(false) {
                    override fun onClick(widget: View) {
                        if (viewMore) {
                            tv!!.layoutParams = tv.layoutParams
                            tv.setText(tv.tag.toString(), TextView.BufferType.SPANNABLE)
                            tv.invalidate()
                            makeTextViewResizable(tv, -1, "See Less", false)
                        } else {
                            tv!!.layoutParams = tv.layoutParams
                            tv.setText(tv.tag.toString(), TextView.BufferType.SPANNABLE)
                            tv.invalidate()
                            makeTextViewResizable(tv, 3, ".. See More", true)
                        }
                    }
                }, str.indexOf(spanableText), str.indexOf(spanableText) + spanableText.length, 0)
            }
            return ssb
        }
    }

    @SuppressLint("CommitPrefEdits")
    fun storeData() {
        if (presentPostDetailsModel?.post?.presentUrl == "" || presentPostDetailsModel?.post?.presentTimeStamp == (0).toLong()) {
            return
        }
        try {
            var totalDuration: Int = 0
            var duration: Int = 0
            if (isVideo) {
                if (isYoutube) {
                    totalDuration = this.totalDuration
                    duration = this.duration
                } else {
                    totalDuration = (binding?.videoView?.player?.duration!! / 1000).toInt()
                    duration = (binding?.videoView?.player?.currentPosition!! / 1000).toInt()
                }
            }
            val postView = PostView(
                FeedSdk.sdkCountryCode ?: "in",
                feed_type,
                isVideo,
                presentPostDetailsModel?.post?.languageString,
                Constants.getInterestsString(presentPostDetailsModel?.post?.interests),
                postId,
                post_source,
                presentPostDetailsModel?.post?.publisherId,
                isVideo,
                presentPostDetailsModel?.post?.publisherName,
                if (isVideo) totalDuration else null,
                if (isVideo) duration else null,
                postId+"NewsFeedPageActivity"
            )
            val sharedPreferences: SharedPreferences = getSharedPreferences("postImpressions", Context.MODE_PRIVATE)
            val postPreferences: SharedPreferences = getSharedPreferences("postIdsDb", Context.MODE_PRIVATE)
            ApiPostImpression().storeImpression(sharedPreferences, postPreferences, presentPostDetailsModel?.post?.presentUrl!!, presentPostDetailsModel?.post?.presentTimeStamp!!, postView)
        } catch (ex: java.lang.Exception) {
            LogDetail.LogEStack(ex)
        }
    }

    private fun setFonts(view: View?) {
        Card.setFontFamily(binding?.tvPublisherImage, true)
        Card.setFontFamily(binding?.tvPublisher)
        Card.setFontFamily(binding?.tvTime)
        Card.setFontFamily(binding?.relatedVideoText)
        Card.setFontFamily(binding?.relatedPostText)
        Card.setFontFamily(binding?.relatedPostDescription)
        Card.setFontFamily(binding?.newsPageCategory, true)
        Card.setFontFamily(binding?.newsPagePublisherName, true)
        Card.setFontFamily(binding?.newsPagePostedOn)
        Card.setFontFamily(binding?.newsPageTitle, true)
        Card.setFontFamily(binding?.newsPageStats)
        Card.setFontFamily(binding?.newsPageComments, true)
        Card.setFontFamily(binding?.newsItemStats)
        Card.setFontFamily(binding?.tvLikes)
        Card.setFontFamily(binding?.tvComments)
        Card.setFontFamily(binding?.tvShare)
        Card.setFontFamily(binding?.tvWhatsappShare)
        Card.setFontFamily(view?.findViewById(R.id.podcastBottomTitle) as TextView)
        Card.setFontFamily(view?.findViewById(R.id.podcastBottomPublisherName) as TextView)
    }

}
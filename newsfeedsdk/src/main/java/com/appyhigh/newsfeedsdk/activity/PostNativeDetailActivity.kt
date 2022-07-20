package com.appyhigh.newsfeedsdk.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.core.text.PrecomputedTextCompat
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.adapter.FeedCommentAdapter
import com.appyhigh.newsfeedsdk.adapter.FeedNextPostAdapter
import com.appyhigh.newsfeedsdk.apicalls.*
import com.appyhigh.newsfeedsdk.apiclient.Endpoints
import com.appyhigh.newsfeedsdk.callbacks.FeedReactionListener
import com.appyhigh.newsfeedsdk.callbacks.GlideCallbackListener
import com.appyhigh.newsfeedsdk.callbacks.OnRelatedPostClickListener
import com.appyhigh.newsfeedsdk.databinding.ActivityPostNativeDetailBinding
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.fragment.FeedMenuBottomSheetFragment
import com.appyhigh.newsfeedsdk.fragment.NonNativeCommentBottomSheet
import com.appyhigh.newsfeedsdk.model.*
import com.appyhigh.newsfeedsdk.model.feeds.Card
import com.appyhigh.newsfeedsdk.utils.*
import com.google.android.flexbox.FlexboxLayout
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.dynamiclinks.DynamicLink
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.firebase.dynamiclinks.ShortDynamicLink
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import com.google.gson.Gson
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import java.lang.ref.WeakReference
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*


class PostNativeDetailActivity : AppCompatActivity() {

    private var binding: ActivityPostNativeDetailBinding? = null
    private var postId: String? = null
    private var imageUrl = ""
    private var nonNativeCommentBottomSheet: NonNativeCommentBottomSheet? = null
    private val TAG = PostNativeDetailActivity::class.java.name
    private var comments: ArrayList<FeedComment> = ArrayList<FeedComment>()
    private var reacted = ""
    var likes = 0
    var position = 0
    var commentsCount = 0
    private var post_source = "unknown"
    private var feed_type = "unknown"
    private var interest = ""
    var presentPostDetailsModel: PostDetailsModel? = null
    var totalDuration = 0
    var duration = 0
    var mInterstitialAd: InterstitialAd? = null
    var nextCardPostId: String = ""
    var nextCardAlreadyExists: Boolean = false
    var nextCardNative: Boolean = false
    private var btwArticleLayout: LinearLayout? = null
    private var loadTrace = FirebasePerformance.startTrace("NativePage-loadTime")
    private var timeSpentTrace: Trace? = null
    private var adUtilsSdk = AdUtilsSDK()
    var timer: Timer? = null
    var nativeTimer: Timer? = null
    var adsModel = ApiConfig().getAdsModel(this)
    private var showArticleBtwAd = false
    private var showArticleEndAd = false

    class LoadAdTask(var function: () -> (Unit)) : TimerTask() {
        override fun run() {
            function()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostNativeDetailBinding.inflate(layoutInflater)
        val view = binding?.root
        setContentView(view)
        val loadedView: View = findViewById(android.R.id.content)
        FirstDrawListener.registerFirstDrawListener(loadedView,
            object : FirstDrawListener.OnFirstDrawCallback {
                override fun onDrawingStart() {}
                override fun onDrawingFinish() {
                    loadTrace.stop()
                    timeSpentTrace = FirebasePerformance.startTrace("NativePage-TimeSpent")
                }

            })
        showAds()
        setFonts(view)
        binding!!.pbLoading.visibility = View.VISIBLE
        interest = intent.getStringExtra(Constants.INTEREST) ?: "unknown"
        position = intent.getIntExtra(Constants.POSITION, 0)
        Constants.postDetailPageNo = 0
        if (intent.hasExtra(Constants.INTEREST)) {
            Constants.postDetailCards = ArrayList()
        }
        if (position == 0) {
            Constants.nativePageCount = 1
        }
        if (intent.hasExtra(Constants.ALREADY_EXISTS) && position < Constants.postDetailCards.size) {
            postId = intent.getStringExtra(Constants.POST_ID)
            handleResults(Constants.postDetailCards[position], true)
        } else if (intent != null) {
            onIntent(intent)
        }
        if (Constants.postDetailCards.isNotEmpty() && position > 0) {
            binding?.prevCard?.visibility = View.VISIBLE
        }
        binding?.prevCard?.setOnClickListener {
            if (Constants.postDetailCards.size > position) {
                val intent = if (Constants.postDetailCards[position - 1].post!!.isNative!!) {
                    Intent(this, PostNativeDetailActivity::class.java)
                } else {
                    Intent(this, NewsFeedPageActivity::class.java)
                }
                intent.putExtra(Constants.POSITION, position - 1)
                intent.putExtra(Constants.ALREADY_EXISTS, true)
                intent.putExtra(Constants.POST_ID, postId)
                intent.putExtra(
                    Constants.LANGUAGE,
                    intent.getStringExtra(Constants.LANGUAGE) ?: "en"
                )
                intent.putExtra("from_app", true)
                startActivity(intent)
                finish()
            }
        }
        binding?.backBtn?.setOnClickListener { onBackPressed() }

        binding?.newsItemMoreOption?.setOnClickListener {
            try{
                val reportBottomSheet = FeedMenuBottomSheetFragment.newInstance(
                    presentPostDetailsModel?.post?.publisherContactUs?:"",
                    postId!!
                )
                reportBottomSheet.show(supportFragmentManager, "reportBottomSheet")
            } catch (ex:Exception){
                LogDetail.LogEStack(ex)
            }
        }
        binding?.tvCommentsExplore?.setOnClickListener {
            try {
                if (SpUtil.eventsListener != null) {
                    SpUtil.eventsListener!!.onFeedInteraction(
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
                object : NewsFeedPageActivity.BlogDetailsFragmentListener {
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
                if (SpUtil.eventsListener != null) {
                    SpUtil.eventsListener!!.onFeedInteraction(
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
            if (!reacted.equals("none", ignoreCase = true) && !reacted.equals(
                    "false",
                    ignoreCase = true
                )
            ) {
                reacted = "none"
                try {
                    if (Constants.cardsMap[interest] != null) {
                        val card = Constants.cardsMap[interest]!![position]
                        var likeCount = card.items[0].reactionsCount?.likeCount!!
                        likeCount -= 1
                        card.items[0].reactionsCount?.likeCount = likeCount
                        card.items[0].isReacted = Constants.ReactionType.NONE.toString()
                        Constants.cardsMap[interest]!![position] = card
                    }
                    val pos = if (Constants.postDetailCards.size == 1) 0 else position
                    var likeCount = Constants.postDetailCards[pos].post?.reactionCount?.likeCount!!
                    likeCount -= 1
                    Constants.postDetailCards[pos].post?.reactionCount?.likeCount = likeCount
                    Constants.postDetailCards[pos].post?.isReacted =
                        Constants.ReactionType.NONE.toString()
                } catch (e: Exception) {
                    LogDetail.LogEStack(e)
                }
                likes -= 1
            } else {
                reacted = "like"
                try {
                    if (Constants.cardsMap[interest] != null) {
                        val card = Constants.cardsMap[interest]!![position]
                        var likeCount = card.items[0].reactionsCount?.likeCount!!
                        likeCount += 1
                        card.items[0].reactionsCount?.likeCount = likeCount
                        card.items[0].isReacted = Constants.ReactionType.LIKE.toString()
                        Constants.cardsMap[interest]!![position] = card
                    }
                    val pos = if (Constants.postDetailCards.size == 1) 0 else position
                    var likeCount = Constants.postDetailCards[pos].post?.reactionCount?.likeCount!!
                    likeCount += 1
                    Constants.postDetailCards[pos].post?.reactionCount?.likeCount = likeCount
                    Constants.postDetailCards[pos].post?.isReacted =
                        Constants.ReactionType.LIKE.toString()
                } catch (e: Exception) {
                    LogDetail.LogEStack(e)
                }
                likes += 1
            }
            setLikePost()
            postReaction()
        }
        binding?.tvLikes?.setOnLongClickListener {
            binding?.tvLikes?.showReactionsPopUpWindow(
                null,
                object : FeedReactionListener {
                    override fun onReaction(item: Post?, reactionType: Constants.ReactionType) {
                        if (reacted.isEmpty() || reacted == "none") likes += 1
                        reacted = reactionType.toString().lowercase(Locale.getDefault())
                        postReaction()
                        setLikePost()
                    }
                },
                false,
                binding?.tvLikes!!
            )
            false
        }
        binding?.headerLike?.setOnClickListener { binding?.tvLikes?.performClick() }
        binding?.headerLike?.setOnLongClickListener {
            binding?.headerLike?.showReactionsPopUpWindow(
                null,
                object : FeedReactionListener {
                    override fun onReaction(item: Post?, reactionType: Constants.ReactionType) {
                        if (reacted.isEmpty() || reacted == "none") likes += 1
                        reacted = reactionType.toString().lowercase(Locale.getDefault())
                        postReaction()
                        setLikePost()
                    }
                },
                false,
                binding?.tvLikes!!
            )
            false
        }
        binding?.headerShare?.setOnClickListener { binding?.tvShare?.performClick() }
        binding?.tvComments?.setOnClickListener {
            val location = IntArray(2)
            binding!!.etComment.getLocationOnScreen(location)
            binding!!.scrollView.scrollTo(0, location[1])
            binding!!.etComment.requestFocus()
        }
        binding?.headerComments?.setOnClickListener { binding?.tvComments?.performClick() }
        binding!!.etComment.setOnEditorActionListener(object : TextView.OnEditorActionListener {
            override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {

                if (actionId == EditorInfo.IME_ACTION_GO) {
                    if (!binding!!.etComment.text.isNullOrEmpty()) {
                        postComment(binding!!.etComment.text.toString())
                        binding!!.etComment.setText("")
                        hideKeyboard()
                    }
                    return true
                }
                return false
            }
        })

    }

    override fun onStop() {
        super.onStop()
        if (timer != null) {
            timer?.cancel()
        }

        if (nativeTimer != null) {
            nativeTimer?.cancel()
        }
    }

    override fun onResume() {
        super.onResume()
        ApiConfig().requestAd(this, "post_detail_footer_banner", object : ConfigAdRequestListener{
            override fun onPrivateAdSuccess(webView: WebView) {
                binding?.bannerAd?.removeAllViews()
                binding?.bannerAd?.addView(webView)
            }

            override fun onAdmobAdSuccess(adId: String) {
                startBannerTimer(adId)
            }

            override fun onAdHide() {
                binding?.bannerAd?.visibility = View.GONE
            }
        }, true)
        ApiConfig().requestAd(this, "post_detail_article_top_native", object : ConfigAdRequestListener{
            override fun onPrivateAdSuccess(webView: WebView) {
                btwArticleLayout?.removeAllViews()
                btwArticleLayout?.addView(webView)
            }

            override fun onAdmobAdSuccess(adId: String) {
                showArticleBtwAd = true
                if(nativeTimer!=null) showArticleBtwAd() else startNativeTimer()
            }

            override fun onAdHide() {
                btwArticleLayout?.visibility = View.GONE
            }
        })
        ApiConfig().requestAd(this, "post_detail_article_end_native", object : ConfigAdRequestListener{
            override fun onPrivateAdSuccess(webView: WebView) {
                binding!!.articleEndNative.removeAllViews()
                binding!!.articleEndNative.addView(webView)
            }

            override fun onAdmobAdSuccess(adId: String) {
                showArticleEndAd = true
                if(nativeTimer!=null) showArticleEndAd() else startNativeTimer()
            }

            override fun onAdHide() {
                binding!!.articleEndNative.visibility = View.GONE
            }
        })
    }


    private fun showAds() {
        try {
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
                                getNextCard(nextCardPostId, nextCardAlreadyExists, nextCardNative)
                            }

                            override fun onAdFailedToShowFullScreenContent(adError: AdError?) {}

                            override fun onAdShowedFullScreenContent() {}
                        })
                }
                btwArticleLayout = LinearLayout(this)

            }
        } catch (ex: Exception) {
            LogDetail.LogEStack(ex)
        }
    }

    private fun startBannerTimer(adId: String) {
        if (timer != null) {
            timer?.cancel()
        }
        timer = Timer()
        val task: TimerTask = LoadAdTask { showAdaptiveBanner(adId) }
        timer?.scheduleAtFixedRate(task, 0, 60000)
    }

    private fun startNativeTimer() {
        if (nativeTimer != null) {
            nativeTimer?.cancel()
        }
        nativeTimer = Timer()
        val task: TimerTask = LoadAdTask {
            showArticleBtwAd()
            showArticleEndAd()
        }
        nativeTimer?.scheduleAtFixedRate(task, 0, 60000)
    }

    private fun showArticleBtwAd(){
        try{
            if(!showArticleBtwAd) return
            LogDetail.LogD(TAG, "showArticleBtwAd: ")
            adUtilsSdk.requestFeedAdWithoutInbuiltTimer(
                btwArticleLayout!!,
                R.layout.native_ad_feed,
                adsModel.postDetailArticleTopNative.admobId,
                "postNativeDetailBetween",
                object : LoadNativeAdListener {
                    override fun onAdLoadFailed() {
                        LogDetail.LogD(TAG, "onAdLoadFailed: ")
                        FirebaseAnalytics.getInstance(this@PostNativeDetailActivity).logEvent("FeedNativeBtwArticleFailure", null)
                    }

                    override fun onAdLoadSuccess() {
                        FirebaseAnalytics.getInstance(this@PostNativeDetailActivity).logEvent("FeedNativeBtwArticleSuccess", null)
                    }

                })
        } catch (ex:Exception){
            LogDetail.LogEStack(ex)
        }
    }

    private fun showArticleEndAd(){
        if(!showArticleEndAd) return
        LogDetail.LogD(TAG, "showArticleEndAd: ")
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                adUtilsSdk.requestFeedAdWithoutInbuiltTimer(
                    binding!!.articleEndNative,
                    R.layout.native_ad_feed,
                    adsModel.postDetailArticleEndNative.admobId,
                    "postNativeDetailEnd",
                    object : LoadNativeAdListener {
                        override fun onAdLoadFailed() {
                            LogDetail.LogD(TAG, "onAdLoadFailed: ")
                            FirebaseAnalytics.getInstance(this@PostNativeDetailActivity).logEvent("FeedNativeEndArticleFailure", null)
                        }

                        override fun onAdLoadSuccess() {
                            FirebaseAnalytics.getInstance(this@PostNativeDetailActivity).logEvent("FeedNativeEndArticleSuccess", null)
                        }

                    })
            } catch (ex: Exception) {
                LogDetail.LogEStack(ex)
            }
        }, 400)
    }


    private fun onIntent(intent: Intent) {
        try {
            if (intent.hasExtra("from_app")) {
                postId = intent.getStringExtra(Constants.POST_ID)
                if (intent.hasExtra(Constants.POST_SOURCE)) post_source =
                    intent.getStringExtra(Constants.POST_SOURCE) ?: "unknown"
                if (intent.hasExtra(Constants.FEED_TYPE)) feed_type =
                    intent.getStringExtra(Constants.FEED_TYPE) ?: "unknown"
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
                    if (data.toString().lowercase(Locale.getDefault())
                            .contains(Constants.FEED_ID)
                    ) {
                        if (data.getQueryParameter(Constants.FEED_ID) != null) {
                            postId = data.getQueryParameter(Constants.FEED_ID)
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

    private fun getData(postId: String) {
        FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let {
            ApiGetPostDetails().getPostDetailsEncrypted(
                Endpoints.GET_POSTS_DETAILS_ENCRYPTED,
                it,
                FeedSdk.userId,
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
                        Toast.makeText(
                            this@PostNativeDetailActivity,
                            getString(R.string.error_some_issue_occurred),
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                })
        }
    }


    private fun handleResults(postDetailsModel: PostDetailsModel, isAlreadyExists: Boolean) {
        try {
            binding!!.pbLoading.visibility = View.GONE
            presentPostDetailsModel = postDetailsModel
            if (!isAlreadyExists) {
                Constants.postDetailCards.add(postDetailsModel)
            }
            comments = postDetailsModel.post?.comments as ArrayList<FeedComment>
            binding!!.rvAllNativeComments.layoutManager = LinearLayoutManager(this)
            val commentsAdapter = FeedCommentAdapter(comments, "native")
            binding!!.rvAllNativeComments.adapter = commentsAdapter
            binding!!.rvAllNativeComments.isNestedScrollingEnabled = false
            if (intent.hasExtra("isReacted")) {
                reacted = intent.getStringExtra("isReacted")!!.toString()
            } else {
                reacted = postDetailsModel.post?.isReacted.toString()
            }
            val logoUrl: String = postDetailsModel.post?.publisherProfilePic.toString()
            val title: String = postDetailsModel.post?.content?.title.toString()
            val description: String = postDetailsModel.post?.content?.description.toString()
            formatHtmlView(description)
//            setDescription(description)
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
            binding!!.commentCount.text = commentsCount.toString()
            binding!!.headerComments.text = commentsCount.toString()
            if (commentsCount > 0) {
                binding!!.commentCountCard.visibility = View.VISIBLE
                binding!!.tvCommentsExplore.visibility = View.VISIBLE
            } else {
                binding!!.commentCountCard.visibility = View.GONE
                binding!!.tvCommentsExplore.visibility = View.GONE
            }
            val category: String = postDetailsModel.post?.platform.toString().uppercase()
            if (postDetailsModel.post?.content?.mediaList?.size!! > 0) {
                imageUrl = postDetailsModel.post?.content?.mediaList!![0]
            }
            setLikePost()
            try {
                publishedOn = getTime(publishedOn)
                LogDetail.LogD("PublishedOn", publishedOn)
            } catch (e: ParseException) {
                LogDetail.LogD("PublishedOn", e.message.toString())
                LogDetail.LogEStack(e)
            }
            binding?.tvTime?.text = publishedOn
            binding?.tvPublisher?.text = publisherName
            binding?.newsPageTitle!!.text = HtmlCompat.fromHtml(
                HtmlCompat.fromHtml(title, HtmlCompat.FROM_HTML_MODE_LEGACY).toString(),
                HtmlCompat.FROM_HTML_MODE_LEGACY
            ).toString()
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

            Constants.loadImageFromGlide(
                this,
                imageUrl,
                binding?.bgImage,
                object : GlideCallbackListener {
                    override fun onSuccess(drawable: Drawable?) {
                        try {
                            binding?.bgImage?.setImageDrawable(drawable)
                        } catch (ex: Exception) {
                            LogDetail.LogEStack(ex)
                        }
                    }

                    override fun onFailure() {
                        Picasso.get()
                            .load(imageUrl)
                            .noFade()
                            .into(binding!!.bgImage, object : Callback {
                                override fun onSuccess() {
                                    LogDetail.LogD("TAG", "onSuccess: Picasso " + logoUrl)
                                }

                                override fun onError(e: java.lang.Exception) {
                                    LogDetail.LogEStack(e)
                                    binding?.bgImage?.setImageResource(R.drawable.placeholder)
                                }
                            })
                    }

                })
            val topics = postDetailsModel.post?.tags!!
            if (topics.isEmpty()) {
                binding!!.flexboxLayout.visibility = View.GONE
            } else {
                binding!!.flexboxLayout.visibility = View.VISIBLE
                setViewInFlexBox(topics, binding!!.flexboxLayout)
            }
            binding?.tvShare?.setOnClickListener { v ->
                sharePost(v.context, postId, title, imageUrl, false, url)

            }
            try {
                if (intent.getBooleanExtra("onSharePostClicked", false)) {
                    binding?.tvShare?.performClick()
                }
            } catch (ex: Exception) {
                LogDetail.LogEStack(ex)
            }
            binding?.tvWhatsappShare?.setOnClickListener { v ->
                sharePost(v.context, postId, title, imageUrl, true, url)
            }
            if (intent.hasExtra("isComment") && intent.getBooleanExtra("isComment", false)) {
                binding?.tvComments?.performClick()
            }
            setRecommendedList(postDetailsModel, isAlreadyExists)
            try {
                if (postDetailsModel.post?.additional_data?.next_post != null) {
                    val nextPost = postDetailsModel.post!!.additional_data.next_post
                    binding?.nextCard?.visibility = View.VISIBLE
                    binding?.nextCard?.setOnClickListener {
                        getNextCard(
                            nextPost!!.post_id,
                            isAlreadyExists,
                            postDetailsModel.post?.additional_data?.next_post!!.isNative!!
                        )
                    }
                }
            } catch (ex: Exception) {
                LogDetail.LogEStack(ex)
            }
        } catch (ex: Exception) {
            LogDetail.LogEStack(ex)
        }
    }

    @SuppressLint("SetJavaScriptEnabled", "ResourceType")
    private fun formatHtmlView(description: String) {
        try {
            if (ApiConfig().checkShowAds(this)) {
                binding!!.nativeBtwArticle.addView(btwArticleLayout)
            }
            val document = Jsoup.parse(description).body()
            document.select("img").first()?.remove()
            document.select("img").attr("width", "100%") // find all images and set with to 100%
            document.select("figure")
                .attr("style", "width: 80%") // find all figures and set with to 80%
            document.select("iframe")
                .attr("style", "width: 100%") // find all iframes and set with to 100%
            var desc = document.toString().replace("<![CDATA[", "")
                .replace("]]>", "")
                .replaceFirst(Regex("<img.+/(img)*>"), "")
                .replace(imageUrl, "")
            if (FeedSdk.sdkTheme != "light") {
                if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                    WebSettingsCompat.setForceDark(
                        binding!!.webview.settings,
                        WebSettingsCompat.FORCE_DARK_ON
                    )
                }
//                desc = "<head><style> body { background-color:${getString(R.color.feedBackground)} </style></head>$desc"
            }
            LogDetail.LogD(TAG, "formatHtmlView: $desc")
            LogDetail.LogD(TAG, "formatPostId: ${postId.toString()}")
            WebView.setWebContentsDebuggingEnabled(true)
            binding!!.webview.loadDataWithBaseURL(null, desc, "text/html", "UTF-8", null)
            binding!!.webview.settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.SINGLE_COLUMN
            binding!!.webview.settings.javaScriptEnabled = true
            binding!!.webview.isVerticalScrollBarEnabled = false
            binding!!.webview.settings.defaultFontSize = 15
            binding!!.webview.visibility = View.VISIBLE
        } catch (ex: Exception) {
            LogDetail.LogEStack(ex)
        }
    }

    private fun getNextCard(postId: String, isAlreadyExists: Boolean, isNative: Boolean) {
        if (mInterstitialAd != null) {
            nextCardPostId = postId
            nextCardAlreadyExists = isAlreadyExists
            nextCardNative = isNative
            mInterstitialAd?.show(this)
        } else {
            Constants.nativePageCount = Constants.nativePageCount + 1
            val intent = if (isNative) {
                Intent(this, PostNativeDetailActivity::class.java)
            } else {
                Intent(this, NewsFeedPageActivity::class.java)
            }
            if (isAlreadyExists && Constants.postDetailCards.size > position + 1) {
                intent.putExtra(Constants.ALREADY_EXISTS, true)
                intent.putExtra(Constants.POSITION, position + 1)
            } else {
                intent.putExtra(Constants.POSITION, Constants.postDetailCards.size)
            }
            intent.putExtra(Constants.LANGUAGE, intent.getStringExtra(Constants.LANGUAGE) ?: "en")
            intent.putExtra(Constants.POST_ID, postId)
            intent.putExtra("from_app", true)
            startActivity(intent)
            finish()
        }
    }

    fun postReaction() {
        try {
            var reactionType = Constants.ReactionType.LIKE
            when (reacted.lowercase(Locale.getDefault())) {
                "like" -> {
                    reactionType = Constants.ReactionType.LIKE
                }
                "wow" -> {
                    reactionType = Constants.ReactionType.WOW
                }
                "love" -> {
                    reactionType = Constants.ReactionType.LOVE
                }
                "angry" -> {
                    reactionType = Constants.ReactionType.ANGRY
                }
                "laugh" -> {
                    reactionType = Constants.ReactionType.LAUGH
                }
                "sad" -> {
                    reactionType = Constants.ReactionType.SAD
                }
                "none" -> {
                    reactionType = Constants.ReactionType.NONE
                }
            }
            FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let {
                ApiReactPost().reactPostEncrypted(
                    Endpoints.REACT_POST_ENCRYPTED,
                    it,
                    FeedSdk.userId,
                    postId!!,
                    reactionType
                )
            }
        } catch (ex: Exception) {
            LogDetail.LogEStack(ex)
        }
    }

    private fun postComment(comment: String) {
        FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let {
            ApiCommentPost().postCommentEncrypted(
                Endpoints.COMMENT_POST_ENCRYPTED,
                it,
                postId!!,
                "text",
                comment,
                object : ApiCommentPost.PostCommentResponse {
                    override fun onSuccess(feedCommentResponseWrapper: FeedCommentResponseWrapper) {
                        handlePostResults(feedCommentResponseWrapper)
                    }
                })
        }
    }

    private fun handlePostResults(feedCommentResponse: FeedCommentResponseWrapper) {
        try {
            commentsCount += 1
            binding!!.commentCount.text = commentsCount.toString()
            binding!!.headerComments.text = commentsCount.toString()
            if (commentsCount > 0) {
                binding!!.commentCountCard.visibility = View.VISIBLE
                binding!!.tvCommentsExplore.visibility = View.VISIBLE
            } else {
                binding!!.commentCountCard.visibility = View.GONE
                binding!!.tvCommentsExplore.visibility = View.GONE
            }
            try {
                FeedSdk.areContentsModified[intent.getStringExtra(Constants.SCREEN_TYPE)!!] = true
            } catch (ex: Exception) {
                FeedSdk.areContentsModified[Constants.FEED] = true
                LogDetail.LogEStack(ex)
            }
            if (Constants.cardsMap[interest] != null) {
                val card = Constants.cardsMap[interest]!![position]
                var commentsCount = card.items[0].appComments!!
                commentsCount += 1
                card.items[0].appComments = commentsCount
                Constants.cardsMap[interest]!![position] = card
            }
            val pos = if (Constants.postDetailCards.size == 1) 0 else position
            var commentsCount = Constants.postDetailCards[pos].post?.appComments!!
            commentsCount += 1
            Constants.postDetailCards[pos].post?.appComments = commentsCount
//            binding?.newsItemStats!!.text = "$likes Likes  & $commentsCount Comments"
        } catch (e: Exception) {
            LogDetail.LogEStack(e)
        }
        feedCommentResponse.result?.comment?.let {
            nonNativeCommentBottomSheet?.updateComments(
                it
            )
        }
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
                if (SpUtil.eventsListener != null) {
                    SpUtil.eventsListener!!.onFeedInteraction(
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
                if (SpUtil.eventsListener != null) {
                    SpUtil.eventsListener!!.onFeedInteraction(
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
        val link = FeedSdk.mFirebaseDynamicLink + "?feed_id=" + id + "&is_native=true"
        if (imageUrl == null) {
            try {
                var prefix = ""
                val card = Constants.cardsMap[interest]!![position]
                if(!card.items[0].content?.title.isNullOrEmpty()){
                    prefix = HtmlCompat.fromHtml(card.items[0].content?.title?:"", HtmlCompat.FROM_HTML_MODE_LEGACY).toString()+"\n"
                } else if(!card.items[0].content?.description.isNullOrEmpty()){
                    prefix = HtmlCompat.fromHtml(card.items[0].content?.description?:"", HtmlCompat.FROM_HTML_MODE_LEGACY).toString()+"\n"
                }
                FirebaseDynamicLinks.getInstance().createDynamicLink()
                    .setLink(Uri.parse(link))
                    .setDomainUriPrefix(FeedSdk.mFirebaseDynamicLink)
                    .setAndroidParameters(
                        DynamicLink.AndroidParameters.Builder(context.packageName).build()
                    )
                    .setSocialMetaTagParameters(
                        DynamicLink.SocialMetaTagParameters.Builder()
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
                                        SpUtil.spUtilInstance
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
                                        SpUtil.spUtilInstance
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
                    .setAndroidParameters(
                        DynamicLink.AndroidParameters.Builder(context.packageName).build()
                    )
                    .setSocialMetaTagParameters(
                        DynamicLink.SocialMetaTagParameters.Builder()
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
                                        SpUtil.spUtilInstance
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
                                        SpUtil.spUtilInstance
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

    private fun setViewInFlexBox(topics: List<String>, flexboxLayout: FlexboxLayout) {
        try {
            flexboxLayout.removeAllViews()
            var count = 0
            for (topic in topics) {
                val topicView = TextView(flexboxLayout.context)
                topicView.text = topic
                topicView.setBackgroundResource(R.drawable.bg_native_page_tags)
                topicView.setTextColor(Color.parseColor("#7f8386"))
                topicView.gravity = Gravity.START
                topicView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                topicView.setTypeface(FeedSdk.font, Typeface.BOLD)
                val layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                layoutParams.setMargins(0, 15, 20, 15)
                topicView.layoutParams = layoutParams
                topicView.setPadding(20, 20, 20, 20)
                topicView.setOnClickListener {
                    val intent =
                        Intent(flexboxLayout.context, FeedsActivity::class.java)
                    intent.putExtra("tag", topic)
                    intent.putExtra(Constants.FEED_TYPE, "explore_trends")
                    intent.putExtra(Constants.POST_SOURCE, "explore_trends")
                    intent.putExtra(Constants.INTEREST, "hashtagActivity")
                    flexboxLayout.context.startActivity(intent)
                }
                flexboxLayout.addView(topicView)
                count++
                if (count > 5) {
                    break
                }
            }
        } catch (e: Exception) {
            LogDetail.LogEStack(e)
        }
    }

    private fun setRecommendedList(postDetailsModel: PostDetailsModel, isAlreadyExists: Boolean) {
        try {
            if (postDetailsModel.post!!.additional_data!!.related_post_list.isNullOrEmpty()) {
                return
            }
            binding?.relatedPostLayout?.visibility = View.VISIBLE
            val nextPostAdapter =
                FeedNextPostAdapter(postDetailsModel.post!!.additional_data!!.related_post_list as ArrayList<PostDetailsModel.NextPost>,
                    object : OnRelatedPostClickListener {
                        override fun onPostClick(postId: String, isNative: Boolean) {
                            getNextCard(postId, isAlreadyExists, isNative)
                        }

                        override fun onSharePost(
                            postId: String,
                            title: String,
                            imageUrl: String,
                            isWhatsapp: Boolean,
                            url: String
                        ) {
                            sharePost(
                                this@PostNativeDetailActivity,
                                postId,
                                title,
                                imageUrl,
                                isWhatsapp,
                                url
                            )
                        }
                    })
            binding!!.rvAllNativeRelated.apply {
                adapter = nextPostAdapter
                layoutManager = LinearLayoutManager(this@PostNativeDetailActivity)
            }
            binding!!.rvAllNativeRelated.isNestedScrollingEnabled = false
        } catch (ex: java.lang.Exception) {
            LogDetail.LogEStack(ex)
        }
    }

    private fun setLikePost() {
        binding?.tvLikes?.setImageDrawable(
            Converters().getDisplayImageNative(
                reacted,
                this,
                false, "blue"
            )
        )
        binding?.headerLike?.setCompoundDrawablesWithIntrinsicBounds(
            Converters().getDisplayImageNative(
                reacted,
                this,
                false,
                "white"
            ), null, null, null
        )
        binding?.headerLike?.text = likes.toString()
    }

    private fun hideKeyboard() {
        try {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding?.etComment?.windowToken, 0)
        } catch (ex: Exception) {
            LogDetail.LogEStack(ex)
        }
    }

    override fun onBackPressed() {
        try {
            if (isTaskRoot) {
                val activity = Class.forName(FeedSdk.feedTargetActivity) as Class<out Activity?>?
                startActivity(Intent(this, activity).putExtra("fromSticky", "true"))
                finish()
            } else {
                super.onBackPressed()
            }
        } catch (ex: Exception) {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        storeData()
        binding = null
        timeSpentTrace?.stop()
        if (timer != null) {
            timer?.cancel()
        }
        if (nativeTimer != null) {
            nativeTimer?.cancel()
        }
    }

    @SuppressLint("CommitPrefEdits")
    fun storeData() {
        if (presentPostDetailsModel?.post?.presentUrl == "" || presentPostDetailsModel?.post?.presentTimeStamp == (0).toLong()) {
            return
        }
        try {
            val postView = PostView(
                FeedSdk.sdkCountryCode ?: "in",
                feed_type,
                false,
                presentPostDetailsModel?.post?.languageString,
                Constants.getInterestsString(presentPostDetailsModel?.post?.interests),
                postId,
                post_source,
                presentPostDetailsModel?.post?.publisherId,
                false,
                presentPostDetailsModel?.post?.publisherName,
                null,
                null
            )
            val postImpressions = ArrayList<PostView>()
            postImpressions.add(postView)
            val postImpressionsModel = PostImpressionsModel(
                presentPostDetailsModel?.post?.presentUrl!!,
                postImpressions,
                presentPostDetailsModel?.post?.presentTimeStamp!!
            )
            val gson = Gson()
            val sharedPrefs = getSharedPreferences("postImpressions", Context.MODE_PRIVATE)
            val postImpressionString = gson.toJson(postImpressionsModel)
            sharedPrefs.edit().putString(postId.toString(), postImpressionString).apply()
            FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let {
                ApiPostImpression().addPostImpressionsEncrypted(
                    Endpoints.POST_IMPRESSIONS_ENCRYPTED,
                    it,
                    this
                )
            }
        } catch (ex: java.lang.Exception) {
            LogDetail.LogEStack(ex)
        }
    }

    private fun showAdaptiveBanner(adId: String) {
        runOnUiThread {
            LogDetail.LogD(TAG, "showAdaptiveBanner: ")
            try {
                val adView = AdView(this)
                adView.adUnitId =adId
                adView.adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        super.onAdLoaded()
                        FirebaseAnalytics.getInstance(this@PostNativeDetailActivity)
                            .logEvent("FeedFooterBannerSuccess", null)
                        binding?.bannerAd?.visibility = View.VISIBLE
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        super.onAdFailedToLoad(loadAdError)
                        LogDetail.LogD(TAG, "onAdFailedToLoad: " + loadAdError.message)
                        FirebaseAnalytics.getInstance(this@PostNativeDetailActivity).logEvent("FeedFooterBannerFailure", null)
                    }

                    override fun onAdClicked() {
                        super.onAdClicked()
                    }
                }
                try {
                    binding!!.bannerAd.removeAllViews()
                } catch (ex: Exception) {
                }
                binding?.bannerAd?.addView(adView)
                val adRequest: AdRequest = AdRequest.Builder().build()
                val adSize = getAdSize()
                adView.adSize = adSize
                adView.loadAd(adRequest)
            } catch (ex: java.lang.Exception) {
                LogDetail.LogEStack(ex)
            }
        }
    }

    private fun getAdSize(): AdSize? {
        val display = windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)
        val widthPixels = outMetrics.widthPixels.toFloat()
        val density = outMetrics.density
        val adWidth = (widthPixels / density).toInt()
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth)
    }

    private fun setFonts(view: View?) {
        Card.setFontFamily(binding?.tvPublisherImage, true)
        Card.setFontFamily(binding?.tvPublisher, true)
        Card.setFontFamily(binding?.tvTime)
        Card.setFontFamily(binding?.headerLike)
        Card.setFontFamily(binding?.headerComments)
        Card.setFontFamily(binding?.tvCommentTitle, true)
        Card.setFontFamily(binding?.commentCount, true)
        Card.setFontFamily(binding?.newsPageTitle, true)
        Card.setFontFamily(binding?.tvCommentsExplore)
        Constants.setFontFamily(binding?.etComment)
        Card.setFontFamily(binding?.tvRelatedPostsTitle, true)
//        try{
//            if(FeedSdk.font==null){
//                binding!!.webview.settings.fixedFontFamily = ResourcesCompat.getFont(view.context,  R.font.roboto_regular)
//            } else{
//                binding!!.webview.settings.fixedFontFamily = FeedSdk.font
//            }
//        } catch (ex:java.lang.Exception){
//            LogDetail.LogEStack(ex)
//        }
    }

    private fun TextView.getTextLineCount(text: String, lineCount: (Int) -> (Unit)) {
        val params: PrecomputedTextCompat.Params = TextViewCompat.getTextMetricsParams(this)
        val ref: WeakReference<TextView>? = WeakReference(this)

        GlobalScope.launch(Dispatchers.Default) {
            val text = PrecomputedTextCompat.create(text, params)
            GlobalScope.launch(Dispatchers.Main) {
                ref?.get()?.let { textView ->
                    TextViewCompat.setPrecomputedText(textView, text)
                    lineCount.invoke(textView.lineCount)
                }
            }
        }
    }

}

interface LoadNativeAdListener {
    fun onAdLoadFailed()
    fun onAdLoadSuccess()
}
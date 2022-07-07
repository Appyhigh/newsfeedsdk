package com.appyhigh.newsfeedsdk.activity

import android.annotation.SuppressLint
import android.content.*
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.text.HtmlCompat
import androidx.fragment.app.FragmentActivity
import com.appyhigh.newsfeedsdk.BuildConfig
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.Constants.ALREADY_EXISTS
import com.appyhigh.newsfeedsdk.Constants.POST_ID
import com.appyhigh.newsfeedsdk.Constants.cardsMap
import com.appyhigh.newsfeedsdk.Constants.setDrawableColor
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.apicalls.ApiCommentPost
import com.appyhigh.newsfeedsdk.apicalls.ApiConfig
import com.appyhigh.newsfeedsdk.apicalls.ApiGetPostDetails
import com.appyhigh.newsfeedsdk.apicalls.ApiReactPost
import com.appyhigh.newsfeedsdk.apiclient.Endpoints
import com.appyhigh.newsfeedsdk.callbacks.FeedReactionListener
import com.appyhigh.newsfeedsdk.databinding.ActivityPodcastPlayerBinding
import com.appyhigh.newsfeedsdk.fragment.NonNativeCommentBottomSheet
import com.appyhigh.newsfeedsdk.fragment.ReportIssueDialogFragment
import com.appyhigh.newsfeedsdk.model.*
import com.appyhigh.newsfeedsdk.model.feeds.Card
import com.appyhigh.newsfeedsdk.utils.*
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.firebase.dynamiclinks.DynamicLink
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.firebase.dynamiclinks.ShortDynamicLink
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import java.util.*

class PodcastPlayerActivity : AppCompatActivity() {
    private var binding: ActivityPodcastPlayerBinding? = null
    var podcastCard: Card?=null
    var podcastPost: PostDetailsModel.Post?=null
    private var reacted = ""
    var likes = 0
    var commentsCount = 0
    var interest = ""
    var feedType = ""
    var postSource = ""
    var position = 0
    var postId = ""
    var presentTimeStamp:Long = 0
    var presentUrl = ""
    var isVideo= false
    var publisherId = ""
    var publisherName = ""
    private var nonNativeCommentBottomSheet: NonNativeCommentBottomSheet? = null
    private var comments: ArrayList<FeedComment> = ArrayList<FeedComment>()


    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPodcastPlayerBinding.inflate(layoutInflater)
        val view = binding?.root
        setContentView(view)
        setFonts()
        if(ApiConfig().checkShowAds() && Constants.checkFeedApp()){
            showAdaptiveBanner(this, Constants.getHomeBannerAd(), binding!!.bannerAd)
        }
        position = intent.getIntExtra(Constants.POSITION, 0)
        interest = intent.getStringExtra(Constants.INTEREST) ?: "unknown"
        if(intent.hasExtra(ALREADY_EXISTS)){
            if(interest=="unknown"){
                getPostTypeData()
            } else{
                getCardTypeData()
            }
        }
        else if(intent.hasExtra(POST_ID)){
            getPostTypeData()
        } else{
            getCardTypeData()
        }
    }

    private fun getPostTypeData(){
        binding!!.pbLoading.visibility = View.VISIBLE
        binding!!.mainLayout.visibility = View.GONE
        postId = intent.getStringExtra(POST_ID)!!
        postSource = intent.getStringExtra(Constants.POST_SOURCE) ?: "unknown"
        feedType = intent.getStringExtra(Constants.FEED_TYPE) ?: "unknown"
        getData()
    }

    private fun getCardTypeData(){
        binding!!.pbLoading.visibility = View.GONE
        binding!!.mainLayout.visibility = View.VISIBLE
        podcastCard = cardsMap[interest]!![position]
        postId = podcastCard!!.items[0].postId!!
        postSource = podcastCard!!.items[0].postSource!!
        feedType = podcastCard!!.items[0].feedType!!
        isVideo = podcastCard!!.items[0].isVideo!!
        publisherId = podcastCard!!.items[0].publisherId!!
        publisherName = podcastCard!!.items[0].publisherName!!
        getData()
        setData(podcastCard!!.items[0].content?.mediaList?.get(0)!!,
            podcastCard!!.items[0].content?.images!![0]!![0].url,
            podcastCard!!.items[0].publisherName,
            podcastCard!!.items[0].content?.title,
            podcastCard!!.items[0].content?.description,
            podcastCard!!.items[0].languageString,
            Constants.getInterestsString( podcastCard!!.items[0].interests),
            feedType,
            postSource,
            podcastCard!!.items[0].isReacted,
            podcastCard!!.items[0].appComments
        )
        if(BuildConfig.DEBUG){
            binding!!.report.visibility = View.VISIBLE
        } else{
            binding!!.report.visibility = View.GONE
        }
        binding!!.report.setOnClickListener {
            val reportIssueFragment = ReportIssueDialogFragment.newInstance(podcastCard!!.items[0], 0, "podcasts")
            if (this is FragmentActivity) {
                reportIssueFragment.show(
                    (this as
                            FragmentActivity).supportFragmentManager,
                    "reportIssueFragment"
                )
            } else if (((this as ContextWrapper).baseContext is FragmentActivity)) {
                reportIssueFragment.show(
                    ((this as ContextWrapper).baseContext as FragmentActivity).supportFragmentManager,
                    "reportIssueFragment"
                )
            }
        }
    }

    private fun getData(){
        FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let {
            ApiGetPostDetails().getPostDetailsEncrypted(
                Endpoints.GET_POSTS_DETAILS_ENCRYPTED,
                it,
                FeedSdk.userId,
                postId,
                postSource,
                feedType,
                object : ApiGetPostDetails.PostDetailsResponse {
                    override fun onSuccess(postDetailsModel: PostDetailsModel, url:String, timeStamp: Long) {
                        presentUrl = url
                        presentTimeStamp = timeStamp
                        if(intent.hasExtra(POST_ID)){
                            binding!!.pbLoading.visibility = View.GONE
                            binding!!.mainLayout.visibility = View.VISIBLE
                            podcastPost = postDetailsModel.post
                            isVideo = podcastPost!!.isVideo
                            publisherId = podcastPost!!.publisherId!!
                            publisherName = podcastPost!!.publisherName!!
                            setData(podcastPost!!.content?.mediaList?.get(0)!!,
                                podcastPost!!.content?.mediaList?.get(1),
                                podcastPost!!.publisherName,
                                podcastPost!!.content?.title,
                                podcastPost!!.content?.description,
                                podcastPost!!.languageString,
                                Constants.getInterestsString( podcastPost!!.interests),
                                feedType,
                                postSource,
                                podcastPost!!.isReacted,
                                podcastPost!!.appComments
                            )
                        }
                        PodcastMediaPlayer.getPodcastMediaCard().presentUrl = presentUrl
                        PodcastMediaPlayer.getPodcastMediaCard().presentTimeStamp = presentTimeStamp
                        comments = postDetailsModel.post?.comments as ArrayList<FeedComment>
                    }

                    override fun onFailure() {
                        Toast.makeText(
                            this@PodcastPlayerActivity,
                            getString(R.string.error_some_issue_occurred),
                            Toast.LENGTH_SHORT
                        ).show()
                        if(intent.hasExtra(POST_ID)) {
                            finish()
                        }
                    }
                })
        }
    }

    private fun setData(mediaUrl:String, imageUrl: String?, publisherName: String?, title: String?, description: String?,
                        languageString: String?, interestString: String?, feedType: String, postSource: String, isReacted:String?, appComments:Int?){
        var msgTitle = ""
        when {
            title!=null -> {
                msgTitle = title!!
                binding!!.itemTitle.text = msgTitle
            }
            description!=null -> {
                msgTitle = description!!
                binding!!.itemTitle.text = msgTitle
            }
            else -> {
                binding!!.itemTitle.visibility = View.GONE
            }
        }
        if(intent.hasExtra(ALREADY_EXISTS)){
            if(interest=="unknown"){
                PodcastMediaPlayer.releasePlayer(this)
            } else{
                binding!!.progressBar.visibility = View.GONE
                binding!!.play.visibility = View.VISIBLE
                binding!!.txtvLength.text = PodcastMediaPlayer.totalDuration()
                if(PodcastMediaPlayer.isPlaying()){
                    binding!!.play.setImageResource(R.drawable.ic_podcast_pause)
                } else{
                    binding!!.play.setImageResource(R.drawable.ic_podcast_play)
                }
            }

        }
        if(PodcastMediaPlayer.getPodcastMediaCard().postId == postId){
            binding!!.progressBar.visibility = View.GONE
            binding!!.play.visibility = View.VISIBLE
            if(PodcastMediaPlayer.isPlaying()){
                binding!!.play.setImageResource(R.drawable.ic_podcast_pause)
            } else{
                binding!!.play.setImageResource(R.drawable.ic_podcast_play)
            }
        }
        Glide.with(this)
            .load(imageUrl)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    Picasso.get()
                        .load(imageUrl)
                        .noFade()
                        .into(binding!!.itemImage, object : Callback {
                            override fun onSuccess() {}
                            override fun onError(e: java.lang.Exception?) {
                                binding!!.imageCard.visibility = View.GONE
                            }
                        })
                    return true
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    binding!!.itemImage.setImageDrawable(resource)
                    PodcastMediaPlayer.getPodcastMediaCard().imageBitmap = resource?.toBitmap()
                    PodcastMediaPlayer.startPodcastNotification(this@PodcastPlayerActivity)
                    return true
                }

            })
            .into(binding!!.itemImage)
        val card = PodcastMediaCard(mediaUrl, postId, msgTitle, publisherName?:"",publisherId,
            imageUrl!!,binding!!.itemImage.drawable?.toBitmap(), position, interest, languageString, interestString, feedType, postSource, presentTimeStamp, presentUrl)
        PodcastMediaPlayer.init(this, card, object : PodcastMediaPlayerListener{
                override fun onStarted() {
                    binding!!.progressBar.visibility = View.GONE
                    binding!!.play.visibility = View.VISIBLE
                    binding!!.play.setImageResource(R.drawable.ic_podcast_pause)
                    binding!!.txtvLength.text = PodcastMediaPlayer.totalDuration()
                }

                override fun onCompleted() {
                    binding!!.play.setImageResource(R.drawable.ic_podcast_play)
                }

                override fun onReleased() {
                    binding!!.play.setImageResource(R.drawable.ic_podcast_play)
                }

                override fun onPlayerProgressed(duration: String, progress: Int) {
                    binding!!.txtvPosition.text = duration
                    binding!!.seekbar.progress = progress
                }

                override fun onPause() {
                    binding!!.play.setImageResource(R.drawable.ic_podcast_play)
                }

                override fun onPlay() {
                    binding!!.play.setImageResource(R.drawable.ic_podcast_pause)
                }

            }, "podcastPlayerActivity")
        binding!!.publisher.text = "By "+ publisherName
        binding!!.play.setOnClickListener {
            if(PodcastMediaPlayer.mediaPlayer!=null){
                if(PodcastMediaPlayer.isPlaying()){
                    PodcastMediaPlayer.pausePlayer(this)
                    binding!!.play.setImageResource(R.drawable.ic_podcast_play)
                } else{
                    PodcastMediaPlayer.resumePlayer(this)
                    binding!!.play.setImageResource(R.drawable.ic_podcast_pause)
                }
            }
        }
        binding!!.seekbar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if(fromUser)
                    PodcastMediaPlayer.movePlayer(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding!!.forward.setOnClickListener {
            PodcastMediaPlayer.movePlayer(true)
        }
        binding!!.backward.setOnClickListener {
            PodcastMediaPlayer.movePlayer(false)
        }
        binding!!.ivClose.setOnClickListener {
            PodcastMediaPlayer.releasePlayer(this)
            finish()
        }
        reacted = isReacted!!
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
            ex.printStackTrace()
        }
        if(interest!="unknown" || !intent.hasExtra(POST_ID)){
            val reactionsCount = podcastCard!!.items[0].reactionsCount!!
            likes = reactionsCount.likeCount
            likes += reactionsCount.angryCount
            likes += reactionsCount.laughCount
            likes += reactionsCount.loveCount
            likes += reactionsCount.sadCount
            likes += reactionsCount.wowCount
        } else{
            val reactionsCount = podcastPost!!.reactionCount!!
            likes = reactionsCount.likeCount
            likes += reactionsCount.angryCount
            likes += reactionsCount.laughCount
            likes += reactionsCount.loveCount
            likes += reactionsCount.sadCount
            likes += reactionsCount.wowCount
        }
        commentsCount = appComments!!
        binding!!.tvLikes.text = likes.toString()
        binding!!.tvComments.text = commentsCount.toString()
        binding?.tvComments?.setOnClickListener {
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
            nonNativeCommentBottomSheet?.show(supportFragmentManager, PodcastPlayerActivity::class.simpleName)
        }
        binding?.tvLikes?.setOnClickListener {
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
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                likes -= 1
                binding?.tvLikes?.setDrawableColor(ContextCompat.getColor(this, R.color.feedSecondaryTintColor))
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
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                likes += 1
                binding?.tvLikes?.setDrawableColor(ContextCompat.getColor(this, R.color.purple_500))
            }
            binding?.tvLikes?.text = likes.toString()
            binding?.tvLikes?.setCompoundDrawablesWithIntrinsicBounds(
                Converters().getDisplayImage(
                    reacted,
                    this@PodcastPlayerActivity,
                    false
                ), null, null, null
            )
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
                        binding?.tvLikes?.setCompoundDrawablesWithIntrinsicBounds(
                            Converters().getDisplayImage(
                                reacted,
                                this@PodcastPlayerActivity,
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
        binding?.tvShare?.setOnClickListener {
            binding?.tvShare!!.isEnabled = false
            Handler(Looper.getMainLooper()).postDelayed({ binding?.tvShare!!.isEnabled = true }, 1000)
            sharePost(this, postId, title, imageUrl, false, "")
        }
        binding?.tvWhatsAppShare?.setOnClickListener {
            binding?.tvWhatsAppShare!!.isEnabled = false
            Handler(Looper.getMainLooper()).postDelayed({ binding?.tvWhatsAppShare!!.isEnabled = true }, 1000)
            sharePost(this, postId, title, imageUrl, true, "")
        }
    }

    fun sharePost(context: Context, id: String?, title: String?, imageUrl: String?, isWhatsApp: Boolean, postUrl: String) {
        val link =  FeedSdk.mFirebaseDynamicLink+"?podcast_id="+id
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
                    .setAndroidParameters(DynamicLink.AndroidParameters.Builder(context.packageName).build())
                    .setSocialMetaTagParameters(
                        DynamicLink.SocialMetaTagParameters.Builder()
                            .setTitle(FeedSdk.appName?:"Podcast").setDescription(
                                title!!
                            ).build()
                    )
                    .buildShortDynamicLink()
                    .addOnSuccessListener { shortDynamicLink ->
                        val shortLink: String =
                            prefix+shortDynamicLink.shortLink.toString() + context.getString(R.string.dynamic_link_url_suffix) + " " + FeedSdk.appName + " " + context.getString(
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
                        e.printStackTrace()
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
                e.printStackTrace()
            }
        } else {
            try {
                FirebaseDynamicLinks.getInstance().createDynamicLink()
                    .setLink(Uri.parse(link))
                    .setDomainUriPrefix(FeedSdk.mFirebaseDynamicLink)
                    .setAndroidParameters(DynamicLink.AndroidParameters.Builder(context.packageName).build())
                    .setSocialMetaTagParameters(
                        DynamicLink.SocialMetaTagParameters.Builder()
                            .setTitle(FeedSdk.appName?:"Podcast").setDescription(
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
                        e.printStackTrace()
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
                e.printStackTrace()
            }
        }
    }

    private fun postComment(comment: String) {
        FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let {
            ApiCommentPost().postCommentEncrypted(
                Endpoints.COMMENT_POST_ENCRYPTED,
                it,
                postId,
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
            if (cardsMap[interest] != null) {
                val card = cardsMap[interest]!![position]
                var commentsCount = card.items[0].appComments!!
                commentsCount += 1
                card.items[0].appComments = commentsCount
                cardsMap[interest]!![position] = card
            }
            binding?.tvComments!!.text = commentsCount.toString()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        feedCommentResponse.result?.comment?.let {
            nonNativeCommentBottomSheet?.updateComments(
                it
            )
        }
    }

    fun postReaction() {
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
                FeedSdk.userId, postId, reactionType)
        }
    }

    private fun setFonts(){
        Card.setFontFamily(binding?.itemTitle, true)
        Card.setFontFamily(binding?.publisher)
        Card.setFontFamily(binding?.txtvPosition)
        Card.setFontFamily(binding?.txtvLength)
        Card.setFontFamily(binding?.tvLikes)
        Card.setFontFamily(binding?.tvComments)
    }

//    override fun onBackPressed() {
//        storeData()
//        PodcastMediaPlayer.releasePlayer()
//        super.onBackPressed()
//    }


}
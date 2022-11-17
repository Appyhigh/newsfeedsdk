package com.appyhigh.newsfeedsdk.model.feeds

import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.text.util.Linkify
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.*
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.text.HtmlCompat
import androidx.core.text.parseAsHtml
import androidx.core.view.setPadding
import androidx.databinding.BindingAdapter
import androidx.databinding.adapters.ListenerUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.activity.FeedsActivity
import com.appyhigh.newsfeedsdk.activity.PublisherPageActivity
import com.appyhigh.newsfeedsdk.adapter.*
import com.appyhigh.newsfeedsdk.apicalls.ApiConfig
import com.appyhigh.newsfeedsdk.apicalls.ApiCrypto
import com.appyhigh.newsfeedsdk.apicalls.ApiUpdateUserPersonalization
import com.appyhigh.newsfeedsdk.apicalls.ConfigAdRequestListener
import com.appyhigh.newsfeedsdk.apiclient.Endpoints
import com.appyhigh.newsfeedsdk.callbacks.*
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.model.Interest
import com.appyhigh.newsfeedsdk.utils.*
import com.appyhigh.newsfeedsdk.utils.SpUtil.Companion.eventsListener
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.flexbox.FlexboxLayout
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


data class Card(
    @SerializedName("items")
    @Expose
    var items: List<Item> = ArrayList(),
    @SerializedName("card_type")
    @Expose
    var cardType: String? = null,
    @SerializedName("side_text")
    @Expose
    var sideText: String? = null,
    @SerializedName("coin_id")
    @Expose
    var coinId: String? = null,
    @SerializedName("coin_name")
    @Expose
    var coinName: String? = null,
    @SerializedName("image_link")
    @Expose
    var imageLink: String? = null,
    @SerializedName("question_text")
    @Expose
    var questionText: String = "",
    @SerializedName("info_text")
    @Expose
    var infoText: String = "",
) {
    companion object {
        var currentSimpleExoPlayer: ExoPlayer? = null

        @JvmStatic
        @BindingAdapter("logoSmall", "tvPublisherImage", "publisherName")
        fun loadImage(
            view: CircleImageView,
            imageUrl: String?,
            tvPublisherImage: Int,
            publisherName: String
        ) {
            val tvPublisher =
                (view.parent as View).findViewById<AppCompatTextView>(tvPublisherImage)
            if (!imageUrl.isNullOrEmpty() && imageUrl.contains(".svg")) {
                val imageLoader = ImageLoader.Builder(view.context)
                    .componentRegistry { add(SvgDecoder(view.context)) }
                    .build()

                val request = ImageRequest.Builder(view.context)
                    .crossfade(true)
                    .crossfade(500)
                    .placeholder(R.drawable.placeholder)
                    .data(imageUrl)
                    .target(
                        onSuccess = {
                            view.setImageDrawable(it)
                        },
                        onError = {
                            view.visibility = View.GONE
                            tvPublisher.visibility = View.VISIBLE
                            tvPublisher.text = publisherName.substring(0, 1).uppercase()
                        }
                    )
                    .build()

                imageLoader.enqueue(request)
            } else {
                Constants.loadImageFromGlide(
                    view.context,
                    imageUrl,
                    view,
                    object : GlideCallbackListener {
                        override fun onSuccess(drawable: Drawable?) {
                            try {
                                view.visibility = View.VISIBLE
                                tvPublisher.visibility = View.GONE
                                view.setImageDrawable(drawable)
                            } catch (ex: Exception) {
                                LogDetail.LogEStack(ex)
                            }
                        }

                        override fun onFailure() {
                            Picasso.get()
                                .load(imageUrl)
                                .noFade()
                                .into(view, object : Callback {
                                    override fun onSuccess() {}
                                    override fun onError(e: java.lang.Exception?) {
                                        view.visibility = View.GONE
                                        tvPublisher.visibility = View.VISIBLE
                                        tvPublisher.text = publisherName.substring(0, 1).uppercase()
                                    }
                                })
                        }

                    },
                    RequestOptions().override(50)
                )
            }
        }

        @JvmStatic
        @BindingAdapter("formattedTime")
        fun setFormattedTime(view: AppCompatTextView, time: String) {
            var publishedOnText: String? = "1 day ago"
            try {
                publishedOnText = getTime(time)
            } catch (e: ParseException) {
                LogDetail.LogEStack(e)
            }
            view.text = publishedOnText
        }

        @JvmStatic
        @BindingAdapter(value = ["publisherData", "position", "postListener"])
        fun setPublisherListener(
            view: View,
            publisherData: Item,
            position: Int,
            postListener: PostViewsClickListener
        ) {
            view.setOnClickListener {
                try {
                    if (view.context.toString().contains("PublisherPageActivity")) {
                        postListener.onPostClicked(view, position)
                    } else {
                        val intent = Intent(view.context, PublisherPageActivity::class.java)
                        intent.putExtra(Constants.FULL_NAME, publisherData.publisherName)
                        intent.putExtra(Constants.PROFILE_PIC, publisherData.publisherProfilePic)
                        intent.putExtra(Constants.PUBLISHER_ID, publisherData.publisherId)
                        intent.putExtra(
                            Constants.IS_FOLLOWING_PUBLISHER,
                            publisherData.isFollowingPublisher
                        )
                        intent.putExtra(
                            Constants.PUBLISHER_CONTACT,
                            publisherData.publisherContactUs
                        )
                        intent.putExtra(Constants.POSITION, position)
                        intent.putExtra(Constants.FEED_TYPE, "feed_publisher")
                        intent.putExtra(Constants.POST_SOURCE, "feed_publisher")
                        intent.putExtra(Constants.SCREEN_TYPE, Constants.EXPLORE)
                        view.context.startActivity(intent)
                    }
                } catch (ex: Exception) {
                    LogDetail.LogEStack(ex)
                }
            }
        }

        @JvmStatic
        @BindingAdapter(value = ["title", "description"], requireAll = true)
        fun setTitle(view: AppCompatTextView, title: String?, description: String?) {
            when {
                !title.isNullOrEmpty() -> {
                    view.text = HtmlCompat.fromHtml(title, HtmlCompat.FROM_HTML_MODE_LEGACY)
                    view.visibility = View.VISIBLE
                }
                !description.isNullOrEmpty() -> {
                    view.text = HtmlCompat.fromHtml(description, HtmlCompat.FROM_HTML_MODE_LEGACY)
                    view.visibility = View.VISIBLE
                }
                else -> {
                    view.visibility = View.GONE
                }
            }
        }

        @JvmStatic
        @BindingAdapter(value = ["title", "description", "caption", "platform"], requireAll = true)
        fun setVideoTitle(
            view: AppCompatTextView,
            title: String?,
            description: String?,
            caption: String?,
            platform: String?
        ) {
            val isYoutube = platform.toString().toLowerCase(Locale.getDefault()) == "youtube"
            if (!isYoutube) {
                when {
                    title != null -> {
                        view.text = HtmlCompat.fromHtml(title, HtmlCompat.FROM_HTML_MODE_LEGACY)
                        view.visibility = View.VISIBLE
                    }
                    description != null -> {
                        view.text =
                            HtmlCompat.fromHtml(description, HtmlCompat.FROM_HTML_MODE_LEGACY)
                        view.visibility = View.VISIBLE
                    }
                    else -> {
                        view.visibility = View.GONE
                    }
                }
            } else {
                when {
                    caption != null -> {
                        view.text = HtmlCompat.fromHtml(caption, HtmlCompat.FROM_HTML_MODE_LEGACY)
                        view.visibility = View.VISIBLE
                    }
                    title != null -> {
                        view.text = HtmlCompat.fromHtml(title, HtmlCompat.FROM_HTML_MODE_LEGACY)
                        view.visibility = View.VISIBLE
                    }
                    else -> {
                        view.visibility = View.GONE
                    }
                }
            }
        }

        @JvmStatic
        @BindingAdapter(value = ["postImage", "blurImage", "cardType"], requireAll = false)
        fun loadImage(
            view: AppCompatImageView,
            imageUrl: String?,
            blurImage: Boolean = false,
            cardType: String? = null
        ) {
            try {
                Constants.loadImageFromGlide(
                    view.context,
                    imageUrl,
                    view,
                    object : GlideCallbackListener {
                        override fun onSuccess(drawable: Drawable?) {
                            try {
                                view.visibility = View.VISIBLE
                                view.setBackgroundResource(0)
                                if (blurImage && cardType == Constants.CardType.MEDIA_IMAGE.toString()
                                        .lowercase(Locale.getDefault())
                                ) {
                                    val scale: Float = view.context.resources.displayMetrics.density
                                    val bitmap =
                                        BlurBuilder.blur(view.context, drawable!!.toBitmap())
                                    val bitmapDrawable =
                                        BitmapDrawable(view.context.resources, bitmap)
                                    view.setImageDrawable(drawable)
                                    view.background = bitmapDrawable
                                    LogDetail.LogD(
                                        "ImageBlur",
                                        "onSuccess: " + drawable.intrinsicWidth + " " + drawable.intrinsicHeight
                                    )
                                    if (drawable.intrinsicWidth < 1000) {
                                        val height = (350 * scale + 0.5f).toInt()
                                        view.layoutParams = FrameLayout.LayoutParams(
                                            FrameLayout.LayoutParams.MATCH_PARENT,
                                            height
                                        )
                                        view.scaleType = ImageView.ScaleType.FIT_CENTER
                                    } else {
                                        val height = (200 * scale + 0.5f).toInt()
                                        view.layoutParams = FrameLayout.LayoutParams(
                                            FrameLayout.LayoutParams.MATCH_PARENT,
                                            height
                                        )
                                        view.scaleType = ImageView.ScaleType.CENTER_CROP
                                    }
                                } else {
                                    view.setImageDrawable(drawable)
                                }
                            } catch (ex: Exception) {
                                LogDetail.LogEStack(ex)
                            }
                        }

                        override fun onFailure() {
                            if (imageUrl.isNullOrEmpty()) {
                                view.visibility = View.GONE
                            } else {
                                Picasso.get()
                                    .load(imageUrl)
                                    .noFade()
                                    .into(view, object : Callback {
                                        override fun onSuccess() {
                                            try {
                                                if (Constants.CardType.MEDIA_IMAGE.toString()
                                                        .lowercase(Locale.getDefault()) == cardType
                                                ) {
                                                    view.scaleType = ImageView.ScaleType.FIT_CENTER
                                                    val scale: Float =
                                                        view.context.resources.displayMetrics.density
                                                    val height = (350 * scale + 0.5f).toInt()
                                                    view.layoutParams = FrameLayout.LayoutParams(
                                                        FrameLayout.LayoutParams.MATCH_PARENT,
                                                        height
                                                    )
                                                }
                                            } catch (ex: Exception) {
                                                LogDetail.LogEStack(ex)
                                            }
                                        }

                                        override fun onError(e: java.lang.Exception?) {
                                            view.visibility = View.GONE
                                        }

                                    })
                            }
                        }

                    },
                    RequestOptions().override(300)
                )
            } catch (ex: Exception) {
                LogDetail.LogEStack(ex)
            }
        }


        @JvmStatic
        @BindingAdapter(
            value = ["likeCount", "angryCount", "laughCount", "loveCount", "sadCount", "wowCount", "comments"],
            requireAll = true
        )
        fun setPostStats(
            view: AppCompatTextView,
            likeCount: Int,
            angryCount: Int,
            laughCount: Int,
            loveCount: Int,
            sadCount: Int,
            wowCount: Int,
            comments: Int
        ) {
            val likes = likeCount + angryCount + laughCount + loveCount + sadCount + wowCount
            view.text = "$likes Likes • $comments Comments"
        }

        @JvmStatic
        @BindingAdapter(
            value = ["likeCount", "angryCount", "laughCount", "loveCount", "sadCount", "wowCount"],
            requireAll = true
        )
        fun setPostLikes(
            view: AppCompatTextView,
            likeCount: Int,
            angryCount: Int,
            laughCount: Int,
            loveCount: Int,
            sadCount: Int,
            wowCount: Int
        ) {
            val likes = likeCount + angryCount + laughCount + loveCount + sadCount + wowCount
            view.text = likes.toString()
        }

        @JvmStatic
        @BindingAdapter("platform")
        fun setPlatform(view: AppCompatImageView, platform: String?) {
            val drawable = Converters().getDisplayImageForPlatForm(platform!!, view.context)
            if (drawable != null) {
                view.visibility = View.VISIBLE
                view.setImageDrawable(drawable)
            } else {
                view.visibility = View.GONE
            }
        }

        @JvmStatic
        @BindingAdapter("platform")
        fun setPlatform(view: CircleImageView, platform: String?) {
            val drawable = Converters().getDisplayImageForPlatForm(platform!!, view.context)
            if (drawable != null) {
                view.visibility = View.VISIBLE
                view.setImageDrawable(drawable)
            } else {
                view.visibility = View.GONE
            }
        }

        @JvmStatic
        @BindingAdapter("isLiked")
        fun setLiked(view: AppCompatImageView, reaction: String?) {
            try {
                if (reaction.toString().uppercase() == Constants.ReactionType.LIKE.toString()) {
                    view.setColorFilter(ContextCompat.getColor(view.context, R.color.purple_500))
                } else {
                    view.setColorFilter(
                        ContextCompat.getColor(
                            view.context,
                            R.color.feedSecondaryTintColor
                        )
                    )
                }
            } catch (ex: Exception) {
                LogDetail.LogEStack(ex)
            }
            view.setImageDrawable(
                Converters().getDisplayImage(
                    reaction.toString(),
                    view.context,
                    false
                )
            )
        }

        @JvmStatic
        @BindingAdapter("isLikedVideo")
        fun setLikedVideo(view: ImageView, reaction: String?) {
            view.setImageDrawable(
                Converters().getDisplayImageForVideos(
                    reaction.toString(),
                    view.context,
                    false
                )
            )
        }

        @JvmStatic
        @BindingAdapter(value = ["bindAd", "type", "contentUrls"], requireAll = true)
        fun loadAd(
            view: LinearLayout,
            bindAd: Boolean = true,
            type: String?,
            contentUrls: ArrayList<String>
        ) {
            Handler(Looper.getMainLooper()).postDelayed({
                ApiConfig().requestAd(view.context, "feed_native", object : ConfigAdRequestListener{
                    override fun onPrivateAdSuccess(webView: WebView) {
                        view.removeAllViews()
                        view.addView(webView)
                    }

                    override fun onAdmobAdSuccess(adId: String) {
                        requestFeedAd(view, R.layout.native_ad_feed, adId, true, "category", contentUrls)
                    }

                    override fun onAdHide() {

                    }

                })
            }, 1000)
        }

        @JvmStatic
        @BindingAdapter("bindAdLarge", "contentUrls")
        fun loadAdLarge(view: LinearLayout, bindAdLarge: Boolean = true, contentUrls: ArrayList<String>) {
            Handler(Looper.getMainLooper()).postDelayed({
                ApiConfig().requestAd(view.context, "video_native", object : ConfigAdRequestListener{
                    override fun onPrivateAdSuccess(webView: WebView) {
                        view.removeAllViews()
                        view.addView(webView)
                    }

                    override fun onAdmobAdSuccess(adId: String) {
                        if (Constants.videoUnitAdFromSticky != "") {
                            requestVideoAd(view, R.layout.native_ad_large, Constants.videoUnitAdFromSticky, true, "videofeed", contentUrls)
                        } else {
                            requestVideoAd(view, R.layout.native_ad_large, adId, true, "videofeed", contentUrls)
                        }
                    }

                    override fun onAdHide() {

                    }

                })
            }, 1000)
        }

        @JvmStatic
        @BindingAdapter(value = ["isVideo", "platform"], requireAll = true)
        fun setYoutubeView(view: View, isVideo: Boolean, platform: String?) {
            if (isVideo && platform.toString().lowercase(Locale.getDefault()) == "youtube") {
                view.visibility = View.VISIBLE
            } else {
                view.visibility = View.GONE
            }
        }

        @JvmStatic
        @BindingAdapter(value = ["isVideo", "platform"], requireAll = true)
        fun setYoutubeView(view: ImageView, isVideo: Boolean, platform: String?) {
            if (isVideo && platform.toString().lowercase(Locale.getDefault()) == "youtube") {
                view.visibility = View.VISIBLE
            } else {
                view.visibility = View.GONE
            }
        }

        @JvmStatic
        @BindingAdapter(value = ["videoUrl", "errorId", "ytLayout", "muteId", "playControllerId", "postId", "isShortVideo"])
        fun setVideoUrl(
            view: StyledPlayerView,
            videoUrl: String,
            errorId: Int,
            ytLayout: Int,
            muteId: Int,
            playControllerId: Int,
            postId: String?,
            isShortVideo: Boolean
        ) {
            val errorLayout = (view.parent.parent as View).findViewById<LinearLayout>(errorId)
            val ytLayout = (view.parent.parent as View).findViewById<RelativeLayout>(ytLayout)
            val mute = (view.parent as View).findViewById<AppCompatImageView>(muteId)
            val playController = (view.parent as View).findViewById<LinearLayout>(playControllerId)
            val playVideo = (view.parent as View).findViewById<AppCompatImageView>(R.id.playVideo)
            val backwardVideo =
                (view.parent as View).findViewById<AppCompatImageView>(R.id.backwardVideo)
            val forwardVideo =
                (view.parent as View).findViewById<AppCompatImageView>(R.id.forwardVideo)
            val duration = (view.parent as View).findViewById<TextView>(R.id.duration)
            val seekbar = (view.parent as View).findViewById<AppCompatSeekBar>(R.id.seekbar)
            val simpleExoPlayer = ExoPlayer.Builder(view.context).build()
            try {
                val scale: Float = view.context.resources.displayMetrics.density
                val height400 = (350 * scale + 0.5f).toInt()
                val layoutParams =
                    FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, height400)
                if (isShortVideo) {
                    view.layoutParams = layoutParams
                }
            } catch (ex: Exception) {
                LogDetail.LogEStack(ex)
            }
            view.player = simpleExoPlayer
            val uri = Uri.parse(videoUrl)
            val mediaSource: MediaSource? = buildMediaSource(view, uri)
            simpleExoPlayer.seekTo(0, 0)
            if (currentSimpleExoPlayer == null) {
                currentSimpleExoPlayer = simpleExoPlayer
            }
            simpleExoPlayer.setMediaSource(mediaSource!!, false)
            simpleExoPlayer.prepare()
            simpleExoPlayer.addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    try {
                        ytLayout.visibility = View.VISIBLE
                        val iv: AppCompatImageView = ytLayout.findViewById(R.id.iv)
                        val contextWrapper = ContextWrapper(iv.context)
                        Glide.with(contextWrapper.baseContext.applicationContext)
                            .load(videoUrl)
                            .into(iv)
                        view.visibility = View.GONE
                    } catch (ex: Exception) {
                        LogDetail.LogEStack(ex)
                    }
                }

                override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                    if (playbackState == ExoPlayer.STATE_ENDED) {
                        simpleExoPlayer.seekTo(0)
                    }
                    if (playWhenReady) {
                        playVideo.setImageResource(R.drawable.ic_podcast_pause_white)
                        if (currentSimpleExoPlayer != null) {
                            if (currentSimpleExoPlayer !== simpleExoPlayer) {
                                currentSimpleExoPlayer!!.playWhenReady = false
                                currentSimpleExoPlayer!!.playbackState
                                currentSimpleExoPlayer = simpleExoPlayer
                            }
                        }
                    } else {
                        playVideo.setImageResource(R.drawable.ic_podcast_play_white)
                    }
                    if (Constants.isMuted) {
                        simpleExoPlayer.volume = 0f
                    } else {
                        simpleExoPlayer.volume = simpleExoPlayer.deviceVolume.toFloat()
                    }
                }
            })
            val updateSeekBar: Runnable = object : Runnable {
                @SuppressLint("SetTextI18n")
                override fun run() {
                    try {
                        val totalDuration = simpleExoPlayer.duration
                        val currentDuration = simpleExoPlayer.currentPosition
                        val progress = ((currentDuration * 1.0) / totalDuration) * 100
                        seekbar.progress = progress.toInt()
                        if (totalDuration <= 0) {
                            duration.text = ""
                        } else {
                            duration.text =
                                PodcastMediaPlayer.convertTime(currentDuration) + "/" + PodcastMediaPlayer.convertTime(
                                    totalDuration
                                )
                        }
                        if (playController.visibility == View.VISIBLE)
                            Handler(Looper.getMainLooper()).postDelayed(this, 1000)
                    } catch (ex: java.lang.Exception) {
                    }
                }
            }
            (view.parent as FrameLayout).setOnClickListener {
                if (playController.visibility == View.GONE) {
                    playController.visibility = View.VISIBLE
                    mute.visibility = View.VISIBLE
                    if (Constants.isMuted) {
                        mute.setImageResource(R.drawable.ic_feed_mute)
                    } else {
                        mute.setImageResource(R.drawable.ic_feed_unmute)
                    }
                    if (simpleExoPlayer.isPlaying) {
                        playVideo.setImageResource(R.drawable.ic_podcast_pause_white)
                    } else {
                        playVideo.setImageResource(R.drawable.ic_podcast_play_white)
                    }
                    Handler(Looper.getMainLooper()).post(updateSeekBar)
                    Handler(Looper.getMainLooper()).postDelayed(
                        { (view.parent as FrameLayout).performClick() },
                        4000
                    )
                } else {
                    playController.visibility = View.GONE
                    mute.visibility = View.GONE
                }
            }
            mute.setOnClickListener {
                Constants.isMuted = !Constants.isMuted
                if (Constants.isMuted) {
                    simpleExoPlayer.volume = 0f
                    mute.setImageResource(R.drawable.ic_feed_mute)
                } else {
                    simpleExoPlayer.volume = simpleExoPlayer.deviceVolume.toFloat()
                    mute.setImageResource(R.drawable.ic_feed_unmute)
                }

            }
            playVideo.setOnClickListener {
                if (simpleExoPlayer.isPlaying) {
                    simpleExoPlayer.playWhenReady = false
                } else {
                    AudioTracker.init(
                        view.context,
                        "Feeds",
                        AudioTracker.VIDEOS,
                        postId,
                        object : AudioTrackerListener {
                            override fun onSuccess() {
                                simpleExoPlayer.playWhenReady = true
                            }

                            override fun onFailure() {
                                simpleExoPlayer.playWhenReady = false
                            }
                        })
                }
            }
            forwardVideo.setOnClickListener { simpleExoPlayer.seekTo(simpleExoPlayer.currentPosition + (10 * 1000)) }
            backwardVideo.setOnClickListener { simpleExoPlayer.seekTo(simpleExoPlayer.currentPosition - (10 * 1000)) }
            seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser) {
                        val totalDuration = simpleExoPlayer.duration
                        val currentDuration = (progress * totalDuration) / 100
                        simpleExoPlayer.seekTo(currentDuration)
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
//            simpleExoPlayer.playWhenReady = true
//            simpleExoPlayer.playbackState
//            view.hideController()
            view.useController = false

        }

        @JvmStatic
        @BindingAdapter(value = ["videoUrl", "youtubeUrl", "isYoutubeVideo", "position", "listener"])
        fun setBigVideoUrl(
            view: StyledPlayerView,
            videoUrl: String,
            youtubeUrl: String,
            isYoutubeVideo: String,
            position: Int,
            listener: VideoPlayerListener
        ) {
            val llYoutubeView =
                (view.parent as ConstraintLayout).findViewById<LinearLayout>(R.id.llYoutubeView)
            val mute = (view.parent as ConstraintLayout).findViewById<AppCompatImageView>(R.id.mute)
            if (isYoutubeVideo.lowercase() == "youtube") {
                view.player = null
                view.visibility = View.GONE
                llYoutubeView.visibility = View.VISIBLE
                listener.setUpYoutubeVideo(view, position, youtubeUrl)
            } else {
                try {
                    llYoutubeView.removeAllViews()
                } catch (e: java.lang.Exception) {
                    LogDetail.LogEStack(e)
                }
                llYoutubeView.visibility = View.GONE
                view.visibility = View.VISIBLE
                listener.releaseYoutubeVideo()
                var currentSimpleExoPlayer: ExoPlayer? = null
                val simpleExoPlayer = ExoPlayer.Builder(view.context).build()
                view.player = simpleExoPlayer
                val uri = Uri.parse(videoUrl)
                val mediaSource: MediaSource? = buildMediaSource(view, uri)
                simpleExoPlayer.seekTo(0, 0)
                if (currentSimpleExoPlayer == null) {
                    currentSimpleExoPlayer = simpleExoPlayer
                }
                simpleExoPlayer.setMediaSource(mediaSource!!, false)
                simpleExoPlayer.prepare()
                simpleExoPlayer.addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        super.onPlayerError(error)
                        try {
                            LogDetail.LogDE("videoUrl", videoUrl)
                        } catch (e: java.lang.Exception) {
                            LogDetail.LogEStack(e)
                        }
                    }

                    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                        if (playbackState == ExoPlayer.STATE_ENDED) {
                            try {
                                llYoutubeView.removeAllViews()
                            } catch (e: java.lang.Exception) {
                                LogDetail.LogEStack(e)
                            }
                            listener.onVideoEnded(position, simpleExoPlayer.duration)
                            simpleExoPlayer.seekTo(0)
                        }
                        if (playWhenReady) {
                            if (currentSimpleExoPlayer != null) {
                                if (currentSimpleExoPlayer !== simpleExoPlayer) {
                                    currentSimpleExoPlayer!!.playWhenReady = false
                                    currentSimpleExoPlayer!!.playbackState
                                    currentSimpleExoPlayer = simpleExoPlayer
                                }
                            }
                        }
                        if (Constants.isMuted) {
                            simpleExoPlayer.volume = 0f
                        } else {
                            simpleExoPlayer.volume = simpleExoPlayer.deviceVolume.toFloat()
                        }
                    }
                })
                if (Constants.isVideoFromSticky) {
                    simpleExoPlayer.playWhenReady = true
                    Constants.isVideoFromSticky = false
                }
                (view.parent as ConstraintLayout).setOnClickListener {
                    if (mute.visibility == View.GONE) {
                        mute.visibility = View.VISIBLE
                        if (Constants.isMuted) {
                            mute.setImageResource(R.drawable.ic_feed_mute)
                        } else {
                            mute.setImageResource(R.drawable.ic_feed_unmute)
                        }
                        Handler(Looper.getMainLooper()).postDelayed(
                            { (view.parent as ConstraintLayout).performClick() },
                            3000
                        )
                    } else {
                        mute.visibility = View.GONE
                    }
                }
                mute.setOnClickListener {
                    Constants.isMuted = !Constants.isMuted
                    if (Constants.isMuted) {
                        simpleExoPlayer.volume = 0f
                        mute.setImageResource(R.drawable.ic_feed_mute)
                    } else {
                        simpleExoPlayer.volume = simpleExoPlayer.deviceVolume.toFloat()
                        mute.setImageResource(R.drawable.ic_feed_unmute)
                    }

                }
//                simpleExoPlayer.playWhenReady = true
//                simpleExoPlayer.playbackState
//                view.hideController()
                view.useController = false

            }
        }

        private fun buildMediaSource(view: StyledPlayerView, uri: Uri): MediaSource? {
            val dataSourceFactory: DataSource.Factory = DefaultDataSource.Factory(view.context)
            ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(uri))
            return ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(uri))
        }

        @JvmStatic
        @BindingAdapter(
            "android:onViewDetachedFromWindow",
            "android:onViewAttachedToWindow",
            "position",
            requireAll = false
        )
        fun setListener(
            view: View,
            detach: OnViewDetachedFromWindow?,
            attach: OnViewAttachedToWindow?,
            position: Int
        ) {
            val newListener: View.OnAttachStateChangeListener? =
                if (detach == null && attach == null) {
                    null
                } else {
                    object : View.OnAttachStateChangeListener {
                        override fun onViewAttachedToWindow(v: View) {
                            attach?.onViewAttachedToWindow(v, position)
                        }

                        override fun onViewDetachedFromWindow(v: View) {
                            detach?.onViewDetachedFromWindow(v, position)
                        }
                    }
                }

            val oldListener: View.OnAttachStateChangeListener? =
                ListenerUtil.trackListener(view, newListener, R.id.onAttachStateChangeListener)
            if (oldListener != null) {
                view.removeOnAttachStateChangeListener(oldListener)
            }
            if (newListener != null) {
                view.addOnAttachStateChangeListener(newListener)
            }
        }

        @JvmStatic
        @BindingAdapter("hashtags")
        fun setHashtagsList(flexboxLayout: FlexboxLayout, hashtags: List<Item>) {
            val topics = ArrayList<String>()
            for (item in hashtags) {
                item.id?.let { topics.add(it) }
            }
            setViewInFlexBox(topics, flexboxLayout)
        }

        @JvmStatic
        @BindingAdapter("popularAccounts")
        fun setPopularAccounts(recyclerView: RecyclerView, popularAccounts: List<Item>) {
            recyclerView.apply {
                layoutManager =
                    LinearLayoutManager(recyclerView.context, LinearLayoutManager.HORIZONTAL, false)
                adapter =
                    PopularAccountsAdapter(popularAccounts)
            }
        }

        @JvmStatic
        @BindingAdapter("isFollowing")
        fun setFollowing(view: AppCompatTextView, isFollowing: Boolean) {
            if (isFollowing) {
                view.text = "✓Following"
            } else {
                view.text = "Follow"
            }
        }


        @JvmStatic
        @BindingAdapter("feedPostsCategory")
        fun setFeedPostsCategory(recyclerView: RecyclerView, feedPosts: List<Item>) {
            recyclerView.apply {
                layoutManager =
                    LinearLayoutManager(recyclerView.context, LinearLayoutManager.HORIZONTAL, false)
                adapter = FeedPostsCategoryAdapter(feedPosts)
            }
        }

        @JvmStatic
        @BindingAdapter("trendingPosts")
        fun setTrendingPosts(recyclerView: RecyclerView, trendingPosts: List<Item>) {
            recyclerView.apply {
                layoutManager =
                    LinearLayoutManager(recyclerView.context, LinearLayoutManager.VERTICAL, false)
                adapter = TrendingPostsAdapter(trendingPosts)
            }
        }

        @JvmStatic
        @BindingAdapter("feedReels")
        fun setFeedReels(recyclerView: RecyclerView, feedReels: List<Item>) {
            recyclerView.apply {
                layoutManager =
                    LinearLayoutManager(recyclerView.context, LinearLayoutManager.HORIZONTAL, false)
                adapter = FeedReelsAdapter(feedReels)
            }
        }

        @JvmStatic
        @BindingAdapter("feedVideosHorizontal")
        fun setFeedVideosHorizontal(recyclerView: RecyclerView, feedVideos: List<Item>) {
            recyclerView.apply {
                layoutManager =
                    LinearLayoutManager(recyclerView.context, LinearLayoutManager.HORIZONTAL, false)
                adapter = FeedVideosHorizontalAdapter(feedVideos)
            }
        }

        @JvmStatic
        @BindingAdapter(value = ["interestsList", "listener"])
        fun setInterestsCard(
            recyclerView: RecyclerView,
            interestsList: List<Item>,
            listener: InterestsCardClickListener
        ) {
            val flexBoxLayoutManager = FlexboxLayoutManager(recyclerView.context)
            flexBoxLayoutManager.justifyContent = JustifyContent.CENTER
            recyclerView.apply {
                layoutManager = flexBoxLayoutManager
                adapter = FeedInterestAdapter(interestsList, listener)
            }
        }

        @JvmStatic
        @BindingAdapter(value = ["languageList", "listener"])
        fun setLanguageCard(
            recyclerView: RecyclerView,
            languageList: List<Item>,
            listener: LanguageCardClickListener
        ) {
            val flexBoxLayoutManager = FlexboxLayoutManager(recyclerView.context)
            flexBoxLayoutManager.justifyContent = JustifyContent.CENTER
            recyclerView.apply {
                layoutManager = flexBoxLayoutManager
                adapter = FeedLanguageAdapter(languageList, listener)
            }
        }

        @JvmStatic
        @BindingAdapter("forInterest")
        fun checkIsInterest(view: AppCompatTextView, id: String) {
            FeedSdk.interestsList.forEach {
                if (id.contains(it.keyId!!) && it.isPinned) {
                    view.visibility = View.GONE
                    return
                }
            }
            if (Constants.allInterestsMap.containsKey(id) && !FeedSdk.isCricketApp()) {
                view.visibility = View.VISIBLE
            } else {
                view.visibility = View.GONE
            }
            view.setOnClickListener {
                try {
                    view.isEnabled = false
                    val interestList = FeedSdk.interestsList
                    if (interestList.indexOf(Constants.allInterestsMap[id]!!) == -1) {
                        interestList.add(Constants.allInterestsMap[id]!!)
                    }
                    interestList[interestList.indexOf(Constants.allInterestsMap[id])].isPinned =
                        true
                    updateInterest(view, interestList)
                    Handler(Looper.getMainLooper()).postDelayed({ view.isEnabled = true }, 1000)
                } catch (ex: Exception) {
                    LogDetail.LogEStack(ex)
                }
            }
        }

        private fun updateInterest(view: AppCompatTextView, interestList: ArrayList<Interest>) {
            try {
                ApiUpdateUserPersonalization().updateUserPersonalizationEncrypted(
                    Endpoints.UPDATE_USER_ENCRYPTED,
                    interestList,
                    FeedSdk.languagesList,
                    object : ApiUpdateUserPersonalization.UpdatePersonalizationListener {
                        override fun onFailure() {
                        }

                        override fun onSuccess() {
                            FeedSdk.interestsList = interestList
                            view.apply {
                                setBackgroundResource(R.drawable.ic_checkbox_selected)
                                text = ""
                                setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
                                setPadding(0)
                            }
                            try {
                                for (listener in SpUtil.onRefreshListeners) {
                                    if (listener.key != "explore")
                                        listener.value.onRefreshNeeded()
                                }
                            } catch (ex: Exception) {
                                LogDetail.LogEStack(ex)
                            }
                        }
                    }
                )
            } catch (ex: Exception) {
                LogDetail.LogEStack(ex)
            }

        }

        private fun setViewInFlexBox(topics: List<String>, flexboxLayout: FlexboxLayout) {
            try {
                flexboxLayout.removeAllViews()
                var count = 0
                for (topic in topics) {
                    val topicView = TextView(flexboxLayout.context)
                    topicView.text = topic
                    topicView.setBackgroundResource(R.drawable.bg_explore_trending_topics)
                    topicView.setTextColor(Color.parseColor("#414141"))
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
                        try {
                            if (eventsListener != null) {
                                eventsListener!!.onExploreInteraction(
                                    "Trending Hashtags",
                                    topic,
                                    "NA"
                                )
                            }
                        } catch (ex: java.lang.Exception) {
                            LogDetail.LogEStack(ex)
                        }
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

        @JvmStatic
        @BindingAdapter("hashtagsPlatforms")
        fun setHashtagsPlatforms(recyclerView: RecyclerView, hashtagsPlatforms: List<Item>) {
            val backupList = ArrayList<Item>(hashtagsPlatforms)
            val collapsedPlatforms = if (hashtagsPlatforms.size > 9) hashtagsPlatforms.subList(
                0,
                9
            ) else hashtagsPlatforms
            recyclerView.apply {
                layoutManager = GridLayoutManager(recyclerView.context, 3)
                adapter = HashtagsCircleAdapter(ArrayList<Item>(collapsedPlatforms), backupList)
            }
            recyclerView.isNestedScrollingEnabled = false
        }

        @JvmStatic
        @BindingAdapter("setPodcastDuration")
        fun setPodcastDuration(textView: TextView, duration: String?) {
            try {
                val result = duration ?: ""
                if (result.isNotEmpty() && !result.contains(":")) {
                    val seconds = result.toLong()
                    val s: Long = seconds % 60
                    val m: Long = (seconds / 60) % 60
                    val h: Long = (seconds / (60 * 60)) % 24
                    if (h > 0) {
                        textView.text = String.format("%d:%02d:%02d", h, m, s)
                    } else {
                        textView.text = String.format("%02d:%02d", m, s)
                    }
                } else {
                    textView.text = result
                }
            } catch (ex: Exception) {
                LogDetail.LogEStack(ex)
            }
        }

        @JvmStatic
        @BindingAdapter("cryptoWatchList", "cryptoType")
        fun setCryptoWatchlist(
            recyclerView: RecyclerView,
            cryptoWatchList: List<Item>,
            cryptoType: String
        ) {
            if (Constants.cryptoWatchListMap.size == 0) {
                for (crypto in cryptoWatchList) {
                    Constants.cryptoWatchListMap[crypto.coinId!!] = crypto.coinId
                }
            }
            Constants.cryptoWatchList = cryptoWatchList as ArrayList<Item>
            val watchListAdapter =
                CryptoDetailsAdapter(cryptoWatchList as ArrayList<Item>, false, cryptoType)
            recyclerView.apply {
                layoutManager =
                    LinearLayoutManager(recyclerView.context, LinearLayoutManager.HORIZONTAL, false)
                adapter = watchListAdapter
            }
            SpUtil.cryptoWatchListUpdateListener = object : CryptoWatchListUpdateListener {
                override fun onCryptoWatchListUpdated(newWatchlist: ArrayList<Item>) {
                    watchListAdapter.updateWatchList(newWatchlist)
                }
            }
        }

        @JvmStatic
        @BindingAdapter("cryptoItems", "cryptoType")
        fun setCryptoItems(
            recyclerView: RecyclerView,
            cryptoItems: List<Item>,
            cryptoType: String
        ) {
            if (cryptoType == Constants.CardType.COIN_MARKETS.toString()
                    .lowercase(Locale.getDefault())
            ) {
                recyclerView.apply {
                    layoutManager = LinearLayoutManager(recyclerView.context)
                    adapter = CryptoMarketAdapter(cryptoItems[0].markets as ArrayList<Item.Market>)
                }
            } else {
                recyclerView.apply {
                    layoutManager = LinearLayoutManager(recyclerView.context)
                    adapter =
                        CryptoDetailsAdapter(cryptoItems as ArrayList<Item>, false, cryptoType)
                }
            }
        }

        @JvmStatic
        @BindingAdapter("cryptoType", "showAdType")
        fun setShowAd(
            view: LinearLayout,
            cryptoType: String,
            showAdType: String
        ) {
            if (cryptoType == Constants.CardType.CRYPTO_LOSERS.toString().lowercase(Locale.getDefault())
                || cryptoType == Constants.CardType.CRYPTO_WATCHLIST.toString().lowercase(Locale.getDefault())
            ) {
                ApiConfig().requestAd(view.context, showAdType, object : ConfigAdRequestListener{
                    override fun onPrivateAdSuccess(webView: WebView) {
                        view.removeAllViews()
                        view.addView(webView)
                    }

                    override fun onAdmobAdSuccess(adId: String) {
                        requestFeedAd(view, R.layout.native_ad_feed_small, adId, true, showAdType, ArrayList())
                    }

                    override fun onAdHide() {

                    }

                })
            }
        }

        @JvmStatic
        @BindingAdapter("description")
        fun setDescription(view: TextView, description: String) {
//            view.text = HtmlCompat.fromHtml(description, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
            if (description == "") {
                view.visibility = View.GONE
            } else {
                view.visibility = View.VISIBLE
                view.text = description.parseAsHtml(HtmlCompat.FROM_HTML_MODE_LEGACY)
                view.movementMethod = LinkMovementMethod.getInstance()
                stripUnderlines(view)
            }
        }

        @SuppressLint("SetTextI18n")
        @JvmStatic
        @BindingAdapter("marketCap")
        fun setMarketCap(view: TextView, marketCap: Double) {
            val marketCapString = marketCap.toString() + ""
            if (marketCap >= 1.0) {
                if (marketCapString.contains("E", false)) {
                    val marketCapList = marketCapString.split("E")
                    view.text = Constants.getCryptoCoinSymbol() + Constants.getUnitFromValue(
                        marketCapList[0].toDouble(),
                        marketCapList[1].toInt()
                    )
                } else {
                    view.text = Constants.getCryptoCoinSymbol() + Constants.getEValueFormat(
                        marketCapString.toDouble(),
                        0
                    )
                }
            } else {
                view.text = Constants.getCryptoCoinSymbol() + Constants.get0EValueFormat(marketCap)
            }
        }

        @JvmStatic
        @BindingAdapter("cryptoHValue")
        fun setCryptoHValue(view: TextView, cryptoHValue: Double) {
            if (cryptoHValue >= 0) {
                view.text =
                    "+ " + BigDecimal(cryptoHValue).setScale(2, RoundingMode.HALF_EVEN) + " %"
                view.setTextColor(Color.parseColor("#21C17A"))
            } else {
                view.text = "" + BigDecimal(cryptoHValue).setScale(2, RoundingMode.HALF_EVEN) + " %"
                view.setTextColor(Color.parseColor("#FF585D"))
            }
        }

        @JvmStatic
        @BindingAdapter("cryptoFormattedValue")
        fun setCryptoFormattedValue(view: TextView, cryptoFormattedValue: Double) {
            val myFormatter = DecimalFormat("#,##,###.##")
            val cryptoValue = BigDecimal(cryptoFormattedValue).setScale(2, RoundingMode.HALF_EVEN)
            view.text =
                Constants.getCryptoCoinSymbol() + if (cryptoFormattedValue >= 1.0) myFormatter.format(
                    cryptoValue
                ) else Constants.get0EValueFormat(cryptoFormattedValue)

        }

        @JvmStatic
        @BindingAdapter("cryptoLiquidityValue")
        fun setCryptoLiquidityValue(view: TextView, cryptoLiquidityValue: Double) {
            val myFormatter = DecimalFormat("#,##,###.##")
            val cryptoValue = BigDecimal(cryptoLiquidityValue).setScale(2, RoundingMode.HALF_EVEN)
            view.text = myFormatter.format(cryptoValue)
        }

        @JvmStatic
        @BindingAdapter("cryptoLinks")
        fun setCryptoLinks(linksLayout: LinearLayout, cryptoLinks: Item.Links?) {
            try {
                linksLayout.removeAllViews()
                if (cryptoLinks != null) {
                    linksLayout.visibility = View.VISIBLE
                    val links = ArrayList<String>()
                    if (!cryptoLinks.homepage.isNullOrEmpty()) {
                        links.add(cryptoLinks.homepage)
                    }
                    links.addAll(cryptoLinks.blockchainSite)
                    val scale: Float = linksLayout.context.resources.displayMetrics.density
                    val marginTop = (15 * scale + 0.5f).toInt()
                    val marginBottom = (10 * scale + 0.5f).toInt()
                    val height = (1 * scale + 0.5f).toInt()
                    for (i in links.indices) {
                        val textView = TextView(linksLayout.context)
                        textView.layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        textView.maxLines = 1
                        textView.ellipsize = TextUtils.TruncateAt.END
                        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16F)
                        textView.text = links[i]
                        textView.setTypeface(FeedSdk.font, Typeface.BOLD)
                        textView.setLinkTextColor(
                            ContextCompat.getColor(
                                linksLayout.context,
                                R.color.purple_500
                            )
                        )
                        Linkify.addLinks(textView, Linkify.WEB_URLS)
                        textView.linksClickable = true
                        stripUnderlines(textView)
                        linksLayout.addView(textView)
                        if (i != links.size - 1) {
                            val view = View(linksLayout.context)
                            val layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                height
                            )
                            layoutParams.setMargins(0, marginTop, 0, marginBottom)
                            view.layoutParams = layoutParams
                            view.setBackgroundColor(Color.parseColor("#EFF2F5"))
                            linksLayout.addView(view)
                        }
                    }
                } else {
                    linksLayout.visibility = View.GONE
                }
            } catch (ex: java.lang.Exception) {
                LogDetail.LogEStack(ex)
            }
        }

        @JvmStatic
        @BindingAdapter("cryptoAlertList")
        fun setCryptoAlertList(view: LinearLayout, cryptoList: List<Item>) {
            var count = 0
            view.removeAllViews()
            for (item in cryptoList) {
                count += 1
                val child =
                    (view.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater).inflate(
                        R.layout.item_crypo_coin_alert,
                        null
                    )
                val alertPointer = child.findViewById<AppCompatImageView>(R.id.alertPointer)
                val alertPrice = child.findViewById<AppCompatTextView>(R.id.alertPrice)
                val alertSwitch = child.findViewById<AppCompatImageView>(R.id.alertSwitch)
                val alertDelete = child.findViewById<AppCompatImageView>(R.id.alertDelete)
                if (item.upperThreshold == null) {
                    alertPointer.setImageResource(R.drawable.ic_crypto_alert_below)
                    alertPrice.text = "Below ${item.lowerThreshold}"
                } else {
                    alertPointer.setImageResource(R.drawable.ic_crypto_alert_above)
                    alertPrice.text = "Above ${item.upperThreshold}"
                }
                if (item.alertStatus == "pending") {
                    alertSwitch.setImageResource(R.drawable.ic_crypto_alert_on)
                } else {
                    alertSwitch.setImageResource(R.drawable.ic_crypto_alert_off)
                }
                alertSwitch.setOnClickListener {
                    if (item.alertStatus == "pending") {
                        ApiCrypto().modifyCryptoAlertEncrypted(
                            Endpoints.CRYPTO_ALERT_MODIFY_ENCRYPTED,
                            item.alertId,
                            "sent",
                            object : ApiCrypto.CryptoAlertResponseListener {
                                override fun onSuccess() {
                                    alertSwitch.setImageResource(R.drawable.ic_crypto_alert_off)
                                    cryptoList[count-1].alertStatus = "sent"
                                }
                            })
                    } else {
                        ApiCrypto().modifyCryptoAlertEncrypted(
                            Endpoints.CRYPTO_ALERT_MODIFY_ENCRYPTED,
                            item.alertId,
                            "pending",
                            object : ApiCrypto.CryptoAlertResponseListener {
                                override fun onSuccess() {
                                    alertSwitch.setImageResource(R.drawable.ic_crypto_alert_on)
                                    cryptoList[count-1].alertStatus = "pending"
                                }
                            })
                    }
                }
                alertDelete.setOnClickListener {
                    ApiCrypto().deleteCryptoAlertEncrypted(
                        Endpoints.CRYPTO_ALERT_DELETE_ENCRYPTED,
                        item.alertId, object : ApiCrypto.CryptoAlertResponseListener {
                            override fun onSuccess() {
                                SpUtil.alertRefreshListener?.onRefreshNeeded()
                            }
                        })
                }
                if (count == cryptoList.size) {
                    child.findViewById<View>(R.id.view1).visibility = View.GONE
                }
                view.addView(child)
            }
        }

        private fun stripUnderlines(textView: TextView) {
            val s: Spannable = SpannableString(textView.text)
            val spans = s.getSpans(0, s.length, URLSpan::class.java)
            for (span in spans) {
                val start = s.getSpanStart(span)
                val end = s.getSpanEnd(span)
                s.removeSpan(span)
                val newSpan = URLSpanNoUnderline(span.url)
                s.setSpan(newSpan, start, end, 0)
            }
            textView.text = s
        }

        @SuppressLint("SetTextI18n")
        @JvmStatic
        @BindingAdapter("covidItem")
        fun setCovidItem(mainView: View, covidItem: Item?) {
            try {
                val autoCompleteTextView: AutoCompleteTextView =
                    mainView.findViewById(R.id.outlined_exposed_dropdown)
                val textInputLayoutHint: TextInputLayout =
                    mainView.findViewById(R.id.tilHinTxt)
                val lastUpdated: AppCompatTextView = mainView.findViewById(R.id.lastUpdated)
                val source: AppCompatTextView = mainView.findViewById(R.id.source)
                textInputLayoutHint.setTypeface(FeedSdk.font)
                val arrayAdapter = object : ArrayAdapter<String>(
                    autoCompleteTextView.context,
                    R.layout.list_item_state_covid_tracker,
                    covidItem!!.dropdownItems
                ) {
                    override fun getView(
                        position: Int,
                        convertView: View?,
                        parent: ViewGroup
                    ): View {
                        val v = super.getView(position, convertView, parent)
                        (v as TextView).setTypeface(FeedSdk.font, Typeface.BOLD)
                        return v
                    }

                    override fun getDropDownView(
                        position: Int,
                        convertView: View?,
                        parent: ViewGroup
                    ): View {
                        val v = super.getDropDownView(position, convertView, parent)
                        (v as TextView).setTypeface(FeedSdk.font, Typeface.BOLD)
                        return v
                    }
                }
                autoCompleteTextView.setAdapter(arrayAdapter)
                val rvTabs = mainView.findViewById<RecyclerView>(R.id.rvTabs)
                var tabsAdapter: TabsAdapter? = null
                tabsAdapter = TabsAdapter(covidItem.scrollableTabs, object : TabSelectedListener {
                    override fun onTabClicked(v: View, position: Int) {
                        tabsAdapter?.onTabCanged(position)
                        if (position == 0) {
                            setCovidData(
                                mainView,
                                covidItem.covidData[covidItem.covidInitialStateIndex]
                            )
                        } else {
                            setCovidData(mainView, covidItem.covidData[covidItem.covidCountryIndex])
                        }
                    }
                })
                rvTabs.apply {
                    layoutManager =
                        LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                    adapter = tabsAdapter
                }
                autoCompleteTextView.setOnItemClickListener { parent, view, position, id ->
                    if (position == covidItem.covidCountryIndex) {
                        tabsAdapter?.onTabCanged(1)
                        setCovidData(mainView, covidItem.covidData[covidItem.covidCountryIndex])
                    } else {
                        tabsAdapter.updatePositionData(0, covidItem.dropdownItems[position])
                        tabsAdapter?.onTabCanged(0)
                        setCovidData(mainView, covidItem.covidData[position])
                    }
                }
                setCovidData(mainView, covidItem.covidData[covidItem.covidInitialStateIndex])
                lastUpdated.text = covidItem.lastUpdatedOn
                source.text = covidItem.source
            } catch (ex: Exception) {
                LogDetail.LogEStack(ex)
            }

        }

        @SuppressLint("SetTextI18n")
        private fun setCovidData(view: View, covidData: Item.CovidData) {
            try {
                val testCases: AppCompatTextView = view.findViewById(R.id.testCases)
                val totalRecoveredCases: AppCompatTextView =
                    view.findViewById(R.id.totalRecoveredCases)
                val todayRecoveredCases: AppCompatTextView =
                    view.findViewById(R.id.todayRecoveredCases)
                val totalDeceasedCases: AppCompatTextView =
                    view.findViewById(R.id.totalDeceasedCases)
                val todayDeceasedCases: AppCompatTextView =
                    view.findViewById(R.id.todayDeceasedCases)
                val totalConfirmedCases: AppCompatTextView =
                    view.findViewById(R.id.totalConfirmedCases)
                val todayConfirmedCases: AppCompatTextView =
                    view.findViewById(R.id.todayConfirmedCases)
                val totalVaccinated: AppCompatTextView = view.findViewById(R.id.totalVaccinated)
                val halfVaccinated: AppCompatTextView = view.findViewById(R.id.halfVaccinated)
                val todayHalfVaccinated: AppCompatTextView =
                    view.findViewById(R.id.todayHalfVaccinated)
                val fullyVaccinated: AppCompatTextView = view.findViewById(R.id.fullyVaccinated)
                val todayFullyVaccinated: AppCompatTextView =
                    view.findViewById(R.id.todayFullyVaccinated)
                val myFormatter = DecimalFormat("###,##,##,###")
                testCases.text = myFormatter.format(covidData.total!!.tested)
                totalRecoveredCases.text = myFormatter.format(covidData.total.recovered)
                if (covidData.today!!.recovered == 0) {
                    todayRecoveredCases.text = " " + myFormatter.format(covidData.today.recovered)
                } else {
                    todayRecoveredCases.text = myFormatter.format(covidData.today.recovered)
                }
                totalDeceasedCases.text = myFormatter.format(covidData.total.deceased)
                if (covidData.today.deceased == 0) {
                    todayDeceasedCases.text = " " + myFormatter.format(covidData.today.deceased)
                } else {
                    todayDeceasedCases.text = myFormatter.format(covidData.today.deceased)
                }
                totalConfirmedCases.text = myFormatter.format(covidData.total.confirmed)
                if (covidData.today.confirmed == 0) {
                    todayConfirmedCases.text = " " + myFormatter.format(covidData.today.confirmed)
                } else {
                    todayConfirmedCases.text = myFormatter.format(covidData.today.confirmed)
                }
                totalVaccinated.text =
                    myFormatter.format(covidData.total.vaccinated1 + covidData.total.vaccinated2)
                halfVaccinated.text = myFormatter.format(covidData.total.vaccinated1)
                if (covidData.today.vaccinated1 == 0) {
                    todayHalfVaccinated.text = " " + myFormatter.format(covidData.today.vaccinated1)
                } else {
                    todayHalfVaccinated.text = myFormatter.format(covidData.today.vaccinated1)
                }
                fullyVaccinated.text = myFormatter.format(covidData.total.vaccinated2)
                if (covidData.today.vaccinated2 == 0) {
                    todayFullyVaccinated.text =
                        " " + myFormatter.format(covidData.today.vaccinated2)
                } else {
                    todayFullyVaccinated.text = myFormatter.format(covidData.today.vaccinated2)
                }
            } catch (ex: Exception) {
                LogDetail.LogEStack(ex)
            }
        }

        @Throws(ParseException::class)
        private fun getTime(dateTime: String): String? {
            try {
                @SuppressLint("SimpleDateFormat")
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
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
                        if (elapsedDays < 2) {
                            "$elapsedDays day ago"
                        } else {
                            "$elapsedDays days ago"
                        }
                    }
                    elapsedHours != 0L -> {
                        if (elapsedHours < 2) {
                            "$elapsedHours hour ago"
                        } else {
                            "$elapsedHours hours ago"
                        }
                    }
                    elapsedMinutes != 0L -> {
                        if (elapsedMinutes < 2) {
                            "$elapsedMinutes minute ago"
                        } else {
                            "$elapsedMinutes minutes ago"
                        }
                    }
                    else -> {
                        if (elapsedSeconds < 2) {
                            "$elapsedSeconds second ago"
                        } else {
                            "$elapsedSeconds seconds ago"
                        }
                    }
                }
            } catch (ex: java.lang.Exception) {
                LogDetail.LogEStack(ex)
                return "1 day ago"
            }
        }

        @JvmStatic
        @BindingAdapter(value = ["isBold"], requireAll = false)
        fun setFontFamily(view: TextView?, isBold: Boolean = false) {
            try {
                LogDetail.LogDE("setFontFamily","called")
                if (isBold) {
                    view!!.setTypeface(FeedSdk.font, Typeface.BOLD)
                } else {
                    view!!.typeface = FeedSdk.font
                }
            } catch (ex: java.lang.Exception) {
                LogDetail.LogEStack(ex)
            }
        }

        @JvmStatic
        @BindingAdapter(value = ["isBold"], requireAll = false)
        fun setFontFamily(view: Button?, isBold: Boolean = false) {
            try {
                LogDetail.LogDE("setFontFamily","called")
                if (isBold) {
                    view!!.setTypeface(FeedSdk.font, Typeface.BOLD)
                } else {
                    view!!.typeface = FeedSdk.font
                }
            } catch (ex: java.lang.Exception) {
                LogDetail.LogEStack(ex)
            }
        }

        @JvmStatic
        @BindingAdapter("checkCryptoApp")
        fun checkForCryptoApp(view: View, checkCryptoApp: Boolean) {
            try {
                if (FeedSdk.isCryptoApp) {
                    view.visibility = View.GONE
                } else {
                    view.visibility = View.VISIBLE
                }
            } catch (ex: java.lang.Exception) {
                LogDetail.LogEStack(ex)
            }
        }

        @JvmStatic
        @BindingAdapter(value = ["likeInterest", "isTitle"], requireAll = true)
        fun setYouMayLikeInterests(
            view: AppCompatTextView,
            likeInterest: String,
            isTitle: Boolean
        ) {
            try {
                if (isTitle) {
                    view.text =
                        view.context.getString(R.string.you_may_like_interests_title, likeInterest)
                } else {
                    val randomLike = Random().nextInt(100 - (80 + 1)) + 80
                    view.text = view.context.getString(
                        R.string.you_may_like_interests_body,
                        randomLike.toString() + "%",
                        likeInterest
                    )
                }
            } catch (ex: java.lang.Exception) {
                LogDetail.LogEStack(ex)
            }
        }

    }

    class URLSpanNoUnderline(url: String?) : URLSpan(url) {
        override fun updateDrawState(ds: TextPaint) {
            super.updateDrawState(ds)
            ds.isUnderlineText = false
        }
    }
}
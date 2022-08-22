package com.appyhigh.newsfeedsdk.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.encryption.LogDetail

class Converters {
    fun getDisplayImageForPlatForm(platformType: String, context: Context): Drawable? {
        LogDetail.LogD("platform", platformType)
        return try{
            when (platformType) {
                "instagram" -> ContextCompat.getDrawable(context, R.drawable.ic_instagram)
                "twitter" -> ContextCompat.getDrawable(context, R.drawable.ic_platform_twitter)
                "youtube" -> ContextCompat.getDrawable(context, R.drawable.ic_platform_youtube)
                "facebook" -> ContextCompat.getDrawable(context, R.drawable.ic_platform_facebook)
                "podcast" -> ContextCompat.getDrawable(context, R.drawable.ic_platform_podcast)
                "native" -> ContextCompat.getDrawable(context, R.drawable.ic_platform_video)
                "videobytes" -> ContextCompat.getDrawable(context, (R.drawable.ic_platform_video))
                else -> ContextCompat.getDrawable(context, R.drawable.ic_platform_new_post)
            }
        } catch (ex:Exception){
            ContextCompat.getDrawable(context, R.drawable.ic_platform_new_post)
        }
    }
    @SuppressLint("UseCompatLoadingForDrawables")
    fun getDisplayImage(reaction: String, context: Context, useDarkImages: Boolean = false): Drawable? {
        return when (reaction.uppercase()) {
            Constants.ReactionType.LIKE.toString() -> if (useDarkImages) ContextCompat.getDrawable(context, R.drawable.ic_like_new)
            else ContextCompat.getDrawable(context, R.drawable.ic_cricket_like_filled)
            Constants.ReactionType.LOVE.toString() -> ContextCompat.getDrawable(context, R.drawable.img_reaction_love)
            Constants.ReactionType.LAUGH.toString() -> ContextCompat.getDrawable(context, R.drawable.img_reaction_laugh) //TODO Replace laugh image
            Constants.ReactionType.WOW.toString() -> ContextCompat.getDrawable(context, R.drawable.img_reaction_wow)
            Constants.ReactionType.SAD.toString() -> ContextCompat.getDrawable(context, R.drawable.img_reaction_sad)
            Constants.ReactionType.ANGRY.toString() -> ContextCompat.getDrawable(context, R.drawable.img_reaction_angry)
            else -> if (useDarkImages) ContextCompat.getDrawable(context, R.drawable.ic_cricket_like)
            else ContextCompat.getDrawable(context, R.drawable.ic_cricket_like)
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    fun getDisplayImageNative(reaction: String, context: Context, useDarkImages: Boolean = false, showColor:String = "white"): Drawable? {
        return when (reaction.uppercase()) {
            Constants.ReactionType.LIKE.toString() -> if (useDarkImages) ContextCompat.getDrawable(context, R.drawable.ic_like_new)
            else ContextCompat.getDrawable(context, R.drawable.ic_cricket_like_filled)
            Constants.ReactionType.LOVE.toString() -> ContextCompat.getDrawable(context, R.drawable.img_reaction_love)
            Constants.ReactionType.LAUGH.toString() -> ContextCompat.getDrawable(context, R.drawable.img_reaction_laugh) //TODO Replace laugh image
            Constants.ReactionType.WOW.toString() -> ContextCompat.getDrawable(context, R.drawable.img_reaction_wow)
            Constants.ReactionType.SAD.toString() -> ContextCompat.getDrawable(context, R.drawable.img_reaction_sad)
            Constants.ReactionType.ANGRY.toString() -> ContextCompat.getDrawable(context, R.drawable.img_reaction_angry)
            else -> if (useDarkImages) ContextCompat.getDrawable(context, R.drawable.ic_cricket_like)
            else if(showColor=="white") ContextCompat.getDrawable(context, R.drawable.ic_like_white)
            else if(showColor=="blue") ContextCompat.getDrawable(context, R.drawable.ic_like_native)
            else ContextCompat.getDrawable(context, R.drawable.ic_cricket_like)
        }

    }

    @SuppressLint("UseCompatLoadingForDrawables")
    fun getDisplayImageForVideos(reaction: String, context: Context, useDarkImages: Boolean = false): Drawable? {
        return when (reaction.uppercase()) {
            Constants.ReactionType.LIKE.toString() -> if (useDarkImages) ContextCompat.getDrawable(context, R.drawable.ic_like_new)
            else if(SpUtil.useReelsV2) ContextCompat.getDrawable(context, R.drawable.ic_cricket_like_filled)
            else ContextCompat.getDrawable(context, R.drawable.ic_like_new_pressed)
            Constants.ReactionType.LOVE.toString() -> ContextCompat.getDrawable(context, R.drawable.img_reaction_love)
            Constants.ReactionType.LAUGH.toString() -> ContextCompat.getDrawable(context, R.drawable.img_reaction_laugh) //TODO Replace laugh image
            Constants.ReactionType.WOW.toString() -> ContextCompat.getDrawable(context, R.drawable.img_reaction_wow)
            Constants.ReactionType.SAD.toString() -> ContextCompat.getDrawable(context, R.drawable.img_reaction_sad)
            Constants.ReactionType.ANGRY.toString() -> ContextCompat.getDrawable(context, R.drawable.img_reaction_angry)
            else -> if (useDarkImages) ContextCompat.getDrawable(context, R.drawable.ic_like_new)
            else if(SpUtil.useReelsV2) ContextCompat.getDrawable(context, R.drawable.ic_like_v3)
            else ContextCompat.getDrawable(context, R.drawable.ic_like_new)
        }

    }


}
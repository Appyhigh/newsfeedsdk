package com.appyhigh.newsfeedsdk.utils

import android.content.Context
import android.content.Context.LAYOUT_INFLATER_SERVICE
import android.content.res.Resources
import android.graphics.Rect
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.callbacks.FeedReactionListener
import com.appyhigh.newsfeedsdk.model.Post


/**
 * Show Like reactions pop up on long click of a view
 * */
fun View.showReactionsPopUpWindow(
    item: Post?,
    feedReactionListener: FeedReactionListener,
    isShortVideo: Boolean = false,
    anchor: View
) {
    val REACTION_TRAY_Y_OFFSET = 50.0f
    val REACTION_TRAY_Y_OFFSET_EXTRA = 15.0f
    val inflater = context.getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater?
    val popupView: View = View.inflate(
        context,
        R.layout.layout_reactions_popup,
        null,
    )

    val reactionsPopUpWindow = PopupWindow(
        popupView,
        LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.WRAP_CONTENT,
        true
    )

    val iv_reaction_like = popupView.findViewById<ImageView>(R.id.iv_reaction_like)
    val iv_reaction_love = popupView.findViewById<ImageView>(R.id.iv_reaction_love)
    val iv_reaction_wow = popupView.findViewById<ImageView>(R.id.iv_reaction_wow)
    val iv_reaction_sad = popupView.findViewById<ImageView>(R.id.iv_reaction_sad)
    val iv_reaction_angry = popupView.findViewById<ImageView>(R.id.iv_reaction_angry)

    iv_reaction_like.setOnClickListener {
        feedReactionListener.onReaction(item, Constants.ReactionType.LIKE)
        reactionsPopUpWindow.dismiss()
    }

    iv_reaction_love.setOnClickListener {
        feedReactionListener.onReaction(item, Constants.ReactionType.LOVE)
        reactionsPopUpWindow.dismiss()
    }

    iv_reaction_wow.setOnClickListener {
        feedReactionListener.onReaction(item, Constants.ReactionType.WOW)
        reactionsPopUpWindow.dismiss()
    }

    iv_reaction_sad.setOnClickListener {
        feedReactionListener.onReaction(item, Constants.ReactionType.SAD)
        reactionsPopUpWindow.dismiss()
    }

    iv_reaction_angry.setOnClickListener {
        feedReactionListener.onReaction(item, Constants.ReactionType.ANGRY)
        reactionsPopUpWindow.dismiss()
    }

    val offsetY = context.getFloatFromDp(REACTION_TRAY_Y_OFFSET).toInt() +
            anchor.measuredHeight + if (isShortVideo) {
        context.getFloatFromDp(REACTION_TRAY_Y_OFFSET_EXTRA).toInt()
    } else 0
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
        reactionsPopUpWindow.showAsDropDown(
            anchor,
            0,
            -(offsetY),
            Gravity.CENTER
        )
    }
}

fun Context.getFloatFromDp(valueInDp: Float) =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, valueInDp, resources.displayMetrics)

fun View.onScreen(): Boolean {
    if (!isShown) {
        return false
    }
    val actualPosition = Rect()
    val isGlobalVisible = getGlobalVisibleRect(actualPosition)
    val screenWidth = Resources.getSystem().displayMetrics.widthPixels
    val screenHeight = Resources.getSystem().displayMetrics.heightPixels
    val screen = Rect(0, 0, screenWidth, screenHeight)
    return isGlobalVisible && Rect.intersects(actualPosition, screen)
}


package com.appyhigh.newsfeedsdk.utils

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.webkit.WebView
import im.delight.android.webview.AdvancedWebView
import android.view.MotionEvent
import android.view.View
import androidx.annotation.RequiresApi
import androidx.viewpager2.widget.ViewPager2


class ExtendedWebView : AdvancedWebView {
    constructor(context: Context) : super(context) {}
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {}


    private val parentViewPager: ViewPager2?
        get() {
            var v: View? = parent as? View
            while (v != null && v !is ViewPager2) {
                v = v.parent as? View
            }
            return v as? ViewPager2
        }

    fun canScrollHor(direction: Int): Boolean {
        val offset: Int = computeHorizontalScrollOffset()
        val range: Int = computeHorizontalScrollRange() - computeHorizontalScrollExtent()
        if (range == 0) return false
        return if (direction < 0) {
            offset > 0
        } else {
            offset < range - 1
        }
    }

    override fun onInterceptTouchEvent(p_event: MotionEvent?): Boolean {
        return true
    }

    override fun onTouchEvent(p_event: MotionEvent): Boolean {
        if (p_event.action == MotionEvent.ACTION_MOVE && parent != null) {
            parent.requestDisallowInterceptTouchEvent(true)
        }
        return super.onTouchEvent(p_event)
    }
}
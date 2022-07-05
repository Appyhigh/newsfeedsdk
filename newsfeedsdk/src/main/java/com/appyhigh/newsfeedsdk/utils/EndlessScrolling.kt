package com.appyhigh.newsfeedsdk.utils

import android.content.DialogInterface
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.appyhigh.newsfeedsdk.callbacks.AdShownListener

abstract class EndlessScrolling
/**
 * Instantiates a new Endless scroll listener.
 *
 * @param linearLayoutManager the linear layout manager
 */(private val mLinearLayoutManager: LinearLayoutManager, var adShownListener: AdShownListener?=null) : RecyclerView.OnScrollListener() {
    private var scrolledDistance = 0
    private var controlsVisible = true
    private var previousTotal = 0
    private var loading = true
    private val visibleThreshold = 1
    private var firstVisibleItem = 0
    private var visibleItemCount = 0
    private var totalItemCount = 0
    private var currentPage = 0
    fun setCurrentPage(currentPage: Int) {
        this.currentPage = currentPage
        previousTotal = 0
    }

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)
        visibleItemCount = recyclerView.childCount
        totalItemCount = mLinearLayoutManager.itemCount
        firstVisibleItem = mLinearLayoutManager.findFirstVisibleItemPosition()
        if(mLinearLayoutManager.findFirstCompletelyVisibleItemPosition()>1){
            adShownListener?.onAdShown("hide")
        } else{
            adShownListener?.onAdShown("show")
        }
        if (totalItemCount < previousTotal) {
            currentPage = 0
            previousTotal = totalItemCount
            if (totalItemCount == 0) {
                loading = true
            }
        }
        if (loading) {
            if (totalItemCount > previousTotal) {
                loading = false
                previousTotal = totalItemCount
            }
        }
        if (!loading && firstVisibleItem + visibleItemCount + visibleThreshold >= totalItemCount) {
            // End has been reached

            // Do something
            currentPage++
            onLoadMore(currentPage)
            loading = true
        }
        if (scrolledDistance > visibleThreshold && controlsVisible) {
            onHide()
            controlsVisible = false
            scrolledDistance = 0
        } else if (scrolledDistance < -visibleThreshold && !controlsVisible) {
            onShow()
            controlsVisible = true
            scrolledDistance = 0
        }
        if (controlsVisible && dy > 0 || !controlsVisible && dy < 0) {
            scrolledDistance += dy
        }
    }

    /**
     * On load more.
     *
     * @param currentPages the current page
     */
    abstract fun onLoadMore(currentPages: Int)

    /**
     * On hide.
     */
    abstract fun onHide()

    /**
     * On show.
     */
    abstract fun onShow()

    companion object {
        /**
         * The constant TAG.
         */
        private val TAG = EndlessScrolling::class.java.simpleName
        private const val HIDE_THRESHOLD = 20
    }
}
package com.appyhigh.newsfeedsdk.fragment

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.fragment.app.Fragment
import com.appyhigh.newsfeedsdk.utils.HtmlHelper

private const val ARG_PARAM1 = "id"

class ChartFragment : Fragment() {
    private lateinit var mWebView: WebView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        mWebView = WebView(requireActivity())
        mWebView.settings.javaScriptEnabled = true
        mWebView.settings.domStorageEnabled = true
        val coinSymbol = requireArguments().getString(ARG_PARAM1)!!
        val html = HtmlHelper.getFullScreenHtml(coinSymbol)
        mWebView.loadDataWithBaseURL(null,html,"text/html","utf-8",null)

    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return mWebView
    }

    override fun onDetach() {
        super.onDetach()
        if (retainInstance && mWebView.parent is ViewGroup) {
            (mWebView.parent as ViewGroup).removeView(mWebView)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(coinSymbol: String) =
            ChartFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, coinSymbol)
                }
            }
    }
}
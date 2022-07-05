package com.appyhigh.newsfeedsdk.activity

import android.os.Bundle
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.model.feeds.Card

class ContactPublisherActivity : AppCompatActivity() {
    private var webView: WebView? = null
    private var ivBroken: AppCompatImageView? = null
    private var ivBack: AppCompatImageView? = null
    private var tvBroken: AppCompatTextView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact_publisher)
        webView = findViewById(R.id.webView)
        ivBroken = findViewById(R.id.ivBroken)
        ivBack = findViewById(R.id.ivBack)
        tvBroken = findViewById(R.id.tvBroken)
        val tvContactPublisher = findViewById<AppCompatTextView>(R.id.tvContactPublisher)
        Card.setFontFamily(tvContactPublisher)
        Card.setFontFamily(tvBroken)
        webView?.loadUrl(intent.getStringExtra("url").toString())
        webView?.settings?.javaScriptEnabled = true
        webView?.settings?.domStorageEnabled = true
        webView?.settings?.databaseEnabled = true
        ivBack?.setOnClickListener {
            onBackPressed()
        }
        webView?.webViewClient = object : WebViewClient() {
            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                webView?.visibility = View.GONE
                tvBroken?.visibility = View.VISIBLE
                ivBroken?.visibility = View.VISIBLE

            }
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                view.loadUrl(url)
                return false
            }
        }
    }
}
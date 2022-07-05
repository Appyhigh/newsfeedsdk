package com.appyhigh.newsfeedsdk.activity

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import android.webkit.WebSettings
import com.appyhigh.newsfeedsdk.databinding.ActivityTradingWebViewBinding

class TradingViewActivity : AppCompatActivity() {
    lateinit var binding: ActivityTradingWebViewBinding

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTradingWebViewBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        binding.ivBack.setOnClickListener {
            onBackPressed()
        }
        if(intent.hasExtra("url")) {
            binding.webView.loadUrl(intent.getStringExtra("url").toString())
        } else if(intent.hasExtra("coin")){
            binding.webView.settings.javaScriptEnabled = true
            binding.webView.settings.javaScriptCanOpenWindowsAutomatically = true
            binding.webView.settings.defaultTextEncodingName = "utf-8"
            binding.webView.loadUrl("file:///android_asset/try.html")
        }
        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                binding.webView.loadUrl("javascript:init('" + intent.getStringExtra("coin") + "')");
                binding.pbLoading.visibility = View.GONE
                binding.webView.visibility = View.VISIBLE
            }
        }
    }
}
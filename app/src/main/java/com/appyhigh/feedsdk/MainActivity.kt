package com.appyhigh.feedsdk

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.appyhigh.newsfeedsdk.encryption.LogDetail

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        LogDetail.init()
    }
}
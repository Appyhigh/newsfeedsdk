package com.appyhigh.newsfeedsdk.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.databinding.ActivityPwaCricketBinding
import com.appyhigh.newsfeedsdk.databinding.ActivityPwaCricketTabsBinding
import com.appyhigh.newsfeedsdk.fragment.CricketPWAFragment
import com.appyhigh.newsfeedsdk.fragment.CryptoAlertSelectFragment
import com.appyhigh.newsfeedsdk.model.feeds.Card

class PWACricketActivity : AppCompatActivity() {

    lateinit var binding: ActivityPwaCricketBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPwaCricketBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        binding.backBtn.setOnClickListener { finish() }
        Card.setFontFamily(binding.tvInterest)
        binding.tvInterest.text = intent.getStringExtra(Constants.INTEREST)
        supportFragmentManager.beginTransaction()
            .add(R.id.cricketPWAFragment, CricketPWAFragment.newInstance(intent.getStringExtra("link")!!, intent.getStringExtra(Constants.INTEREST)!!), "cricketPWA")
            .disallowAddToBackStack()
            .commit()
    }
}
package com.appyhigh.newsfeedsdk.activity

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.Constants.IS_STICKY_SERVICE_ON
import com.appyhigh.newsfeedsdk.Constants.STICKY_NOTIFICATION
import com.appyhigh.newsfeedsdk.Constants.getStickyBackground
import com.appyhigh.newsfeedsdk.Constants.getStickyTint
import com.appyhigh.newsfeedsdk.Constants.getWidgetImage
import com.appyhigh.newsfeedsdk.Constants.searchStickyModel
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.adapter.FeedFragmentPagerAdapter
import com.appyhigh.newsfeedsdk.apicalls.ApiCreateOrUpdateUser
import com.appyhigh.newsfeedsdk.apiclient.Endpoints
import com.appyhigh.newsfeedsdk.callbacks.SearchStickyItemListener
import com.appyhigh.newsfeedsdk.databinding.ActivityStickySettingsBinding
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.fragment.StickyBackgroundsFragment
import com.appyhigh.newsfeedsdk.fragment.StickyIconsFragment
import com.appyhigh.newsfeedsdk.model.SearchStickyItemModel
import com.appyhigh.newsfeedsdk.model.SearchStickyModel
import com.appyhigh.newsfeedsdk.model.feeds.Card
import com.appyhigh.newsfeedsdk.service.StickyNotificationService
import com.appyhigh.newsfeedsdk.utils.SpUtil
import com.appyhigh.newsfeedsdk.utils.isMyServiceRunning
import com.appyhigh.newsfeedsdk.utils.startStickyNotificationService
import com.appyhigh.newsfeedsdk.utils.stopStickyNotificationService
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.gson.Gson

class SettingsActivity : AppCompatActivity(), SearchStickyItemListener {

    private var binding: ActivityStickySettingsBinding? = null
    private var fragmentsList: ArrayList<Fragment> = ArrayList()
    var currentString : String =""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStickySettingsBinding.inflate(layoutInflater)
        val view = binding?.root
        setContentView(view)
        setFonts()
        val spUtil = SpUtil.spUtilInstance
        binding!!.backBtn.setOnClickListener { onBackPressed() }
        binding!!.save.setOnClickListener {
            try {
                if(searchStickyModel.icons!!.size< 4){
                    Toast.makeText(this, "Please select any 4 icons!", Toast.LENGTH_SHORT).show()
                } else{
                    val searchStickyModelString = Gson().toJson(searchStickyModel)
                    spUtil?.putString(STICKY_NOTIFICATION, searchStickyModelString)
                    binding?.save?.setTextColor(Color.parseColor("#A3B8D9"))
                    if (spUtil!!.getBoolean(IS_STICKY_SERVICE_ON)) {
                        startStickyNotificationService()
                    }
                    val currentStickyModel = Gson().fromJson(currentString, SearchStickyModel::class.java)
                    if(currentStickyModel.type!= searchStickyModel.type){
                        if(searchStickyModel.type=="solid"){
                            logFirebaseEvent("CustomiseBar","IconType","Solid")
                        } else{
                            logFirebaseEvent("CustomiseBar","IconType","Colorful")
                        }
                    }
                    if(currentStickyModel.background!=searchStickyModel.background){
                        if(searchStickyModel.backgroundType=="solid"){
                            logFirebaseEvent("CustomiseBar","Bkgdselected","Solid")
                        } else {
                            when (searchStickyModel.background!!.substring(0, 2)) {
                                "ga" -> logFirebaseEvent("CustomiseBar","Bkgdselected","Gaming")
                                "fa" -> logFirebaseEvent("CustomiseBar","Bkgdselected","Fashion")
                                "te" -> logFirebaseEvent("CustomiseBar","Bkgdselected","Tech")
                                "be" -> logFirebaseEvent("CustomiseBar","Bkgdselected","Beauty")
                                "ed" -> logFirebaseEvent("CustomiseBar","Bkgdselected","Education")
                                "gl" -> logFirebaseEvent("CustomiseBar","Bkgdselected","Glass")
                                "mi" -> logFirebaseEvent("CustomiseBar","Bkgdselected","Miscellaneous")
                            }
                        }
                    }
                }
            } catch (ex:Exception){
                LogDetail.LogEStack(ex)
            }
        }
        if(spUtil!!.getBoolean(IS_STICKY_SERVICE_ON)){
            binding!!.turnOffIcon.setImageResource(R.drawable.ic_switch_on)
        } else{
            binding!!.turnOffIcon.setImageResource(R.drawable.ic_switch_off)
        }
        if(Constants.isChecked){
            binding!!.turnOffCricketIcon.setImageResource(R.drawable.ic_switch_on)
        } else{
            binding!!.turnOffCricketIcon.setImageResource(R.drawable.ic_switch_off)
        }
        if (!spUtil!!.contains(STICKY_NOTIFICATION)) {
            spUtil.putString(STICKY_NOTIFICATION,"{ \"icons\": [\"Camera\",\"News\", \"Video\", \"Whatsapp\"], \"tint\" : \"#FFFFFF\", \"background\" : \"default\", \"type\":\"solid\", \"backgroundType\":\"solid\" }")
        }
        SpUtil.searchStickyItemListener = this
        val stickyModel = Gson().fromJson(spUtil.getString(STICKY_NOTIFICATION), SearchStickyModel::class.java)
        searchStickyModel = stickyModel
        currentString = Gson().toJson(stickyModel)
        setSampleNotification()
        fragmentsList.add(StickyIconsFragment())
        fragmentsList.add(StickyBackgroundsFragment())
        val feedFragmentPagerAdapter = FeedFragmentPagerAdapter(this, fragmentsList)
        binding!!.vpTabs.adapter = feedFragmentPagerAdapter
        val tabTitles = arrayListOf("Icons","Background")
        TabLayoutMediator(binding!!.tabLayout, binding!!.vpTabs) { tab, position ->
            tab.text = tabTitles[position]
            binding!!.vpTabs.setCurrentItem(tab.position, true)
        }.attach()
        binding!!.closeNotificationBar.setOnClickListener {
            binding!!.closeNotificationBar.isEnabled = false
            if (isMyServiceRunning(StickyNotificationService::class.java)){
                logFirebaseEvent("SettingsChange","Action","Close notification")
                stopStickyNotificationService()
            }
            finish()
        }
        binding!!.turnOffIcon.setOnClickListener {
            binding!!.turnOffIcon.isEnabled = false
            logFirebaseEvent("SettingsChange","Action","Turnoff notification")
            if(spUtil.getBoolean(IS_STICKY_SERVICE_ON)){
                spUtil.putBoolean(IS_STICKY_SERVICE_ON, false)
                binding!!.closeNotificationBar.performClick()
                binding!!.turnOffIcon.setImageResource(R.drawable.ic_switch_off)
            } else{
                spUtil.putBoolean(IS_STICKY_SERVICE_ON, true)
                startStickyNotificationService()
                binding!!.turnOffIcon.setImageResource(R.drawable.ic_switch_on)
            }
            Handler(Looper.getMainLooper()).postDelayed({
                binding!!.turnOffIcon.isEnabled = true
            },2000)
        }
        binding!!.turnOffCricketIcon.setOnClickListener {
            binding!!.turnOffCricketIcon.isEnabled = false
            logFirebaseEvent("SettingsChange","Action","Turnoff cricket notification")
            if(Constants.isChecked){
                Constants.isChecked = false
                FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let { it1 ->
                    ApiCreateOrUpdateUser().updateCricketNotificationEncrypt(
                        Endpoints.UPDATE_USER_ENCRYPTED,
                        it1,
                        false)
                }
                binding!!.turnOffCricketIcon.setImageResource(R.drawable.ic_switch_off)
            } else{
                Constants.isChecked = true
                FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let { it1 ->
                    ApiCreateOrUpdateUser().updateCricketNotificationEncrypt(
                        Endpoints.UPDATE_USER_ENCRYPTED,
                        it1,
                        true)
                }
                binding!!.turnOffCricketIcon.setImageResource(R.drawable.ic_switch_on)
            }
            Handler(Looper.getMainLooper()).postDelayed({
                binding!!.turnOffCricketIcon.isEnabled = true
            },2000)
        }
    }

    override fun onBackPressed() {
        try {
            if (isTaskRoot) {
                val activity = Class.forName(FeedSdk.feedTargetActivity) as Class<out Activity?>?
                startActivity(Intent(this, activity).putExtra("fromSticky", "true"))
                finish()
            } else {
                super.onBackPressed()
            }
        } catch (ex:Exception){
            super.onBackPressed()
        }
    }

    private fun setSampleNotification(){
        try {
            val tilesLayouts = arrayOf(binding!!.tile1, binding!!.tile2, binding!!.tile3, binding!!.tile4)
            val tileIconUrls = arrayOf(binding!!.tileIconUrl1, binding!!.tileIconUrl2, binding!!.tileIconUrl3, binding!!.tileIconUrl4)
            val tileIconFrames = arrayOf(binding!!.tileIconFrame1, binding!!.tileIconFrame2, binding!!.tileIconFrame3, binding!!.tileIconFrame4)
            val tilesNames = arrayOf(binding!!.tileName1, binding!!.tileName2, binding!!.tileName3, binding!!.tileName4)
            val scale: Float = resources.displayMetrics.density
            val padding = (5 * scale + 0.5f).toInt()
            val isColored = searchStickyModel.type!! == "color"
            val background = getStickyBackground(searchStickyModel.backgroundType!!, searchStickyModel.background!!)
            if(searchStickyModel.backgroundType=="color"){
                binding!!.notificationLayout.setBackgroundResource(background)
            } else {
                binding!!.notificationLayout.setBackgroundColor(background)
            }
            binding!!.searchIconUrl.setImageResource(if(isColored) R.drawable.ic_sticky_color_search else R.drawable.ic_sticky_solid_search)
            binding!!.tileName0.setTextColor(Color.parseColor(searchStickyModel.tint))
//            if(isColored){
//                binding!!.searchIconFrame.setBackgroundResource(R.drawable.bg_sticky_icon_not_selected)
//                binding!!.searchIconUrl.setPadding(padding, padding, padding, padding)
//            } else{
//                binding!!.searchIconFrame.background = null
//                binding!!.searchIconUrl.setPadding(0, 0, 0, 0)
//            }
            val searchPadding = if(!isColored) 5 else 0
            binding!!.searchIconUrl.setPadding(searchPadding, searchPadding, searchPadding, searchPadding)
            binding!!.settings.setImageResource(if(isColored) R.drawable.ic_sticky_color_settings else R.drawable.ic_sticky_solid_settings)
//            if(!isColored){
//                binding!!.searchIconUrl.setColorFilter(Color.parseColor(searchStickyModel.tint))
//            } else{
                binding!!.searchIconUrl.colorFilter = null
//            }
            for (i in 0..3) {
                if (i < searchStickyModel.icons!!.size) {
                    tilesLayouts[i].visibility =  View.VISIBLE
                    tilesNames[i].text =searchStickyModel.icons!![i]
                    tilesNames[i].setTextColor(Color.parseColor(searchStickyModel.tint))
                    if(!isColored){
                        tileIconFrames[i].background = null
                        if(searchStickyModel.icons!![i]!="Whatsapp") {
                            tileIconUrls[i].setPadding(0, 0, 0, 0)
                            tileIconUrls[i].setColorFilter(Color.parseColor(searchStickyModel.tint))
                        } else{
                            tileIconUrls[i].setPadding(7, 7, 7, 7)
                        }
                    } else{
                        tileIconFrames[i].setBackgroundResource(R.drawable.bg_sticky_icon_not_selected)
                        tileIconUrls[i].setPadding(padding, padding, padding, padding)
                        tileIconUrls[i].setColorFilter(null)
                    }
                    tileIconUrls[i].setImageResource(getWidgetImage(isColored, searchStickyModel.icons!![i]))
                } else {
                    tilesLayouts[i].visibility =  View.GONE
                }
            }
        } catch (ex:Exception){
            LogDetail.LogEStack(ex)
        }
    }

    override fun onItemSelected(iconModel: SearchStickyItemModel) {
        val refreshNeeded = searchStickyModel.type!=iconModel.type
        if(iconModel.isSelected){
            if(!searchStickyModel.icons!!.contains(iconModel.iconName)){
                if(searchStickyModel.icons!!.size >=4){
                    SpUtil.stickyIconListener?.onUnSelected(searchStickyModel.icons!![0], searchStickyModel.type!!)
                    searchStickyModel.icons!!.removeAt(0)
                }
                searchStickyModel.icons!!.add(iconModel.iconName)
            }
            searchStickyModel.type = iconModel.type
        } else{
            searchStickyModel.icons!!.remove(iconModel.iconName)
        }
        if(searchStickyModel.backgroundType =="color" && searchStickyModel.background == "default") {
            searchStickyModel.tint ="#B9C5D9"
        } else{
            searchStickyModel.tint = getStickyTint(searchStickyModel.background!!)
        }
        if(refreshNeeded){
            SpUtil.stickyIconListener?.onRefresh()
        }
        setSampleNotification()
        if(currentString!= searchStickyModel.toString()){
            binding?.save?.setTextColor(Color.parseColor("#007AFF"))
        } else{
            binding?.save?.setTextColor(Color.parseColor("#A3B8D9"))
        }
    }

    override fun onBackgroundClicked(iconModel: SearchStickyItemModel) {
        if(iconModel.isSelected){
            searchStickyModel.background = iconModel.iconName
            searchStickyModel.backgroundType = iconModel.type
        } else{
            searchStickyModel.background = "default"
        }
        if(searchStickyModel.backgroundType=="color" && searchStickyModel.background == "default") {
            searchStickyModel.tint ="#B9C5D9"
        } else{
            searchStickyModel.tint = getStickyTint(searchStickyModel.background!!)
        }
        setSampleNotification()
            if(currentString!= searchStickyModel.toString()){
            binding?.save?.setTextColor(Color.parseColor("#007AFF"))
        } else{
            binding?.save?.setTextColor(Color.parseColor("#A3B8D9"))
        }
    }

    fun logFirebaseEvent(event:String, param:String, value:String){
        val bundle = Bundle()
        bundle.putString(param, value)
        FirebaseAnalytics.getInstance(this).logEvent(event, bundle)
    }

    private fun setFonts(){
        Card.setFontFamily(binding?.title)
        Card.setFontFamily(binding?.save)
        Card.setFontFamily(binding?.tileName0)
        Card.setFontFamily(binding?.tileName1)
        Card.setFontFamily(binding?.tileName2)
        Card.setFontFamily(binding?.tileName3)
        Card.setFontFamily(binding?.tileName4)
        Card.setFontFamily(binding?.customiseTitle)
        Card.setFontFamily(binding?.notificationText)
        Card.setFontFamily(binding?.cricketNotificationText)
        Card.setFontFamily(binding?.closeNotificationBar)
    }

}
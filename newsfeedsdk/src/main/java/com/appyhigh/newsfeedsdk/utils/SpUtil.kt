package com.appyhigh.newsfeedsdk.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.Constants.IS_GEO_POINTS_UPDATED
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.adapter.CryptoWatchListUpdateListener
import com.appyhigh.newsfeedsdk.apicalls.ApiUserDetails
import com.appyhigh.newsfeedsdk.apiclient.APIClient
import com.appyhigh.newsfeedsdk.callbacks.*
import com.appyhigh.newsfeedsdk.model.IPDetailsModel
import com.appyhigh.newsfeedsdk.model.UpdateGEOPointsRequest
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import hu.akarnokd.rxjava3.retrofit.RxJava3CallAdapterFactory
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET


class SpUtil private constructor() {

    private var mContext: Context? = null
    private var mPref: SharedPreferences? = null

    fun init(context: Context) {
        try {
            if (mContext == null) {
                mContext = context
            }
            if (mPref == null) {
                mPref = mContext!!.getSharedPreferences("NEWSSDK", Context.MODE_PRIVATE)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var mInstance: SpUtil? = null
        var userResponseListener: ApiUserDetails.UserResponseListener?= null
        var personalizeCallListener: PersonalizeCallListener? = null
        var eventsListener: EventsListener? = null
        var onRefreshListeners: HashMap<String, OnRefreshListener> = HashMap()
        var isPopupAlreadyOpen = false
        var pushIntent:Intent? = null
        var liveMatchesViewRefreshListener:OnRefreshListener? = null
        var searchStickyItemListener:SearchStickyItemListener? = null
        var stickyIconListener:StickyIconListener? = null
        var matchAlertListeners:HashMap<String, CricketAlertsListener> = HashMap()
        var seeAllClickListener:HashMap<String, SeeAllClickListener> = HashMap()
        var cryptoWatchListUpdateListener: CryptoWatchListUpdateListener?=null
        var adShownListener: AdShownListener?=null
        var cryptoPodcastsListener: CryptoPodcastsListener?=null
        var cryptoEventsListener: CryptoEventsListener?=null
        var backPressListener: BackPressListener?=null
        var alertRefreshListener: OnRefreshListener?=null
        var telegramCardListener: OnRefreshListener?=null
        val spUtilInstance: SpUtil?
            get() {
                if (null == mInstance) {
                    synchronized(SpUtil::class.java) {
                        if (null == mInstance) {
                            mInstance = SpUtil()
                        }
                    }
                }
                return mInstance
            }


        fun getStateFromIP(activity: Activity){
            if (spUtilInstance!!.getBoolean(IS_GEO_POINTS_UPDATED, false)) {
                return
            }
            val retrofit: Retrofit = Retrofit.Builder()
                .baseUrl("http://ip-api.com/")
                .client(OkHttpClient())
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava3CallAdapterFactory.createWithScheduler(Schedulers.io()))
                .build()

            retrofit.create(ApiIP::class.java).getGEOFromIP()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                try{
                    if(!it.lon.isNaN() && !it.lat.isNaN()){
                        updateGEOPoints(it.lat, it.lon)
                    }
                } catch (ex:Exception){
                    ex.printStackTrace()
                }
            }, {
                it.printStackTrace()
            })
        }

        private fun showLocationPrompt(activity: Activity) {
            val locationRequest = LocationRequest.create()
            locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

            val result: Task<LocationSettingsResponse> =
                LocationServices.getSettingsClient(activity).checkLocationSettings(builder.build())

            result.addOnCompleteListener { task ->
                try {
                    val response = task.getResult(ApiException::class.java)
                    // All location settings are satisfied. The client can initialize location
                    // requests here.
                } catch (exception: ApiException) {
                    when (exception.statusCode) {
                        LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                            try {
                                // Cast to a resolvable exception.
                                val resolvable: ResolvableApiException =
                                    exception as ResolvableApiException
                                // Show the dialog by calling startResolutionForResult(),
                                // and check the result in onActivityResult().
                                resolvable.startResolutionForResult(
                                    activity, LocationRequest.PRIORITY_HIGH_ACCURACY
                                )
                                getStateFromIP(activity)
                            } catch (e: IntentSender.SendIntentException) {
                                // Ignore the error.
                            } catch (e: ClassCastException) {
                                // Ignore, should be an impossible error.
                            }
                        }
                        LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                            // Location settings are not satisfied. But could be fixed by showing the
                            // user a dialog.

                            // Location settings are not satisfied. However, we have no way to fix the
                            // settings so we won't show the dialog.
                        }
                    }
                }
            }
        }


        @SuppressLint("MissingPermission")
        fun getGEOPoints(context: Activity) {
            try {
                if (spUtilInstance!!.getBoolean(IS_GEO_POINTS_UPDATED, false)) {
                    return
                }
                val coarsePermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
                val finePermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
                if (coarsePermission == PackageManager.PERMISSION_GRANTED && finePermission == PackageManager.PERMISSION_GRANTED) {
                    val mFusedLocationClient: FusedLocationProviderClient =
                        LocationServices.getFusedLocationProviderClient(context)
                    mFusedLocationClient.lastLocation
                        .addOnSuccessListener {
                            // GPS location can be null if GPS is switched off
                            if (it != null) {
                                updateGEOPoints(it.latitude, it.longitude)
                            } else {
                                if(!isPopupAlreadyOpen) {
                                    isPopupAlreadyOpen = true
                                    showLocationPrompt(context)
                                }
                            }

                        }
                        .addOnFailureListener {
                            Log.d("TAG", "getGEOPoints: error " + it.message)
                            it.printStackTrace()
                        }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        private fun updateGEOPoints(lat: Double, lon: Double){
            val geoPoints = UpdateGEOPointsRequest(lat, lon)
            val apiInterface = APIClient().getApiInterface()
            var userResponse: Observable<String?>? = null
            if (FeedSdk.userId != null && FeedSdk.userId != "") {
                userResponse = apiInterface?.updateUser(
                    spUtilInstance!!.getString(Constants.JWT_TOKEN),
                    geoPoints
                )
                userResponse?.subscribeOn(Schedulers.newThread())
                    ?.observeOn(AndroidSchedulers.mainThread())
                    ?.subscribe({ s: String? ->
                        spUtilInstance!!.putBoolean(
                            IS_GEO_POINTS_UPDATED,
                            true
                        )
                        personalizeCallListener?.onGEOPointsUpdate()
                    },
                        { throwable: Throwable? ->
                            Log.d("TAG", "updateGEOPoints: error " + throwable?.message)
                        })
            }
        }
    }

    fun putString(key: String, value: String) {
        try {
            val editor = mPref!!.edit()
            editor.putString(key, value)
            editor.commit()
        } catch (ex:Exception){
            ex.printStackTrace()
        }
    }

    fun putLong(key: String, value: Long) {
        try {
            val editor = mPref!!.edit()
            editor.putLong(key, value)
            editor.commit()
        } catch (ex:Exception){
            ex.printStackTrace()
        }
    }

    fun putInt(key: String, value: Int) {
        try {
            val editor = mPref!!.edit()
            editor.putInt(key, value)
            editor.commit()
        } catch (ex:Exception){
            ex.printStackTrace()
        }
    }

    fun putBoolean(key: String, value: Boolean) {
        try {
            val editor = mPref!!.edit()
            editor.putBoolean(key, value)
            editor.commit()
        } catch (ex:Exception){
            ex.printStackTrace()
        }
    }

    fun getBoolean(key: String): Boolean {
        return try {
            mPref!!.getBoolean(key, false)
        } catch (ex:Exception){
            false
        }
    }

    fun getBoolean(key: String, def: Boolean): Boolean {
        return try {
            mPref!!.getBoolean(key, def)
        } catch (ex:Exception){
            ex.printStackTrace()
            def
        }
    }

    fun getString(key: String): String? {
        return try {
            mPref!!.getString(key, "")
        } catch (ex:Exception){
            ex.printStackTrace()
            ""
        }
    }

    fun getString(key: String, def: String): String? {
        return try {
            mPref!!.getString(key, def)
        } catch (ex:Exception){
            ex.printStackTrace()
            def
        }
    }

    fun getLong(key: String): Long {
        return mPref!!.getLong(key, 0)
    }

    fun getLong(key: String, defInt: Int): Long {
        return try {
            mPref!!.getLong(key, defInt.toLong())
        } catch (ex:Exception){
            ex.printStackTrace()
            defInt.toLong()
        }
    }

    fun getInt(key: String): Int {
        try {
            return mPref!!.getInt(key, 0)
        } catch (ex:Exception){
            return 0
        }
    }

    fun getInt(key: String, defInt: Int): Int {
        try {
            return mPref!!.getInt(key, defInt)
        } catch (ex:Exception){
            return defInt
        }
    }

    operator fun contains(key: String): Boolean {
        try {
            return mPref!!.contains(key)
        } catch (ex:Exception){
            return false
        }
    }


    fun remove(key: String) {
        val editor = mPref!!.edit()
        editor.remove(key)
        editor.commit()
    }

    fun clear() {
        val editor = mPref!!.edit()
        editor.clear()
        editor.commit()
    }

    interface ApiIP{
        @GET("json/")
        fun getGEOFromIP():Observable<IPDetailsModel>
    }


}

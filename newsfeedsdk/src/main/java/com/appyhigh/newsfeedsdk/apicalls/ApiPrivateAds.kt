package com.appyhigh.newsfeedsdk.apicalls

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.apiclient.APISearchStickyInterface
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.model.PrivateAdRequest
import com.appyhigh.newsfeedsdk.model.PrivateAdResponse
import com.pluto.plugins.network.okhttp.PlutoOkhttpInterceptor
import hu.akarnokd.rxjava3.retrofit.RxJava3CallAdapterFactory
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.concurrent.TimeUnit


class ApiPrivateAds {

    private var retrofit:Retrofit?=null
    private var client: OkHttpClient?=null

    private fun setRetrofit(){
        if(client==null){
            client = if(FeedSdk.hasPluto){
                OkHttpClient.Builder()
                    .readTimeout(60, TimeUnit.SECONDS)
                    .callTimeout(60, TimeUnit.SECONDS)
                    .addInterceptor(PlutoOkhttpInterceptor).build()
            } else {
                OkHttpClient.Builder()
                    .readTimeout(60, TimeUnit.SECONDS)
                    .callTimeout(60, TimeUnit.SECONDS)
                    .build()
            }
        }
        if(retrofit==null){
            retrofit = Retrofit.Builder()
                .baseUrl("https://ssp.surgex.ai/c2s/SDK/")
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .client(client!!)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
    }

    @SuppressLint("HardwareIds")
    fun getPrivateAd(context: Context, isBanner:Boolean, listener: PrivateAdResponseListener){
        setRetrofit()
        val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        val storeUrl = "https://play.google.com/store/apps/details?id=${context.packageName}"
        val privateAdRequest = PrivateAdRequest("vpv713jk24", context.packageName, deviceId,
            if(isBanner) "320x50" else "300x250", "Board Games/Puzzles", storeUrl, FeedSdk.appVersionName,
            Constants.userAgent,
        getIpv4HostAddress())
        retrofit!!.create(APISearchStickyInterface::class.java).getMobAvenue(privateAdRequest)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                try {
                    listener.onSuccess(it.body()!!)
                } catch (ex: Exception) {
                    listener.onFailure()
                }
            }, {
                listener.onFailure()
                LogDetail.LogEStack(it)
            })
    }

    fun hitAdUrls(eUrl:String, nUrl: String){
        setRetrofit()
        retrofit!!.create(APISearchStickyInterface::class.java).hitUrl(nUrl)
            ?.subscribeOn(Schedulers.io())
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribe({
                LogDetail.LogDE("hitUrl","nUrl Success")
            }, {
                LogDetail.LogEStack(it)
            })
        retrofit!!.create(APISearchStickyInterface::class.java).hitUrl(eUrl)
            ?.subscribeOn(Schedulers.io())
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribe({
                LogDetail.LogDE("hitUrl","eUrl Success")
            }, {
                LogDetail.LogEStack(it)
            })
    }

    private fun getIpv4HostAddress(): String {
        NetworkInterface.getNetworkInterfaces()?.toList()?.map { networkInterface ->
            networkInterface.inetAddresses?.toList()?.find {
                !it.isLoopbackAddress && it is Inet4Address
            }?.let { return it.hostAddress }
        }
        return "121.54.2.9"
    }
}

interface PrivateAdResponseListener {
    fun onSuccess(privateAdResponse: PrivateAdResponse)
    fun onFailure()
}
package com.appyhigh.newsfeedsdk.apiclient

import com.appyhigh.newsfeedsdk.BuildConfig
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.utils.RSAKeyGenerator
import com.appyhigh.newsfeedsdk.utils.SpUtil
import com.mocklets.pluto.PlutoInterceptor
import hu.akarnokd.rxjava3.retrofit.RxJava3CallAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


class APIClient {
    fun getClient(useV3:Boolean = false): Retrofit? {
        val interceptor = HttpLoggingInterceptor()
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY)
        val client: OkHttpClient = if(FeedSdk.hasPluto){
            OkHttpClient.Builder()
                .readTimeout(60, TimeUnit.SECONDS)
                .callTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(PlutoInterceptor())
                .addInterceptor(interceptor).build()
        } else{
            OkHttpClient.Builder()
                .readTimeout(60, TimeUnit.SECONDS)
                .callTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(interceptor).build()
        }
        try {
            val token: String =
                RSAKeyGenerator.getJwtToken(FeedSdk.appId, FeedSdk.userId) ?: ""
            SpUtil.spUtilInstance!!.putString(Constants.JWT_TOKEN, token)
//            if(!Constants.isNetworkAvailable(FeedSdk.mContext!!)){
//                Constants.Toaster.show(FeedSdk.mContext!!, "No Internet Connection!")
//            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Retrofit.Builder()
            .baseUrl(if(useV3) BuildConfig.BASE_V3_URL else BuildConfig.BASE_URL)
            .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun getApiInterface(useV3:Boolean = false): ApiInterface? {
        return getClient(useV3)!!.create(ApiInterface::class.java)
    }

    private fun getQAClient(): Retrofit? {
        val interceptor = HttpLoggingInterceptor()
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY)
        val client: OkHttpClient = if(FeedSdk.hasPluto){
            OkHttpClient.Builder()
                .addInterceptor(PlutoInterceptor())
                .addInterceptor(interceptor).build()
        }else {
            OkHttpClient.Builder()
                .addInterceptor(interceptor).build()
        }
        try {
            val token: String =
                RSAKeyGenerator.getJwtToken(FeedSdk.appId, FeedSdk.userId) ?: ""
            SpUtil.spUtilInstance!!.putString(Constants.JWT_TOKEN, token)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Retrofit.Builder()
            .baseUrl("https://feeds.apyhi.com/api/v2/")
            .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun getQAApiInterface(): ApiInterface? {
        return getQAClient()!!.create(ApiInterface::class.java)
    }
}
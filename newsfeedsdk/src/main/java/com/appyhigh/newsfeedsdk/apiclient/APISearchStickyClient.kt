package com.appyhigh.newsfeedsdk.apiclient

import com.appyhigh.newsfeedsdk.BuildConfig
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.utils.SpUtil
import com.appyhigh.newsfeedsdk.utils.StickyRSAKeyGenerator
import com.mocklets.pluto.PlutoInterceptor
import hu.akarnokd.rxjava3.retrofit.RxJava3CallAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


class APISearchStickyClient {
    fun getSearchStickyClient(): Retrofit? {
        val interceptor = HttpLoggingInterceptor()
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY)
        val client: OkHttpClient = if(FeedSdk.hasPluto){
            OkHttpClient.Builder()
                .readTimeout(60, TimeUnit.SECONDS)
                .callTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(PlutoInterceptor())
                .addInterceptor(interceptor).build()
        } else {
            OkHttpClient.Builder()
                .readTimeout(60, TimeUnit.SECONDS)
                .callTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(interceptor).build()
        }
        try {
            val token: String =
                StickyRSAKeyGenerator.getStickyJwtToken(FeedSdk.appId, FeedSdk.userId) ?: ""
            SpUtil.spUtilInstance!!.putString(Constants.SEARCH_STICKY_JWT_TOKEN, token)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Retrofit.Builder()
            .baseUrl(BuildConfig.SEARCH_STICKY_BASE_URL)
            .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun getSearchStickyApiInterface(): APISearchStickyInterface? {
        return getSearchStickyClient()!!.create(APISearchStickyInterface::class.java)
    }

}
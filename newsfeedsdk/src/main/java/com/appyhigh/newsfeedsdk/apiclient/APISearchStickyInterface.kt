package com.appyhigh.newsfeedsdk.apiclient

import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.model.PrivateAdRequest
import com.appyhigh.newsfeedsdk.model.PrivateAdResponse
import com.appyhigh.newsfeedsdk.model.SearchStickyActionModel
import com.appyhigh.newsfeedsdk.model.SearchStickyWidgetModel
import com.appyhigh.newsfeedsdk.model.crypto.CryptoFinderResponse
import io.reactivex.rxjava3.core.Observable
import retrofit2.Response
import retrofit2.http.*

interface APISearchStickyInterface {
    @GET(Endpoints.GET_ICONS_NOTIFICATION)
    fun getIconsNotification(
        @Header(Constants.AUTHORIZATION) authorization: String?,
    ): Observable<SearchStickyWidgetModel?>?

    @GET(Endpoints.GET_ICONS_SEARCH)
    fun getIconsSearch(
        @Header(Constants.AUTHORIZATION) authorization: String?,
    ): Observable<SearchStickyWidgetModel?>?

    @POST(Endpoints.USER_ACTION_NOTIFICATION)
    fun userActionNotification(
        @Header(Constants.AUTHORIZATION) authorization: String?,
        @Body searchStickyActionModel: SearchStickyActionModel
    ): Observable<String?>?

    @POST(Endpoints.USER_ACTION_SEARCH)
    fun userActionSearch(
        @Header(Constants.AUTHORIZATION) authorization: String?,
        @Body searchStickyActionModel: SearchStickyActionModel
    ): Observable<String?>?

    @GET(".")
    fun findCryptoInTV(): Observable<Response<CryptoFinderResponse>>

    @POST(".")
    fun getMobAvenue(@Body privateAdRequest: PrivateAdRequest): Observable<Response<PrivateAdResponse>>

    @GET
    fun hitEUrl(@Url url: String): Observable<String?>?

    @GET
    fun hitNUrl(@Url url: String): Observable<String?>?
}
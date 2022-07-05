package com.appyhigh.newsfeedsdk.apiclient

import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.model.SearchStickyActionModel
import com.appyhigh.newsfeedsdk.model.SearchStickyWidgetModel
import io.reactivex.rxjava3.core.Observable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

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
}
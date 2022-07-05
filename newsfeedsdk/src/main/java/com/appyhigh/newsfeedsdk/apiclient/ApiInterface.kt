package com.appyhigh.newsfeedsdk.apiclient

import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.Constants.ANOTHER_INTEREST
import com.appyhigh.newsfeedsdk.Constants.AUTHORIZATION
import com.appyhigh.newsfeedsdk.Constants.COUNTRY
import com.appyhigh.newsfeedsdk.Constants.COUNTRY_CODE
import com.appyhigh.newsfeedsdk.Constants.EMAIL
import com.appyhigh.newsfeedsdk.Constants.FEED_TYPE
import com.appyhigh.newsfeedsdk.Constants.FIRST_POST_ID
import com.appyhigh.newsfeedsdk.Constants.INTERESTS
import com.appyhigh.newsfeedsdk.Constants.IS_VIDEO
import com.appyhigh.newsfeedsdk.Constants.LANG
import com.appyhigh.newsfeedsdk.Constants.LANGUAGE
import com.appyhigh.newsfeedsdk.Constants.LATITUDE
import com.appyhigh.newsfeedsdk.Constants.LONGITUDE
import com.appyhigh.newsfeedsdk.Constants.MATCH_TYPE
import com.appyhigh.newsfeedsdk.Constants.PAGE_NUMBER
import com.appyhigh.newsfeedsdk.Constants.PHONE_NUMBER
import com.appyhigh.newsfeedsdk.Constants.POST_ID
import com.appyhigh.newsfeedsdk.Constants.POST_SOURCE
import com.appyhigh.newsfeedsdk.Constants.PUBLISHER_ID
import com.appyhigh.newsfeedsdk.Constants.SHORT_VIDEO
import com.appyhigh.newsfeedsdk.Constants.STATE
import com.appyhigh.newsfeedsdk.Constants.STATE_CODE
import com.appyhigh.newsfeedsdk.Constants.TAG
import com.appyhigh.newsfeedsdk.apicalls.ApiCrypto
import com.appyhigh.newsfeedsdk.apicalls.IssueResponseModel
import com.appyhigh.newsfeedsdk.apicalls.PodcastResponse
import com.appyhigh.newsfeedsdk.apicalls.ReportIssueModel
import com.appyhigh.newsfeedsdk.apiclient.Endpoints.CHECK_EMAIL_NUMBER_AVAILABILITY
import com.appyhigh.newsfeedsdk.apiclient.Endpoints.COMMENT_POST
import com.appyhigh.newsfeedsdk.apiclient.Endpoints.CRYPTO_ALERT_ADD
import com.appyhigh.newsfeedsdk.apiclient.Endpoints.CRYPTO_ALERT_DELETE
import com.appyhigh.newsfeedsdk.apiclient.Endpoints.CRYPTO_ALERT_MODIFY
import com.appyhigh.newsfeedsdk.apiclient.Endpoints.CRYPTO_ALERT_VIEW
import com.appyhigh.newsfeedsdk.apiclient.Endpoints.CRYPTO_SEARCH
import com.appyhigh.newsfeedsdk.apiclient.Endpoints.EXPLORE
import com.appyhigh.newsfeedsdk.apiclient.Endpoints.FOLLOW_PUBLISHER
import com.appyhigh.newsfeedsdk.apiclient.Endpoints.GET_CRICKET_SCHEDULE
import com.appyhigh.newsfeedsdk.apiclient.Endpoints.GET_CRICKET_TABS
import com.appyhigh.newsfeedsdk.apiclient.Endpoints.GET_CRYPTO_COIN_DETAILS
import com.appyhigh.newsfeedsdk.apiclient.Endpoints.GET_CRYPTO_COIN_LIST
import com.appyhigh.newsfeedsdk.apiclient.Endpoints.GET_CRYPTO_DETAILS
import com.appyhigh.newsfeedsdk.apiclient.Endpoints.GET_CRYPTO_HOME
import com.appyhigh.newsfeedsdk.apiclient.Endpoints.GET_FEEDS
import com.appyhigh.newsfeedsdk.apiclient.Endpoints.GET_INTERESTS
import com.appyhigh.newsfeedsdk.apiclient.Endpoints.GET_INTERESTS_APPWISE
import com.appyhigh.newsfeedsdk.apiclient.Endpoints.GET_LANGUAGES
import com.appyhigh.newsfeedsdk.apiclient.Endpoints.GET_POSTS_BY_TAG
import com.appyhigh.newsfeedsdk.apiclient.Endpoints.GET_POSTS_DETAILS
import com.appyhigh.newsfeedsdk.apiclient.Endpoints.GET_PUBLISHER_POSTS
import com.appyhigh.newsfeedsdk.apiclient.Endpoints.GET_REGIONAL_FEEDS
import com.appyhigh.newsfeedsdk.apiclient.Endpoints.GET_STATE_LIST
import com.appyhigh.newsfeedsdk.apiclient.Endpoints.PODCAST_CATEGORY
import com.appyhigh.newsfeedsdk.apiclient.Endpoints.PODCAST_HOME
import com.appyhigh.newsfeedsdk.apiclient.Endpoints.PODCAST_PUBLISHER
import com.appyhigh.newsfeedsdk.apiclient.Endpoints.POST_IMPRESSIONS
import com.appyhigh.newsfeedsdk.apiclient.Endpoints.REACT_POST
import com.appyhigh.newsfeedsdk.apiclient.Endpoints.REPORT_ISSUES
import com.appyhigh.newsfeedsdk.apiclient.Endpoints.REPORT_POST
import com.appyhigh.newsfeedsdk.apiclient.Endpoints.UPDATE_USER
import com.appyhigh.newsfeedsdk.apiclient.Endpoints.USER_DETAILS
import com.appyhigh.newsfeedsdk.model.*
import com.appyhigh.newsfeedsdk.model.crypto.ConvertorResponse
import com.appyhigh.newsfeedsdk.model.crypto.CryptoFinderResponse
import com.appyhigh.newsfeedsdk.model.crypto.CryptoSearchResponse
import com.appyhigh.newsfeedsdk.model.explore.ExploreResponseModel
import com.appyhigh.newsfeedsdk.model.feeds.GetFeedsResponse
import io.reactivex.rxjava3.core.Observable
import org.json.JSONObject
import retrofit2.Response
import retrofit2.http.*

interface ApiInterface {

    @GET(CHECK_EMAIL_NUMBER_AVAILABILITY)
    fun checkUserId(
        @Header(AUTHORIZATION) authorization: String?,
        @Query(EMAIL) email: String?,
        @Query(PHONE_NUMBER) phone_number: String?
    ): Observable<UserIdResponse?>?

    @POST(UPDATE_USER)
    fun updateUser(
        @Header(AUTHORIZATION) authorization: String?,
        @Body createUserRequest: CreateOrUpdateUserRequest?
    ): Observable<String?>?

    @POST(UPDATE_USER)
    fun updateUser(
        @Header(AUTHORIZATION) authorization: String?,
        @Body createUserRequest: CreateOrUpdateUserRequestWithUserName?
    ): Observable<String?>?

    @POST(UPDATE_USER)
    fun updateUser(
        @Header(AUTHORIZATION) authorization: String?,
        @Body updateNotificationRequest: UpdateNotificationRequest?
    ): Observable<String?>?

    @POST(UPDATE_USER)
    fun updateUserForBoth(
        @Header(AUTHORIZATION) authorization: String?,
        @Body createUserRequest: CreateOrUpdateUserRequestWithBoth?
    ): Observable<String?>?

    @POST(UPDATE_USER)
    fun updateUserForLanguage(
        @Header(AUTHORIZATION) authorization: String?,
        @Body createUserRequest: CreateOrUpdateUserRequestWithLanguages?
    ): Observable<String?>?

    @POST(UPDATE_USER)
    fun updateUserForInterests(
        @Header(AUTHORIZATION) authorization: String?,
        @Body createUserRequest: CreateOrUpdateUserRequestWithInterests?
    ): Observable<String?>?

    @POST(UPDATE_USER)
    fun updateUser(
        @Header(AUTHORIZATION) authorization: String?,
        @Body createUserRequest: UpdateUserPersonalizationRequest?
    ): Observable<String?>?

    @POST(UPDATE_USER)
    fun updateUser(
        @Header(AUTHORIZATION) authorization: String?,
        @Body updateGEOPointsRequest: UpdateGEOPointsRequest?
    ): Observable<String?>?

    @POST(UPDATE_USER)
    fun updateUser(
        @Header(AUTHORIZATION) authorization: String?,
        @Body updateCryptoWatchlist: UpdateCryptoWatchlist?
    ): Observable<String?>?

    @POST(UPDATE_USER)
    fun updateUser(
        @Header(AUTHORIZATION) authorization: String?,
        @Body updateUserDislikeInterests: UpdateUserDislikeInterests?
    ): Observable<String?>?

    @POST(UPDATE_USER)
    fun updateUser(
        @Header(AUTHORIZATION) authorization: String?,
        @Body updateUserState: UpdateUserState
    ): Observable<String?>?

    @POST(UPDATE_USER)
    fun updateUser(
        @Header(AUTHORIZATION) authorization: String?,
        @Body updateInterestsRequest: UpdateInterestsRequest
    ): Observable<String?>?

    @POST(UPDATE_USER)
    fun updateUserWithInterests(
        @Header(AUTHORIZATION) authorization: String?,
        @Body createUserRequest: CreateOrUpdateUserRequestWithInterests?
    ): Observable<String?>?

    @GET(GET_INTERESTS)
    fun getInterests(
        @Header(AUTHORIZATION) authorization: String?,
        @Query(LANG) language: String?
    ): Observable<InterestResponseModel?>?

    @GET(GET_INTERESTS_APPWISE)
    fun getInterestsAppWise(
        @Header(AUTHORIZATION) authorization: String?,
        @Query(INTERESTS) interests: String,
    ): Observable<InterestStringResponseModel?>?

    @GET(USER_DETAILS)
    fun getUserDetails(
        @Header(AUTHORIZATION) authorization: String?
    ): Observable<UserResponse?>?

    @GET(GET_FEEDS)
    fun getFeedsForFirstPostId(
        @Header(AUTHORIZATION) authorization: String?,
        @Query(COUNTRY_CODE) countryCode: String?,
        @Query(INTERESTS) interests: String?,
        @Query(PAGE_NUMBER) skip: Int,
        @Query(LANGUAGE) language: String?,
        @Query(FEED_TYPE) feedType: String?,
        @Query(FIRST_POST_ID) firstPostId: String?,
        @Query(POST_SOURCE) post_source: String?
    ): Observable<Response<GetFeedsResponse?>?>?

    @GET(GET_FEEDS)
    fun getFeeds(
        @Header(AUTHORIZATION) authorization: String?,
        @Query(COUNTRY_CODE) countryCode: String?,
        @Query(INTERESTS) interests: String?,
        @Query(PAGE_NUMBER) skip: Int,
        @Query(LANGUAGE) language: String?,
        @Query(POST_SOURCE) post_source: String?,
        @Query(FEED_TYPE) feedType: String?
    ): Observable<Response<GetFeedsResponse?>?>?

    @POST(REPORT_POST)
    fun reportPost(
        @Header(AUTHORIZATION) authorization: String?,
        @Body request: ReportRequest?
    ): Observable<JSONObject?>?

    @POST(REACT_POST)
    fun updateReaction(
        @Header(AUTHORIZATION) authorization: String?,
        @Body feedReactionRequest: FeedReactionRequest?
    ): Observable<FeedReactionResponse?>?

    @GET(GET_LANGUAGES)
    fun getLanguages(
        @Query(COUNTRY_CODE) country_code: String?
    ): Observable<List<Language>>

    @GET(EXPLORE)
    fun explore(
        @Header(AUTHORIZATION) authorization: String?,
        @Query(LANG) lang: String?,
        @Query(COUNTRY) country: String?,
        @Query(ANOTHER_INTEREST) another_interest: String?
    ): Observable<Response<ExploreResponseModel?>?>?

    @GET(GET_POSTS_DETAILS)
    fun getPostDetails(
        @Header(AUTHORIZATION) authorization: String?,
        @Query(POST_ID) postId: String?,
        @Query(POST_SOURCE) post_source: String?,
        @Query(FEED_TYPE) feed_type: String?,
        @Query(PAGE_NUMBER) page_number: Int?
    ): Observable<Response<PostDetailsModel?>?>?

    @POST(COMMENT_POST)
    fun postComment(
        @Header(AUTHORIZATION) authorization: String?,
        @Body request: FeedCommentRequest?
    ): Observable<FeedCommentResponseWrapper?>?

    @GET(GET_POSTS_BY_TAG)
    fun getPostsByTag(
        @Header(AUTHORIZATION) authorization: String?,
        @Query(TAG) tag: String?,
        @Query(POST_SOURCE) post_source: String?,
        @Query(FEED_TYPE) feed_type: String?,
        @Query(LANG) lang: String?,
    ): Observable<Response<GetFeedsResponse?>?>?

    @GET(GET_PUBLISHER_POSTS)
    fun getPublisherPosts(
        @Header(AUTHORIZATION) authorization: String?,
        @Query(PAGE_NUMBER) page_number: Int,
        @Query(PUBLISHER_ID) publisher_id: String?
    ): Observable<Response<GetFeedsResponse?>?>?

    @GET(GET_FEEDS)
    fun getVideoFeeds(
        @Header(AUTHORIZATION) authorization: String?,
        @Query(COUNTRY_CODE) countryCode: String?,
        @Query(PAGE_NUMBER) skip: Int,
        @Query(IS_VIDEO) isVideo: Boolean,
        @Query(SHORT_VIDEO) isShortVideo: Boolean,
        @Query(INTERESTS) interests: String?,
        @Query(LANGUAGE) language: String?,
        @Query(POST_SOURCE) post_source: String?,
        @Query(FEED_TYPE) feed_type: String?
    ): Observable<Response<GetFeedsResponse?>?>?

    @GET(GET_FEEDS)
    fun getVideoFeedsForFirstPostId(
        @Header(AUTHORIZATION) authorization: String?,
        @Query(COUNTRY_CODE) countryCode: String?,
        @Query(PAGE_NUMBER) skip: Int,
        @Query(IS_VIDEO) isVideo: Boolean,
        @Query(SHORT_VIDEO) isShortVideo: Boolean,
        @Query(INTERESTS) interests: String?,
        @Query(LANGUAGE) language: String?,
        @Query(FEED_TYPE) feedType: String?,
        @Query(FIRST_POST_ID) firstPostId: String?,
        @Query(POST_SOURCE) post_source: String?
    ): Observable<Response<GetFeedsResponse?>?>?

    @POST(FOLLOW_PUBLISHER)
    fun followPublisher(
        @Header(AUTHORIZATION) authorization: String?,
        @Body followPublisherRequest: FollowPublisherRequest?
    ): Observable<FollowPublisherResponse?>?

    @POST(POST_IMPRESSIONS)
    fun addPostImpressions(
        @Header(AUTHORIZATION) authorization: String?,
        @Body impressions_list: ImpressionsListModel
    ): Observable<Response<JSONObject>>


    @GET(GET_CRICKET_SCHEDULE)
    fun getCricketSchedule(
        @Header(AUTHORIZATION) authorization: String?,
        @Query(MATCH_TYPE) matchType: String,
        @Query(PAGE_NUMBER) page_number: Int,
        @Query(LANGUAGE) language: String?
    ): Observable<Response<CricketScheduleResponse?>?>?

    @GET(GET_CRICKET_TABS)
    fun getCricketTabs(
        @Header(AUTHORIZATION) authorization: String?
    ): Observable<Response<CricketScheduleResponse?>?>?

    @POST(REPORT_ISSUES)
    fun reportIssues(
        @Header(AUTHORIZATION) authorization: String?,
        @Body reportIssueModel: ReportIssueModel
    ): Observable<IssueResponseModel?>?


    @GET(PODCAST_HOME)
    fun getPodcastHome(
        @Header(AUTHORIZATION) authorization: String?,
        @Query(LANGUAGE) language: String?,
        @Query(PAGE_NUMBER) pageNo: Int,
        @Query(POST_SOURCE) post_source: String?,
        @Query(FEED_TYPE) feed_type: String?
    ) : Observable<Response<PodcastResponse?>?>?

    @GET(PODCAST_CATEGORY)
    fun getPodcastCategory(
        @Header(AUTHORIZATION) authorization: String?,
        @Query(INTERESTS) interests: String?,
        @Query(LANGUAGE) language: String?,
        @Query(PAGE_NUMBER) pageNo: Int
    ) : Observable<Response<PodcastResponse?>?>?

    @GET(PODCAST_PUBLISHER)
    fun getPodcastPublisher(
        @Header(AUTHORIZATION) authorization: String?,
        @Query(PUBLISHER_ID) publisher_id: String?,
        @Query(LANGUAGE) language: String?,
        @Query(PAGE_NUMBER) pageNo: Int
    ) : Observable<Response<PodcastResponse?>?>?

    @GET(GET_CRYPTO_HOME)
    fun getCryptoHome(
        @Header(AUTHORIZATION) authorization: String?,
        @Query(Constants.WATCHLIST) watchlist: String?,
        @Query(Constants.CURRENCY) currency: String?,
        @Query(PAGE_NUMBER) pageNo: Int,
        @Query(FEED_TYPE) feed_type: String?
    ) : Observable<Response<ApiCrypto.CryptoResponse?>?>?

    @GET(GET_CRYPTO_DETAILS)
    fun getCryptoDetails(
        @Header(AUTHORIZATION) authorization: String?,
        @Query(Constants.WATCHLIST) watchlist: String?,
        @Query(Constants.ORDER) order: String?,
        @Query(Constants.CURRENCY) currency: String?,
        @Query(PAGE_NUMBER) pageNo: Int
    ) : Observable<Response<ApiCrypto.CryptoDetailsResponse?>?>?

    @GET(GET_CRYPTO_COIN_DETAILS)
    fun getCryptoCoinDetails(
        @Header(AUTHORIZATION) authorization: String?,
        @Query(Constants.COIN_ID) coinId: String,
        @Query(Constants.TAB) tab: String?,
        @Query("start") start: Long?,
        @Query("end") end: Long?,
        @Query(Constants.CURRENCY) currency: String?,
        @Query(FEED_TYPE) feed_type: String?
    ) : Observable<Response<ApiCrypto.CryptoResponse?>?>?


    @GET(CRYPTO_ALERT_VIEW)
    fun getCryptoAlertView(
        @Header(AUTHORIZATION) authorization: String?
    ): Observable<Response<ApiCrypto.CryptoResponse?>?>?

    @POST(CRYPTO_ALERT_ADD)
    fun addCryptoAlerts(
        @Header(AUTHORIZATION) authorization: String?,
        @Body cryptoAlertAddModel: ApiCrypto.CryptoAlertAddModel
    ) : Observable<Response<JSONObject>>

    @POST(CRYPTO_ALERT_MODIFY)
    fun modifyCryptoAlerts(
        @Header(AUTHORIZATION) authorization: String?,
        @Body cryptoAlertAddModel: ApiCrypto.CryptoAlertAddModel
    ) : Observable<Response<JSONObject>>

    @POST(CRYPTO_ALERT_DELETE)
    fun deleteCryptoAlerts(
        @Header(AUTHORIZATION) authorization: String?,
        @Body cryptoAlertAddModel: ApiCrypto.CryptoAlertAddModel
    ) : Observable<Response<JSONObject>>

    @GET(GET_CRYPTO_COIN_LIST)
    fun getCryptoCoinList(
        @Header(AUTHORIZATION) authorization: String?,
        @Query(Constants.COIN_ID_LIST) coinId: String,
    ) : Observable<Response<ConvertorResponse?>?>?

    @GET(GET_CRYPTO_COIN_LIST)
    fun getDefaultCryptoCoinList(
        @Header(AUTHORIZATION) authorization: String?,
    ) : Observable<Response<ConvertorResponse?>?>?

    @GET(CRYPTO_SEARCH)
    fun searchCrypto(
        @Header(AUTHORIZATION) authorization: String?,
        @Query("coin") query: String
    ): Observable<Response<CryptoSearchResponse>>

    @GET
    fun findCryptoInTV(
        @Url url: String
    ): Observable<Response<CryptoFinderResponse>>

    @GET(GET_REGIONAL_FEEDS)
    fun getRegionalFeeds(
        @Header(AUTHORIZATION) authorization: String?,
        @Query(LATITUDE) latitude: Double?,
        @Query(LONGITUDE) longitude: Double?,
        @Query(STATE_CODE) stateCode: String?,
        @Query(PAGE_NUMBER) skip: Int?
    ): Observable<Response<GetFeedsResponse?>>

    @GET(GET_STATE_LIST)
    fun getStateList(
        @Header(AUTHORIZATION) authorization: String?,
    ): Observable<Response<StateListResponse?>>
}

package com.appyhigh.newsfeedsdk.apicalls

import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.Constants.GET
import com.appyhigh.newsfeedsdk.Constants.POST
import com.appyhigh.newsfeedsdk.Constants.cryptoWatchListMap
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.apiclient.APISearchStickyInterface
import com.appyhigh.newsfeedsdk.apiclient.Endpoints
import com.appyhigh.newsfeedsdk.encryption.AESCBCPKCS5Encryption
import com.appyhigh.newsfeedsdk.encryption.AuthSocket
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.encryption.SessionUser
import com.appyhigh.newsfeedsdk.model.crypto.ConvertorResponse
import com.appyhigh.newsfeedsdk.model.crypto.CryptoFinderResponse
import com.appyhigh.newsfeedsdk.model.crypto.CryptoSearchResponse
import com.appyhigh.newsfeedsdk.model.feeds.Card
import com.appyhigh.newsfeedsdk.utils.SpUtil
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import hu.akarnokd.rxjava3.retrofit.RxJava3CallAdapterFactory
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.Call
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.nio.charset.StandardCharsets

class ApiCrypto {
    private var spUtil = SpUtil.spUtilInstance

    fun getCryptoHomeEncrypted(
        apiUrl: String,
        page_number: Int,
        watchlist: String? = null,
        cryptoResponseListener: CryptoResponseListener
    ) {
        var newWatchList = ""
        if (watchlist == null && cryptoWatchListMap.size > 0) {
            var i = 0
            for (crypto in cryptoWatchListMap.values) {
                newWatchList += if (i != cryptoWatchListMap.size - 1) {
                    "$crypto,"
                } else {
                    crypto
                }
                i += 1
            }
        }
        var feedType: String? = null
        if (SpUtil.pushIntent != null && SpUtil.pushIntent!!.hasExtra("page")
            && SpUtil.pushIntent!!.getStringExtra("page") == "SDK://cryptoHome"
            && SpUtil.pushIntent!!.hasExtra("feed_type")
        ) {
            feedType = SpUtil.pushIntent!!.getStringExtra("feed_type")!!
        }
        val currency =
            if (FeedSdk.sdkCountryCode == null || FeedSdk.sdkCountryCode!!.lowercase() == "in") "inr" else "usd"

        val keys = ArrayList<String?>()
        keys.add(Constants.WATCHLIST)
        keys.add(Constants.CURRENCY)
        keys.add(Constants.PAGE_NUMBER)
        keys.add(Constants.FEED_TYPE)
        keys.add(Constants.BLOCKED_PUBLISHERS)

        val values = ArrayList<String?>()
        values.add(watchlist)
        values.add(currency)
        values.add(page_number.toString())
        values.add(feedType)
        values.add(Constants.userDetails?.let { Constants.getStringFromList(it.blockedPublishers) })

        val allDetails = BaseAPICallObject().getBaseObjectWithAuth(GET, apiUrl, keys, values)

        LogDetail.LogDE("Test Data", allDetails.toString())
        val publicKey = SessionUser.Instance().publicKey
        val instanceEncryption = AESCBCPKCS5Encryption().getInstance(
            SessionUser.Instance().getPrivateKey(publicKey)
        )
        val sendingData: String = instanceEncryption.encrypt(
            allDetails.toString().toByteArray(
                StandardCharsets.UTF_8
            )
        ) + "." + publicKey
        LogDetail.LogD("Data to be Sent -> ", sendingData)

        AuthSocket.Instance().postData(sendingData, object : ResponseListener {
            override fun onSuccess(apiUrl: String, response: String) {
                LogDetail.LogDE("ApiGetCrypto $apiUrl", response.toString())
                val gson: Gson = GsonBuilder().create()
                val cryptoResponseBase: CryptoResponse =
                    gson.fromJson(
                        response.toString(),
                        object : TypeToken<CryptoResponse>() {}.type
                    )
                val cryptoResponse: Response<CryptoResponse> =
                    Response.success(cryptoResponseBase)

                try {
                    cryptoResponseListener.onSuccess(
                        cryptoResponse!!.body()!!,
                        cryptoResponse.raw().request.url.toString(),
                        cryptoResponse.raw().sentRequestAtMillis
                    )
                    if (page_number == 1) {
                        val gson = Gson()
                        SpUtil.spUtilInstance!!.putString(
                            "crypto_home_response",
                            gson.toJson(cryptoResponse.body())
                        )
                    }
                } catch (ex: Exception) {
                    LogDetail.LogEStack(ex)
                }
            }

            override fun onError(call: Call, e: IOException) {
                LogDetail.LogDE("ApiGetCrypto $apiUrl", e.toString())
                if (page_number == 1) {
                    try {
                        val res = SpUtil.spUtilInstance!!.getString("crypto_home_response", "")
                        LogDetail.LogD("CryptoHome", "getCryptoHome: " + res)
                        if (!res.isNullOrEmpty()) {
                            val gson = Gson()
                            val response: CryptoResponse =
                                gson.fromJson(res, CryptoResponse::class.java)
                            cryptoResponseListener.onSuccess(response, "", 0)
                        }
                    } catch (ex: Exception) {
                        LogDetail.LogEStack(ex)
                    }
                }
            }
        })
    }

    fun getCryptoDetailsEncrypted(
        apiUrl: String,
        page_number: Int,
        watchlist: String? = null,
        order: String? = null,
        cryptoResponseListener: CryptoDetailsResponseListener
    ) {
        val currency =
            if (FeedSdk.sdkCountryCode == null || FeedSdk.sdkCountryCode!!.lowercase() == "in") "inr" else "usd"
        val keys = ArrayList<String?>()
        val values = ArrayList<String?>()

        keys.add(Constants.WATCHLIST)
        keys.add(Constants.ORDER)
        keys.add(Constants.CURRENCY)
        keys.add(Constants.PAGE_NUMBER)

        values.add(watchlist)
        values.add(order)
        values.add(currency)
        values.add(page_number.toString())

        val allDetails = BaseAPICallObject().getBaseObjectWithAuth(GET, apiUrl, keys, values)

        LogDetail.LogDE("Test Data", allDetails.toString())
        val publicKey = SessionUser.Instance().publicKey
        val instanceEncryption = AESCBCPKCS5Encryption().getInstance(
            SessionUser.Instance().getPrivateKey(publicKey)
        )
        val sendingData: String = instanceEncryption.encrypt(
            allDetails.toString().toByteArray(
                StandardCharsets.UTF_8
            )
        ) + "." + publicKey
        LogDetail.LogD("Data to be Sent -> ", sendingData)

        AuthSocket.Instance().postData(sendingData, object : ResponseListener {
            override fun onSuccess(apiUrl: String, response: String) {
                LogDetail.LogDE("ApiGetCryptoDetails $apiUrl", response.toString())
                val gson: Gson = GsonBuilder().create()
                val cryptoResponseBase: CryptoDetailsResponse =
                    gson.fromJson(
                        response.toString(),
                        object : TypeToken<CryptoDetailsResponse>() {}.type
                    )
                val cryptoResponse: Response<CryptoDetailsResponse> =
                    Response.success(cryptoResponseBase)

                try {
                    cryptoResponseListener.onSuccess(
                        cryptoResponse!!.body()!!,
                        cryptoResponse.raw().request.url.toString(),
                        cryptoResponse.raw().sentRequestAtMillis
                    )
                } catch (ex: Exception) {
                    LogDetail.LogEStack(ex)
                }
            }

            override fun onError(call: Call, e: IOException) {
                LogDetail.LogDE("ApiGetCrypto $apiUrl", e.toString())
            }
        })
    }

    fun getCryptoCoinDetailsEncrypted(
        apiUrl: String,
        coinId: String,
        tab: String? = null,
        start: Long? = null,
        end: Long? = null,
        feedType: String? = null,
        cryptoResponseListener: CryptoResponseListener
    ) {
        val currency =
            if (FeedSdk.sdkCountryCode == null || FeedSdk.sdkCountryCode!!.lowercase() == "in") "inr" else "usd"
        val keys = ArrayList<String?>()
        val values = ArrayList<String?>()
        keys.add(Constants.TAB)
        keys.add("start")
        keys.add("end")
        keys.add(Constants.FEED_TYPE)
        keys.add(Constants.COIN_ID)
        keys.add(Constants.CURRENCY)

        values.add(tab)
        values.add(start?.toString())
        values.add(end?.toString())
        values.add(feedType)
        values.add(coinId)
        values.add(currency)

        val allDetails = BaseAPICallObject().getBaseObjectWithAuth(GET, apiUrl, keys, values)

        LogDetail.LogDE("Test Data", allDetails.toString())
        val publicKey = SessionUser.Instance().publicKey
        val instanceEncryption = AESCBCPKCS5Encryption().getInstance(
            SessionUser.Instance().getPrivateKey(publicKey)
        )
        val sendingData: String = instanceEncryption.encrypt(
            allDetails.toString().toByteArray(
                StandardCharsets.UTF_8
            )
        ) + "." + publicKey
        LogDetail.LogD("Data to be Sent -> ", sendingData)

        AuthSocket.Instance().postData(sendingData, object : ResponseListener {
            override fun onSuccess(apiUrl: String, response: String) {
                LogDetail.LogDE("ApiGetCryptoCoinDetails $apiUrl", response.toString())
                val gson: Gson = GsonBuilder().create()
                val cryptoResponseBase: CryptoResponse =
                    gson.fromJson(
                        response.toString(),
                        object : TypeToken<CryptoResponse>() {}.type
                    )
                val cryptoResponse: Response<CryptoResponse> =
                    Response.success(cryptoResponseBase)
                try {
                    cryptoResponseListener.onSuccess(
                        cryptoResponse!!.body()!!,
                        cryptoResponse.raw().request.url.toString(),
                        cryptoResponse.raw().sentRequestAtMillis
                    )
                } catch (ex: Exception) {
                    LogDetail.LogEStack(ex)
                }
            }

            override fun onError(call: Call, e: IOException) {
                LogDetail.LogDE("ApiGetCrypto $apiUrl", e.toString())
            }
        })

    }

    fun getCryptoAlertViewEncrypted(
        apiUrl: String,
        listener: CryptoResponseListener
    ) {
        val allDetails = BaseAPICallObject().getBaseObjectWithAuth(GET, apiUrl, ArrayList(), ArrayList())
        LogDetail.LogDE("Test Data", allDetails.toString())
        val publicKey = SessionUser.Instance().publicKey
        val instanceEncryption = AESCBCPKCS5Encryption().getInstance(
            SessionUser.Instance().getPrivateKey(publicKey)
        )
        val sendingData: String = instanceEncryption.encrypt(
            allDetails.toString().toByteArray(
                StandardCharsets.UTF_8
            )
        ) + "." + publicKey
        LogDetail.LogD("Data to be Sent -> ", sendingData)

        AuthSocket.Instance().postData(sendingData, object : ResponseListener {
            override fun onSuccess(apiUrl: String, response: String) {
                LogDetail.LogDE("ApiGetCryptoAlertView $apiUrl", response.toString())
                val gson: Gson = GsonBuilder().create()
                val cryptoResponseBase: CryptoResponse =
                    gson.fromJson(
                        response.toString(),
                        object : TypeToken<CryptoResponse>() {}.type
                    )
                val cryptoResponse: Response<CryptoResponse> =
                    Response.success(cryptoResponseBase)
                listener.onSuccess(
                    cryptoResponse.body()!!,
                    cryptoResponse.raw().request.url.toString(),
                    cryptoResponse.raw().sentRequestAtMillis
                )
            }

            override fun onError(call: Call, e: IOException) {
                LogDetail.LogDE("ApiGetCrypto $apiUrl", e.toString())
            }
        })

    }


    fun addCryptoAlertEncrypted(
        apiUrl: String,
        coinId: String,
        upperThreshold: Double?,
        lowerThreshold: Double?,
        listener: CryptoAlertResponseListener
    ) {
        val currency =
            if (FeedSdk.sdkCountryCode == null || FeedSdk.sdkCountryCode!!.lowercase() == "in") "inr" else "usd"

        val keys = ArrayList<String?>()
        val values = ArrayList<String?>()
        keys.add("coin_id")
        keys.add("currency")
        keys.add("upper_threshold")
        keys.add("lower_threshold")

        values.add(coinId)
        values.add(currency)
        values.add(upperThreshold.toString())
        values.add(lowerThreshold.toString())

        val allDetails = BaseAPICallObject().getBaseObjectWithAuth(POST, apiUrl, keys, values)
        LogDetail.LogDE("Test Data", allDetails.toString())
        val publicKey = SessionUser.Instance().publicKey
        val instanceEncryption = AESCBCPKCS5Encryption().getInstance(
            SessionUser.Instance().getPrivateKey(publicKey)
        )
        val sendingData: String = instanceEncryption.encrypt(
            allDetails.toString().toByteArray(
                StandardCharsets.UTF_8
            )
        ) + "." + publicKey
        LogDetail.LogD("Data to be Sent -> ", sendingData)

        AuthSocket.Instance().postData(sendingData, object : ResponseListener {
            override fun onSuccess(apiUrl: String, response: String) {
                LogDetail.LogDE("ApiGetCrypto $apiUrl", response.toString())
                listener.onSuccess()
            }

            override fun onError(call: Call, e: IOException) {
                LogDetail.LogDE("ApiGetCrypto $apiUrl", e.toString())
            }
        })
    }


    fun modifyCryptoAlertEncrypted(
        apiUrl: String,
        alertId: String,
        status: String,
        listener: CryptoAlertResponseListener
    ) {
        val currency =
            if (FeedSdk.sdkCountryCode == null || FeedSdk.sdkCountryCode!!.lowercase() == "in") "inr" else "usd"

        val keys = ArrayList<String?>()
        val values = ArrayList<String?>()
        keys.add("alert_id")
        keys.add("currency")
        keys.add("alert_status")

        values.add(alertId)
        values.add(currency)
        values.add(status)

        val allDetails = BaseAPICallObject().getBaseObjectWithAuth(POST, apiUrl, keys, values)
        LogDetail.LogDE("Test Data", allDetails.toString())
        val publicKey = SessionUser.Instance().publicKey
        val instanceEncryption = AESCBCPKCS5Encryption().getInstance(
            SessionUser.Instance().getPrivateKey(publicKey)
        )
        val sendingData: String = instanceEncryption.encrypt(
            allDetails.toString().toByteArray(
                StandardCharsets.UTF_8
            )
        ) + "." + publicKey
        LogDetail.LogD("Data to be Sent -> ", sendingData)

        AuthSocket.Instance().postData(sendingData, object : ResponseListener {
            override fun onSuccess(apiUrl: String, response: String) {
                LogDetail.LogDE("ApiGetCrypto $apiUrl", response)
                listener.onSuccess()
            }

            override fun onError(call: Call, e: IOException) {
                LogDetail.LogDE("ApiGetCrypto $apiUrl", e.toString())
            }
        })
    }

    fun deleteCryptoAlertEncrypted(
        apiUrl: String,
        alertId: String, listener: CryptoAlertResponseListener
    ) {
        val keys = ArrayList<String?>()
        val values = ArrayList<String?>()
        keys.add("alert_id")
        values.add(alertId)

        val allDetails = BaseAPICallObject().getBaseObjectWithAuth(POST, apiUrl, keys, values)
        LogDetail.LogDE("Test Data", allDetails.toString())
        val publicKey = SessionUser.Instance().publicKey
        val instanceEncryption = AESCBCPKCS5Encryption().getInstance(
            SessionUser.Instance().getPrivateKey(publicKey)
        )
        val sendingData: String = instanceEncryption.encrypt(
            allDetails.toString().toByteArray(
                StandardCharsets.UTF_8
            )
        ) + "." + publicKey
        LogDetail.LogD("Data to be Sent -> ", sendingData)

        AuthSocket.Instance().postData(sendingData, object : ResponseListener {
            override fun onSuccess(apiUrl: String, response: String) {
                LogDetail.LogDE("ApiGetCrypto $apiUrl", response.toString())
                listener.onSuccess()
            }

            override fun onError(call: Call, e: IOException) {
                LogDetail.LogDE("ApiGetCrypto $apiUrl", e.toString())
            }
        })
    }

    fun getCryptoCoinList(
        coinId: String,
        cryptoConvertorResponseListener: CryptoConvertorResponseListener
    ) {
        if (coinId.isEmpty()) {
            getDefaultCoinsEncrypted(
                Endpoints.GET_CRYPTO_COIN_LIST_ENCRYPTED,
                cryptoConvertorResponseListener
            )
        } else {
            getCryptoCoinsEncrypted(
                Endpoints.GET_CRYPTO_COIN_LIST_ENCRYPTED,
                coinId, cryptoConvertorResponseListener
            )
        }
    }

    fun searchCryptoCoinsEncrypted(
        apiUrl: String,
        query: String, listener: CryptoSearchListener
    ) {
        val keys = ArrayList<String?>()
        keys.add("coin")

        val values = ArrayList<String?>()
        values.add(query)
        val allDetails = BaseAPICallObject().getBaseObjectWithAuth(GET, apiUrl, keys, values)

        LogDetail.LogDE("Test Data", allDetails.toString())
        val publicKey = SessionUser.Instance().publicKey
        val instanceEncryption = AESCBCPKCS5Encryption().getInstance(
            SessionUser.Instance().getPrivateKey(publicKey)
        )
        val sendingData: String = instanceEncryption.encrypt(
            allDetails.toString().toByteArray(
                StandardCharsets.UTF_8
            )
        ) + "." + publicKey
        LogDetail.LogD("Data to be Sent -> ", sendingData)

        AuthSocket.Instance().postData(sendingData, object : ResponseListener {
            override fun onSuccess(apiUrl: String, response: String) {
                LogDetail.LogDE("searchCryptoCoinsEncrypted $apiUrl", response.toString())
                val gson: Gson = GsonBuilder().create()
                val cryptoResponseBase: CryptoSearchResponse =
                    gson.fromJson(
                        response,
                        object : TypeToken<CryptoSearchResponse>() {}.type
                    )
                val cryptoResponse: Response<CryptoSearchResponse> =
                    Response.success(cryptoResponseBase)
                listener.onSuccess(cryptoResponse.body()!!)
            }

            override fun onError(call: Call, e: IOException) {
                LogDetail.LogDE("ApiGetCrypto $apiUrl", e.toString())
            }
        })
    }

    private fun getCryptoCoinsEncrypted(
        apiUrl: String,
        coinId: String,
        cryptoConvertorResponseListener: CryptoConvertorResponseListener
    ) {
        val keys = ArrayList<String?>()
        keys.add(Constants.COIN_ID_LIST)

        val values = ArrayList<String?>()
        values.add(coinId)

        val allDetails = BaseAPICallObject().getBaseObjectWithAuth(GET, apiUrl, keys, values)

        LogDetail.LogDE("Test Data", allDetails.toString())
        val publicKey = SessionUser.Instance().publicKey
        val instanceEncryption = AESCBCPKCS5Encryption().getInstance(
            SessionUser.Instance().getPrivateKey(publicKey)
        )
        val sendingData: String = instanceEncryption.encrypt(
            allDetails.toString().toByteArray(
                StandardCharsets.UTF_8
            )
        ) + "." + publicKey
        LogDetail.LogD("Data to be Sent -> ", sendingData)

        AuthSocket.Instance().postData(sendingData, object : ResponseListener {
            override fun onSuccess(apiUrl: String, response: String) {
                LogDetail.LogDE("ApiGetCrypto $apiUrl", response.toString())
                val gson: Gson = GsonBuilder().create()
                val cryptoResponseBase: ConvertorResponse =
                    gson.fromJson(
                        response.toString(),
                        object : TypeToken<ConvertorResponse>() {}.type
                    )
                val cryptoResponse: Response<ConvertorResponse> =
                    Response.success(cryptoResponseBase)
                try {
                    cryptoConvertorResponseListener.onSuccess(cryptoResponse!!.body()!!)
                } catch (ex: Exception) {
                    LogDetail.LogEStack(ex)
                }
            }

            override fun onError(call: Call, e: IOException) {
                LogDetail.LogDE("ApiGetCrypto $apiUrl", e.toString())
            }
        })
    }

    private fun getDefaultCoinsEncrypted(
        apiUrl: String,
        cryptoConvertorResponseListener: CryptoConvertorResponseListener
    ) {
        val allDetails = BaseAPICallObject().getBaseObjectWithAuth(GET, apiUrl, ArrayList(), ArrayList())

        LogDetail.LogDE("Test Data", allDetails.toString())
        val publicKey = SessionUser.Instance().publicKey
        val instanceEncryption = AESCBCPKCS5Encryption().getInstance(
            SessionUser.Instance().getPrivateKey(publicKey)
        )
        val sendingData: String = instanceEncryption.encrypt(
            allDetails.toString().toByteArray(
                StandardCharsets.UTF_8
            )
        ) + "." + publicKey
        LogDetail.LogD("Data to be Sent -> ", sendingData)

        AuthSocket.Instance().postData(sendingData, object : ResponseListener {
            override fun onSuccess(apiUrl: String, response: String) {
                LogDetail.LogDE("ApiGetCrypto $apiUrl", response)
                val gson: Gson = GsonBuilder().create()
                val cryptoResponseBase: ConvertorResponse =
                    gson.fromJson(
                        response,
                        object : TypeToken<ConvertorResponse>() {}.type
                    )
                val cryptoResponse: Response<ConvertorResponse> =
                    Response.success(cryptoResponseBase)
                try {
                    cryptoConvertorResponseListener.onSuccess(cryptoResponse!!.body()!!)
                } catch (ex: Exception) {
                    LogDetail.LogEStack(ex)
                }
            }

            override fun onError(call: Call, e: IOException) {
                LogDetail.LogDE("ApiGetCrypto $apiUrl", e.toString())
            }
        })
    }


    fun findCrypto(symbol: String, findCryptoResponse: FindCryptoResponse) {
        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl("https://symbol-search.tradingview.com/symbol_search/?text=$symbol&hl=2&exchange=&lang=en&type=crypto&domain=production")
            .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
            .client(OkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit.create(APISearchStickyInterface::class.java).findCryptoInTV()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                try {
                    findCryptoResponse.onSuccess(it.body()!!)
                } catch (ex: Exception) {
                    LogDetail.LogEStack(ex)
                }
            }, {
                LogDetail.LogEStack(it)
                handleApiError(it)
            })
    }

    /**
     * Handle Error messages
     */
    private fun handleApiError(throwable: Throwable) {
        throwable.message?.let {
            LogDetail.LogDE(ApiCrypto::class.java.simpleName, "handleApiError: $it")
        }
    }

    interface CryptoResponseListener {
        fun onSuccess(cryptoResponse: CryptoResponse, url: String, timeStamp: Long)
    }

    interface CryptoDetailsResponseListener {
        fun onSuccess(cryptoResponse: CryptoDetailsResponse, url: String, timeStamp: Long)
    }

    interface CryptoAlertResponseListener {
        fun onSuccess()
    }

    interface CryptoConvertorResponseListener {
        fun onSuccess(convertorResponse: ConvertorResponse)
    }

    interface CryptoSearchListener {
        fun onSuccess(cryptoResponse: CryptoSearchResponse)
    }

    interface FindCryptoResponse {
        fun onSuccess(cryptoResponse: CryptoFinderResponse)
    }


    data class CryptoResponse(
        @SerializedName("cards")
        @Expose
        var cards: List<Card> = ArrayList()
    )

    data class CryptoDetailsResponse(
        @SerializedName("cards")
        @Expose
        var cards: Card? = null
    )

    data class CryptoAlertAddModel(
        var alert_id: String?,
        var coin_id: String?,
        var currency: String?,
        var upper_threshold: Double?,
        var lower_threshold: Double?,
        var alert_status: String?
    )
}
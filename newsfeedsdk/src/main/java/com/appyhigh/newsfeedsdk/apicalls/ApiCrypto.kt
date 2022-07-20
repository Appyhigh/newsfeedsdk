package com.appyhigh.newsfeedsdk.apicalls

import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.Constants.GET
import com.appyhigh.newsfeedsdk.Constants.cryptoWatchListMap
import com.appyhigh.newsfeedsdk.Constants.notNull
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
import com.google.gson.JsonObject
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import hu.akarnokd.rxjava3.retrofit.RxJava3CallAdapterFactory
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.Call
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.nio.charset.StandardCharsets

class ApiCrypto {
    private var spUtil = SpUtil.spUtilInstance

    fun getCryptoHomeEncrypted(
        apiUrl: String,
        token: String,
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

        val values = ArrayList<String?>()
        values.add(watchlist)
        values.add(currency)
        values.add(page_number.toString())
        feedType.notNull {
            keys.add(Constants.FEED_TYPE)
            values.add(it)
        }

        val allDetails = BaseAPICallObject().getBaseObjectWithAuth(GET, apiUrl, token, keys, values)

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
            override fun onSuccess(apiUrl: String?, response: JSONObject?) {
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

            override fun onSuccess(apiUrl: String?, response: JSONArray?) {
                LogDetail.LogDE("ApiGetCrypto $apiUrl", response.toString())
            }

            override fun onSuccess(apiUrl: String?, response: String?) {
                LogDetail.LogDE("ApiGetCrypto $apiUrl", response.toString())
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
        token: String,
        page_number: Int,
        watchlist: String? = null,
        order: String? = null,
        cryptoResponseListener: CryptoDetailsResponseListener
    ) {
        val currency =
            if (FeedSdk.sdkCountryCode == null || FeedSdk.sdkCountryCode!!.lowercase() == "in") "inr" else "usd"
        val keys = ArrayList<String?>()
        val values = ArrayList<String?>()
        watchlist.notNull {
            keys.add(Constants.WATCHLIST)
            values.add(it)
        }
        order.notNull {
            keys.add(Constants.ORDER)
            values.add(it)
        }
        keys.add(Constants.CURRENCY)
        keys.add(Constants.PAGE_NUMBER)
        values.add(currency)
        values.add(page_number.toString())

        val allDetails = BaseAPICallObject().getBaseObjectWithAuth(GET, apiUrl, token, keys, values)

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
            override fun onSuccess(apiUrl: String?, response: JSONObject?) {
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

            override fun onSuccess(apiUrl: String?, response: JSONArray?) {
                LogDetail.LogDE("ApiGetCrypto $apiUrl", response.toString())
            }

            override fun onSuccess(apiUrl: String?, response: String?) {
                LogDetail.LogDE("ApiGetCrypto $apiUrl", response.toString())
            }

            override fun onError(call: Call, e: IOException) {
                LogDetail.LogDE("ApiGetCrypto $apiUrl", e.toString())
            }
        })
    }

    fun getCryptoCoinDetailsEncrypted(
        apiUrl: String,
        token: String,
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
        tab.notNull {
            keys.add(Constants.TAB)
            values.add(it)
        }
        start.notNull {
            keys.add("start")
            values.add(it.toString())
        }
        end.notNull {
            keys.add("end")
            values.add(it.toString())
        }
        feedType.notNull {
            keys.add(Constants.FEED_TYPE)
            values.add(it)
        }
        keys.add(Constants.COIN_ID)
        keys.add(Constants.CURRENCY)
        values.add(coinId)
        values.add(currency)

        val allDetails = BaseAPICallObject().getBaseObjectWithAuth(GET, apiUrl, token, keys, values)

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
            override fun onSuccess(apiUrl: String?, response: JSONObject?) {
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

            override fun onSuccess(apiUrl: String?, response: JSONArray?) {
                LogDetail.LogDE("ApiGetCrypto $apiUrl", response.toString())
            }

            override fun onSuccess(apiUrl: String?, response: String?) {
                LogDetail.LogDE("ApiGetCrypto $apiUrl", response.toString())
            }

            override fun onError(call: Call, e: IOException) {
                LogDetail.LogDE("ApiGetCrypto $apiUrl", e.toString())
            }
        })

    }

    fun getCryptoAlertViewEncrypted(
        apiUrl: String,
        token: String,
        listener: CryptoResponseListener
    ) {
        val allDetails = BaseAPICallObject().getBaseObjectWithAuth(GET, apiUrl, token, ArrayList(), ArrayList())
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
            override fun onSuccess(apiUrl: String?, response: JSONObject?) {
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

            override fun onSuccess(apiUrl: String?, response: JSONArray?) {
                LogDetail.LogDE("ApiGetCrypto $apiUrl", response.toString())
            }

            override fun onSuccess(apiUrl: String?, response: String?) {
                LogDetail.LogDE("ApiGetCrypto $apiUrl", response.toString())
            }

            override fun onError(call: Call, e: IOException) {
                LogDetail.LogDE("ApiGetCrypto $apiUrl", e.toString())
            }
        })

    }


    fun addCryptoAlertEncrypted(
        apiUrl: String,
        token: String,
        coinId: String,
        upperThreshold: Double?,
        lowerThreshold: Double?,
        listener: CryptoAlertResponseListener
    ) {
        val currency =
            if (FeedSdk.sdkCountryCode == null || FeedSdk.sdkCountryCode!!.lowercase() == "in") "inr" else "usd"
        val allDetails = JsonObject()
        val main = JsonObject()
        main.addProperty(Constants.API_URl, apiUrl)
        main.addProperty(Constants.API_METHOD, Constants.POST)
        main.addProperty(Constants.API_INTERNAL, SessionUser.Instance().apiInternal)
        val dataJO = JsonObject()
        dataJO.addProperty("coin_id", coinId)
        dataJO.addProperty("currency", currency)
        upperThreshold.notNull {
            dataJO.addProperty("upper_threshold", upperThreshold)
        }
        lowerThreshold.notNull {
            dataJO.addProperty("lower_threshold", lowerThreshold)
        }
        main.add(Constants.API_DATA, dataJO)

        val headerJO = JsonObject()
        headerJO.addProperty(Constants.AUTHORIZATION, token)
        main.add(Constants.API_HEADER, headerJO)
        try {
            allDetails.add(Constants.API_CALLING, main)
            allDetails.add(Constants.USER_DETAIL, SessionUser.Instance().userDetails)
            allDetails.add(Constants.DEVICE_DETAIL, SessionUser.Instance().deviceDetails)
        } catch (e: Exception) {
            LogDetail.LogEStack(e)
        }
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
            override fun onSuccess(apiUrl: String?, response: JSONObject?) {
                LogDetail.LogDE("ApiGetCrypto $apiUrl", response.toString())
                listener.onSuccess()
            }

            override fun onSuccess(apiUrl: String?, response: JSONArray?) {
                LogDetail.LogDE("ApiGetCrypto $apiUrl", response.toString())
            }

            override fun onSuccess(apiUrl: String?, response: String?) {
                LogDetail.LogDE("ApiGetCrypto $apiUrl", response.toString())
            }

            override fun onError(call: Call, e: IOException) {
                LogDetail.LogDE("ApiGetCrypto $apiUrl", e.toString())
            }
        })
    }


    fun modifyCryptoAlertEncrypted(
        apiUrl: String,
        token: String,
        alertId: String,
        status: String,
        listener: CryptoAlertResponseListener
    ) {
        val currency =
            if (FeedSdk.sdkCountryCode == null || FeedSdk.sdkCountryCode!!.lowercase() == "in") "inr" else "usd"
        val allDetails = JsonObject()
        val main = JsonObject()
        main.addProperty(Constants.API_URl, apiUrl)
        main.addProperty(Constants.API_METHOD, Constants.POST)
        main.addProperty(Constants.API_INTERNAL, SessionUser.Instance().apiInternal)
        val dataJO = JsonObject()
        dataJO.addProperty("alert_id", alertId)
        dataJO.addProperty("currency", currency)
        dataJO.addProperty("alert_status", status)
        main.add(Constants.API_DATA, dataJO)

        val headerJO = JsonObject()
        headerJO.addProperty(Constants.AUTHORIZATION, token)
        main.add(Constants.API_HEADER, headerJO)
        try {
            allDetails.add(Constants.API_CALLING, main)
            allDetails.add(Constants.USER_DETAIL, SessionUser.Instance().userDetails)
            allDetails.add(Constants.DEVICE_DETAIL, SessionUser.Instance().deviceDetails)
        } catch (e: Exception) {
            LogDetail.LogEStack(e)
        }
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
            override fun onSuccess(apiUrl: String?, response: JSONObject?) {
                LogDetail.LogDE("ApiGetCrypto $apiUrl", response.toString())
                listener.onSuccess()
            }

            override fun onSuccess(apiUrl: String?, response: JSONArray?) {
                LogDetail.LogDE("ApiGetCrypto $apiUrl", response.toString())
            }

            override fun onSuccess(apiUrl: String?, response: String?) {
                LogDetail.LogDE("ApiGetCrypto $apiUrl", response.toString())
            }

            override fun onError(call: Call, e: IOException) {
                LogDetail.LogDE("ApiGetCrypto $apiUrl", e.toString())
            }
        })
    }

    fun deleteCryptoAlertEncrypted(
        apiUrl: String,
        token: String,
        alertId: String, listener: CryptoAlertResponseListener
    ) {
        val allDetails = JsonObject()
        val main = JsonObject()
        main.addProperty(Constants.API_URl, apiUrl)
        main.addProperty(Constants.API_METHOD, Constants.POST)
        main.addProperty(Constants.API_INTERNAL, SessionUser.Instance().apiInternal)
        val dataJO = JsonObject()
        dataJO.addProperty("alert_id", alertId)
        main.add(Constants.API_DATA, dataJO)

        val headerJO = JsonObject()
        headerJO.addProperty(Constants.AUTHORIZATION, token)
        main.add(Constants.API_HEADER, headerJO)
        try {
            allDetails.add(Constants.API_CALLING, main)
            allDetails.add(Constants.USER_DETAIL, SessionUser.Instance().userDetails)
            allDetails.add(Constants.DEVICE_DETAIL, SessionUser.Instance().deviceDetails)
        } catch (e: Exception) {
            LogDetail.LogEStack(e)
        }
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
            override fun onSuccess(apiUrl: String?, response: JSONObject?) {
                LogDetail.LogDE("ApiGetCrypto $apiUrl", response.toString())
                listener.onSuccess()
            }

            override fun onSuccess(apiUrl: String?, response: JSONArray?) {
                LogDetail.LogDE("ApiGetCrypto $apiUrl", response.toString())
            }

            override fun onSuccess(apiUrl: String?, response: String?) {
                LogDetail.LogDE("ApiGetCrypto $apiUrl", response.toString())
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
        val token = spUtil!!.getString(Constants.JWT_TOKEN)
        if (coinId.isEmpty()) {
            getDefaultCoinsEncrypted(
                Endpoints.GET_CRYPTO_COIN_LIST_ENCRYPTED,
                token, cryptoConvertorResponseListener
            )
        } else {
            getCryptoCoinsEncrypted(
                Endpoints.GET_CRYPTO_COIN_LIST_ENCRYPTED,
                coinId, token, cryptoConvertorResponseListener
            )
        }
    }

    fun searchCryptoCoinsEncrypted(
        apiUrl: String,
        token: String,
        query: String, listener: CryptoSearchListener
    ) {
        val keys = ArrayList<String?>()
        keys.add("coin")

        val values = ArrayList<String?>()
        values.add(query)
        val allDetails = BaseAPICallObject().getBaseObjectWithAuth(GET, apiUrl, token, keys, values)

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
            override fun onSuccess(apiUrl: String?, response: JSONObject?) {
                LogDetail.LogDE("searchCryptoCoinsEncrypted $apiUrl", response.toString())
                val gson: Gson = GsonBuilder().create()
                val cryptoResponseBase: CryptoSearchResponse =
                    gson.fromJson(
                        response.toString(),
                        object : TypeToken<CryptoSearchResponse>() {}.type
                    )
                val cryptoResponse: Response<CryptoSearchResponse> =
                    Response.success(cryptoResponseBase)
                listener.onSuccess(cryptoResponse.body()!!)
            }

            override fun onSuccess(apiUrl: String?, response: JSONArray?) {
                LogDetail.LogDE("searchCryptoCoinsEncrypted $apiUrl", response.toString())
                val gson: Gson = GsonBuilder().create()
                val cryptoResponseBase: CryptoSearchResponse =
                    gson.fromJson(
                        response.toString(),
                        object : TypeToken<CryptoSearchResponse>() {}.type
                    )
                val cryptoResponse: Response<CryptoSearchResponse> =
                    Response.success(cryptoResponseBase)
                listener.onSuccess(cryptoResponse.body()!!)
            }

            override fun onSuccess(apiUrl: String?, response: String?) {
                LogDetail.LogDE("ApiGetCrypto $apiUrl", response.toString())
            }

            override fun onError(call: Call, e: IOException) {
                LogDetail.LogDE("ApiGetCrypto $apiUrl", e.toString())
            }
        })
    }

    private fun getCryptoCoinsEncrypted(
        apiUrl: String,
        coinId: String,
        token: String?,
        cryptoConvertorResponseListener: CryptoConvertorResponseListener
    ) {
        val keys = ArrayList<String?>()
        keys.add(Constants.COIN_ID_LIST)

        val values = ArrayList<String?>()
        values.add(coinId)

        val allDetails = BaseAPICallObject().getBaseObjectWithAuth(GET, apiUrl, token.toString(), keys, values)

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
            override fun onSuccess(apiUrl: String?, response: JSONObject?) {
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

            override fun onSuccess(apiUrl: String?, response: JSONArray?) {
                LogDetail.LogDE("ApiGetCrypto $apiUrl", response.toString())
            }

            override fun onSuccess(apiUrl: String?, response: String?) {
                LogDetail.LogDE("ApiGetCrypto $apiUrl", response.toString())
            }

            override fun onError(call: Call, e: IOException) {
                LogDetail.LogDE("ApiGetCrypto $apiUrl", e.toString())
            }
        })
    }

    private fun getDefaultCoinsEncrypted(
        apiUrl: String,
        token: String?,
        cryptoConvertorResponseListener: CryptoConvertorResponseListener
    ) {
        val allDetails = BaseAPICallObject().getBaseObjectWithAuth(GET, apiUrl, token.toString(), ArrayList(), ArrayList())

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
            override fun onSuccess(apiUrl: String?, response: JSONObject?) {
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

            override fun onSuccess(apiUrl: String?, response: JSONArray?) {
                LogDetail.LogDE("ApiGetCrypto $apiUrl", response.toString())
            }

            override fun onSuccess(apiUrl: String?, response: String?) {
                LogDetail.LogDE("ApiGetCrypto $apiUrl", response.toString())
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
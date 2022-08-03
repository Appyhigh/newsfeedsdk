package com.appyhigh.newsfeedsdk.utils

import android.os.Build
import android.util.Base64
import android.util.Log
import com.appyhigh.newsfeedsdk.BuildConfig
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.Constants.IAT
import com.appyhigh.newsfeedsdk.Constants.JWT_TOKEN
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.utils.SpUtil.Companion.spUtilInstance
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import java.security.GeneralSecurityException
import java.security.Key
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*
import java.util.concurrent.TimeUnit

object RSAKeyGenerator {
    private val TAG = RSAKeyGenerator::class.java.canonicalName

    @get:Throws(GeneralSecurityException::class)
    private val privateKey: PrivateKey
        get() {
            val pKey = BuildConfig.PRIVATE_KEY
            var kf = KeyFactory.getInstance("RSA")
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                kf = KeyFactory.getInstance("RSA","BC")
            }
            val decode: ByteArray = Base64.decode(pKey, Base64.DEFAULT)
            val keySpecPKCS8 = PKCS8EncodedKeySpec(decode)
            return kf.generatePrivate(keySpecPKCS8)
        }

    fun getNewJwtToken(appId: String?, userId: String?): String? {
        val spUtilInstance = spUtilInstance
        val validityMs = TimeUnit.MINUTES.toMillis(180)
        val now: Date
        val exp: Date

        //get the real time in unix epoch format (milliseconds since midnight on 1 january 1970)
        val nowMillis: Long = System.currentTimeMillis()
        now = Date(nowMillis)
        exp = Date(nowMillis + validityMs)
        var privateKey: Key? = null
        try {
            privateKey = RSAKeyGenerator.privateKey
        } catch (e: GeneralSecurityException) {
            e.printStackTrace()
        }
        val jws = Jwts.builder()
            .claim("sdk_version", FeedSdk.getSDKVersion())
            .claim("app_id", appId)
            .claim("user_id", userId)
            .claim("app_version_code", FeedSdk.appVersionCode)
            .claim("app_version_name", FeedSdk.appVersionName)
            .claim("os_type", Constants.OS_PLATFORM)
            .claim("os_version", Build.VERSION.SDK_INT)
            .claim(Constants.NETWORK, SpUtil.spUtilInstance!!.getString(Constants.NETWORK))
            .claim(
                Constants.DEVICE_MODEL,
                SpUtil.spUtilInstance!!.getString(Constants.DEVICE_MODEL)
            )
            .setIssuedAt(now)
            .setExpiration(exp)
            .signWith(privateKey, SignatureAlgorithm.RS256)
            .setAudience("news-sdk")
            .compact()
        LogDetail.LogD(TAG, jws)
        if (spUtilInstance != null) {
            spUtilInstance.putString(JWT_TOKEN, jws)
            spUtilInstance.putLong(IAT, nowMillis)
        }
        return jws
    }

    fun getJwtToken(appId: String?, userId: String?): String? {
        val spUtilInstance = spUtilInstance
        val validityMs = TimeUnit.MINUTES.toMillis(180)
        val now: Date
        val exp: Date
        var prevIAT: Long = 0
        var prevJwt: String? = ""

        //get the real time in unix epoch format (milliseconds since midnight on 1 january 1970)
        val nowMillis: Long = System.currentTimeMillis()
        now = Date(nowMillis)
        exp = Date(nowMillis + validityMs)
        if (spUtilInstance != null) {
            prevIAT = spUtilInstance.getLong(IAT, 0)
            prevJwt = spUtilInstance.getString(JWT_TOKEN, "")
        }
        val timeOutInMinutes = 50
       LogDetail.LogD(
            "456__",
            "prevIAT " + prevIAT + " nowActual " + nowMillis + " diff " + (nowMillis - prevIAT)
        )
        return if (nowMillis - prevIAT > timeOutInMinutes * 60 * 1000 * 3) {
            var privateKey: Key? = null
            try {
                privateKey = RSAKeyGenerator.privateKey
            } catch (e: GeneralSecurityException) {
                e.printStackTrace()
            }
            val jws = Jwts.builder()
                .claim("sdk_version", FeedSdk.getSDKVersion())
                .claim("app_id", appId)
                .claim("user_id", userId)
                .claim("app_version_code", FeedSdk.appVersionCode)
                .claim("app_version_name", FeedSdk.appVersionName)
                .claim("os_type", Constants.OS_PLATFORM)
                .claim("os_version", Build.VERSION.SDK_INT)
                .claim(Constants.NETWORK, SpUtil.spUtilInstance!!.getString(Constants.NETWORK))
                .claim(
                    Constants.DEVICE_MODEL,
                    SpUtil.spUtilInstance!!.getString(Constants.DEVICE_MODEL)
                )
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .setAudience("news-sdk")
                .compact()
           LogDetail.LogD(TAG, jws)
            if (spUtilInstance != null) {
                spUtilInstance.putString(JWT_TOKEN, jws)
                spUtilInstance.putLong(IAT, nowMillis)
            }
            jws
        } else {
            prevJwt?.let {
               LogDetail.LogD(TAG, prevJwt)
            }
            prevJwt
        }
    }
}
package com.appyhigh.newsfeedsdk.utils

import android.os.Build
import android.util.Base64
import android.util.Log
import com.appyhigh.newsfeedsdk.BuildConfig
import com.appyhigh.newsfeedsdk.Constants
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import java.security.GeneralSecurityException
import java.security.Key
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*
import java.util.concurrent.TimeUnit


object StickyRSAKeyGenerator {
    private val TAG = "StickyAuthorisation"

    @get:Throws(GeneralSecurityException::class)
    private val searchStickyPrivateKey: PrivateKey
        get() {
            val pKey = BuildConfig.SEARCH_STICKY_PRIVATE_KEY
            var kf = KeyFactory.getInstance("RSA")
            if(android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                kf = KeyFactory.getInstance("RSA","BC")
            }
            val decode: ByteArray = Base64.decode(pKey, Base64.DEFAULT)
            val keySpecPKCS8 = PKCS8EncodedKeySpec(decode)
            return kf.generatePrivate(keySpecPKCS8)
        }

    fun getStickyJwtToken(appId: String?, userId: String?): String? {
        val spUtilInstance = SpUtil.spUtilInstance
        val validityMs = TimeUnit.MINUTES.toMillis(60)
        val now: Date
        val exp: Date
        var prevIAT: Long = 0
        var prevJwt: String? = ""

        //get the real time in unix epoch format (milliseconds since midnight on 1 january 1970)
        val nowMillis: Long = System.currentTimeMillis()
        now = Date(nowMillis)
        exp = Date(nowMillis + validityMs)
        if (spUtilInstance != null) {
            prevIAT = spUtilInstance.getLong(Constants.SEARCH_STICKY_IAT, 0)
            prevJwt = spUtilInstance.getString(Constants.SEARCH_STICKY_JWT_TOKEN, "")
        }
        val timeOutInMinutes = 50
        Log.d(
            "456__",
            "prevIAT " + prevIAT + " nowActual " + nowMillis + " diff " + (nowMillis - prevIAT)
        )
        return if (nowMillis - prevIAT > timeOutInMinutes * 60 * 1000) {
            var privateKey: Key? = null
            try {
                privateKey = StickyRSAKeyGenerator.searchStickyPrivateKey
            } catch (e: GeneralSecurityException) {
                e.printStackTrace()
            }
            val jws = Jwts.builder()
                .claim("sdk_version", 1005)
                .claim("app_id", appId)
                .claim("user_id", userId)
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
                .compact()
            Log.d(TAG, jws)
            if (spUtilInstance != null) {
                spUtilInstance.putString(Constants.SEARCH_STICKY_JWT_TOKEN, jws)
                spUtilInstance.putLong(Constants.SEARCH_STICKY_IAT, nowMillis)
            }
            jws
        } else {
            prevJwt?.let {
                Log.d(TAG, prevJwt)
            }
            prevJwt
        }
    }
}
package com.appyhigh.newsfeedsdk.encryption

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import com.appyhigh.newsfeedsdk.Constants

object LogDetail {
    private var debugger = true
    
//    @JvmStatic
//    fun init(context: Context){
//        try{
//            val ai: ApplicationInfo = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
//            val bundle = ai.metaData
//            debugger = bundle.getBoolean(Constants.FEED_DEBUGGER, false)
//        } catch (ex:Exception){ }
//    }
    @JvmStatic
    fun init(){}
    
    @JvmStatic
    fun LogD(T: String?, S: String?) {
        if (debugger) {
            if (S != null) {
                Log.d(T, S)
            }
        }
    }

    @JvmStatic
    fun LogDE(T: String?, S: String?) {
        if (debugger) {
            if (S != null) {
                Log.e(T, S)
            }
        }
    }

    @JvmStatic
    fun LogDE(T: String?, S: String?, e:Exception?) {
        if (debugger) {
            if (S != null) {
                Log.e(T, S)
            }
            e?.printStackTrace()
        }
    }

    @JvmStatic
    fun LogEStack(e:Exception?) {
        if (debugger) {
            e?.printStackTrace()
        }
    }

    @JvmStatic
    fun LogEStack(e:Throwable?) {
        if (debugger) {
            e?.printStackTrace()
        }
    }

    fun ToastD(T: Context?, S: String?) {
        if (debugger) {
            Toast.makeText(T, S, Toast.LENGTH_SHORT).show()
        }
    }

    // Release mode code
    fun LogR(T: String?, S: String?) {
        if (S != null) {
            Log.d(T, S)
        }
    }

    fun ToastR(T: Context?, S: String?) {
        Toast.makeText(T, S, Toast.LENGTH_SHORT).show()
    }

    fun println(ss: Int) {
        println(ss.toString())
    }

    @JvmStatic
    fun println(ss: String?) {
        try{
//            println(ss)
            /*if (debugger) {
                LogDetail.LogDE(ss);
            }*/
        } catch (ex:Exception){
            LogDetail.LogEStack(ex)
        }
    }
}
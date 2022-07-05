package com.appyhigh.newsfeedsdk.utils

import android.annotation.TargetApi
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.CONNECTIVITY_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkInfo
import android.net.NetworkRequest
import android.os.Build
import androidx.lifecycle.LiveData
import com.appyhigh.newsfeedsdk.Constants.NetworkState

class ConnectivityLiveData(val context: Context) : LiveData<NetworkState>() {

    private var connectivityManager: ConnectivityManager = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

    private var mPreviousState = NetworkState.UNINITIALIZED

    private lateinit var networkCallback: ConnectivityManager.NetworkCallback

    override fun onActive() {
        super.onActive()
        notifyNetworkStatus()
        when {
            // for devices above Nougat
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> connectivityManager.registerDefaultNetworkCallback(getConnectivityManagerCallback())

            // for devices b/w Lollipop and Nougat
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> lollipopNetworkAvailableRequest()

            //below lollipop
            else -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    context.registerReceiver(networkReceiver, IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"))
                }
            }
        }
    }

    override fun onInactive() {
        // When all observers are gone, remove connectivity callback or un register the receiver.
        super.onInactive()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } else {
            context.unregisterReceiver(networkReceiver)
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun lollipopNetworkAvailableRequest() {
        val builder = NetworkRequest.Builder()
            .addTransportType(android.net.NetworkCapabilities.TRANSPORT_CELLULAR)
            .addTransportType(android.net.NetworkCapabilities.TRANSPORT_WIFI)
        connectivityManager.registerNetworkCallback(builder.build(), getConnectivityManagerCallback())
    }

    private fun getConnectivityManagerCallback(): ConnectivityManager.NetworkCallback {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    postValue(NetworkState.CONNECTED)
                }

                override fun onLost(network: Network) {
                    postValue(NetworkState.DISCONNECTED)
                }
            }
            return networkCallback
        } else {
            throw IllegalAccessError("Should not have happened")
        }
    }

    private var networkReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            notifyNetworkStatus()
        }
    }

    private fun notifyNetworkStatus() {
        val activeNetwork: NetworkInfo? = connectivityManager.activeNetworkInfo
        if( activeNetwork?.isConnected == true ) {
            postValue(NetworkState.CONNECTED)
        } else {
            postValue(NetworkState.DISCONNECTED)
        }
    }

}
package com.appyhigh.newsfeedsdk.utils

import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.Constants.SOCKET_URL
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.engineio.client.transports.WebSocket
import org.json.JSONObject
import java.util.*


object SocketConnection {
    private var TAG = "SocketConnection---->"
    private var mSocket: Socket? = null
    private var mOptions: IO.Options? = null
    private var socketClientCallback: SocketClientCallback? = null
    private var socketClientCallbackNotification: SocketClientCallback? = null
    private var socketClientCallbackCommentary: SocketClientCallback? = null
    private var isSocketConnected = false

    fun initSocketConnection() {
        try {
            mSocket = getSocketConnection()
            isSocketConnected = mSocket != null && mSocket!!.connected()
            if (!isSocketConnected) {
                LogDetail.LogDE(TAG, "Initiate Connection")
                mSocket?.connect()
                setSocketEvents()
            }
        } catch (e: Exception) {
            LogDetail.LogEStack(e)
        }
    }

    fun closeSocketConnection(){
        try{
            mSocket?.close()
            isSocketConnected = mSocket!!.connected()
            LogDetail.LogDE(TAG, "Close Connection")
        } catch (ex:java.lang.Exception) {}
    }

    private fun getSocketConnection(): Socket? {
        if (mSocket == null) {
            mOptions = IO.Options()
            val token: String = RSAKeyGenerator.getJwtToken(FeedSdk.appId, FeedSdk.userId) ?: ""
            mOptions?.query = "Authorization=$token"
            mOptions?.transports = arrayOf(WebSocket.NAME)
            mOptions?.reconnection = true
            mOptions?.timeout = 15000
            mOptions?.secure = true
            mSocket = IO.socket(SOCKET_URL, mOptions)
        }
        return mSocket
    }

    private fun setSocketEvents() {

        mSocket?.off(Constants.SocketEvent.CONNECT.toString().lowercase(Locale.getDefault()))
        mSocket?.on(Constants.SocketEvent.CONNECT.toString().lowercase(Locale.getDefault())) { args ->
            LogDetail.LogDE(TAG,
                    Constants.SocketEvent.CONNECT.toString() + " to " + mSocket?.id())
        }

        mSocket?.off(Constants.SocketEvent.CONNECT_ERROR.toString().lowercase(Locale.getDefault()))
        mSocket?.on(Constants.SocketEvent.CONNECT_ERROR.toString().lowercase(Locale.getDefault())) { args ->
            LogDetail.LogDE(TAG,
                    Constants.SocketEvent.CONNECT_ERROR.toString().lowercase(Locale.getDefault()) + args[0].toString())
//            (args[0] as Exception).printStackTrace()
        }


        mSocket?.off(Constants.SocketEvent.CONNECTING.toString().lowercase(Locale.getDefault()))
        mSocket?.on(Constants.SocketEvent.CONNECTING.toString().lowercase(Locale.getDefault())) { args ->
            LogDetail.LogDE(TAG,
                    Constants.SocketEvent.CONNECTING.toString().lowercase(Locale.getDefault()))
        }


        mSocket?.off(Constants.SocketEvent.CONNECT_TIMEOUT.toString().lowercase(Locale.getDefault()))
        mSocket?.on(Constants.SocketEvent.CONNECT_TIMEOUT.toString().lowercase(Locale.getDefault())) { args ->
            LogDetail.LogDE(TAG,
                    Constants.SocketEvent.CONNECT_TIMEOUT.toString().lowercase(Locale.getDefault()))
        }


        mSocket?.off(Constants.SocketEvent.RECONNECT.toString().lowercase(Locale.getDefault()))
        mSocket?.on(Constants.SocketEvent.RECONNECT.toString().lowercase(Locale.getDefault())) { args ->
            LogDetail.LogDE(TAG,
                    Constants.SocketEvent.RECONNECT.toString().lowercase(Locale.getDefault()))
        }

        mSocket?.off(Constants.SocketEvent.RECONNECTING.toString().lowercase(Locale.getDefault()))
        mSocket?.on(Constants.SocketEvent.RECONNECTING.toString().lowercase(Locale.getDefault())) { args ->
            LogDetail.LogDE(TAG,
                    Constants.SocketEvent.RECONNECTING.toString().lowercase(Locale.getDefault()))
        }

        mSocket?.off(Constants.SocketEvent.RECONNECT_ATTEMPT.toString().lowercase(Locale.getDefault()))
        mSocket?.on(Constants.SocketEvent.RECONNECT_ATTEMPT.toString().lowercase(Locale.getDefault())) { args ->
            LogDetail.LogDE(TAG,
                    Constants.SocketEvent.RECONNECT_ATTEMPT.toString().lowercase(Locale.getDefault()))
        }

        mSocket?.off(Constants.SocketEvent.RECONNECT_ERROR.toString().lowercase(Locale.getDefault()))
        mSocket?.on(Constants.SocketEvent.RECONNECT_ERROR.toString().lowercase(Locale.getDefault())) { args ->
            LogDetail.LogDE(TAG,
                    Constants.SocketEvent.RECONNECT_ERROR.toString().lowercase(Locale.getDefault()))
        }

        mSocket?.off(Constants.SocketEvent.SCORES.toString().lowercase(Locale.getDefault()))
        mSocket?.on(Constants.SocketEvent.SCORES.toString().lowercase(Locale.getDefault())) { args ->
            LogDetail.LogDE(TAG,
                Constants.SocketEvent.SCORES.toString() + " event called closed? isSocketConnected   "+ isSocketConnected())
            if(isSocketConnected()){
                LogDetail.LogDE(TAG,
                        Constants.SocketEvent.SCORES.toString() + " livescore to " + args[0].toString())
                socketClientCallbackNotification?.getLiveScore(JSONObject(args[0].toString()))

                try {
                    LogDetail.LogDE(TAG,
                            Constants.SocketEvent.SCORES.toString() + " to " + args[0].toString().lowercase(Locale.getDefault()))
    //                val gson = Gson()
    //                val liveScoreData = gson.fromJson(args[0].toString(), LiveScoreData::class.java)
                    socketClientCallback?.onLiveScoreUpdate(args[0].toString())
                    socketClientCallbackCommentary?.onLiveScoreUpdate(args[0].toString())
                } catch (e: java.lang.Exception) {
                    LogDetail.LogEStack(e)
                }
            }
        }

        mSocket?.on("test-broadcast"){ args ->
            try {
                LogDetail.LogDE(TAG, "test-broadcast event called ")
                LogDetail.LogDE(TAG, "test-broadcast event called " + args.get(0))
            } catch (ex:Exception){
                LogDetail.LogEStack(ex)
            }

        }
    }

    fun isSocketConnected(): Boolean {
        try {
            return mSocket?.connected()!!
        } catch (e: Exception) {
            return false
        }
    }

    fun setSocketListeners(socketClientCallback: SocketClientCallback) {
        this.socketClientCallback = socketClientCallback
    }

    fun setSocketListenersNotification(socketClientCallback: SocketClientCallback) {
        this.socketClientCallbackNotification = socketClientCallback
    }

    fun setSocketListenersCommentary(socketClientCallback: SocketClientCallback) {
        this.socketClientCallbackCommentary = socketClientCallback
    }

    fun isSocketListenersNotificationSet(): Boolean{
        return this.socketClientCallbackNotification!=null
    }



    interface SocketClientCallback {
        fun onLiveScoreUpdate(liveScoreData: String)
        fun getLiveScore(liveScoreObject: JSONObject)
    }
}
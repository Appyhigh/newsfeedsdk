package com.appyhigh.newsfeedsdk.encryption;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;

import com.appyhigh.newsfeedsdk.Constants;
import com.appyhigh.newsfeedsdk.FeedSdk;
import com.appyhigh.newsfeedsdk.utils.SpUtil;
import com.google.gson.JsonObject;

import java.util.LinkedHashMap;

public class SessionUser {
    private static String TAG = SessionUser.class.getSimpleName();


    private String publicKey = "";
    private String apiMethod = "get";
    private Boolean apiInternal = true;
    private LinkedHashMap<String, String> keysMap= new LinkedHashMap<>();


    private String token = "";
    private long ttl = 0;
    private String app_version_code = "";
    private String app_version_name = "";
    private String secretKey = "MY_SECRET";

    private boolean isConnected = false;

    private static SessionUser sessionUserObj = null;

    public static SessionUser Instance() {
        if (sessionUserObj == null) {
            sessionUserObj = new SessionUser();
        }
        return sessionUserObj;
    }

    public void addPairToMap(String publicKey, String privateKey){
        keysMap.put(publicKey, privateKey);
    }


    public String getPrivateKey(String publicKey){
        return keysMap.get(publicKey);
    }

    public JsonObject getDeviceDetails() {

        JsonObject sessionDetails = new JsonObject();
        try {
            sessionDetails.addProperty("DeviceId", FeedSdk.Companion.getUserId());
            sessionDetails.addProperty("appId", FeedSdk.Companion.getAppId());
            sessionDetails.addProperty("sdk_version", FeedSdk.Companion.getSDKVersion());
            sessionDetails.addProperty("platform", "android");
            sessionDetails.addProperty("app_version_code", app_version_code);
            sessionDetails.addProperty("app_version_name", app_version_name);
            sessionDetails.addProperty("os_version", Build.VERSION.BASE_OS);
            sessionDetails.addProperty("os_type", "android");
            sessionDetails.addProperty("network", SpUtil.Companion.getSpUtilInstance().getString(Constants.NETWORK));
            sessionDetails.addProperty("manufacturer", Build.MANUFACTURER);
            sessionDetails.addProperty("device_model", Build.MODEL);
        } catch (Exception e) {
            LogDetail.LogEStack(e);
        }

        return sessionDetails;
    }

    public JsonObject getUserDetails() {

        JsonObject deviceDetails = new JsonObject();
        try {
            deviceDetails.addProperty("userId", FeedSdk.Companion.getUserId());
            deviceDetails.addProperty("true_client_ip", "");
        } catch (Exception e) {
            LogDetail.LogEStack(e);
        }

        return deviceDetails;
    }

    public String getApiMethod() {
        return apiMethod;
    }

    public void setAppDetails(Context context){
        try {
            final PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                app_version_code = String.valueOf(info.getLongVersionCode());
            } else{
                app_version_code = String.valueOf(info.versionCode);
            }
            app_version_name = String.valueOf(info.versionName);
        } catch (Exception ex){
            LogDetail.LogEStack(ex);
        }
    }

    public Boolean getApiInternal() {
        return apiInternal;
    }


    public String getToken() {
        return token;
    }

    public String getPublicKey() {
        return publicKey;
    }


    public void setPublicKey(String Key) {
        this.publicKey = Key;
    }

    public void setToken(String Key) {
        this.token = Key;
    }

    public long getTtl(){
        return ttl;
    }

    public void setTtl(String ttl){
        this.ttl = System.currentTimeMillis()+Long.parseLong(ttl)-5000;
    }

    public boolean isConnected() {
        return isConnected;
    }

}

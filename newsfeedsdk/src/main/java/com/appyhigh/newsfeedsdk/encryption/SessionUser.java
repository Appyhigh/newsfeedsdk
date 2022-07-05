package com.appyhigh.newsfeedsdk.encryption;

import android.content.Intent;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.LinkedHashMap;

public class SessionUser {
    private static String TAG = SessionUser.class.getSimpleName();


    private String publicKey = "";

    private String DeviceId = "111234567890qwertyuiop";
    private String appId = "com.appyhigh.sampleencryption";
    private String sdkId = "com.appyhigh.sdk";
    private String versionCode = "1";
    private String versionName = "v1.0.0";
    private String platform = "android";
    private String app_versionCode = "1";
    private String app_versionName = "v1.0.0";
    private String os_version = "12";
    private String network = "WiFi";
    private String manufacturer = "Google";
    private String device_model = "Pixel 6A";
    private String sha1 = "7unB9QIzrWyuIDS3haUSpnrq7Fk=";


    private String userNumber = "9819184007";
    private String userName = "suraj";
    private String userID = "1234567890";
    private String userEmail = "email@example.com";
    private String userLatitude = "0.0";
    private String userLongitude = "0.0";
    private String userFCM = "";
    private String initialMessage = "";
    private String customMsg_1 = "";
    private String customMsg_2 = "";

    private String apiURL = "/api/v2/get-interests";
    private String apiMethod = "get";
    private String apiData = "{'lang':'hi'}";
    private String apiHeader = "{}";
    private Boolean apiInternal = true;
    private LinkedHashMap<String, String> keysMap= new LinkedHashMap<>();


    private String token = "";

    private String urlAuthServer = "";

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

    public void setData(Intent mIntentData) {
        if (mIntentData != null) {


            if (mIntentData.hasExtra(AuthSocket.INTENT_CONSTANTS.AUTH_USER_NUMBER)) {
                userNumber = mIntentData.getStringExtra(AuthSocket.INTENT_CONSTANTS.AUTH_USER_NUMBER);
            }

            if (mIntentData.hasExtra(AuthSocket.INTENT_CONSTANTS.AUTH_USER_NAME)) {
                userName = mIntentData.getStringExtra(AuthSocket.INTENT_CONSTANTS.AUTH_USER_NAME);
            }

            if (mIntentData.hasExtra(AuthSocket.INTENT_CONSTANTS.AUTH_USER_ID)) {
                userID = mIntentData.getStringExtra(AuthSocket.INTENT_CONSTANTS.AUTH_USER_ID);
            }

            if (mIntentData.hasExtra(AuthSocket.INTENT_CONSTANTS.AUTH_USER_EMAIL)) {
                userEmail = mIntentData.getStringExtra(AuthSocket.INTENT_CONSTANTS.AUTH_USER_EMAIL);
            }

            if (mIntentData.hasExtra(AuthSocket.INTENT_CONSTANTS.AUTH_USER_LATITUDE)) {
                userLatitude = mIntentData.getStringExtra(AuthSocket.INTENT_CONSTANTS.AUTH_USER_LATITUDE);
            }

            if (mIntentData.hasExtra(AuthSocket.INTENT_CONSTANTS.AUTH_USER_LONGITUDE)) {
                userLongitude = mIntentData.getStringExtra(AuthSocket.INTENT_CONSTANTS.AUTH_USER_LONGITUDE);
            }

            if (mIntentData.hasExtra(AuthSocket.INTENT_CONSTANTS.AUTH_USER_FCM)) {
                userFCM = mIntentData.getStringExtra(AuthSocket.INTENT_CONSTANTS.AUTH_USER_FCM);
            }

            if (mIntentData.hasExtra(AuthSocket.INTENT_CONSTANTS.AUTH_CUSTOM_1)) {
                customMsg_1 = mIntentData.getStringExtra(AuthSocket.INTENT_CONSTANTS.AUTH_CUSTOM_1);
            }

            if (mIntentData.hasExtra(AuthSocket.INTENT_CONSTANTS.AUTH_CUSTOM_2)) {
                customMsg_2 = mIntentData.getStringExtra(AuthSocket.INTENT_CONSTANTS.AUTH_CUSTOM_2);
            }

        }
    }

    public JsonObject getDeviceDetails() {

        JsonObject sessionDetails = new JsonObject();
        try {
            sessionDetails.addProperty("DeviceId", DeviceId);
            sessionDetails.addProperty("appId", appId);
            sessionDetails.addProperty("sdkId", sdkId);
            sessionDetails.addProperty("versionCode", versionCode);
            sessionDetails.addProperty("versionName", versionName);
            sessionDetails.addProperty("platform", platform);
            sessionDetails.addProperty("app_versionCode", app_versionCode);
            sessionDetails.addProperty("app_versionName", app_versionName);
            sessionDetails.addProperty("os_version", os_version);
            sessionDetails.addProperty("network", network);
            sessionDetails.addProperty("manufacturer", manufacturer);
            sessionDetails.addProperty("device_model", device_model);
            sessionDetails.addProperty("sha1", sha1);
            sessionDetails.addProperty("uniqueId", appId);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return sessionDetails;
    }

    public JsonObject getUserDetails() {

        JsonObject deviceDetails = new JsonObject();
        try {
            deviceDetails.addProperty("userNumber", userNumber);
            deviceDetails.addProperty("userName", userName);
            deviceDetails.addProperty("userId", userID);
            deviceDetails.addProperty("userEmail", userEmail);
            deviceDetails.addProperty("userLatitude", userLatitude);
            deviceDetails.addProperty("userLongitude", userLongitude);
            deviceDetails.addProperty("userFCM", userFCM);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return deviceDetails;
    }

    public String getApp_versionCode() {
        return app_versionCode;
    }

    public void setApp_versionCode(String app_versionCode) {
        this.app_versionCode = app_versionCode;
    }

    public String getApp_versionName() {
        return app_versionName;
    }

    public void setApp_versionName(String app_versionName) {
        this.app_versionName = app_versionName;
    }

    public String getOs_version() {
        return os_version;
    }

    public void setOs_version(String os_version) {
        this.os_version = os_version;
    }

    public String getNetwork() {
        return network;
    }

    public void setNetwork(String network) {
        this.network = network;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getDevice_model() {
        return device_model;
    }

    public void setDevice_model(String device_model) {
        this.device_model = device_model;
    }

    public String getApiURL() {
        return apiURL;
    }

    public String setApiURL(String apiURL) {
        return this.apiURL = apiURL;
    }

    public String getApiMethod() {
        return apiMethod;
    }

    public String setApiMethod(String apiMethod) {
        return this.apiMethod = apiMethod;
    }

    public String getApiData() {
        return apiData;
    }

    public String setApiData(String apiData) {
        return this.apiData = apiData;
    }

    public String getApiHeader() {
        return apiHeader;
    }

    public Boolean setApiInternal(Boolean apiInternal) {
        return this.apiInternal = apiInternal;
    }

    public Boolean getApiInternal() {
        return apiInternal;
    }

    public String setApiHeader(String apiHeader) {
        return this.apiHeader = apiHeader;
    }

    public String getToken() {
        return token;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getSdkId() {
        return sdkId;
    }

    public void setSdkId(String sdkId) {
        this.sdkId = sdkId;
    }

    public String getVersionCode() {
        return versionCode;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setPublicKey(String Key) {
        this.publicKey = Key;
    }

    public void setToken(String Key) {
        this.token = Key;
    }

    public void setVersionCode(String versionCode) {
        this.versionCode = versionCode;
    }

    public String getUrlAuthServer() {
        return urlAuthServer;
    }

    public String getInitialMessage() {
        return initialMessage;
    }

    public String getCustomMsg_1() {
        return customMsg_1;
    }

    public String getCustomMsg_2() {
        return customMsg_2;
    }

    public String getUserNumber() {
        return userNumber;
    }

    public String getUserName() {
        return userName;
    }

    public String getUserID() {
        return userID;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public String getUserLatitude() {
        return userLatitude;
    }

    public String getUserLongitude() {
        return userLongitude;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void isConnected(boolean isConnected) {
        this.isConnected = isConnected;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public JsonObject getUserDataJson() {
        JsonObject mJsonObj = new JsonObject();

        mJsonObj.addProperty("userNumber", userNumber);
        mJsonObj.addProperty("userName", userName);
        mJsonObj.addProperty("userID", userID);
        mJsonObj.addProperty("userEmail", userEmail);
        mJsonObj.addProperty("userLatitude", userLatitude);
        mJsonObj.addProperty("userLongitude", userLongitude);
        mJsonObj.addProperty("userFCM", userFCM);

        return mJsonObj;
    }

    public void setDeviceDetails(JsonObject asJsonObject) {
    }

    /* TODO Move to this Device Details
    public JsonObject getDeviceDetails() {
        return deviceDetails;
    }*/


    public String getDeviceId() {
        return DeviceId;
    }

    public void setDeviceId(String deviceId) {
        DeviceId = deviceId;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getSha1() {
        return sha1;
    }

    public void setSha1(String sha1) {
        this.sha1 = sha1;
    }

    public JsonObject getApiDetails() {
        JsonObject mJsonObj = new JsonObject();

        mJsonObj.addProperty("apiURL", apiURL);
        mJsonObj.addProperty("apiMethod", apiMethod);
        mJsonObj.addProperty("apiData", apiData);
        mJsonObj.addProperty("apiHeader", apiHeader);
        mJsonObj.addProperty("apiInternal", apiInternal);

        return mJsonObj;
    }
}

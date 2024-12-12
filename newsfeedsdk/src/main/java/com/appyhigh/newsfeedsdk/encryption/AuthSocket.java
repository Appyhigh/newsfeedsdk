package com.appyhigh.newsfeedsdk.encryption;

import static com.appyhigh.newsfeedsdk.Constants.NEWS_FEED_APP_ID;
import static com.appyhigh.newsfeedsdk.Constants.USER_ID;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.appyhigh.newsfeedsdk.FeedSdk;
import com.appyhigh.newsfeedsdk.callbacks.OnAPISuccess;
import com.appyhigh.newsfeedsdk.utils.SpUtil;
import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

import io.socket.client.Socket;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class AuthSocket {
    private static final String TAG = AuthSocket.class.getSimpleName();

    static {
        System.loadLibrary("keys");
    }

    private native String nativeKey1();

    //Native SDK Get Value of String

    public interface CoLabSocketListener {
        void onConfigResponse(Object[] args);
    }

    private Context mContext = null;

    @Nullable
    private String license = null;

    private boolean started = false;
    private boolean alreadyAuthenticated = false;
    private AuthenticationSuccess authenticationSuccess;

    private final String serverURL = "https://secure-sdk-prod.apyhi.com/api/";
//    private final String serverURL = "https://secure-sdk-qa.apyhi.com/api/";
//    private final String serverURL = "http://104.161.92.74:4711/api/";

    //Authentication Encryption
    public AESCBCPKCS5Encryption instanceEncryption;
    private Socket mSocket = null;

    public Context getmContext() {
        return mContext;
    }

    private static AuthSocket mAuthSocketObj = null;
    private static CoLabSocketListener mAuthSocketListener = null;
    private String encryptionKey;

    public static AuthSocket Instance() {
        if (mAuthSocketObj == null) {
            mAuthSocketObj = new AuthSocket();
        }

        return mAuthSocketObj;
    }


    public AuthSocket() {

    }

    /**
     * Verify if NEWS_FEED_APP_ID is provided or not
     * if not throw an error
     */
    private String getAppIdFromManifest(Context context) {
        String appId = "";
        try {
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = ai.metaData;
            appId = bundle.getString(NEWS_FEED_APP_ID);
        } catch (Exception e) {
            LogDetail.LogEStack(e);
        }
        FeedSdk.Companion.setAppId(appId);
        return appId;
    }


    /**
     * Set User Id
     */
    @SuppressLint("HardwareIds")
    private String setUserId(Context context) {
        String existingId =  SpUtil.Companion.getSpUtilInstance().getString(USER_ID, "");
        String userId = (existingId!=null && !existingId.isEmpty())? existingId :
                FeedSdk.Companion.getAppId() + "_" + Settings.Secure.getString(
                        context.getContentResolver(),
                        Settings.Secure.ANDROID_ID
                );
        FeedSdk.Companion.setUserId(userId);
        return userId;
    }

    public void start(Context context, String license, AuthenticationSuccess authenticationSuccess) {
        try {
            this.license = license;
            String keyInit = adea(nativeKey1());
            this.authenticationSuccess = authenticationSuccess;
            if (alreadyAuthenticated) {
                LogDetail.LogDE("Auth Session", "Already Authenticated");
                authenticationSuccess.onAuthSuccess();
                return;
            }

            if (this.license == null || this.license.equals("")) {
                LogDetail.LogDE("Auth Session", "You must set a license before calling start(...)");
                return;
            }
            this.started = true;
            mContext = context;
            SpUtil.Companion.getSpUtilInstance().init(context);
            getAppIdFromManifest(context);
            setUserId(context);
            SessionUser.Instance().setAppDetails(context);
            String SHA1 = getSHA1(context);
            String NativeKey = String.valueOf(keyInit);
            byte[] decodedBytes = Base64.decode(NativeKey, Base64.DEFAULT);
            String decodedString = new String(decodedBytes);

            //Another Key can Be configured in Remote Config of Firebase - Challenge it would have to come from Client Firebase
//            String encryptionKey = String.valueOf(FeedSdk.Companion.getSDKVersion()).trim() + decodedString.trim() + SHA1.trim();
            String encryptionKey = "1008" + decodedString.trim() + SHA1.trim();
            LogDetail.LogDE("encryptionKey", encryptionKey);
            this.encryptionKey = encryptionKey;
            instanceEncryption = new AESCBCPKCS5Encryption().getInstance(encryptionKey.trim());
            instanceEncryption.updateKEY_IV(encryptionKey.trim());
            verifyUser();

        } catch (Exception e) {
            LogDetail.LogEStack(e);
        }
    }

    private String adea(String nativeKey1) {
        String[] values = nativeKey1.split("a0a|c0c|b0b|a0d|d0d|a0b|b0a|b0c|b0d|c0a|c0b|c0d|d0a|d0b|d0c|e0e|e0f");
        String returnKey = "";
        for (String s : values) {
            try{
                String k = String.valueOf(Integer.parseInt(s) + 1);
                returnKey = returnKey + Character.toString((char) Integer.parseInt(k));
            }catch (Exception e){

            }
        }
        return returnKey;
    }

    private void verifyUser() {
        JsonObject allDetails = new JsonObject();

        try {
            allDetails.add("userDetail", SessionUser.Instance().getUserDetails());
            allDetails.add("deviceDetail", SessionUser.Instance().getDeviceDetails());
        } catch (Exception e) {
            LogDetail.LogEStack(e);
        }

        //Data to be converted to string
        LogDetail.LogDE("DATA - > ", allDetails.toString());

        //Encrypting String
        String initialIEncryptionString = instanceEncryption.encrypt(allDetails.toString().getBytes(StandardCharsets.UTF_8)) + "." + license;
        LogDetail.LogDE("ENCRYPTED TEXT - ", initialIEncryptionString);

        verifyData(initialIEncryptionString, new com.appyhigh.newsfeedsdk.apicalls.ResponseListener() {
            @Override
            public void onSuccess(@Nullable String apiUrl, @Nullable String response, long timeStamp) {
                LogDetail.LogDE("verify", response.toString());
            }

            @Override
            public void onError(@NonNull Call call, @NonNull IOException e) {

            }
        });
    }

    //GET SHA1 Key for Application
    private String getSHA1(Context context) {
        try {
            @SuppressLint("PackageManagerGetSignatures") final PackageInfo info = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                final MessageDigest md = MessageDigest.getInstance("SHA1");
                md.update(signature.toByteArray());
//
                final byte[] digest = md.digest();

                final StringBuilder toRet = new StringBuilder();
                for (int i = 0; i < digest.length; i++) {
                    if (i != 0) toRet.append(":");
                    int b = digest[i] & 0xff;
                    String hex = Integer.toHexString(b);
                    if (hex.length() == 1) toRet.append("0");
                    toRet.append(hex);
                }

                byte[] dataBytes = toRet.toString().getBytes(StandardCharsets.UTF_8);
                MessageDigest md2 = MessageDigest.getInstance("SHA1");
                return com.appyhigh.newsfeedsdk.encryption.Base64.getEncoder().encodeToString(md2.digest(dataBytes));
//                    com.appyhigh.newsfeedsdk.encryption.Base64.getEncoder().encodeToString(md.digest());
            }
        } catch (PackageManager.NameNotFoundException e1) {
            LogDetail.LogDE("name not found", e1.toString());
            return "";
        } catch (NoSuchAlgorithmException e) {
            LogDetail.LogDE("no such an algorithm", e.toString());
            return "";
        } catch (Exception e) {
            LogDetail.LogDE("exception", e.toString());
            return "";
        }
        return "";
    }

    public static String getBase64SHA1FromHex(String hex){
        String baseRes = "";
        try{
            LogDetail.LogDE("Hexa", hex);
            byte[] dataBytes = hex.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8);
            MessageDigest md2 = MessageDigest.getInstance("SHA1");
            baseRes =  com.appyhigh.newsfeedsdk.encryption.Base64.getEncoder().encodeToString(md2.digest(dataBytes));
            LogDetail.LogDE("Hexa", "Base 64 "+baseRes);
        } catch (Exception ex){
            ex.printStackTrace();
        }
        return baseRes;
    }

    public void verifyData(String sendingData, com.appyhigh.newsfeedsdk.apicalls.ResponseListener responseListener) {
        OkHttpClient client = new OkHttpClient();
        RequestBody formBody = new FormBody.Builder()
                .add("data", sendingData)
                .build();
        Request request = new Request.Builder()
                .url(serverURL+"verify")
                .header("auth-token", SessionUser.Instance().getToken())
                .post(formBody)
                .build();


        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                LogDetail.LogEStack(e);
                LogDetail.LogDE("Error",serverURL+"verify  -"+e.getMessage());
                responseListener.onError(call, e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                LogDetail.LogDE("API RESPONSE - ", response.toString());
                if (response.isSuccessful()) {
                    try {
                        assert response.body() != null;
                        String encryptedResponse = response.body().string();//new Gson().toJson(args[0]).replaceAll("('|\")", "");

                        JSONObject rb = new JSONObject(encryptedResponse);
                        LogDetail.LogDE("TAG_", rb.toString());
                        String[] data = rb.getString("encyptedData").split("\\.", 2);

                        if (data[1].equals(license)) {
                            LogDetail.LogD(TAG, " Equals LICENSE KEY ");
                            LogDetail.println(data[0]);
                            String initialIDecryptionString = instanceEncryption.decrypt(data[0].getBytes());
                            LogDetail.LogDE("TAG--dec", initialIDecryptionString);

                            try {
                                JSONObject jsonObject = new JSONObject(initialIDecryptionString);
                                //EVENTNAME - Can be used for VERSION CONTROL
                                String eventName = jsonObject.getString("eventName");

                                //TODO Manage Version Response
                                switch (eventName) {
                                    case "versionSuccess":
                                        LogDetail.LogDE("Version - ", "This is Latest");
                                        break;
                                    case "versionCanBeUpdated":
                                        LogDetail.LogDE("Version - ", "This version can be updated");
                                        break;
                                    case "versionHasToBeUpdated":
                                        LogDetail.LogDE("Version - ", "This version Has to be updated");
                                        break;
                                    case "versionSystemMaintenance":
                                        LogDetail.LogDE("Version - ", "Server is Under Maintenance");
                                        break;
                                    case "versionNoLongerSupported":
                                        LogDetail.LogDE("Version - ", "This Version is no Longer Supported");
                                        break;
                                    default:
                                        LogDetail.LogDE("Version - ", "This version is Not compatible");
                                        break;
                                }

                                String versionResponse = String.valueOf(jsonObject.getJSONObject("versionResponse"));
                                String jwtTokenDetails = String.valueOf(jsonObject.getJSONObject("jwtTokenDetails"));

                                //This Object can be used in case of EventNames other than versionSuccess
                                JSONObject versionObject = new JSONObject(versionResponse);

                                JSONObject jwtTokenDetailsObject = new JSONObject(jwtTokenDetails);
                                if(jsonObject.has("privateKey") && jsonObject.has("publicKey")){
                                    instanceEncryption.updateKEY_IV(jsonObject.getString("privateKey"));
                                    SessionUser.Instance().setPublicKey(jsonObject.getString("publicKey"));
                                    SessionUser.Instance().addPairToMap(jsonObject.getString("publicKey"), jsonObject.getString("privateKey"));
                                }
                                SessionUser.Instance().setToken(jwtTokenDetailsObject.getString("token"));
                                SessionUser.Instance().setTtl(jwtTokenDetailsObject.getString("ttl"));
                                authenticationSuccess.onAuthSuccess();
                                alreadyAuthenticated = true;
                            } catch (JSONException e) {
                                LogDetail.LogEStack(e);
                            }


                        }
                    } catch (Exception e) {
                        LogDetail.LogEStack(e);
                    }

                } else {
                    LogDetail.LogDE(TAG, "onResponse: " + call.request());
                }
            }

        });
    }


    private void checkAndRefreshToken(OnAPISuccess listener){
        long now = System.currentTimeMillis();
        if(now<SessionUser.Instance().getTtl()){
            listener.onSuccess();
        } else {
            JsonObject allDetails = new JsonObject();
            try {
                allDetails.add("userDetail", SessionUser.Instance().getUserDetails());
                allDetails.add("deviceDetail", SessionUser.Instance().getDeviceDetails());
            } catch (Exception e) {
                LogDetail.LogEStack(e);
            }

            String publicKey = SessionUser.Instance().getPublicKey();
            AESCBCPKCS5Encryption instanceEncryption = new AESCBCPKCS5Encryption().getInstance(
                    SessionUser.Instance().getPrivateKey(publicKey));
            String sendingData = instanceEncryption.encrypt(
                    allDetails.toString().getBytes(StandardCharsets.UTF_8)) + "." + publicKey;
            String[] data = sendingData.split("\\.", 2);
            OkHttpClient client = new OkHttpClient();
            RequestBody formBody = new FormBody.Builder()
                    .add("data", sendingData)
                    .build();
            Request request = new Request.Builder()
                    .url(serverURL+"refresh-token")
                    .header("auth-token", SessionUser.Instance().getToken())
                    .post(formBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    LogDetail.LogEStack(e);
                }
                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) {
                    LogDetail.LogDE("API RESPONSE - ", response.toString());
                    if (response.isSuccessful()) {
                        try {
                            assert response.body() != null;
                            String encryptedResponse = response.body().string();//new Gson().toJson(args[0]).replaceAll("('|\")", "");

                            JSONObject rb = new JSONObject(encryptedResponse);
                            String[] SplitData = rb.getString("data").split("\\.", 2);
                            String publickey = SplitData[1];
                            AESCBCPKCS5Encryption instanceEncryption = new AESCBCPKCS5Encryption().getInstance(SessionUser.Instance().getPrivateKey(publickey));
                            String DecryptedText = instanceEncryption.decrypt(SplitData[0].getBytes(StandardCharsets.UTF_8));
                            LogDetail.LogDE("Decrypted Response  => ", DecryptedText);
                            JSONObject RespJson = new JSONObject(DecryptedText);

                            JSONObject jwtTokenDetailsObject = new JSONObject(String.valueOf(RespJson.getJSONObject("jwtTokenDetails")));
                            SessionUser.Instance().setToken(jwtTokenDetailsObject.getString("token"));
                            SessionUser.Instance().setTtl(jwtTokenDetailsObject.getString("ttl"));
                            listener.onSuccess();
                            try{
                                JSONObject encryptionData = new JSONObject(String.valueOf(RespJson.getJSONObject("encryptionData")));
                                if(encryptionData.has("privateKey") && encryptionData.has("publicKey")){
                                    instanceEncryption.updateKEY_IV(encryptionData.getString("privateKey"));
                                    SessionUser.Instance().setPublicKey(encryptionData.getString("publicKey"));
                                    SessionUser.Instance().addPairToMap(encryptionData.getString("publicKey"), encryptionData.getString("privateKey"));
                                }
                            } catch (Exception ex){}
                        } catch (Exception e) {
                            LogDetail.LogDE("Error",e.getMessage());
                        }
                    } else {
                        LogDetail.LogDE(TAG, "onResponse: " + call.request());
                    }
                }
            });
        }
    }

    //API Calling Using Encrypted Data
    public void postData(String sendingData, com.appyhigh.newsfeedsdk.apicalls.ResponseListener responseListener) {
        checkAndRefreshToken(() -> {
            if(!checkUserId()) return;
            OkHttpClient client = new OkHttpClient();
            RequestBody formBody = new FormBody.Builder()
                    .add("data", sendingData)
                    .build();
            LogDetail.LogD("auth-token", SessionUser.Instance().getToken());
            Request request = new Request.Builder()
                    .url(serverURL+"grpcdata")
                    .header("auth-token", SessionUser.Instance().getToken())
                    .post(formBody)
                    .build();


            client.newCall(request).enqueue(new Callback() {

                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    LogDetail.LogEStack(e);
                    responseListener.onError(call, e);
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) {
                    LogDetail.LogD("API RESPONSE - ", response.toString());
                    if (response.isSuccessful()) {
                        try {
                            assert response.body() != null;
                            String resStr = response.body().string();
                            JSONObject rb = new JSONObject(resStr);
                            String[] SplitData = rb.getString("data").split("\\.", 2);
                            String publickey = SplitData[1];
                            AESCBCPKCS5Encryption instanceEncryption = new AESCBCPKCS5Encryption().getInstance(SessionUser.Instance().getPrivateKey(publickey));
                            String DecryptedText = instanceEncryption.decrypt(SplitData[0].getBytes(StandardCharsets.UTF_8));
                            LogDetail.LogDE("Decrypted Response  => ", DecryptedText);
                            JSONObject RespJson = new JSONObject(DecryptedText);
                            LogDetail.LogDE("Decrypted Response API URL  => ", RespJson.getString("apiURL"));
                            try{
                                JSONObject encryptionData = new JSONObject(String.valueOf(RespJson.getJSONObject("encryptionData")));
                                if(encryptionData.has("privateKey") && encryptionData.has("publicKey")){
                                    instanceEncryption.updateKEY_IV(encryptionData.getString("privateKey"));
                                    SessionUser.Instance().setPublicKey(encryptionData.getString("publicKey"));
                                    SessionUser.Instance().addPairToMap(encryptionData.getString("publicKey"), encryptionData.getString("privateKey"));
                                }
                            } catch (Exception ex){}
                            new Handler(Looper.getMainLooper()).post(() -> {
                                try {
                                    JSONObject responseJson = RespJson.getJSONObject("data");
                                    String dataJSON = responseJson.getString("response");
                                    try{
                                        JSONObject statusJson = new JSONObject(dataJSON);
                                        if(statusJson.has("status_code") && statusJson.getInt("status_code")>300){
                                            responseListener.onError(call, new IOException(RespJson.getString("apiURL")+" "+statusJson.getString("msg")));
                                        } else {
                                            responseListener.onSuccess(RespJson.getString("apiURL"), dataJSON, response.sentRequestAtMillis());
                                        }
                                    } catch (Exception ex) {
                                        responseListener.onSuccess(RespJson.getString("apiURL"), dataJSON, response.sentRequestAtMillis());
                                    }
                                } catch (JSONException e) {
                                    LogDetail.LogEStack(e);
                                }
                            });

                        } catch (Exception e) {
                            LogDetail.LogEStack(e);
                        }

                    } else {
                        LogDetail.LogD(TAG, "onResponse: " + call.request());
                    }
                }

            });
        });
    }

    private boolean checkUserId(){
        return FeedSdk.Companion.getAppId() != null &&
                !FeedSdk.Companion.getAppId().isEmpty() &&
                FeedSdk.Companion.getUserId() != null &&
                !FeedSdk.Companion.getUserId().isEmpty();
    }

    public interface AuthenticationSuccess {
        void onAuthSuccess();
    }
}